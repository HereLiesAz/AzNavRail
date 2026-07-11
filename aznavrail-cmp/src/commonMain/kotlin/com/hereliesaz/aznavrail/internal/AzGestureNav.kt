package com.hereliesaz.aznavrail.internal

import androidx.compose.runtime.Composable

/**
 * Whether the device uses gesture navigation (no on-screen button nav bar). Drives the bottom
 * safe-zone rule ([azResolveSafeBottom]): in gesture nav the host imposes no bottom margin. Android
 * reads `Settings.Secure`; other platforms have no Android-style navigation bar, so they report
 * `false` and the host keeps its 10% content safe-zone (the strict-layout aesthetic).
 */
@Composable
internal expect fun rememberIsGestureNav(): Boolean
