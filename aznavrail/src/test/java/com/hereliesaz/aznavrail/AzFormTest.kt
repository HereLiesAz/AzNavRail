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

}
