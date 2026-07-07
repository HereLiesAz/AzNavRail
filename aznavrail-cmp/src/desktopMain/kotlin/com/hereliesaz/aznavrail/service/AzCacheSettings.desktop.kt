package com.hereliesaz.aznavrail.service

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import java.util.prefs.Preferences

internal actual fun createAzCacheSettings(): Settings =
    PreferencesSettings(Preferences.userRoot().node("com/hereliesaz/aznavrail/httpcache"))
