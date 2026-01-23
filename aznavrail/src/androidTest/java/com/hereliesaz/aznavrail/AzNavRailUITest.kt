package com.hereliesaz.aznavrail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AzNavRailUITest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun azNavRail_isDisplayed() {
        composeTestRule.setContent {
            val navController = rememberNavController()
            AzHostActivityLayout(navController = navController) {
                // AzNavRail is implicitly rendered by the host
            }
        }
        composeTestRule.onNodeWithContentDescription("Toggle menu, showing App icon").assertIsDisplayed()
    }

    @Test
    fun azNavRail_expandsAndCollapses() {
        composeTestRule.setContent {
            val navController = rememberNavController()
            AzHostActivityLayout(navController = navController) {
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
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val appIcon = context.packageManager.getApplicationIcon(context.packageName)
        val expectedWidth = appIcon.intrinsicWidth
        val expectedHeight = appIcon.intrinsicHeight

        composeTestRule.setContent {
            val navController = rememberNavController()
            AzHostActivityLayout(navController = navController) {
                // Empty rail
            }
        }

        val iconNode = composeTestRule.onNodeWithContentDescription("Toggle menu, showing App icon", useUnmergedTree = true)
        iconNode.assertExists()

        val bounds = iconNode.fetchSemanticsNode().size

        assertEquals(expectedWidth, bounds.width)
        assertEquals(expectedHeight, bounds.height)
    }
}
