package com.hereliesaz.aznavrail

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AzFormExpandedTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testAzFormExpandedInteraction() {
        composeTestRule.setContent {
            AzForm(formName = "login_form", onSubmit = {}) {
                entry(entryName = "username", hint = "Enter Username")
            }
        }

        composeTestRule.onNodeWithText("Enter Username", useUnmergedTree = true).assertExists()
    }
}
