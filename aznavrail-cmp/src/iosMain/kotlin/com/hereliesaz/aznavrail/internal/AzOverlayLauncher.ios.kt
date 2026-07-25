package com.hereliesaz.aznavrail.internal

import androidx.compose.runtime.Composable

/** No system-overlay concept on this target; undocking stays an in-app FAB. */
@Composable
internal actual fun rememberAzOverlayLauncher(): (Any?) -> Unit = { }
