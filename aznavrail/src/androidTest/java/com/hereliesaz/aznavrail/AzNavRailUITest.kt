package com.hereliesaz.aznavrail

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class AzNavRailUITest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun azNavRail_isDisplayed() {
        composeTestRule.setContent {
            AzNavRail {}
        }
        composeTestRule.onNodeWithText("App").assertIsDisplayed()
    }

    @Test
    fun azNavRail_expandsAndCollapses() {
        composeTestRule.setContent {
            AzNavRail {
                azMenuItem("home", "Home") {}
            }
        }

        composeTestRule.onNodeWithText("App").performClick()
        composeTestRule.onNodeWithText("Home").assertIsDisplayed()

        composeTestRule.onNodeWithText("App").performClick()
        composeTestRule.onNodeWithText("Home").assertDoesNotExist()
    }
}
