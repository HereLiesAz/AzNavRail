package com.hereliesaz.aznavrail.service

import com.russhwolf.settings.Settings

/**
 * Shared process-lifetime key-value store backing [AzHttpCache]'s persistence tier.
 *
 * `multiplatform-settings-no-arg` supplies the commonMain `Settings()` factory, which resolves the
 * right platform store on its own: java.util.prefs on Desktop, `SharedPreferences` on Android (its
 * `Context` obtained via an androidx-startup initializer — no `Context` param needed here),
 * localStorage on wasmJs, and `NSUserDefaults` on iOS.
 */
internal val azCacheSettings: Settings by lazy { Settings() }
