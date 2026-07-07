package com.hereliesaz.aznavrail.service

import com.russhwolf.settings.Settings
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/**
 * Tiny GET-with-cache helper shared by the About reader and the "More from Az" carousel — the CMP
 * port of the Android sibling's `AzHttpCache`.
 *
 * Same three rate-limit mitigations as the Android version:
 *  - **TTL short-circuit**: within [DEFAULT_TTL_MILLIS] of the last successful in-process fetch the
 *    cached body is returned with no network call.
 *  - **Conditional requests**: an `ETag` is stored per URL and resent as `If-None-Match`; a `304`
 *    serves the cached body (and doesn't consume GitHub's primary quota).
 *  - **Graceful fallback**: on any network/HTTP error (incl. `403`/`429`) the last cached body is
 *    returned when available.
 *
 * ## Two-tier cache
 *  - **Hot tier**: an in-memory `Mutex`-guarded map (fast; carries a monotonic TTL mark).
 *  - **Persistent tier**: a `multiplatform-settings` store (java.util.prefs on JVM/Android,
 *    localStorage on wasmJs, NSUserDefaults on iOS) so etags+bodies survive a process restart,
 *    cutting cold-start GitHub-rate-limit hits. Persistence is **best-effort**: only bodies under
 *    [MAX_PERSIST_CHARS] are written (java.util.prefs caps values at 8 KB), keys are hashed (prefs
 *    caps keys at 80 chars), and the URL is stored inside the blob so a hash collision is detected
 *    and treated as a miss. A persisted entry has no live TTL mark, so it's always revalidated with
 *    its etag on the first cold read (a cheap 304 when unchanged).
 */
internal object AzHttpCache {

    const val DEFAULT_TTL_MILLIS: Long = 6 * 60 * 60 * 1000 // 6h

    /** Bodies larger than this are not persisted (java.util.prefs value cap is 8 KB). */
    private const val MAX_PERSIST_CHARS = 6000

    /** Result of a cached fetch. [fromCache] is true when no fresh body was downloaded this call. */
    data class Result(val body: String, val fromCache: Boolean, val rateLimited: Boolean)

    // `mark` is null for entries hydrated from the persistent tier — they never TTL-short-circuit
    // and are always revalidated via their etag.
    private data class Entry(
        val body: String,
        val etag: String?,
        val mark: TimeSource.Monotonic.ValueTimeMark?,
    )

    private val mutex = Mutex()
    private val entries = mutableMapOf<String, Entry>()
    private val json = Json { ignoreUnknownKeys = true }

    // Shared process-lifetime client; engine chosen per platform (see AzHttpClient.kt).
    private val client get() = azSharedHttpClient
    private val settings: Settings get() = azCacheSettings

    private fun persistKey(url: String): String = "azc_" + url.hashCode()

    /** Reads the persisted entry for [url] (verifying the stored URL to guard hash collisions). */
    private fun loadPersisted(url: String): Entry? = runCatching {
        val blob = settings.getStringOrNull(persistKey(url)) ?: return null
        val obj = json.parseToJsonElement(blob) as? JsonObject ?: return null
        if ((obj["u"] as? JsonPrimitive)?.contentOrNull != url) return null // hash collision
        val body = (obj["b"] as? JsonPrimitive)?.contentOrNull ?: return null
        val etag = (obj["e"] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotEmpty() }
        Entry(body, etag, mark = null)
    }.getOrNull()

    /** Persists [url]'s body+etag when small enough; otherwise clears any stale persisted copy. */
    private fun persist(url: String, body: String, etag: String?) {
        runCatching {
            if (body.length <= MAX_PERSIST_CHARS) {
                val blob = buildJsonObject {
                    put("u", url)
                    put("e", etag ?: "")
                    put("b", body)
                }.toString()
                settings.putString(persistKey(url), blob)
            } else {
                settings.remove(persistKey(url))
            }
        }
    }

    /**
     * Fetches [url], honouring the cache. Returns null only when there is neither a usable network
     * response nor a cached body (true cold failure).
     *
     * @param ttlMillis When the in-memory cached copy is younger than this, return it without any
     *   network call.
     */
    suspend fun get(url: String, ttlMillis: Long = DEFAULT_TTL_MILLIS): Result? {
        val inMem = mutex.withLock { entries[url] }

        // TTL short-circuit only applies to hot in-memory entries (persisted ones have a null mark).
        val mark = inMem?.mark
        if (mark != null && mark.elapsedNow() < ttlMillis.milliseconds) {
            return Result(inMem.body, fromCache = true, rateLimited = false)
        }

        // Resolve the entry to condition on: hot tier first, else persistent tier.
        val cachedEntry = inMem ?: loadPersisted(url)
        val cached = cachedEntry?.body

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
                    // Body unchanged. Promote to a fresh hot entry — but only if the hot tier still
                    // holds the entry this request was conditioned on (a concurrent 200 may have
                    // stored a newer body/etag; don't clobber it with the older snapshot).
                    mutex.withLock {
                        val current = entries[url]
                        if (current == null || current.etag == cachedEntry?.etag) {
                            entries[url] = Entry(cached, cachedEntry?.etag, TimeSource.Monotonic.markNow())
                        }
                    }
                    Result(cached, fromCache = true, rateLimited = false)
                }
                code in 200..299 -> {
                    val body = response.bodyAsText()
                    // Store ONLY the etag the server sent for THIS body (see the review thread on
                    // #489 — pairing a stale etag with a fresh body can trigger a bogus 304).
                    val newEtag = response.headers["ETag"]
                    mutex.withLock {
                        entries[url] = Entry(body, newEtag, TimeSource.Monotonic.markNow())
                    }
                    persist(url, body, newEtag)
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
