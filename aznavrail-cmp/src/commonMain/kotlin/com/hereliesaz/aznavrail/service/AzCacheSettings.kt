package com.hereliesaz.aznavrail.service

import com.russhwolf.settings.Settings

/**
 * Platform key-value store backing [AzHttpCache]'s persistence layer. `multiplatform-settings`
 * abstracts the per-platform storage (java.util.prefs on JVM/Android, localStorage on wasmJs,
 * NSUserDefaults on iOS), but its construction is platform-specific — hence this `expect`/`actual`.
 */
internal expect fun createAzCacheSettings(): Settings

/** Shared process-lifetime settings store for the HTTP cache. */
internal val azCacheSettings: Settings by lazy { createAzCacheSettings() }
