package com.hereliesaz.aznavrail

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.remember
import com.hereliesaz.aznavrail.model.AzNavItem
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ItemBoundsCacheTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testItemBoundsCachePopulatedCollapsed() {
        var scope: AzNavRailScopeImpl? = null

        composeTestRule.setContent {
            val navController = rememberNavController()
            AzHostActivityLayout(navController = navController) {
                // Capture scope
                if (this is AzNavHostScopeImpl) {
                    scope = this.getRailScopeImpl()
                }
                azRailItem(id = "item1", text = "Item 1", onClick = {})
                onscreen { }
            }
        }

        composeTestRule.waitForIdle()

        // Verify cache is populated
        val bounds = scope?.itemBoundsCache?.get("item1")
        assertTrue("Bounds for item1 should be cached but was null", bounds != null)
    }

    @Test
    fun testItemBoundsCachePopulatedExpanded() {
        var scope: AzNavRailScopeImpl? = null

        composeTestRule.setContent {
            val navController = rememberNavController()
            AzHostActivityLayout(
                navController = navController,
                initiallyExpanded = true // Start expanded
            ) {
                if (this is AzNavHostScopeImpl) {
                    scope = this.getRailScopeImpl()
                }
                // Use menu item (only visible when expanded)
                azMenuItem(id = "menu1", text = "Menu 1", onClick = {})
                onscreen { }
            }
        }

        composeTestRule.waitForIdle()

        // Verify cache is populated for menu item
        val bounds = scope?.itemBoundsCache?.get("menu1")
        assertTrue("Bounds for menu1 should be cached when expanded but was null", bounds != null)
    }
}
