package com.hereliesaz.aznavrail.internal

import androidx.compose.runtime.Composable

/**
 * Current device rotation in degrees clockwise from portrait: 0, 90, 180, or 270.
 *
 * The Android sibling reads `LocalView.current.display?.rotation` and maps
 * `android.view.Surface.ROTATION_*` to degrees; the CMP variant delegates to a platform-specific
 * implementation. All non-Android targets return 0f because rotation-aware FAB drag is an
 * Android-only affordance (desktop / iOS / wasmJs don't have the concept in a form the rail cares
 * about) — the drag math simply doesn't compensate for orientation on those targets.
 */
@Composable
internal expect fun rememberDeviceRotationDegrees(): Float
