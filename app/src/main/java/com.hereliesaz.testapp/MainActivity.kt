package com.hereliesaz.testapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
    // In your screen's Composable, e.g., inside a Row
    AzNavRail(
        menuSections = listOf(
            NavRailMenuSection(
                title = "Main",
                items = listOf(
                    NavItem(
                        text = "Home",
                        data = NavItemData.Action(predefinedAction = PredefinedAction.HOME),
                        showOnRail = true
                    ),
                    NavItem(
                        text = "Online",
                        data = NavItemData.Toggle(
                            initialIsChecked = true,
                            onStateChange = { isOnline -> /* ... */ }
                        ),
                        showOnRail = true,
                        railButtonText = "On"
                    ),
                )
            )
        ),
        onPredefinedAction = { action ->
            when (action) {
                PredefinedAction.HOME -> { /* Navigate to Home */
                }

                else -> {}
            }
        },
    )
}
