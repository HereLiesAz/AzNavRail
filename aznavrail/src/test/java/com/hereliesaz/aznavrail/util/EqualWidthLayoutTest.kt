package com.hereliesaz.aznavrail.util

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class EqualWidthLayoutTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testEqualWidth() {
        var width1 = 0
        var width2 = 0
        var width3 = 0

        composeTestRule.setContent {
            EqualWidthLayout {
                Box(modifier = Modifier.size(50.dp, 20.dp).onGloballyPositioned { width1 = it.size.width })
                Box(modifier = Modifier.size(100.dp, 20.dp).onGloballyPositioned { width2 = it.size.width })
                Box(modifier = Modifier.size(75.dp, 20.dp).onGloballyPositioned { width3 = it.size.width })
            }
        }

        composeTestRule.waitForIdle()

        val expectedWidth = with(composeTestRule.density) { 100.dp.roundToPx() }
        assertEquals(expectedWidth, width1)
        assertEquals(expectedWidth, width2)
        assertEquals(expectedWidth, width3)
    }

    @Test
    fun testVerticalSpacing() {
        var y1 = 0f
        var y2 = 0f
        var height1 = 0
        val spacing = 10.dp

        composeTestRule.setContent {
            EqualWidthLayout(verticalSpacing = spacing) {
                Box(modifier = Modifier.size(100.dp, 50.dp).onGloballyPositioned {
                    y1 = it.positionInParent().y
                    height1 = it.size.height
                })
                Box(modifier = Modifier.size(100.dp, 50.dp).onGloballyPositioned {
                    y2 = it.positionInParent().y
                })
            }
        }

        composeTestRule.waitForIdle()

        val spacingPx = with(composeTestRule.density) { spacing.toPx() }
        assertEquals(0f, y1, 0.1f)
        // In EqualWidthLayout, y is rounded to int when placing: placeable.place(0, y.roundToInt())
        val expectedY2 = (height1 + spacingPx).roundToInt().toFloat()
        assertEquals(expectedY2, y2, 1.0f) // Using a bit more tolerance or roundToInt
    }

    @Test
    fun testEmptyLayout() {
        composeTestRule.setContent {
            EqualWidthLayout {
                // Empty
            }
        }
        composeTestRule.waitForIdle()
        // Should not crash
    }

    @Test
    fun testSingleChild() {
        var width = 0
        val childWidth = 80.dp

        composeTestRule.setContent {
            EqualWidthLayout {
                Box(modifier = Modifier.size(childWidth, 20.dp).onGloballyPositioned { width = it.size.width })
            }
        }

        composeTestRule.waitForIdle()

        val expectedWidth = with(composeTestRule.density) { childWidth.roundToPx() }
        assertEquals(expectedWidth, width)
    }
}
