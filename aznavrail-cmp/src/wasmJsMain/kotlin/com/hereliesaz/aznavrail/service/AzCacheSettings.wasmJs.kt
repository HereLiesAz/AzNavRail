package com.hereliesaz.aznavrail.service

import com.russhwolf.settings.Settings
import com.russhwolf.settings.StorageSettings

// StorageSettings defaults to the browser's localStorage.
internal actual fun createAzCacheSettings(): Settings = StorageSettings()
