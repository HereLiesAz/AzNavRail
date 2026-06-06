package com.hereliesaz.aznavrail

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression coverage for rendering vector graphics as button content.
 *
 * Previously an [androidx.compose.ui.graphics.vector.ImageVector] (e.g. `Icons.Default.Delete`)
 * fell through to Coil's `rememberAsyncImagePainter`, which throws
 * `IllegalArgumentException: Unsupported type: ImageVector`, crashing any rail/nested-rail button
 * whose `content` was a vector. These tests ensure the shared content renderer composes such
 * content without throwing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ItemContentRendererTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun azNavRailButton_rendersImageVectorContent_withoutThrowing() {
        composeTestRule.setContent {
            AzNavRailButton(
                onClick = {},
                text = "x",
                itemContent = Icons.Default.Delete,
            )
        }
        composeTestRule.waitForIdle()
        assertTrue(true)
    }

    @Test
    fun azNavRailButton_rendersPainterContent_withoutThrowing() {
        composeTestRule.setContent {
            AzNavRailButton(
                onClick = {},
                text = "x",
                itemContent = ColorPainter(Color.Red),
            )
        }
        composeTestRule.waitForIdle()
        assertTrue(true)
    }

    @Test
    fun azRailItem_withImageVectorContent_rendersOnMainRail_withoutThrowing() {
        composeTestRule.setContent {
            AzHostActivityLayout(
                navController = rememberNavController()
            ) {
                azRailItem(id = "delete", text = "Delete", content = Icons.Default.Delete)
            }
        }
        composeTestRule.waitForIdle()
        assertTrue(true)
    }
}
