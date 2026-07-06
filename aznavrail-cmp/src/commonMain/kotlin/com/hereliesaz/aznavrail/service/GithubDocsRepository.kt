package com.hereliesaz.aznavrail.service

import com.hereliesaz.aznavrail.model.AzDocEntry

/**
 * Discovers and fetches the markdown documentation of a GitHub repository for the in-app About
 * reader.
 *
 * ## Port note
 * The Android sibling uses `java.net.HttpURLConnection` + `org.json` + `Context.cacheDir` for
 * ETag/file caching. Neither the network client nor the file cache is portable to CMP as-is.
 * This commonMain copy keeps:
 *
 *  - the two portable pure helpers ([repoUrlFromPackage] and [parseRepo] — string mangling only),
 *  - the [DocsResult] data class,
 *
 * and returns "empty / offline" from the network-requiring methods. A follow-up PR can wire
 * a real Ktor client + kotlinx-serialization + multiplatform-settings cache without changing
 * these signatures (they intentionally already drop the `Context` parameter the Android sibling
 * requires — CMP callers just don't have one to pass).
 */
object GithubDocsRepository {

    private val REPO_REGEX = Regex("""github\.com[/:]([^/]+)/([^/?#]+)""")

    /** Build suffixes an `applicationIdSuffix` commonly appends; stripped before deriving the repo. */
    private val BUILD_SUFFIXES = listOf("debug", "dev", "staging", "release", "beta", "alpha")

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

    /** Result of [listDocs]: the TOC plus a flag noting the network was unavailable/limited. */
    data class DocsResult(val entries: List<AzDocEntry>, val rateLimitedOrOffline: Boolean)

    /**
     * Stubbed on CMP — always returns an empty result. See the class KDoc for the follow-up plan.
     */
    suspend fun listDocs(repoUrl: String): Result<DocsResult> =
        Result.success(DocsResult(emptyList(), rateLimitedOrOffline = true))

    /**
     * Stubbed on CMP — always returns a placeholder document body. See the class KDoc.
     */
    suspend fun fetchDoc(entry: AzDocEntry): Result<String> =
        Result.success("_Documentation isn't available on this platform in the current CMP port._")
}
