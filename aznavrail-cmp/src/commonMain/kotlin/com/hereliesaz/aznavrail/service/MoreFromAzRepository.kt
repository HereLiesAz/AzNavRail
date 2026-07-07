package com.hereliesaz.aznavrail.service

import com.hereliesaz.aznavrail.model.AzMoreFromApp
import io.ktor.client.request.head
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

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
 *
 * ## Port note vs the Android sibling
 * The Android version used `HttpURLConnection` + `org.json`. This CMP copy keeps the identical
 * parsing + repo-asset-enrichment logic, but fetches the manifest through the Ktor-backed
 * [AzHttpCache], does the icon/banner HEAD probes with a Ktor [HttpClient], and parses the
 * heterogeneous `apps` array with the `kotlinx.serialization` [JsonElement] runtime API (the array
 * mixes full-app objects, bare link objects, and URL strings — a union `org.json`/`optJSONObject`
 * modelled by hand, mirrored here). The `Context` param is dropped, and `MoreFromResult` carries no
 * `version` field (CMP consumers never read it).
 */
object MoreFromAzRepository {

    /** Result of [fetch]: the app list plus a flag noting the network was unavailable/limited. */
    data class MoreFromResult(val apps: List<AzMoreFromApp>, val rateLimitedOrOffline: Boolean = false)

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // Shared HEAD-probe client; engine chosen per platform (see AzHttpClient.kt).
    private val client get() = azSharedHttpClient

    private fun JsonObject.str(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
    private fun JsonObject.strOrNull(key: String): String? = str(key)?.takeIf { it.isNotBlank() }
    private fun JsonObject.boolean(key: String, default: Boolean): Boolean =
        (this[key] as? JsonPrimitive)?.booleanOrNull ?: default

    /**
     * Fetches and parses the baked manifest from [jsonUrl] (cached via [AzHttpCache]).
     *
     * After parsing, entries with a blank/avatar iconUrl trigger a repo-level icon walk against
     * raw.githubusercontent.com (standard mipmap paths, opengraph fallback). Entries with an unknown
     * bannerUrl trigger the same walk for `docs/banner.*`. Both walks are best-effort: any failure
     * leaves the field as-is.
     */
    suspend fun fetch(jsonUrl: String): Result<MoreFromResult> {
        val res = AzHttpCache.get(jsonUrl)
            ?: return Result.failure(IllegalStateException("Could not load $jsonUrl"))
        val apps = runCatching { parse(res.body) }.getOrElse { return Result.failure(it) }
        val enriched = runCatching { enrichWithRepoAssets(apps) }.getOrDefault(apps)
        return Result.success(MoreFromResult(enriched, rateLimitedOrOffline = res.rateLimited))
    }

    /** Fills in [AzMoreFromApp.iconUrl] and [AzMoreFromApp.bannerUrl] from the app's GitHub repo when
     *  the manifest didn't already supply them. Runs the per-app walks in parallel. */
    internal suspend fun enrichWithRepoAssets(apps: List<AzMoreFromApp>): List<AzMoreFromApp> =
        coroutineScope {
            apps.map { app ->
                // No explicit dispatcher — Ktor handles its own IO, and Dispatchers.IO isn't
                // available in commonMain (absent on wasmJs).
                async {
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
     *  falls back to the repo's OpenGraph social preview. Returns "" only on total failure. */
    internal suspend fun resolveRepoIcon(owner: String, repo: String): String {
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

    private suspend fun headOk(url: String): Boolean = try {
        val response: HttpResponse = client.head(url)
        response.status.value in 200..299
    } catch (_: Exception) {
        false
    }

    /** Parses the manifest JSON into a Play-first list of apps; tolerant of un-baked entries. */
    internal fun parse(jsonStr: String): List<AzMoreFromApp> {
        val obj = json.parseToJsonElement(jsonStr) as? JsonObject ?: return emptyList()
        val arr = obj["apps"] as? JsonArray ?: return emptyList()
        val apps = buildList {
            for (el in arr) {
                when (el) {
                    is JsonObject -> appFromObject(el)?.let { add(it) }
                    is JsonPrimitive -> el.contentOrNull
                        ?.takeIf { it.isNotBlank() }
                        ?.let { degradedFromUrl(it)?.let(::add) }
                    else -> { /* skip nested arrays / nulls */ }
                }
            }
        }
        return sortPlayFirst(apps)
    }

    /** Stable sort placing apps with a Play link first. */
    internal fun sortPlayFirst(apps: List<AzMoreFromApp>): List<AzMoreFromApp> =
        apps.sortedBy { if (it.playStoreUrl != null) 0 else 1 }

    private fun appFromObject(o: JsonObject): AzMoreFromApp? {
        val name = o.strOrNull("name")
        if (name != null) {
            return AzMoreFromApp(
                name = name,
                iconUrl = o.str("iconUrl") ?: "",
                description = o.str("description") ?: "",
                githubUrl = o.strOrNull("github"),
                playStoreUrl = o.strOrNull("play"),
                webUrl = o.strOrNull("web"),
                isPwa = o.boolean("isPwa", false),
                bannerUrl = o.strOrNull("bannerUrl"),
            )
        }
        // Un-baked link object { github?, play?, web? } -> degraded card.
        val github = o.strOrNull("github")
        val play = o.strOrNull("play")
        val web = o.strOrNull("web")
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
