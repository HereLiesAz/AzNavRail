package com.hereliesaz.aznavrail.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.hereliesaz.aznavrail.AzAppMeta

/**
 * Desktop (JVM): there is no OS-level "launcher icon"/app name queryable at runtime — a desktop app's
 * icon is set by the developer on its `Window`, not discoverable generically. So return defaults; a
 * desktop consumer that wants an icon/name provides `LocalAzAppMeta` explicitly.
 */
@Composable
internal actual fun rememberPlatformAppMeta(): AzAppMeta = remember { AzAppMeta() }
