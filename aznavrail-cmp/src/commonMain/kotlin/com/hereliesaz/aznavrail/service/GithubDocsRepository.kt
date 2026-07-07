package com.hereliesaz.aznavrail.service

import com.hereliesaz.aznavrail.model.AzDocEntry
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Discovers and fetches the markdown documentation of a GitHub repository for the in-app About
 * reader. Discovery is automatic: given the repo URL it lists the `.md` files in the repo root and
 * the `docs/` folder via the GitHub contents API, then fetches each doc's raw markdown lazily.
 *
 * All results are served through [AzHttpCache], so repeated opens are cheap and the screen keeps
 * working offline / when rate-limited (showing the last cached copy).
 *
 * ## Port note vs the Android sibling
 * The Android version used `HttpURLConnection` + `org.json` + a `Context.cacheDir` cache. This CMP
 * copy keeps the exact same discovery logic and public API (minus the `Context` params — CMP
 * callers don't have one), but does HTTP through the Ktor-backed [AzHttpCache] and parses the
 * GitHub contents JSON with the `kotlinx.serialization` [JsonElement] runtime API (a faithful mirror
 * of the Android `optJSONObject`/`optString` field access — no `@Serializable` DTOs, so the
 * serialization compiler plugin isn't needed).
 */
object GithubDocsRepository {

    private val REPO_REGEX = Regex("""github\.com[/:]([^/]+)/([^/?#]+)""")

    /** Build suffixes an `applicationIdSuffix` commonly appends; stripped before deriving the repo. */
    private val BUILD_SUFFIXES = listOf("debug", "dev", "staging", "release", "beta", "alpha")

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** Reads a string field from a [JsonObject], or null when absent / not a string primitive. */
    private fun JsonObject.str(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

    /**
     * Derives the host app's GitHub repository URL from its package namespace, following the
     * convention that the namespace reveals the repo: `com.<owner>.<repo>` →
     * `https://github.com/<owner>/<repo>`. Returns null when the package has fewer than three
     * segments to derive from.
     */
    fun repoUrlFromPackage(packageName: String): String? {
        val segments = packageName.split('.').filter { it.isNotBlank() }
        if (segments.size < 3) return null
        val owner = segments[1]
        val last = segments.last()
        val repo = if (last.lowercase() in BUILD_SUFFIXES && segments.size >= 4) segments[segments.size - 2] else last
        if (owner.isBlank() || repo.isBlank()) return null
        return "https://github.com/$owner/$repo"
    }

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
            .joinToString(" ") { w ->
                if (w.equals("README", ignoreCase = true)) "README"
                else w.lowercase().replaceFirstChar { it.uppercase() }
            }

    /** Parses a GitHub contents-API JSON array into the `.md` [AzDocEntry] list it contains. */
    internal fun parseContents(contentsJson: String): List<AzDocEntry> {
        val out = mutableListOf<AzDocEntry>()
        val arr = json.parseToJsonElement(contentsJson) as? JsonArray ?: return out
        for (el in arr) {
            val o = el as? JsonObject ?: continue
            if (o.str("type") != "file") continue
            val name = o.str("name") ?: continue
            if (!name.endsWith(".md", ignoreCase = true)) continue
            val download = o.str("download_url")
            if (download.isNullOrBlank()) continue
            out.add(AzDocEntry(title = humanize(name), path = o.str("path") ?: name, downloadUrl = download))
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
    suspend fun listDocs(repoUrl: String): Result<DocsResult> {
        val (owner, repo) = parseRepo(repoUrl)
            ?: return Result.failure(IllegalArgumentException("Not a GitHub repo URL: $repoUrl"))

        val rootUrl = "https://api.github.com/repos/$owner/$repo/contents/"
        val docsUrl = "https://api.github.com/repos/$owner/$repo/contents/docs"

        val rootRes = AzHttpCache.get(rootUrl)
        val docsRes = AzHttpCache.get(docsUrl) // null => no docs/ folder

        if (rootRes == null && docsRes == null) {
            return Result.failure(IllegalStateException("Could not reach $repoUrl"))
        }

        val root = rootRes?.body?.let { runCatching { parseContents(it) }.getOrDefault(emptyList()) } ?: emptyList()
        val docs = docsRes?.body?.let { runCatching { parseContents(it) }.getOrDefault(emptyList()) } ?: emptyList()
        val limited = (rootRes?.rateLimited == true) || (docsRes?.rateLimited == true)

        // Honor a repo-root `.azignore` (or `.aiexclude`): docs it lists are excluded from the About
        // TOC. Fetch it via the contents listing's resolved `download_url`.
        val ignoreUrl = rootRes?.body?.let {
            runCatching { findDownloadUrl(it, ".azignore") ?: findDownloadUrl(it, ".aiexclude") }.getOrNull()
        }
        val patterns = ignoreUrl?.let { AzHttpCache.get(it)?.body?.let(::parseIgnore) } ?: emptyList()
        val toc = orderToc(root, docs).filterNot { isIgnored(it.path, patterns) }
        return Result.success(DocsResult(toc, limited))
    }

    /** Returns the `download_url` of a root file named [fileName] in a contents-API JSON array, or null. */
    internal fun findDownloadUrl(contentsJson: String, fileName: String): String? {
        val arr = json.parseToJsonElement(contentsJson) as? JsonArray ?: return null
        for (el in arr) {
            val o = el as? JsonObject ?: continue
            if (o.str("type") != "file" || o.str("name") != fileName) continue
            return o.str("download_url")?.takeIf { it.isNotBlank() }
        }
        return null
    }

    /** Parses a `.azignore` file into its non-comment, non-blank patterns. */
    internal fun parseIgnore(text: String): List<String> =
        text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .map { it.removePrefix("./") }
            .toList()

    /** True if [path] (a repo-relative doc path) matches any `.azignore` [patterns]. */
    internal fun isIgnored(path: String, patterns: List<String>): Boolean {
        if (patterns.isEmpty()) return false
        val fileName = path.substringAfterLast('/')
        return patterns.any { pat ->
            when {
                pat.endsWith("/") -> path == pat.dropLast(1) || path.startsWith(pat)
                else -> {
                    val regex = "^" + pat.split("*").joinToString(".*") { Regex.escape(it) } + "$"
                    Regex(regex).matches(path) || Regex(regex).matches(fileName)
                }
            }
        }
    }

    /** Fetches the raw markdown for a single TOC [entry] (cached). */
    suspend fun fetchDoc(entry: AzDocEntry): Result<String> {
        val res = AzHttpCache.get(entry.downloadUrl)
            ?: return Result.failure(IllegalStateException("Could not load ${entry.path}"))
        return Result.success(res.body)
    }
}
