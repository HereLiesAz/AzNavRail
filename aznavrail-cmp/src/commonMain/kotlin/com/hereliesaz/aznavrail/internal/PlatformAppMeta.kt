package com.hereliesaz.aznavrail.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.hereliesaz.aznavrail.AzAppMeta
import com.hereliesaz.aznavrail.LocalAzAppMeta

/**
 * Resolves the host app's metadata — display name, launcher icon (a Coil3-compatible model), and
 * package/bundle id — from the platform, so the rail header and dropdown trigger show the real app
 * icon without the consumer wiring [LocalAzAppMeta] by hand. This matches the Android `aznavrail`
 * library, which scrapes `packageManager.getApplicationIcon`/`getApplicationLabel`.
 *
 * Best-effort per platform; any field may be blank/null where the platform has no notion of it:
 * - **Android** — launcher icon + label + package name (full).
 * - **Web (wasmJs)** — document title + favicon URL.
 * - **iOS** — bundle display name + bundle id (no runtime-loadable icon).
 * - **Desktop (JVM)** — no reliable OS-level app icon/name; returns defaults (wire [LocalAzAppMeta]).
 */
@Composable
internal expect fun rememberPlatformAppMeta(): AzAppMeta

/**
 * The effective app metadata the rail/dropdown should use: whatever the consumer supplied via
 * [LocalAzAppMeta], with each field left at its default filled in from [rememberPlatformAppMeta].
 * Explicit consumer values always win; the platform only fills the gaps. This is why the app icon
 * "just works" on Android (and where else the platform exposes one) while still honoring overrides.
 */
@Composable
internal fun rememberEffectiveAppMeta(): AzAppMeta {
    val provided = LocalAzAppMeta.current
    val platform = rememberPlatformAppMeta()
    return remember(provided, platform) {
        val default = AzAppMeta()
        AzAppMeta(
            name = if (provided.name != default.name) provided.name else platform.name,
            icon = provided.icon ?: platform.icon,
            packageId = provided.packageId ?: platform.packageId,
        )
    }
}
