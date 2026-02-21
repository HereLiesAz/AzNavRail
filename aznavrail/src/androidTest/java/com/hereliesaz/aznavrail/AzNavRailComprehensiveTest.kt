package com.hereliesaz.aznavrail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AzNavRailComprehensiveTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testInitialSetup() {
        composeTestRule.setContent {
            val navController = rememberNavController()
            AzHostActivityLayout(navController = navController) {
                azRailItem(id = "home", text = "Home", onClick = {})
            }
        }

        composeTestRule.onNodeWithText("Home").assertIsDisplayed()
    }

    @Test
    fun testHostActivityLayoutRendering() {
        composeTestRule.setContent {
            val navController = rememberNavController()
            AzHostActivityLayout(navController = navController) {
                onscreen(Alignment.Center) {
                    Text("Main Content", modifier = Modifier.testTag("MainContent"))
                }
                azRailItem(id = "rail1", text = "Rail Item 1", onClick = {})
            }
        }

        composeTestRule.onNodeWithTag("MainContent").assertIsDisplayed()
        composeTestRule.onNodeWithText("Rail Item 1").assertIsDisplayed()
    }

    @Test
    fun testSafeZoneContentPlacement() {
        composeTestRule.setContent {
            val navController = rememberNavController()
            AzHostActivityLayout(navController = navController) {
                background(weight = 0) {
                    Box(modifier = Modifier.fillMaxSize().testTag("Background"))
                }
                onscreen(Alignment.TopStart) {
                    Text("Top Start Content", modifier = Modifier.testTag("TopStart"))
                }
            }
        }

        composeTestRule.onNodeWithTag("Background").assertIsDisplayed()
        composeTestRule.onNodeWithTag("TopStart").assertIsDisplayed()
    }

    @Test
    fun testRailItemClick() {
        var clicked = false
        composeTestRule.setContent {
            val navController = rememberNavController()
            AzHostActivityLayout(navController = navController) {
                azRailItem(id = "clickMe", text = "Click Me", onClick = { clicked = true })
            }
        }

        composeTestRule.onNodeWithText("Click Me").performClick()
        assert(clicked)
    }

    @Test
    fun testToggleItem() {
        var isChecked = false
        composeTestRule.setContent {
            val navController = rememberNavController()
            AzHostActivityLayout(navController = navController) {
                azRailToggle(
                    id = "toggle",
                    isChecked = isChecked,
                    toggleOnText = "On",
                    toggleOffText = "Off",
                    onClick = { isChecked = !isChecked }
                )
            }
        }

        composeTestRule.onNodeWithText("Off").performClick()
        assert(isChecked)
    }

    @Test
    fun testCyclerItem() {
        val options = listOf("A", "B", "C")
        val selected = "A"
        composeTestRule.setContent {
            val navController = rememberNavController()
            AzHostActivityLayout(navController = navController) {
                azRailCycler(
                    id = "cycler",
                    options = options,
                    selectedOption = selected,
                    onClick = { /* Delayed action */ }
                )
            }
        }

        composeTestRule.onNodeWithText("A").assertIsDisplayed()
        composeTestRule.onNodeWithText("A").performClick()
        composeTestRule.onNodeWithText("B").assertIsDisplayed()
    }

    @Test
    fun testRailExpansion() {
        composeTestRule.setContent {
            val navController = rememberNavController()
            AzHostActivityLayout(navController = navController) {
                azRailItem(id = "rail", text = "Rail", onClick = {})
                azMenuItem(id = "menu", text = "Menu Item", onClick = {})
            }
        }

        composeTestRule.onAllNodesWithText("Menu Item").assertCountEquals(0)
    }

    @Test
    fun testHostItemExpansion() {
        composeTestRule.setContent {
            val navController = rememberNavController()
            AzHostActivityLayout(navController = navController) {
                azRailHostItem(id = "host", text = "Host Group", onClick = {})
                azRailSubItem(id = "sub1", hostId = "host", text = "Sub Item 1", onClick = {})
            }
        }

        composeTestRule.onNodeWithText("Host Group").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Sub Item 1").assertCountEquals(0)

        composeTestRule.onNodeWithText("Host Group").performClick()
        composeTestRule.onNodeWithText("Sub Item 1").assertIsDisplayed()

        composeTestRule.onNodeWithText("Host Group").performClick()
        composeTestRule.onAllNodesWithText("Sub Item 1").assertCountEquals(0)
    }

    @Test(expected = IllegalStateException::class)
    fun testMissingHostError() {
        composeTestRule.setContent {
            @OptIn(AzStrictLayout::class)
            AzNavRail {
                azRailItem(id = "home", text = "Home", onClick = {})
            }
        }
    }
}
