package com.hereliesaz.aznavrail.service

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.hereliesaz.aznavrail.model.AzDocEntry
import com.hereliesaz.aznavrail.model.AzMoreFromApp
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Fetches everything the About reader needs *before* the user asks for it.
 *
 * The About page is a network screen: a GitHub contents listing, a markdown body, and a manifest of
 * other apps. Fetching those when the user taps "About" means the first thing they get is a spinner,
 * for work that could have been done while they were doing something else. So the rail warms all
 * three the moment it composes, holds the parsed results for the process lifetime, and the reader
 * opens already populated.
 *
 * Results are snapshot state, so a reader opened mid-flight (cold start, slow network) still fills
 * in the instant the fetch lands — it never has to be told to look again.
 *
 * The bodies themselves are still served through the repositories' own disk cache, which is what
 * keeps this cheap: a warm start revalidates with an etag and usually gets a 304.
 */
object AzAboutPrefetch {

    /** The most recently warmed table of contents, and the repo it came from. */
    private var docsRepoUrl: String? = null
    var docs: GithubDocsRepository.DocsResult? by mutableStateOf(null)
        private set

    /** The most recently warmed "More from Az" manifest, and the URL it came from. */
    private var moreUrl: String? = null
    var moreApps: List<AzMoreFromApp>? by mutableStateOf(null)
        private set

    /** Doc bodies warmed ahead of a tap, keyed by [AzDocEntry.downloadUrl]. */
    private val bodies = mutableMapOf<String, String>()

    private val lock = Mutex()

    /** The warmed TOC for [repoUrl], or null when nothing has been fetched for that repo yet. */
    fun docsFor(repoUrl: String): GithubDocsRepository.DocsResult? =
        docs.takeIf { docsRepoUrl == repoUrl }

    /** The warmed manifest for [jsonUrl], or null when it hasn't been fetched yet. */
    fun moreAppsFor(jsonUrl: String): List<AzMoreFromApp>? =
        moreApps.takeIf { moreUrl == jsonUrl }

    /** The warmed markdown for [entry], or null when it wasn't among the ones warmed ahead. */
    fun bodyFor(entry: AzDocEntry): String? = bodies[entry.downloadUrl]

    /**
     * Fetches the repo's table of contents (and the body of its first document — almost always the
     * README, and almost always the one opened first).
     *
     * Idempotent per repo: a second call for a repo already warmed does nothing.
     */
    suspend fun warmDocs(context: Context, repoUrl: String) {
        if (repoUrl.isBlank()) return
        lock.withLock {
            if (docsRepoUrl == repoUrl && docs != null) return
        }
        val result = GithubDocsRepository.listDocs(context, repoUrl).getOrNull() ?: return
        lock.withLock {
            docsRepoUrl = repoUrl
            docs = result
        }
        // The first entry is the one the user is most likely to open, so it is worth the extra call.
        result.entries.firstOrNull()?.let { warmDoc(context, it) }
    }

    /** Fetches one document's markdown so opening it is instantaneous. */
    suspend fun warmDoc(context: Context, entry: AzDocEntry) {
        lock.withLock { if (bodies.containsKey(entry.downloadUrl)) return }
        val body = GithubDocsRepository.fetchDoc(context, entry).getOrNull() ?: return
        lock.withLock { bodies[entry.downloadUrl] = body }
    }

    /** Fetches the "More from Az" manifest backing the About page's carousel. */
    suspend fun warmMoreFromAz(context: Context, jsonUrl: String) {
        if (jsonUrl.isBlank()) return
        lock.withLock {
            if (moreUrl == jsonUrl && moreApps != null) return
        }
        val apps = MoreFromAzRepository.fetch(context, jsonUrl).getOrNull()?.apps ?: return
        lock.withLock {
            moreUrl = jsonUrl
            moreApps = apps
        }
    }

    /** Drops everything warmed. Test seam; nothing in the library calls it. */
    fun clear() {
        docsRepoUrl = null
        docs = null
        moreUrl = null
        moreApps = null
        bodies.clear()
    }
}

/**
 * Warms the About reader's content in the background, from wherever the About affordance lives.
 *
 * Costs one contents listing, one markdown body and one manifest per process — all of them cached,
 * conditional requests — and buys an About page that is already drawn when it opens.
 */
@Composable
internal fun AzAboutWarmup(
    repoUrl: String,
    moreFromAzEnabled: Boolean,
    moreFromAzJsonUrl: String,
) {
    val context = LocalContext.current.applicationContext
    LaunchedEffect(repoUrl) {
        runCatching { AzAboutPrefetch.warmDocs(context, repoUrl) }
    }
    LaunchedEffect(moreFromAzEnabled, moreFromAzJsonUrl) {
        if (moreFromAzEnabled) runCatching { AzAboutPrefetch.warmMoreFromAz(context, moreFromAzJsonUrl) }
    }
}
