package com.hereliesaz.aznavrail.internal

import androidx.compose.runtime.Composable

/**
 * Platform window chrome for [com.hereliesaz.aznavrail.AzHostActivityLayout]. On Android this walks
 * up to the enclosing Activity to enable edge-to-edge (`WindowCompat.setDecorFitsSystemWindows`) and
 * — when [wantDrawBehindNavBar] and the device uses button navigation — makes the system navigation
 * bar see-through so a registered bottom sheet can draw behind it (restored on dispose). Off Android
 * there is no such window chrome, so it's a no-op: those targets render edge-to-edge by default and
 * have no Android navigation bar.
 */
@Composable
internal expect fun PlatformHostChrome(wantDrawBehindNavBar: Boolean)
