package com.hereliesaz.aznavrail

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
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
        composeTestRule.onNodeWithContentDescription("Toggle menu, showing App icon").assertIsDisplayed()
    }

    @Test
    fun azNavRail_expandsAndCollapses() {
        composeTestRule.setContent {
            AzNavRail {
                azMenuItem("home", "Home") {}
            }
        }

        composeTestRule.onNodeWithContentDescription("Toggle menu, showing App icon").performClick()
        composeTestRule.onNodeWithText("Home").assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Toggle menu, showing App icon").performClick()
        composeTestRule.onNodeWithText("Home").assertDoesNotExist()
    }

    @Test
    fun appIcon_hasCorrectSize() {
        composeTestRule.setContent {
            AzNavRail {}
        }

        val iconNode = composeTestRule.onNodeWithContentDescription("Toggle menu, showing App icon", useUnmergedTree = true)
        iconNode.assertExists()

        val density = composeTestRule.density
        val expectedSize = with(density) { 72.dp.toPx() }

        val bounds = iconNode.fetchSemanticsNode().size

        assertEquals(expectedSize.toInt(), bounds.width)
        assertEquals(expectedSize.toInt(), bounds.height)
    }
}
