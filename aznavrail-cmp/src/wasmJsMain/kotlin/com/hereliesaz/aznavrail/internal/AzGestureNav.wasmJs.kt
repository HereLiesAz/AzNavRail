package com.hereliesaz.aznavrail.internal

import androidx.compose.runtime.Composable

/** Web has no Android navigation bar — keep the content safe-zone (button-nav behavior). */
@Composable
internal actual fun rememberIsGestureNav(): Boolean = false
