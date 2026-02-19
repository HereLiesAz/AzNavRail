// SampleApp/src/main/kotlin/com/example/sampleapp/MainActivity.kt
package com.example.sampleapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.hereliesaz.aznavrail.AzActivity
import com.hereliesaz.aznavrail.AzNavRailScope
import com.hereliesaz.aznavrail.annotation.App
import com.hereliesaz.aznavrail.annotation.Az
import com.hereliesaz.aznavrail.model.AzDockingSide
import com.hereliesaz.aznavrail.model.AzNestedRailAlignment

@Az(app = App(dock = AzDockingSide.LEFT))
class MainActivity : AzActivity() {
    override val graph = AzGraph

    // Configuration State
    var dockingSide by mutableStateOf(AzDockingSide.LEFT)
    var packButtons by mutableStateOf(false)
    var showFooter by mutableStateOf(true)
    var displayAppName by mutableStateOf(true)
    var usePhysicalDocking by mutableStateOf(false)
    var infoScreen by mutableStateOf(false)
    var noMenu by mutableStateOf(false)
    var vibrate by mutableStateOf(true)
    var isLoading by mutableStateOf(false)

    // State for manual items
    var wifiEnabled by mutableStateOf(true)
    var currentMode by mutableStateOf("Auto")
    val modes = listOf("Light", "Dark", "Auto")

    override fun AzNavRailScope.configureRail() {
        azConfig(
            dockingSide = dockingSide,
            packButtons = packButtons,
            displayAppName = displayAppName,
            usePhysicalDocking = usePhysicalDocking,
            noMenu = noMenu,
            vibrate = vibrate,
            showFooter = showFooter
        )

        azAdvanced(
            infoScreen = infoScreen,
            onDismissInfoScreen = { infoScreen = false },
            enableRailDragging = true,
            isLoading = isLoading,
            overlayService = SampleOverlayService::class.java
        )

        // Manual Dynamic Items

        azRailItem(
            id = "number_item",
            text = "42",
            content = 42,
            info = "Item with Number content",
            onClick = {}
        )
        
        azRailItem(
            id = "image_item",
            text = "Camera",
            content = android.R.drawable.ic_menu_camera,
            info = "Item with Image Resource content",
            onClick = {}
        )

        // Toggles
        azRailToggle(
            id = "wifi",
            isChecked = wifiEnabled,
            toggleOnText = "Wi-Fi: On",
            toggleOffText = "Wi-Fi: Off",
            onClick = { wifiEnabled = !wifiEnabled },
            info = "Toggle Wi-Fi connection"
        )

        azRailCycler(
            id = "mode",
            options = modes,
            selectedOption = currentMode,
            onClick = { currentMode = modes[(modes.indexOf(currentMode) + 1) % modes.size] },
            info = "Cycle through display modes"
        )

        // Nested Rails (Manual)
        azNestedRail(
            id = "nested_vert",
            text = "Vertical Nest",
            alignment = AzNestedRailAlignment.VERTICAL,
            info = "Vertical nested rail"
        ) {
            azRailItem(id = "nv1", text = "V Nested 1", onClick = {})
            azRailItem(id = "nv2", text = "V Nested 2", onClick = {})
        }
    }
}
