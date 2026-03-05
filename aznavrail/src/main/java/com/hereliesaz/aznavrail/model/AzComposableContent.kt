package com.hereliesaz.aznavrail.model

import androidx.compose.runtime.Composable

/**
 * A wrapper to provide custom @Composable content for AzNavRail items.
 * Pass an instance of this to the `content` parameter of an item.
 *
 * Example usage:
 * ```
 * azRailItem(
 *     id = "custom_item",
 *     text = "Custom",
 *     content = AzComposableContent {
 *         Box(modifier = Modifier.fillMaxSize()) {
 *             // Custom layout
 *         }
 *     }
 * )
 * ```
 */
class AzComposableContent(val content: @Composable () -> Unit)
