package com.example.testapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.hereliesaz.aznavrail.model.NavItem
import com.hereliesaz.aznavrail.model.NavItemData
import com.hereliesaz.aznavrail.model.NavRailHeader
import com.hereliesaz.aznavrail.model.NavRailMenuSection
import com.hereliesaz.aznavrail.model.PredefinedAction
import com.hereliesaz.aznavrail.ui.AzNavRail

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TestAppScreen()
        }
    }
}

@Composable
fun TestAppScreen() {
    // 1. Define dummy data for the NavRail
    val header = NavRailHeader(
        content = { Text("Menu") }
    )

    val menuSections = listOf(
        NavRailMenuSection(
            title = "Main",
            items = listOf(
                NavItem(
                    text = "Home",
                    data = NavItemData.Action(onClick = {})
                ),
                NavItem(
                    text = "Settings",
                    data = NavItemData.Action(predefinedAction = PredefinedAction.SETTINGS)
                )
            )
        )
    )

    // 2. Define a dummy action handler
    val onPredefinedAction: (PredefinedAction) -> Unit = { action ->
        println("Action clicked: $action")
    }

    // 3. Instantiate the AzNavRail component
    AzNavRail(
        header = header,
        menuSections = menuSections,
        onPredefinedAction = onPredefinedAction
    )
}
