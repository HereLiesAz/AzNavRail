package com.hereliesaz.aznavrail.cmp

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/**
 * Placeholder composable that gives the aznavrail-cmp module something to compile in every target.
 * Phase A of the Compose Multiplatform port ships only the module scaffold + gradle wiring; the real
 * navigation-rail composables (AzNavRail, AzDropdownMenu, AzForm, etc.) will be ported to
 * `commonMain` in later phases as their Android-platform coupling is resolved via `expect/actual`.
 */
@Composable
fun AzCmpPreview() {
    Text(
        text = "aznavrail-cmp Phase A",
        style = MaterialTheme.typography.titleLarge,
    )
}
