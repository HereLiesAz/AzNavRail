package com.hereliesaz.aznavrail.service

import io.ktor.client.HttpClient

/**
 * The Ktor engine differs per platform (Android → `Android`, Desktop → `CIO`, wasmJs → `Js`), and
 * the no-arg `HttpClient()` classpath auto-selection isn't guaranteed callable from commonMain — so
 * the engine choice is made explicitly per target via this `expect`/`actual`. Both [AzHttpCache] and
 * [MoreFromAzRepository] share the single lazily-created [azSharedHttpClient] (process-lifetime).
 */
internal expect fun createAzHttpClient(): HttpClient

/** Shared client for the whole service layer — one engine instance for the process. */
internal val azSharedHttpClient: HttpClient by lazy { createAzHttpClient() }
