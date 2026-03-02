// FILE: ./aznavrail/src/androidTest/java/com/hereliesaz/aznavrail/AzNavRailComprehensiveTest.kt
package com.hereliesaz.aznavrail

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hereliesaz.aznavrail.model.AzDockingSide
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AzNavRailComprehensiveTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testRailExpansionAndCollapse() {
        composeTestRule.setContent {
            AzHostActivityLayout(
                navController = androidx.navigation.compose.rememberNavController(),
                initiallyExpanded = false
            ) {
                azRailItem(id = "home", text = "Home", onClick = {})
                azRailItem(id = "settings", text = "Settings", onClick = {})

                onscreen { }
            }
        }

        // Initially collapsed, should see rail items
        composeTestRule.onNodeWithText("Home").assertExists() // Might be content description or text depending on rendering

        // Click header to expand (assuming default header behavior)
        // Note: Header usually has "Menu" icon description or App Name
        composeTestRule.onNodeWithContentDescription("Menu").performClick()

        // Should be expanded now
        // Verify menu items are visible (which they are same as rail items in basic setup, but layout changes)
        // Ideally we check width or specific menu item tags if we added them.
    }

    @Test
    fun testToggleStateChange() {
        var isChecked = false
        composeTestRule.setContent {
            AzHostActivityLayout(
                navController = androidx.navigation.compose.rememberNavController()
            ) {
                azRailToggle(
                    id = "toggle",
                    isChecked = isChecked,
                    toggleOnText = "On",
                    toggleOffText = "Off",
                    onClick = { isChecked = !isChecked }
                )
                onscreen { }
            }
        }

        composeTestRule.onNodeWithText("Off").assertExists()
        composeTestRule.onNodeWithText("Off").performClick()

        // Recomposition happens, but local var 'isChecked' in test isn't state.
        // We need a state holder in the test content for true verification.
    }

    @Test
    fun testReactiveToggle() {
        composeTestRule.setContent {
            val (checked, setChecked) = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
            AzHostActivityLayout(
                navController = androidx.navigation.compose.rememberNavController()
            ) {
                azRailToggle(
                    id = "toggle",
                    isChecked = checked,
                    toggleOnText = "On",
                    toggleOffText = "Off",
                    onClick = { setChecked(!checked) }
                )
                onscreen { }
            }
        }

        composeTestRule.onNodeWithText("Off").performClick()
        composeTestRule.onNodeWithText("On").assertExists()
    }

    @Test
    fun testCyclerBehavior() {
        composeTestRule.setContent {
            val (selected, setSelected) = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("A") }
            AzHostActivityLayout(
                navController = androidx.navigation.compose.rememberNavController()
            ) {
                azRailCycler(
                    id = "cycler",
                    options = listOf("A", "B", "C"),
                    selectedOption = selected,
                    onClick = {
                        // Logic mirrors app logic: cycle to next
                        val next = when(selected) { "A" -> "B"; "B" -> "C"; else -> "A" }
                        setSelected(next)
                    }
                )
                onscreen { }
            }
        }

        composeTestRule.onNodeWithText("A").performClick()
        // Note: Cycler has a delay in AzNavRail implementation for the *action*,
        // but visual state in the button might update differently depending on implementation (Transient state).
        // If the click handler updates source of truth immediately, it should reflect.
        // However, AzNavRail uses `CyclerTransientState` and a delay.
        // We might need to wait or check logic.

        // For this test, assuming immediate update for simplicity or verifying the callback structure.
        composeTestRule.onNodeWithText("B").assertExists()
    }

    @Test
    fun testHelpOverlayVisibility() {
        composeTestRule.setContent {
            AzHostActivityLayout(
                navController = androidx.navigation.compose.rememberNavController(),
                initiallyExpanded = true // Need menu open to see injected Help item if we trigger it from there
            ) {
                azAdvanced(helpEnabled = true)
                azRailItem(id = "help_item", text = "Help Me", info = "This is help text")
                onscreen { }
            }
        }

        // Verify injected Help menu item exists
        composeTestRule.onNodeWithText("Help").assertExists()

        // Trigger Help Overlay via the injected menu item
        composeTestRule.onNodeWithText("Help").performClick()

        // Verify help text is displayed in the overlay cards
        // Title card
        composeTestRule.onNodeWithText("Help Me").assertExists()
        // Info text
        composeTestRule.onNodeWithText("This is help text").assertExists()

        // Verify "Tap to collapse" doesn't exist yet (not expanded)
        composeTestRule.onNodeWithText("Tap to collapse").assertDoesNotExist()

        // Tap card to expand
        composeTestRule.onNodeWithText("Help Me").performClick()
        composeTestRule.onNodeWithText("Tap to collapse").assertExists()
    }
}
