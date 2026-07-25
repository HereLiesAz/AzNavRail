package com.hereliesaz.aznavrail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
    fun azLoad_renders() {
        composeTestRule.setContent {
            AzLoad()
        }

        // The indicator is a morphing shape now, not a ring captioned "loading...". A spinner that
        // prints the word "loading" is two elements doing one element's job — and that word was the
        // only English sentence the core rail put in front of a user. Its identity for assistive
        // tech (and for this assertion) is its content description.
        composeTestRule.onNodeWithContentDescription(AzLoadContentDescription).assertIsDisplayed()
    }
}
