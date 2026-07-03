package com.hereliesaz.aznavrail.service

import android.content.Context
import com.hereliesaz.aznavrail.model.AzMoreFromApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Reads the **baked** "More from Az" manifest produced by `.github/workflows/bump-more-from-az.yml`.
 *
 * All resolution (grouping by repo, constructing+verifying the Play link, reading the website/PWA from
 * the repo homepage, dropping WIP apps, sorting Play-first, filling name/icon/description) happens in
 * CI, so the rail does no network resolution here — it just parses ready-to-render [AzMoreFromApp]s.
 *
 * For resilience there's a lenient fallback: if the manifest hasn't been baked yet (the maintainer
 * pasted bare GitHub links and CI hasn't run), entries that are plain URL strings or `{github,…}`
 * link objects render as degraded cards (name from the repo path; no icon) until CI bakes it.
 */
object MoreFromAzRepository {

    /** Result of [fetch]: the manifest version plus the apps to render (already Play-first sorted). */
    data class MoreFromResult(val version: Int, val apps: List<AzMoreFromApp>)

    /** Fetches and parses the baked manifest from [jsonUrl] (cached via [AzHttpCache]).
     *
     *  After parsing, entries with a blank/avatar iconUrl trigger a repo-level icon walk against
     *  raw.githubusercontent.com (standard mipmap paths, adaptive-icon xml, opengraph fallback).
     *  Entries with an unknown bannerUrl trigger the same walk for `docs/banner.*`. Both walks are
     *  best-effort: any failure leaves the field as-is. */
    suspend fun fetch(context: Context, jsonUrl: String): Result<MoreFromResult> {
        val res = AzHttpCache.get(context, jsonUrl)
            ?: return Result.failure(IllegalStateException("Could not load $jsonUrl"))
        val parsed = runCatching { parse(res.body) }.getOrElse { return Result.failure(it) }
        val enriched = runCatching { enrichWithRepoAssets(parsed.apps) }.getOrDefault(parsed.apps)
        return Result.success(parsed.copy(apps = enriched))
    }

    /** Fills in [AzMoreFromApp.iconUrl] and [AzMoreFromApp.bannerUrl] from the app's GitHub repo when
     *  the manifest didn't already supply them. Runs the per-app walks in parallel. */
    internal suspend fun enrichWithRepoAssets(apps: List<AzMoreFromApp>): List<AzMoreFromApp> =
        coroutineScope {
            apps.map { app ->
                async(Dispatchers.IO) {
                    val gh = app.githubUrl ?: return@async app
                    val (owner, repo) = GithubDocsRepository.parseRepo(gh) ?: return@async app
                    val needsIcon = app.iconUrl.isBlank() ||
                        app.iconUrl.contains("avatars.githubusercontent.com")
                    val needsBanner = app.bannerUrl.isNullOrBlank()
                    if (!needsIcon && !needsBanner) return@async app
                    val newIcon = if (needsIcon) resolveRepoIcon(owner, repo) else app.iconUrl
                    val newBanner = if (needsBanner) resolveRepoBanner(owner, repo) else app.bannerUrl
                    app.copy(iconUrl = newIcon.ifBlank { app.iconUrl }, bannerUrl = newBanner)
                }
            }.awaitAll()
        }

    /** Standard Android launcher icon paths, densities highest → lowest, webp before png. */
    private val launcherIconPaths = listOf(
        "app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp",
        "app/src/main/res/mipmap-xxxhdpi/ic_launcher.png",
        "app/src/main/res/mipmap-xxhdpi/ic_launcher.webp",
        "app/src/main/res/mipmap-xxhdpi/ic_launcher.png",
        "app/src/main/res/mipmap-xhdpi/ic_launcher.webp",
        "app/src/main/res/mipmap-xhdpi/ic_launcher.png",
        "app/src/main/res/mipmap-hdpi/ic_launcher.png",
    )

    private val bannerNames = listOf(
        "docs/banner.png", "docs/banner.webp", "docs/banner.jpg", "docs/banner.jpeg",
        "docs/Banner.png", "docs/Banner.webp",
        "docs/hero.png", "docs/hero.webp",
    )

    /** HEAD-checks common Android launcher-icon paths on the default branch's raw content. On miss,
     *  falls back to the repo's OpenGraph social preview (always present, but is the repo card, not
     *  necessarily the app icon). Returns "" only on total failure. */
    internal suspend fun resolveRepoIcon(owner: String, repo: String): String {
        // Try both "main" and "HEAD" (HEAD is resolved by GitHub to the default branch).
        for (branch in listOf("main", "master", "HEAD")) {
            for (path in launcherIconPaths) {
                val url = "https://raw.githubusercontent.com/$owner/$repo/$branch/$path"
                if (headOk(url)) return url
            }
        }
        return "https://opengraph.githubassets.com/1/$owner/$repo"
    }

    internal suspend fun resolveRepoBanner(owner: String, repo: String): String? {
        for (branch in listOf("main", "master", "HEAD")) {
            for (name in bannerNames) {
                val url = "https://raw.githubusercontent.com/$owner/$repo/$branch/$name"
                if (headOk(url)) return url
            }
        }
        return null
    }

    private suspend fun headOk(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = true
            conn.requestMethod = "HEAD"
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            val code = conn.responseCode
            conn.disconnect()
            code in 200..299
        } catch (_: Exception) {
            false
        }
    }

    /** Parses the manifest JSON into a Play-first list of apps; tolerant of un-baked entries. */
    internal fun parse(json: String): MoreFromResult {
        val obj = JSONObject(json)
        val version = obj.optInt("version", 0)
        val arr = obj.optJSONArray("apps") ?: JSONArray()
        val apps = buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i)
                if (o != null) {
                    appFromObject(o)?.let { add(it) }
                } else {
                    arr.optString(i).takeIf { it.isNotBlank() }?.let { degradedFromUrl(it)?.let(::add) }
                }
            }
        }
        return MoreFromResult(version, sortPlayFirst(apps))
    }

    /** Stable sort placing apps with a Play link first. */
    internal fun sortPlayFirst(apps: List<AzMoreFromApp>): List<AzMoreFromApp> =
        apps.sortedBy { if (it.playStoreUrl != null) 0 else 1 }

    private fun JSONObject.optStringOrNull(key: String): String? =
        optString(key).takeIf { it.isNotBlank() }

    private fun appFromObject(o: JSONObject): AzMoreFromApp? {
        val name = o.optStringOrNull("name")
        if (name != null) {
            return AzMoreFromApp(
                name = name,
                iconUrl = o.optString("iconUrl"),
                description = o.optString("description"),
                githubUrl = o.optStringOrNull("github"),
                playStoreUrl = o.optStringOrNull("play"),
                webUrl = o.optStringOrNull("web"),
                isPwa = o.optBoolean("isPwa", false),
                bannerUrl = o.optStringOrNull("bannerUrl"),
            )
        }
        // Un-baked link object { github?, play?, web? } -> degraded card.
        val github = o.optStringOrNull("github")
        val play = o.optStringOrNull("play")
        val web = o.optStringOrNull("web")
        val anchor = github ?: play ?: web ?: return null
        return AzMoreFromApp(
            name = displayNameFor(anchor),
            iconUrl = "",
            description = "",
            githubUrl = github,
            playStoreUrl = play,
            webUrl = web,
        )
    }

    private fun degradedFromUrl(url: String): AzMoreFromApp? {
        val name = displayNameFor(url)
        return when {
            url.contains("github.com") -> AzMoreFromApp(name, "", "", githubUrl = url)
            url.contains("play.google.com") -> AzMoreFromApp(name, "", "", playStoreUrl = url)
            else -> AzMoreFromApp(name, "", "", webUrl = url)
        }
    }

    /** Best-effort display name from a URL (the GitHub repo name, else the last path segment / host). */
    private fun displayNameFor(url: String): String {
        GithubDocsRepository.parseRepo(url)?.let { return it.second }
        val cleaned = url.substringBefore('?').trimEnd('/')
        val last = cleaned.substringAfterLast('/')
        return last.takeIf { it.isNotBlank() && !it.contains('.') }
            ?: cleaned.removePrefix("https://").removePrefix("http://").substringBefore('/')
    }
}
