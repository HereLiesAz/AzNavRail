package com.hereliesaz.SampleApp.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.AzButton
import com.hereliesaz.aznavrail.AzToggle
import com.hereliesaz.aznavrail.bottomsheet.AzSheetController
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzSheetDetent

/**
 * Control panel for the host-registered `azBottomSheet` DSL.
 *
 * The actual sheet is registered in `MainApp` via `AzNavHostScope.azBottomSheet { ... }` so it
 * draws above the rail/menu and inside the system navigation-bar inset. This screen mutates the
 * shared `AzSheetController` and live `AzSheetConfig` toggles owned at host scope.
 */
@Composable
fun BottomSheetDemoScreen(
    controller: AzSheetController,
    horizontalSwipeEnabled: Boolean,
    onHorizontalSwipeChange: (Boolean) -> Unit,
    collapseOnBack: Boolean,
    onCollapseOnBackChange: (Boolean) -> Unit,
    handleVisible: Boolean,
    onHandleVisibleChange: (Boolean) -> Unit,
    animateInTree: Boolean,
    onAnimateInTreeChange: (Boolean) -> Unit,
    swipeCount: Int,
    swipeLog: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Bottom Sheets", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "The sheet itself is registered via the host-scope `azBottomSheet` DSL in MainApp so it draws above the rail/menu and respects the system navigation-bar inset. This panel only mutates the shared controller and config.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Text("Current detent: ${controller.detent}", style = MaterialTheme.typography.titleMedium)
        Text("Controller isEnabled: ${controller.isEnabled}", style = MaterialTheme.typography.bodyMedium)

        Spacer(Modifier.height(8.dp))
        Text("snapTo", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AzSheetDetent.values().forEach { detent ->
                AzButton(
                    onClick = { controller.snapTo(detent) },
                    text = detent.name,
                    shape = AzButtonShape.RECTANGLE,
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("Step controls", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AzButton(onClick = { controller.stepUp() }, text = "stepUp()", shape = AzButtonShape.RECTANGLE)
            AzButton(onClick = { controller.stepDown() }, text = "stepDown()", shape = AzButtonShape.RECTANGLE)
        }

        Spacer(Modifier.height(8.dp))
        Text("Live AzSheetConfig", fontWeight = FontWeight.SemiBold)
        ConfigToggle("Horizontal swipe enabled", horizontalSwipeEnabled, onHorizontalSwipeChange)
        ConfigToggle("Collapse on back press", collapseOnBack, onCollapseOnBackChange)
        ConfigToggle("Drag handle visible", handleVisible, onHandleVisibleChange)
        ConfigToggle("Animate detent transitions", animateInTree, onAnimateInTreeChange)
        ConfigToggle("Controller isEnabled", controller.isEnabled) { controller.isEnabled = it }

        Spacer(Modifier.height(12.dp))
        Text("Swipe log", fontWeight = FontWeight.SemiBold)
        Text("$swipeCount swipes — last: $swipeLog", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ConfigToggle(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AzToggle(
            isChecked = value,
            onToggle = { onChange(!value) },
            toggleOnText = "On",
            toggleOffText = "Off",
            shape = AzButtonShape.RECTANGLE,
        )
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
