package com.hereliesaz.aznavrail

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.Assert.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AzButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun azButton_renders_and_handles_click() {
        var clicked = false
        composeTestRule.setContent {
            AzButton(
                onClick = { clicked = true },
                text = "Button"
            )
        }

        composeTestRule.onNodeWithText("Button").assertIsDisplayed()
        composeTestRule.onNodeWithText("Button").performClick()
        assertEquals(true, clicked)
    }

    @Test
    fun azButton_disabled_state() {
        var clicked = false
        composeTestRule.setContent {
            AzButton(
                onClick = { clicked = true },
                text = "Disabled",
                enabled = false
            )
        }

        composeTestRule.onNodeWithText("Disabled").assertIsDisplayed()
        // Compose disabled buttons can still be "clicked" in tests but won't trigger onClick?
        // assertIsNotEnabled checks semantics
        composeTestRule.onNodeWithText("Disabled").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Disabled").performClick() // Should not trigger onClick
        assertEquals(false, clicked)
    }

    @Test
    fun azButton_loading_state() {
        // When loading, the text is still in the tree but transparent. AzLoad is present.
        // We can't easily assert transparency or AzLoad presence without test tags or specific content description.
        // But we can check if it compiles and runs without crash.
        composeTestRule.setContent {
            AzButton(
                onClick = {},
                text = "Loading",
                isLoading = true
            )
        }

        // Text node still exists (alpha 0)
        composeTestRule.onNodeWithText("Loading").assertIsDisplayed() // assertIsDisplayed checks if it's placed, not if visible/alpha > 0
        // We can inspect semantics or just rely on manual verification/visuals, but here we confirm it renders.

        // To be more precise, we could check if "loading..." text from AzLoad is present.
        composeTestRule.onNodeWithText("loading...").assertIsDisplayed()
    }

    @Test
    fun azButton_contentPadding() {
        // Check if content padding compiles. Hard to verify layout visually in unit test without screenshot.
        // This ensures the parameter is accepted and doesn't crash.
        composeTestRule.setContent {
            AzButton(
                onClick = {},
                text = "Padding",
                contentPadding = PaddingValues(16.dp)
            )
        }
        composeTestRule.onNodeWithText("Padding").assertIsDisplayed()
    }
}
