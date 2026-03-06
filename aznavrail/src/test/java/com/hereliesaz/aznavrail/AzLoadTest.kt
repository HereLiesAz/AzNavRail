package com.hereliesaz.aznavrail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AzLoadTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun azLoad_renders_text() {
        composeTestRule.setContent {
            AzLoad()
        }

        composeTestRule.onNodeWithText("loading...").assertIsDisplayed()
    }
}
