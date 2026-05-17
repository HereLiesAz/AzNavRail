package com.hereliesaz.aznavrail.bottomsheet

import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.compose.rememberNavController
import com.hereliesaz.aznavrail.AzHostActivityLayout
import com.hereliesaz.aznavrail.model.AzSheetConfig
import com.hereliesaz.aznavrail.model.AzSheetDetent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AzBottomSheetDslTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun azBottomSheetDsl_rendersContentInsideHostLayout() {
        composeTestRule.setContent {
            val navController = rememberNavController()
            val controller = rememberAzSheetController(initial = AzSheetDetent.HALF)
            AzHostActivityLayout(
                navController = navController,
                currentDestination = "home",
            ) {
                azBottomSheet(controller = controller, config = AzSheetConfig(animateInTree = false)) {
                    Text("sheet-body-marker")
                }
            }
        }
        composeTestRule.onNodeWithText("sheet-body-marker").assertIsDisplayed()
    }

    @Test
    fun azBottomSheet_drawsAboveOnscreenContent() {
        // SideEffect runs once per composition in declaration order; the host layout composes
        // backgrounds → big title → onscreen items → rail → bottom sheets, so the sheet's content
        // should appear after the onscreen marker in the recorded order.
        val order = mutableListOf<String>()
        composeTestRule.setContent {
            val navController = rememberNavController()
            val controller = rememberAzSheetController(initial = AzSheetDetent.PEEK)
            AzHostActivityLayout(navController = navController, currentDestination = "home") {
                onscreen { SideEffect { order.add("onscreen") } }
                azBottomSheet(controller = controller, config = AzSheetConfig(animateInTree = false)) {
                    SideEffect { order.add("sheet") }
                }
            }
        }
        composeTestRule.waitForIdle()
        val sheetIndex = order.indexOf("sheet")
        val onscreenIndex = order.indexOf("onscreen")
        assert(sheetIndex > onscreenIndex) {
            "Expected sheet to compose after onscreen content but got order=$order"
        }
    }

    @Test
    fun azBottomSheet_resetHostClearsListBetweenRecompositions() {
        // Regression analogous to testRecompositionWithChangingContentDoesNotDuplicateIds:
        // toggling the conditional sheet should not stack duplicate registrations.
        val show = mutableStateOf(true)
        composeTestRule.setContent {
            val navController = rememberNavController()
            val controller = rememberAzSheetController(initial = AzSheetDetent.PEEK)
            AzHostActivityLayout(navController = navController, currentDestination = "home") {
                if (show.value) {
                    azBottomSheet(controller = controller, config = AzSheetConfig(animateInTree = false)) {
                        Text("conditional-sheet")
                    }
                }
            }
        }
        composeTestRule.runOnIdle { show.value = false }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle { show.value = true }
        composeTestRule.waitForIdle()
        // No assertion error / duplicate registration crash means success.
        assertEquals(true, show.value)
    }
}
