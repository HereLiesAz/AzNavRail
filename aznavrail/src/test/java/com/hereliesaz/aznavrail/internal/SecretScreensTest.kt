package com.hereliesaz.aznavrail.internal

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.hasText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.test.runTest

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], instrumentedPackages = ["androidx.loader.content"])
class SecretScreensTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun secLocViewerDialog_showsError_onSyncFailure() {
        composeTestRule.setContent {
            val showDialog = SecretScreens(secLoc = "my_secret", secLocPort = 10203)
            LaunchedEffect(Unit) {
                showDialog()
            }
        }

        // Wait for idle to let the dialog render
        composeTestRule.waitForIdle()

        // 1. Enter credentials in SecretCredentialsDialog
        composeTestRule.onNodeWithText("Enter credentials...").performTextInput("my_secret")
        composeTestRule.onNodeWithText("Unlock").performClick()

        composeTestRule.waitForIdle()

        // 2. Select Viewer mode in ModeSelectionDialog
        composeTestRule.onNodeWithText("Run as Viewer (Client)").performClick()

        composeTestRule.waitForIdle()

        // 3. Enter an IP we can mock or wait for it to fail.
        composeTestRule.onNodeWithText("Enter Source IP (e.g. 192.168.1.5)").performTextInput("255.255.255.255")

        // 4. Click Download Log
        composeTestRule.onNodeWithText("Download Log").performClick()

        // Since it's a coroutine, advance time.
        // The fetchLogs does not have a hard timeout for connection without explicit setup, but network on robolectric throws right away mostly or we can advance the main clock.
        composeTestRule.mainClock.advanceTimeBy(5000)
        composeTestRule.waitForIdle()

        // 5. Wait for the error message to appear. The text should start with "Sync failed:"
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodes(hasText("Sync failed:", substring = true)).fetchSemanticsNodes().isNotEmpty()
        }

        // 6. Assert that the specific error message node is displayed
        composeTestRule.onNode(hasText("Sync failed:", substring = true)).assertIsDisplayed()
    }
}
