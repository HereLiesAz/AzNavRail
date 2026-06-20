package com.hereliesaz.aznavrail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.model.AzDropdownDesign
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
    fun app_icon_trigger_renders_and_toggles_the_panel() {
        composeTestRule.setContent {
            AzDropdownMenu {
                azItem("Settings") { }
            }
        }

        // Closed initially.
        composeTestRule.onNodeWithText("Settings").assertDoesNotExist()

        // Tap the app-icon trigger (labeled "Menu") to unfold.
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

    @Test
    fun rail_design_via_azConfig_renders_items_and_fires_clicks() {
        var clicked = false
        composeTestRule.setContent {
            AzDropdownMenu {
                azConfig(design = AzDropdownDesign.RAIL)
                azItem("Home") { clicked = true }
            }
        }

        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        composeTestRule.onNodeWithText("Home").assertIsDisplayed()
        composeTestRule.onNodeWithText("Home").performClick()
        assertTrue(clicked)
    }

    @Test
    fun configurable_app_icon_shape_and_size_still_open_the_panel() {
        composeTestRule.setContent {
            AzDropdownMenu {
                azConfig(
                    headerIconShape = com.hereliesaz.aznavrail.model.AzHeaderIconShape.SQUARE,
                    headerIconSize = 72.dp
                )
                azItem("Settings") { }
            }
        }

        composeTestRule.onNodeWithText("Settings").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun menu_design_shows_the_footer_unless_disabled() {
        composeTestRule.setContent {
            AzDropdownMenu(expanded = true) {
                azItem("Profile") { }
            }
        }
        // The MENU design carries the rail's footer.
        composeTestRule.onNodeWithText("About").assertIsDisplayed()
        composeTestRule.onNodeWithText("Feedback").assertIsDisplayed()
        composeTestRule.onNodeWithText("@HereLiesAz").assertIsDisplayed()
    }

    @Test
    fun footer_is_hidden_when_showFooter_is_false() {
        composeTestRule.setContent {
            AzDropdownMenu(expanded = true) {
                azConfig(showFooter = false)
                azItem("Profile") { }
            }
        }
        composeTestRule.onNodeWithText("Feedback").assertDoesNotExist()
    }

    @Test
    fun route_navigates_the_nav_controller() {
        lateinit var navController: androidx.navigation.NavHostController
        composeTestRule.setContent {
            navController = androidx.navigation.compose.rememberNavController()
            androidx.navigation.compose.NavHost(navController, startDestination = "start") {
                androidx.navigation.compose.composable("start") {
                    AzDropdownMenu(navController = navController) {
                        azItem("Home", route = "home") { }
                    }
                }
                androidx.navigation.compose.composable("home") {
                    androidx.compose.material3.Text("home screen")
                }
            }
        }

        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        composeTestRule.onNodeWithText("Home").performClick()
        composeTestRule.runOnIdle {
            assertEquals("home", navController.currentDestination?.route)
        }
    }
}
