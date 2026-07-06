package com.hereliesaz.aznavrail.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

/**
 * A wrapper to provide custom @Composable content for AzNavRail items.
 * Pass an instance of this to the `content` parameter of an item.
 *
 * Example usage:
 * ```
 * azRailItem(
 *     id = "custom_item",
 *     text = "Custom",
 *     content = AzComposableContent { isEnabled ->
 *         Box(modifier = Modifier.fillMaxSize().alpha(if (isEnabled) 1f else 0.5f)) {
 *             // Custom layout
 *         }
 *     }
 * )
 * ```
 */
@Immutable
data class AzComposableContent(val content: @Composable (isEnabled: Boolean) -> Unit)
