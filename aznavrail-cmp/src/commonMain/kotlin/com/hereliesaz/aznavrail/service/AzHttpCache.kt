package com.hereliesaz.aznavrail.service

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/**
 * Tiny GET-with-cache helper shared by the About reader and the "More from Az" carousel — the CMP
 * port of the Android sibling's `AzHttpCache`.
 *
 * Same three rate-limit mitigations as the Android version:
 *  - **TTL short-circuit**: within [DEFAULT_TTL_MILLIS] of the last successful fetch the cached body
 *    is returned with no network call.
 *  - **Conditional requests**: an `ETag` is stored per URL and resent as `If-None-Match`; a `304`
 *    serves the cached body (and doesn't consume GitHub's primary quota).
 *  - **Graceful fallback**: on any network/HTTP error (incl. `403`/`429`) the last cached body is
 *    returned when available.
 *
 * Port differences vs the Android sibling:
 *  - HTTP is done with a Ktor [HttpClient] (no-arg — the engine is whichever `ktor-client-*`
 *    artifact is on the target's classpath: `android` / `cio` / `js`).
 *  - The cache is **in-memory only** (a `Mutex`-guarded map keyed by URL), not disk-backed —
 *    there's no `Context.cacheDir` off Android. This mirrors the `HistoryStore` precedent; a
 *    follow-up can add persistence (multiplatform-settings / Okio) behind this same [get] surface.
 *  - TTL uses a monotonic [TimeSource] mark instead of `System.currentTimeMillis()` (multiplatform,
 *    and correct for elapsed-time comparisons).
 */
internal object AzHttpCache {

    const val DEFAULT_TTL_MILLIS: Long = 6 * 60 * 60 * 1000 // 6h

    /** Result of a cached fetch. [fromCache] is true when no fresh body was downloaded this call. */
    data class Result(val body: String, val fromCache: Boolean, val rateLimited: Boolean)

    private data class Entry(val body: String, val etag: String?, val mark: TimeSource.Monotonic.ValueTimeMark)

    private val mutex = Mutex()
    private val entries = mutableMapOf<String, Entry>()

    // Shared process-lifetime client; engine chosen per platform (see AzHttpClient.kt).
    private val client get() = azSharedHttpClient

    /**
     * Fetches [url], honouring the cache. Returns null only when there is neither a usable network
     * response nor a cached body (true cold failure).
     *
     * @param ttlMillis When the cached copy is younger than this, return it without any network call.
     */
    suspend fun get(url: String, ttlMillis: Long = DEFAULT_TTL_MILLIS): Result? {
        val cachedEntry = mutex.withLock { entries[url] }
        val cached = cachedEntry?.body

        if (cachedEntry != null && cachedEntry.mark.elapsedNow() < ttlMillis.milliseconds) {
            return Result(cachedEntry.body, fromCache = true, rateLimited = false)
        }

        return try {
            val response: HttpResponse = client.get(url) {
                header("User-Agent", "AzNavRail")
                header("Accept", "application/vnd.github+json")
                cachedEntry?.etag?.let { header("If-None-Match", it) }
            }
            val code = response.status.value
            val remaining = response.headers["X-RateLimit-Remaining"]?.toIntOrNull()

            when {
                response.status == HttpStatusCode.NotModified && cached != null -> {
                    // Refresh the freshness mark, keep the body + etag — but ONLY if the cache still
                    // holds the entry this request was conditioned on. A concurrent call may have
                    // stored a newer 200 body/etag while this request was in flight; refreshing
                    // blindly would clobber it with the older snapshot.
                    mutex.withLock {
                        val current = entries[url]
                        if (current != null && current.etag == cachedEntry?.etag) {
                            entries[url] = current.copy(mark = TimeSource.Monotonic.markNow())
                        }
                    }
                    Result(cached, fromCache = true, rateLimited = false)
                }
                code in 200..299 -> {
                    val body = response.bodyAsText()
                    // Store ONLY the etag the server sent for THIS body. Don't fall back to the old
                    // etag — it describes the previous representation, so pairing it with a fresh
                    // body could later trigger a bogus 304 that serves stale content.
                    val newEtag = response.headers["ETag"]
                    mutex.withLock {
                        entries[url] = Entry(body, newEtag, TimeSource.Monotonic.markNow())
                    }
                    Result(body, fromCache = false, rateLimited = false)
                }
                // 404 with no cache is a legitimate "not found" -> signal via null so callers can
                // treat e.g. a missing docs/ folder as empty rather than an error.
                code == 404 && cached == null -> null
                else -> {
                    val limited = code == 403 || code == 429 || remaining == 0
                    cached?.let { Result(it, fromCache = true, rateLimited = limited) }
                }
            }
        } catch (e: Exception) {
            cached?.let { Result(it, fromCache = true, rateLimited = false) }
        }
    }
}
