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

        // Click to open (using dropdown icon)
        composeTestRule.onNodeWithContentDescription("Dropdown").performClick()

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

        // Open popup (using dropdown icon)
        composeTestRule.onNodeWithContentDescription("Dropdown").performClick()

        // Select "Two".
        composeTestRule.onAllNodesWithText("Two").onFirst().performClick()

        // Verify selection callback
        assert(selected == "Two")

        // Verify popup closed
        composeTestRule.waitForIdle()
        // "One" should be hidden.
        composeTestRule.onNodeWithText("One").assertDoesNotExist()
    }
}
