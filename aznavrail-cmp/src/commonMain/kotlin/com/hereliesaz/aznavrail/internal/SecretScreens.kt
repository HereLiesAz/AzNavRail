package com.hereliesaz.aznavrail.internal

import androidx.compose.runtime.Composable

/**
 * Developer diagnostic overlay activated by long-pressing the @HereLiesAz footer row.
 *
 * ## Port note
 * The Android sibling opens a full-fledged dialog with GPS location broadcast, permissions,
 * `LazyColumn` of `LocationListener` events, a raw-socket receiver on [secLocPort], `WifiInfo`
 * inspection, etc. — all Android-only APIs (Manifest.permission.ACCESS_FINE_LOCATION,
 * LocationManager, NetworkInterface enumeration, ContextCompat).
 *
 * The CMP module stubs this to a no-op callable — long-pressing the footer just does nothing.
 * A dedicated dev-tools port would be a separate PR (and would need
 * `expect class DevToolsHost { fun open() }` per target).
 */
@Composable
internal fun SecretScreens(
    secLoc: String?,
    secLocPort: Int,
): () -> Unit = {}
