package com.hereliesaz.aznavrail

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
@Config(instrumentedPackages = ["androidx.loader.content.ModernAsyncTask"])
class AzRollerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun azRoller_displaysSelectedOption() {
        ShadowLog.stream = System.out
        val options = listOf("Option 1", "Option 2")
        val selected = "Option 1"

        composeTestRule.setContent {
            AzRoller(
                options = options,
                selectedOption = selected,
                onOptionSelected = {}
            )
        }

        composeTestRule.onNodeWithText("Option 1").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Dropdown").assertIsDisplayed()
    }

    @Test
    fun azRoller_displaysHint_whenNoSelection() {
        val options = listOf("Option 1", "Option 2")

        composeTestRule.setContent {
            AzRoller(
                options = options,
                selectedOption = null,
                hint = "Select an option",
                onOptionSelected = {}
            )
        }

        composeTestRule.onNodeWithText("Select an option").assertIsDisplayed()
    }

    @Test
    fun azRoller_opensPopup_onClick() {
        val options = listOf("Apple", "Banana", "Cherry")

        composeTestRule.setContent {
            AzRoller(
                options = options,
                selectedOption = null,
                hint = "Select Fruit",
                onOptionSelected = {}
            )
        }

        // Initially popup content should not be visible (checking for one option)
        composeTestRule.onNodeWithText("Apple").assertDoesNotExist()

        // Click to open
        composeTestRule.onNodeWithText("Select Fruit").performClick()

        // Now options should be visible
        composeTestRule.onAllNodesWithText("Apple").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Banana").onFirst().assertIsDisplayed()
    }

    @Test
    fun azRoller_selectsOption_andClosesPopup() {
        val options = listOf("One", "Two", "Three")
        var selected: String? = null

        composeTestRule.setContent {
            AzRoller(
                options = options,
                selectedOption = selected,
                hint = "Pick Number",
                onOptionSelected = { selected = it }
            )
        }

        // Open popup
        composeTestRule.onNodeWithText("Pick Number").performClick()

        // Select "Two". Might be multiple due to infinite list. Pick first.
        composeTestRule.onAllNodesWithText("Two").onFirst().performClick()

        // Verify selection callback
        assert(selected == "Two")

        // Verify popup closed (option "One" or "Three" should not be visible in main tree,
        // though strictly they might exist in a closed popup state depending on implementation.
        // Usually assertDoesNotExist checks semantic tree.)
        // However, since "Two" is now selected, it might be displayed in the box.
        // Let's check if "One" (unselected option) is gone.

        // Need to wait for composition update if relying on state change driving UI
        composeTestRule.waitForIdle()

        // Re-assert: "One" should not be visible anymore as popup is closed
        composeTestRule.onNodeWithText("One").assertDoesNotExist()
    }
}
