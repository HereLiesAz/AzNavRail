package com.hereliesaz.aznavrail.service

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import java.util.prefs.Preferences

// java.util.prefs works on Android (it's a JVM API) and needs no `Context`, keeping the module
// Context-free. Its per-value 8 KB / per-key 80-char limits are handled by AzHttpCache (hashed keys
// + a body size cap).
internal actual fun createAzCacheSettings(): Settings =
    PreferencesSettings(Preferences.userRoot().node("com/hereliesaz/aznavrail/httpcache"))
