package com.hereliesaz.aznavrail

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.Assert.assertEquals
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.assertIsFocused

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AzFormTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun azForm_collects_data_and_submits() {
        var submittedData: Map<String, String>? = null
        composeTestRule.setContent {
            AzForm(
                formName = "testForm",
                onSubmit = { submittedData = it }
            ) {
                entry("name", "Name")
                entry("email", "Email")
            }
        }

        // Initially empty
        composeTestRule.onNodeWithText("Submit").performClick()
        assertEquals("", submittedData?.get("name"))
        assertEquals("", submittedData?.get("email"))

        // Enter text into fields
        composeTestRule.onNodeWithTag("Name").performTextInput("John")
        composeTestRule.onNodeWithTag("Email").performTextInput("john@example.com")

        composeTestRule.onNodeWithText("Submit").performClick()

        assertEquals("John", submittedData?.get("name"))
        assertEquals("john@example.com", submittedData?.get("email"))
    }

    @Test
    fun azForm_pressingNextOnAnEarlierField_movesFocusToTheNextField() {
        // Regression test: AzForm's own ImeAction "is this unset" check compared against
        // ImeAction.Default, but KeyboardOptions.Default.imeAction is actually
        // ImeAction.Unspecified — so this Next-to-advance-focus wiring never activated.
        composeTestRule.setContent {
            AzForm(formName = "focusChainForm", onSubmit = {}) {
                entry("name", "Name")
                entry("email", "Email")
            }
        }

        composeTestRule.onNodeWithTag("Name").performTextInput("Jane")
        composeTestRule.onNodeWithTag("Name").performImeAction()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("Email").assertIsFocused()
    }

    @Test
    fun azForm_pressingSendOnTheLastField_submitsTheForm() {
        // Regression test: same ImeAction.Default/Unspecified mismatch as above meant the
        // keyboard's Send action on the last field never fired AzForm's onSubmit.
        var submittedData: Map<String, String>? = null
        composeTestRule.setContent {
            AzForm(formName = "keyboardSubmitForm", onSubmit = { submittedData = it }) {
                entry("name", "Name")
                entry("email", "Email")
            }
        }

        composeTestRule.onNodeWithTag("Name").performTextInput("Jane")
        composeTestRule.onNodeWithTag("Email").performTextInput("jane@example.com")
        composeTestRule.onNodeWithTag("Email").performImeAction()
        composeTestRule.waitForIdle()

        assertEquals("Jane", submittedData?.get("name"))
        assertEquals("jane@example.com", submittedData?.get("email"))
    }
}
