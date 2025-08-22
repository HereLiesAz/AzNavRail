package com.hereliesaz.testapp

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hereliesaz.aznavrail.model.NavItem
import com.hereliesaz.aznavrail.model.NavItemData
import com.hereliesaz.aznavrail.model.NavRailHeader
import com.hereliesaz.aznavrail.model.NavRailMenuSection
import com.hereliesaz.aznavrail.model.PredefinedAction
import com.hereliesaz.aznavrail.ui.AzNavRail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AzNavRailUsageTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun azNavRail_isDisplayed() {
        composeTestRule.setContent {
            AzNavRail(
                appName = "Test App",
                header = NavRailHeader { androidx.compose.material3.Text("Menu") },
                menuSections = listOf(
                    NavRailMenuSection(
                        title = "Main",
                        items = listOf(
                            NavItem(
                                text = "Home",
                                data = NavItemData.Action(predefinedAction = PredefinedAction.HOME),
                                showOnRail = true
                            )
                        )
                    )
                ),
                onPredefinedAction = { }
            )
        }

        composeTestRule.onNodeWithText("Home").assertExists()
    }

    @Test
    fun azNavRail_expandsAndCollapses() {
        composeTestRule.setContent {
            AzNavRail(
                appName = "Test App",
                header = NavRailHeader { androidx.compose.material3.Text("Menu") },
                menuSections = listOf(
                    NavRailMenuSection(
                        title = "Main",
                        items = listOf(
                            NavItem(
                                text = "Home",
                                data = NavItemData.Action(predefinedAction = PredefinedAction.HOME),
                                showOnRail = true
                            )
                        )
                    )
                ),
                onPredefinedAction = { }
            )
        }

        // Rail is initially collapsed
        composeTestRule.onNodeWithText("Main").assertDoesNotExist()

        // Expand the rail by clicking the header
        composeTestRule.onNodeWithText("Menu").performClick()

        // Rail is expanded
        composeTestRule.onNodeWithText("Main").assertExists()
    }

    @Test
    fun azNavRail_toggleButton_stateChanges() {
        var isOnline = false
        composeTestRule.setContent {
            AzNavRail(
                appName = "Test App",
                header = NavRailHeader { androidx.compose.material3.Text("Menu") },
                menuSections = listOf(
                    NavRailMenuSection(
                        title = "Main",
                        items = listOf(
                            NavItem(
                                text = "Online",
                                data = NavItemData.Toggle(
                                    initialIsChecked = false,
                                    onStateChange = { isOnline = it }
                                ),
                                showOnRail = true,
                                railButtonText = "Offline"
                            )
                        )
                    )
                ),
                onPredefinedAction = { }
            )
        }

        // Initial state
        composeTestRule.onNodeWithText("Offline").assertExists()
        assert(!isOnline)

        // Click to toggle
        composeTestRule.onNodeWithText("Offline").performClick()

        // State updated
        assert(isOnline)
    }

    @Test
    fun azNavRail_cycleButton_stateChanges() {
        var currentStatus = "Away"
        val statuses = listOf("Away", "Busy", "Online")
        composeTestRule.setContent {
            AzNavRail(
                appName = "Test App",
                header = NavRailHeader { androidx.compose.material3.Text("Menu") },
                menuSections = listOf(
                    NavRailMenuSection(
                        title = "Main",
                        items = listOf(
                            NavItem(
                                text = "Status",
                                data = NavItemData.Cycle(
                                    options = statuses,
                                    initialOption = "Away",
                                    onStateChange = { currentStatus = it }
                                ),
                                showOnRail = true
                            )
                        )
                    )
                ),
                onPredefinedAction = { },
                allowCyclersOnRail = true
            )
        }

        // Initial state
        composeTestRule.onNodeWithText("Away").assertExists()
        assert(currentStatus == "Away")

        // Click to cycle
        composeTestRule.onNodeWithText("Away").performClick()
        composeTestRule.onNodeWithText("Busy").assertExists()
        assert(currentStatus == "Busy")

        // Click to cycle again
        composeTestRule.onNodeWithText("Busy").performClick()
        composeTestRule.onNodeWithText("Online").assertExists()
        assert(currentStatus == "Online")
    }

    @Test
    fun azNavRail_customButtonContent_isDisplayed() {
        composeTestRule.setContent {
            AzNavRail(
                appName = "Test App",
                header = NavRailHeader { androidx.compose.material3.Text("Menu") },
                menuSections = listOf(
                    NavRailMenuSection(
                        title = "Main",
                        items = listOf(
                            NavItem(
                                text = "Custom",
                                data = NavItemData.Action(),
                                showOnRail = true
                            )
                        )
                    )
                ),
                onPredefinedAction = { },
                buttonContent = { item, _ ->
                    androidx.compose.material3.Text("Custom ${item.text}")
                }
            )
        }

        composeTestRule.onNodeWithText("Custom Custom").assertExists()
    }
}
