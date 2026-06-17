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

    /** Parses the manifest into its `version` and the raw link list. */
    internal fun parseLinks(json: String): Pair<Int, List<AzMoreFromLink>> {
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

    /** Resolves one link entry into an app (richest metadata first: Play, then web/PWA, then GitHub). */
    internal suspend fun resolve(context: Context, link: AzMoreFromLink): AzMoreFromApp? =
        (if (link.play != null) resolvePlay(context, link) else null)
            ?: (if (link.web != null) resolveWeb(context, link) else null)
            ?: resolveGithub(context, link)

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
