package com.hereliesaz.aznavrail.service

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Tiny GET-with-cache helper shared by the About reader and the "More from Az" carousel.
 *
 * It mitigates GitHub's ~60 req/hr unauthenticated rate limit three ways:
 *  - **TTL short-circuit**: within [ttlMillis] of the last successful fetch the cached body is
 *    returned with no network call at all.
 *  - **Conditional requests**: an `ETag` is stored per URL and resent as `If-None-Match`; a `304`
 *    response (which does not consume the primary rate-limit quota) serves the cached body.
 *  - **Graceful fallback**: on any network/HTTP error (including a `403`/`429` rate-limit response),
 *    the last cached body is returned when available.
 *
 * Bodies live in `cacheDir/aznavrail_about/`; ETags and timestamps live in a private SharedPreferences.
 */
internal object AzHttpCache {

    private const val PREFS = "aznavrail_about_cache"
    private const val DIR = "aznavrail_about"
    const val DEFAULT_TTL_MILLIS: Long = 6 * 60 * 60 * 1000 // 6h

    /** Result of a cached fetch. [fromCache] is true when no fresh body was downloaded this call. */
    data class Result(val body: String, val fromCache: Boolean, val rateLimited: Boolean)

    private fun cacheFile(context: Context, url: String): File {
        val dir = File(context.cacheDir, DIR).apply { mkdirs() }
        return File(dir, url.hashCode().toString() + ".cache")
    }

    private fun readCache(context: Context, url: String): String? =
        cacheFile(context, url).takeIf { it.exists() }?.readText()

    /**
     * Fetches [url], honouring the cache. Returns null only when there is neither a usable network
     * response nor a cached body (true cold failure).
     *
     * @param ttlMillis When the cached copy is younger than this, return it without any network call.
     */
    suspend fun get(
        context: Context,
        url: String,
        ttlMillis: Long = DEFAULT_TTL_MILLIS,
    ): Result? = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val etag = prefs.getString("etag:$url", null)
        val lastFetch = prefs.getLong("ts:$url", 0L)
        val cached = readCache(context, url)

        if (cached != null && System.currentTimeMillis() - lastFetch < ttlMillis) {
            return@withContext Result(cached, fromCache = true, rateLimited = false)
        }

        var connection: HttpURLConnection? = null
        try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 15_000
                setRequestProperty("User-Agent", "AzNavRail")
                setRequestProperty("Accept", "application/vnd.github+json")
                if (etag != null) setRequestProperty("If-None-Match", etag)
            }
            val code = connection.responseCode
            val remaining = connection.getHeaderField("X-RateLimit-Remaining")?.toIntOrNull()

            when {
                code == HttpURLConnection.HTTP_NOT_MODIFIED && cached != null -> {
                    prefs.edit().putLong("ts:$url", System.currentTimeMillis()).apply()
                    Result(cached, fromCache = true, rateLimited = false)
                }
                code in 200..299 -> {
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    cacheFile(context, url).writeText(body)
                    val newEtag = connection.getHeaderField("ETag")
                    prefs.edit().apply {
                        if (newEtag != null) putString("etag:$url", newEtag)
                        putLong("ts:$url", System.currentTimeMillis())
                    }.apply()
                    Result(body, fromCache = false, rateLimited = false)
                }
                // 404 with no cache is a legitimate "not found" -> signal via null so callers can
                // treat e.g. a missing docs/ folder as empty rather than an error.
                code == HttpURLConnection.HTTP_NOT_FOUND && cached == null -> null
                else -> {
                    val limited = code == 403 || code == 429 || remaining == 0
                    cached?.let { Result(it, fromCache = true, rateLimited = limited) }
                }
            }
        } catch (e: Exception) {
            cached?.let { Result(it, fromCache = true, rateLimited = false) }
        } finally {
            connection?.disconnect()
        }
    }
}
