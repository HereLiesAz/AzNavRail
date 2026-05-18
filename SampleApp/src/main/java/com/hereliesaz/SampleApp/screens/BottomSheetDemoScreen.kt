package com.hereliesaz.SampleApp.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.AzButton
import com.hereliesaz.aznavrail.AzToggle
import com.hereliesaz.aznavrail.bottomsheet.AzBottomSheet
import com.hereliesaz.aznavrail.bottomsheet.AzBottomSheetInsetAware
import com.hereliesaz.aznavrail.bottomsheet.rememberAzSheetController
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzSheetConfig
import com.hereliesaz.aznavrail.model.AzSheetDetent
import kotlinx.coroutines.delay

@Composable
fun BottomSheetDemoScreen() {
    val controller = rememberAzSheetController(initial = AzSheetDetent.PEEK)

    var horizontalSwipeEnabled by remember { mutableStateOf(true) }
    var collapseOnBack by remember { mutableStateOf(true) }
    var handleVisible by remember { mutableStateOf(true) }
    var animateInTree by remember { mutableStateOf(true) }
    var insetAware by remember { mutableStateOf(false) }
    var swipeLog by remember { mutableStateOf("(no swipes yet)") }
    var swipeCount by remember { mutableIntStateOf(0) }

    val config = AzSheetConfig(
        horizontalSwipeEnabled = horizontalSwipeEnabled,
        collapseOnBack = collapseOnBack,
        handleVisible = handleVisible,
        animateInTree = animateInTree,
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Bottom Sheets", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Four detents (HIDDEN/PEEK/HALF/FULL). Drag the handle, tap snapTo to jump, or step through with the controller.",
                style = MaterialTheme.typography.bodyMedium,
            )

            Text("Current detent: ${controller.detent}", style = MaterialTheme.typography.titleMedium)
            Text("Enabled: ${controller.isEnabled}", style = MaterialTheme.typography.bodyMedium)

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
            Text("Sheet config", fontWeight = FontWeight.SemiBold)
            ConfigToggle("Horizontal swipe enabled", horizontalSwipeEnabled) { horizontalSwipeEnabled = it }
            ConfigToggle("Collapse on back press", collapseOnBack) { collapseOnBack = it }
            ConfigToggle("Drag handle visible", handleVisible) { handleVisible = it }
            ConfigToggle("Animate detent transitions", animateInTree) { animateInTree = it }
            ConfigToggle("Use inset-aware variant", insetAware) { insetAware = it }
            ConfigToggle("Controller isEnabled", controller.isEnabled) { controller.isEnabled = it }

            Spacer(Modifier.height(12.dp))
            Text("Swipe log", fontWeight = FontWeight.SemiBold)
            Text("$swipeCount swipes — last: $swipeLog", style = MaterialTheme.typography.bodySmall)

            Spacer(Modifier.height(360.dp))
        }

        val onSwipeLeft: () -> Unit = {
            swipeCount++
            swipeLog = "left @ ${System.currentTimeMillis() % 100000}"
        }
        val onSwipeRight: () -> Unit = {
            swipeCount++
            swipeLog = "right @ ${System.currentTimeMillis() % 100000}"
        }

        if (insetAware) {
            AzBottomSheetInsetAware(
                controller = controller,
                config = config,
                onSwipeLeft = onSwipeLeft,
                onSwipeRight = onSwipeRight,
            ) { SheetContents() }
        } else {
            AzBottomSheet(
                controller = controller,
                config = config,
                onSwipeLeft = onSwipeLeft,
                onSwipeRight = onSwipeRight,
            ) { SheetContents() }
        }
    }

    // Pulse the detent on first launch so it's obvious the sheet exists.
    LaunchedEffect(Unit) {
        delay(400)
        if (controller.detent == AzSheetDetent.HIDDEN) controller.snapTo(AzSheetDetent.PEEK)
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

@Composable
private fun SheetContents() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Sheet contents", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "Drag the handle up or down to step detents. The accumulated-delta gesture means each gesture advances exactly one step.",
            style = MaterialTheme.typography.bodySmall,
        )
        Text("When horizontal swipe is enabled, swipe the header left or right to fire the callbacks.")
        Text("Body content scrolls independently when the sheet is at HALF or FULL detent.")
        repeat(20) { i ->
            Text("Line ${i + 1} of sheet body — fill this with whatever you like.")
        }
    }
}
