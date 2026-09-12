package com.hereliesaz.aznavrail.util.text

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AutoSizeTextTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testAutoSizeText_rendersCorrectly() {
        composeTestRule.setContent {
            Box(modifier = Modifier.size(200.dp, 100.dp)) {
                AutoSizeText(
                    text = "Hello, World!",
                    minTextSize = 10.sp,
                    maxTextSize = 100.sp,
                )
            }
        }

        composeTestRule.onNodeWithText("Hello, World!").assertIsDisplayed()
    }

    @Test
    fun testAutoSizeText_suggestedFontSizes() {
        composeTestRule.setContent {
            Box(modifier = Modifier.size(200.dp, 100.dp)) {
                AutoSizeText(
                    text = "Hello, World!",
                    suggestedFontSizes = listOf(10.sp, 20.sp, 30.sp),
                    suggestedFontSizesStatus = SuggestedFontSizesStatus.VALID
                )
            }
        }

        composeTestRule.onNodeWithText("Hello, World!").assertIsDisplayed()
    }

    @Test
    fun testAutoSizeText_textTooLarge() {
        composeTestRule.setContent {
            Box(modifier = Modifier.size(10.dp, 10.dp)) {
                AutoSizeText(
                    text = "A very long text that definitely won't fit in 10dp x 10dp at 50sp",
                    minTextSize = 50.sp,
                    maxTextSize = 100.sp,
                )
            }
        }
    }

    @Test
    fun testSuggestedFontSizesStatus_valid() {
        val list = listOf(10.sp, 20.sp, 30.sp)
        val status = with(SuggestedFontSizesStatus.Companion) { list.suggestedFontSizesStatus }
        assertEquals(SuggestedFontSizesStatus.VALID, status)
    }

    @Test
    fun testSuggestedFontSizesStatus_invalid_unsorted() {
        val list = listOf(30.sp, 20.sp, 10.sp)
        val status = with(SuggestedFontSizesStatus.Companion) { list.suggestedFontSizesStatus }
        assertEquals(SuggestedFontSizesStatus.INVALID, status)
    }

    @Test
    fun testValidSuggestedFontSizes_returnsSorted() {
        val list = listOf(30.sp, 10.sp, 20.sp)
        val validList = with(SuggestedFontSizesStatus.Companion) { list.validSuggestedFontSizes }
        assertEquals(listOf(10.sp, 20.sp, 30.sp), validList)
    }

    @Test
    fun testValidSuggestedFontSizes_empty_returnsNull() {
        val list = emptyList<androidx.compose.ui.unit.TextUnit>()
        val validList = with(SuggestedFontSizesStatus.Companion) { list.validSuggestedFontSizes }
        assertNull(validList)
    }

    @Test
    fun testFindElectedValue() {
        val list = listOf(10, 20, 30, 40, 50)
        val result = list.findElectedValue { it > 35 }
        assertEquals(30, result)
    }
}
