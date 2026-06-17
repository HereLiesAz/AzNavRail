package com.hereliesaz.aznavrail.service

import android.content.Context
import com.hereliesaz.aznavrail.model.AzMoreFromApp
import com.hereliesaz.aznavrail.model.AzMoreFromLink
import org.json.JSONObject

/**
 * Backs the "More from Az" carousel from a **link-only** manifest. The author maintains
 * `more-from-az.json` as nothing more than a `version` and a list of `{ github?, play? }` links;
 * this repository resolves each link into a full [AzMoreFromApp] by fetching metadata at runtime:
 *  - **Play** link → the listing page's OpenGraph tags (`og:title`, `og:image`, `og:description`).
 *  - **GitHub** link → the repository API (`name`, `description`, `owner.avatar_url`).
 *
 * Play metadata is preferred when both links are present (it describes the actual app). Everything is
 * served through [AzHttpCache], so resolution is cheap on repeat opens and degrades to cache offline.
 */
object MoreFromAzRepository {

    /**
     * Parses the manifest into its `version` and the raw link list. Tries strict JSON first; if that
     * fails (e.g. a maintainer pasted bare URLs and CI hasn't normalized the file yet), falls back to
     * a lenient scan that extracts URLs from `{ ... }` blocks (or, failing that, per line) so the
     * carousel still works.
     */
    internal fun parseLinks(json: String): Pair<Int, List<AzMoreFromLink>> = try {
        val obj = JSONObject(json)
        val version = obj.optInt("version", 0)
        val arr = obj.optJSONArray("apps")
        val links = buildList {
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val a = arr.optJSONObject(i) ?: continue
                    val github = a.optString("github").takeIf { it.isNotBlank() }
                    val play = a.optString("play").takeIf { it.isNotBlank() }
                    val web = a.optString("web").takeIf { it.isNotBlank() }
                    if (github != null || play != null || web != null) add(AzMoreFromLink(github, play, web))
                }
            }
        }
        version to links
    } catch (e: Exception) {
        parseLinksLoose(json)
    }

    private val URL_RE = Regex("""https?://[^\s"'<>,}\]]+""")

    private fun classifyUrls(urls: List<String>): AzMoreFromLink? {
        var gh: String? = null; var play: String? = null; var web: String? = null
        for (raw in urls) {
            val u = raw.trimEnd('.', ',', ';')
            when {
                u.contains("github.com") -> if (gh == null) gh = u
                u.contains("play.google.com") -> if (play == null) play = u
                else -> if (web == null) web = u
            }
        }
        return if (gh != null || play != null || web != null) AzMoreFromLink(gh, play, web) else null
    }

    /** Lenient fallback: pull URLs out of a not-quite-JSON manifest, grouping by `{}` block or line. */
    internal fun parseLinksLoose(raw: String): Pair<Int, List<AzMoreFromLink>> {
        val version = Regex("""\"version\"\s*:\s*(\d+)""").find(raw)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val links = mutableListOf<AzMoreFromLink>()
        Regex("""\{[^{}]*\}""").findAll(raw).forEach { block ->
            classifyUrls(URL_RE.findAll(block.value).map { it.value }.toList())?.let { links.add(it) }
        }
        if (links.isEmpty()) {
            raw.lineSequence().forEach { line ->
                val urls = URL_RE.findAll(line).map { it.value }.toList()
                if (urls.isNotEmpty()) classifyUrls(urls)?.let { links.add(it) }
            }
        }
        return version to links
    }

    /** Extracts an OpenGraph `content` value for [property] from raw HTML, tolerant of attribute order. */
    internal fun extractOg(html: String, property: String): String? {
        val a = Regex("""<meta[^>]+property=["']og:$property["'][^>]+content=["']([^"']*)["']""", RegexOption.IGNORE_CASE)
        val b = Regex("""<meta[^>]+content=["']([^"']*)["'][^>]+property=["']og:$property["']""", RegexOption.IGNORE_CASE)
        return (a.find(html)?.groupValues?.get(1) ?: b.find(html)?.groupValues?.get(1))
            ?.let { decodeEntities(it).trim() }
            ?.takeIf { it.isNotBlank() }
    }

    /** Decodes the handful of HTML entities that appear in OpenGraph text (pure-JVM, no android.text). */
    private fun decodeEntities(s: String): String = s
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")

    private suspend fun resolvePlay(context: Context, link: AzMoreFromLink): AzMoreFromApp? {
        val url = link.play ?: return null
        val html = AzHttpCache.get(context, url)?.body ?: return null
        val title = extractOg(html, "title")?.removeSuffix(" - Apps on Google Play")?.trim() ?: return null
        return AzMoreFromApp(
            name = title,
            iconUrl = extractOg(html, "image") ?: "",
            description = extractOg(html, "description") ?: "",
            githubUrl = link.github,
            playStoreUrl = url,
            webUrl = link.web,
        )
    }

    private suspend fun resolveWeb(context: Context, link: AzMoreFromLink): AzMoreFromApp? {
        val url = link.web ?: return null
        val html = AzHttpCache.get(context, url)?.body ?: return null
        val title = extractOg(html, "title")?.trim() ?: return null
        return AzMoreFromApp(
            name = title,
            iconUrl = extractOg(html, "image") ?: "",
            description = extractOg(html, "description") ?: "",
            githubUrl = link.github,
            playStoreUrl = link.play,
            webUrl = url,
        )
    }

    private suspend fun resolveGithub(context: Context, link: AzMoreFromLink): AzMoreFromApp? {
        val url = link.github ?: return null
        val (owner, repo) = GithubDocsRepository.parseRepo(url) ?: return null
        val json = AzHttpCache.get(context, "https://api.github.com/repos/$owner/$repo")?.body ?: return null
        return runCatching {
            val o = JSONObject(json)
            AzMoreFromApp(
                name = o.optString("name", repo),
                iconUrl = o.optJSONObject("owner")?.optString("avatar_url") ?: "",
                description = o.optString("description", ""),
                githubUrl = url,
                playStoreUrl = link.play,
                webUrl = link.web,
            )
        }.getOrNull()
    }

    /**
     * Derives the conventional Play Store URL for a GitHub repo (`com.<owner>.<repo>`, lower-cased),
     * e.g. `HereLiesAz/CueDetat` -> `…?id=com.hereliesaz.cuedetat`. The candidate is only used if it
     * actually resolves to a real listing (see [resolve]), so a wrong guess for an unpublished app is
     * silently dropped.
     */
    internal fun derivePlayUrl(githubUrl: String): String? {
        val (owner, repo) = GithubDocsRepository.parseRepo(githubUrl) ?: return null
        val pkg = "com.${owner.lowercase()}.${repo.lowercase()}"
        return "https://play.google.com/store/apps/details?id=$pkg"
    }

    /**
     * Resolves one link entry into an app. Order (richest metadata first):
     *  1. explicit Play link, 2. **derived** Play link from the GitHub repo (verified by fetch),
     *  3. website/PWA link, 4. GitHub repo API.
     * Resolving via Play also avoids a GitHub API call (saving the rate-limit budget) for published apps.
     */
    internal suspend fun resolve(context: Context, link: AzMoreFromLink): AzMoreFromApp? {
        if (link.play != null) resolvePlay(context, link)?.let { return it }
        if (link.play == null && link.github != null) {
            derivePlayUrl(link.github)?.let { derived ->
                resolvePlay(context, link.copy(play = derived))?.let { return it }
            }
        }
        if (link.web != null) resolveWeb(context, link)?.let { return it }
        return resolveGithub(context, link)
    }

    /** Result of [fetch]: the manifest version plus the resolved apps (failed entries are dropped). */
    data class MoreFromResult(val version: Int, val apps: List<AzMoreFromApp>)

    /** Fetches the manifest from [jsonUrl] and resolves every link into a displayable app. */
    suspend fun fetch(context: Context, jsonUrl: String): Result<MoreFromResult> {
        val res = AzHttpCache.get(context, jsonUrl)
            ?: return Result.failure(IllegalStateException("Could not load $jsonUrl"))
        return runCatching {
            val (version, links) = parseLinks(res.body)
            val apps = links.mapNotNull { resolve(context, it) }
            MoreFromResult(version, apps)
        }
    }
}
