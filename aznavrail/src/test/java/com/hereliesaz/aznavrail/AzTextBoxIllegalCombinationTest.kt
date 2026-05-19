package com.hereliesaz.aznavrail

import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifies the `require()` guard inside [AzTextBox] that forbids `multiline = true` + `secret = true`.
 *
 * Visually, a masked password field that wraps over multiple lines is incoherent: the
 * PasswordVisualTransformation collapses to bullets so multi-line input is invisible. The library
 * therefore fails fast at composition time and the error message must explain *which* parameters
 * conflict and *how* to recover (pick one).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AzTextBoxIllegalCombinationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun multilineAndSecret_throwsWithEducationalMessage() {
        val thrown = try {
            composeTestRule.setContent {
                AzTextBox(
                    multiline = true,
                    secret = true,
                    onSubmit = {},
                )
            }
            null
        } catch (e: Throwable) {
            // Compose wraps composition failures, so we unwrap to find the original IllegalArgumentException.
            var cause: Throwable? = e
            while (cause != null && cause !is IllegalArgumentException) cause = cause.cause
            cause ?: e
        }

        assertNotNull(
            "AzTextBox(multiline = true, secret = true) must throw at composition time — the " +
                "PasswordVisualTransformation + multi-line layout combination is incoherent. " +
                "Verify the `require(!multiline || !secret)` line at the top of AzTextBox.",
            thrown,
        )

        val msg = thrown!!.message.orEmpty()
        assertTrue(
            "AzTextBox error message must name the `multiline` parameter so the developer can locate it. Got: $msg",
            msg.contains("multiline"),
        )
        assertTrue(
            "AzTextBox error message must name the `secret` parameter so the developer can locate it. Got: $msg",
            msg.contains("secret"),
        )
        assertTrue(
            "AzTextBox error message must tell the caller to pick one of the two parameters (so " +
                "the fix is obvious without reading the source). Acceptable phrasing: 'Pick one' / " +
                "'pick one'. Got: $msg",
            msg.contains("pick one", ignoreCase = true) || msg.contains("Pick one"),
        )
    }

    @Test
    fun multilineWithoutSecret_doesNotThrow() {
        try {
            composeTestRule.setContent {
                AzTextBox(
                    multiline = true,
                    secret = false,
                    onSubmit = {},
                )
            }
        } catch (e: Throwable) {
            fail(
                "AzTextBox(multiline = true, secret = false) must compose without error — the " +
                    "guard `require(!multiline || !secret)` should only fire when BOTH are true. " +
                    "Got: ${e.message}",
            )
        }
    }

    @Test
    fun secretWithoutMultiline_doesNotThrow() {
        try {
            composeTestRule.setContent {
                AzTextBox(
                    multiline = false,
                    secret = true,
                    onSubmit = {},
                )
            }
        } catch (e: Throwable) {
            fail(
                "AzTextBox(multiline = false, secret = true) must compose without error — the " +
                    "guard `require(!multiline || !secret)` should only fire when BOTH are true. " +
                    "Got: ${e.message}",
            )
        }
    }
}
