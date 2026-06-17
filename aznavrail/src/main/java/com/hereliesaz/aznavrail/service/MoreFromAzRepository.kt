package com.hereliesaz.aznavrail.service

import android.content.Context
import com.hereliesaz.aznavrail.model.AzMoreFromApp
import org.json.JSONArray
import org.json.JSONObject

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

    /** Fetches and parses the baked manifest from [jsonUrl] (cached via [AzHttpCache]). */
    suspend fun fetch(context: Context, jsonUrl: String): Result<MoreFromResult> {
        val res = AzHttpCache.get(context, jsonUrl)
            ?: return Result.failure(IllegalStateException("Could not load $jsonUrl"))
        return runCatching { parse(res.body) }
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
