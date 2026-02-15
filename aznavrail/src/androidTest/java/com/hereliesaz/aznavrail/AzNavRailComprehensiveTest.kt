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

        // Initially collapsed, menu item should not be visible
        composeTestRule.onAllNodesWithText("Menu Item").assertCountEquals(0)
    }

    @Test
    fun testNestedRail() {
        composeTestRule.setContent {
            val navController = rememberNavController()
            AzHostActivityLayout(navController = navController) {
                azNestedRail(id = "nested", text = "Nested Parent") {
                    azRailItem(id = "child", text = "Child Item", onClick = {})
                }
            }
        }

        // Parent should be visible
        composeTestRule.onNodeWithText("Nested Parent").assertIsDisplayed()

        // Child should not be visible initially (it's in a popup)
        composeTestRule.onAllNodesWithText("Child Item").assertCountEquals(0)

        // Click parent to open nested rail
        composeTestRule.onNodeWithText("Nested Parent").performClick()

        // Child should now be visible
        composeTestRule.onNodeWithText("Child Item").assertIsDisplayed()
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

        // Host should be visible
        composeTestRule.onNodeWithText("Host Group").assertIsDisplayed()

        // Sub item should NOT be visible initially
        composeTestRule.onAllNodesWithText("Sub Item 1").assertCountEquals(0)

        // Click host to expand
        composeTestRule.onNodeWithText("Host Group").performClick()

        // Sub item should now be visible
        composeTestRule.onNodeWithText("Sub Item 1").assertIsDisplayed()

        // Click host to collapse
        composeTestRule.onNodeWithText("Host Group").performClick()

        // Sub item should not be visible again
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
