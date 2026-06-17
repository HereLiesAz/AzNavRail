package com.hereliesaz.aznavrail.service

import android.content.Context
import com.hereliesaz.aznavrail.model.AzDocEntry
import org.json.JSONArray

/**
 * Discovers and fetches the markdown documentation of a GitHub repository for the in-app About
 * reader. Discovery is automatic: given the repo URL it lists the `.md` files in the repo root and
 * the `docs/` folder via the GitHub contents API, then fetches each doc's raw markdown lazily.
 *
 * All results are served through [AzHttpCache], so repeated opens are cheap and the screen keeps
 * working offline / when rate-limited (showing the last cached copy).
 */
object GithubDocsRepository {

    private val REPO_REGEX = Regex("""github\.com[/:]([^/]+)/([^/?#]+)""")

    /** Extracts `owner` / `repo` from a GitHub URL, stripping a trailing `.git`. Null if not GitHub. */
    fun parseRepo(repoUrl: String): Pair<String, String>? {
        val m = REPO_REGEX.find(repoUrl) ?: return null
        val owner = m.groupValues[1]
        val repo = m.groupValues[2].removeSuffix(".git")
        if (owner.isBlank() || repo.isBlank()) return null
        return owner to repo
    }

    /** Turns a filename like `MIGRATION_GUIDE.md` into a human title like "Migration Guide". */
    internal fun humanize(fileName: String): String =
        fileName.removeSuffix(".md").removeSuffix(".MD")
            .replace('-', ' ').replace('_', ' ')
            .split(' ').filter { it.isNotBlank() }
            .joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }

    /** Parses a GitHub contents-API JSON array into the `.md` [AzDocEntry] list it contains. */
    internal fun parseContents(json: String): List<AzDocEntry> {
        val out = mutableListOf<AzDocEntry>()
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            if (o.optString("type") != "file") continue
            val name = o.optString("name")
            if (!name.endsWith(".md", ignoreCase = true)) continue
            val download = o.optString("download_url")
            if (download.isBlank()) continue
            out.add(AzDocEntry(title = humanize(name), path = o.optString("path", name), downloadUrl = download))
        }
        return out
    }

    /**
     * Orders the table of contents: README first, then remaining root docs, then `docs/` entries —
     * each group alphabetised by title.
     */
    internal fun orderToc(root: List<AzDocEntry>, docs: List<AzDocEntry>): List<AzDocEntry> {
        val readme = root.filter { it.path.substringAfterLast('/').startsWith("README", ignoreCase = true) }
        val otherRoot = (root - readme.toSet()).sortedBy { it.title.lowercase() }
        val docsSorted = docs.sortedBy { it.title.lowercase() }
        return readme + otherRoot + docsSorted
    }

    /** Result of [listDocs]: the TOC plus a flag noting the network was unavailable/limited. */
    data class DocsResult(val entries: List<AzDocEntry>, val rateLimitedOrOffline: Boolean)

    /**
     * Lists the repo's root + `docs/` markdown files. Returns an empty TOC (not an error) when the
     * repo has no markdown. Returns failure only when the repo URL isn't a GitHub URL or nothing
     * could be fetched at all.
     */
    suspend fun listDocs(context: Context, repoUrl: String): Result<DocsResult> {
        val (owner, repo) = parseRepo(repoUrl)
            ?: return Result.failure(IllegalArgumentException("Not a GitHub repo URL: $repoUrl"))

        val rootUrl = "https://api.github.com/repos/$owner/$repo/contents/"
        val docsUrl = "https://api.github.com/repos/$owner/$repo/contents/docs"

        val rootRes = AzHttpCache.get(context, rootUrl)
        val docsRes = AzHttpCache.get(context, docsUrl) // null => no docs/ folder

        if (rootRes == null && docsRes == null) {
            return Result.failure(IllegalStateException("Could not reach $repoUrl"))
        }

        val root = rootRes?.body?.let { runCatching { parseContents(it) }.getOrDefault(emptyList()) } ?: emptyList()
        val docs = docsRes?.body?.let { runCatching { parseContents(it) }.getOrDefault(emptyList()) } ?: emptyList()
        val limited = (rootRes?.rateLimited == true) || (docsRes?.rateLimited == true)
        return Result.success(DocsResult(orderToc(root, docs), limited))
    }

    /** Fetches the raw markdown for a single TOC [entry] (cached). */
    suspend fun fetchDoc(context: Context, entry: AzDocEntry): Result<String> {
        val res = AzHttpCache.get(context, entry.downloadUrl)
            ?: return Result.failure(IllegalStateException("Could not load ${entry.path}"))
        return Result.success(res.body)
    }
}
