package com.hereliesaz.aznavrail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AzDropdownMenuTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun trigger_renders_and_toggles_the_panel() {
        composeTestRule.setContent {
            AzDropdownMenu {
                azItem("Settings") { }
            }
        }

        // Closed initially.
        composeTestRule.onNodeWithText("Settings").assertDoesNotExist()

        // Tap the hamburger to unfold.
        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun item_click_fires_and_folds_by_default() {
        var clicked = false
        composeTestRule.setContent {
            AzDropdownMenu {
                azItem("Sign out") { clicked = true }
            }
        }

        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        composeTestRule.onNodeWithText("Sign out").performClick()

        assertTrue(clicked)
        // Folded back up after the click.
        composeTestRule.onNodeWithText("Sign out").assertDoesNotExist()
    }

    @Test
    fun item_with_closeOnClick_false_keeps_the_panel_open() {
        var count = 0
        composeTestRule.setContent {
            AzDropdownMenu {
                azItem("Bump", closeOnClick = false) { count++ }
            }
        }

        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        composeTestRule.onNodeWithText("Bump").performClick()

        assertEquals(1, count)
        // Still open.
        composeTestRule.onNodeWithText("Bump").assertIsDisplayed()
    }

    @Test
    fun controlled_expanded_shows_the_panel() {
        composeTestRule.setContent {
            AzDropdownMenu(expanded = true) {
                azItem("Profile") { }
            }
        }
        composeTestRule.onNodeWithText("Profile").assertIsDisplayed()
    }
}
