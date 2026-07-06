package com.hereliesaz.aznavrail.service

import com.hereliesaz.aznavrail.model.AzMoreFromApp

/**
 * Fetches HereLiesAz's showcase JSON — the "More from Az" carousel data.
 *
 * ## Port note
 * The Android sibling uses `HttpURLConnection` + `org.json` + `Context.cacheDir`. The CMP module
 * keeps just the [MoreFromResult] data class and stubs [fetch] to return an empty list; a follow-up
 * PR can wire Ktor + kotlinx-serialization without changing the signature. The `Context` parameter
 * that the Android sibling requires is dropped here.
 */
object MoreFromAzRepository {

    /** Result of [fetch]: the app list plus a flag noting the network was unavailable/limited. */
    data class MoreFromResult(val apps: List<AzMoreFromApp>, val rateLimitedOrOffline: Boolean = false)

    /**
     * Stubbed on CMP — always returns an empty list. See the class KDoc.
     */
    suspend fun fetch(jsonUrl: String): Result<MoreFromResult> =
        Result.success(MoreFromResult(emptyList(), rateLimitedOrOffline = true))
}
