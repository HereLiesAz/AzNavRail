// FILE: ./aznavrail/src/main/java/com/hereliesaz/aznavrail/internal/AzBottomSheetItem.kt
package com.hereliesaz.aznavrail.internal

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import com.hereliesaz.aznavrail.bottomsheet.AzSheetController
import com.hereliesaz.aznavrail.model.AzSheetConfig

/**
 * Holds a bottom-sheet registration produced by
 * [com.hereliesaz.aznavrail.AzNavHostScope.azBottomSheet]. Iterated by
 * [com.hereliesaz.aznavrail.AzHostActivityLayout] to render each registered sheet
 * in the top z-layer of the host.
 */
internal data class AzBottomSheetItem(
    val controller: AzSheetController,
    val config: AzSheetConfig,
    val onSwipeLeft: (() -> Unit)?,
    val onSwipeRight: (() -> Unit)?,
    val content: @Composable BoxScope.() -> Unit,
)
