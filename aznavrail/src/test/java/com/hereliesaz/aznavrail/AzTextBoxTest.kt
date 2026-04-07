package com.hereliesaz.aznavrail

import android.content.Context
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.hereliesaz.aznavrail.util.HistoryManager
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.assertIsDisplayed
import org.junit.Assert.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AzTextBoxTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        HistoryManager.init(context, 5)
        HistoryManager.resetForTesting()
        HistoryManager.init(context, 5) // Re-init
        // Force synchronous scope for file IO to avoid leaks, though we don't care about file saving here
        HistoryManager.coroutineScope = CoroutineScope(Dispatchers.Unconfined)

        // Add entries
        HistoryManager.addEntry("apple", "test")
        HistoryManager.addEntry("apricot", "test")
    }

    @Test
    fun azTextBox_inside_lazyColumn_renders_correctly_without_crash() {
        // This test places AzTextBox inside a LazyColumn.
        // It verifies that the autocomplete Popup dropdown intelligently measures
        // its own bounds and parent's width, breaking free from contextual constraints 
        // without causing nested scroll exception crashes.
        composeTestRule.setContent {
            LazyColumn {
                item {
                    AzTextBox(
                        value = "a",
                        onValueChange = {},
                        historyContext = "test", // Should match "apple", "apricot"
                        onSubmit = {}
                    )
                }
            }
        }

        // Wait for suggestions to appear
        composeTestRule.waitForIdle()

        // "apple" should be visible if suggestions are showing.
        composeTestRule.onNodeWithText("apple").assertIsDisplayed()
    }

    @Test
    fun azTextBox_uncontrolledMode_updatesInternalText() {
        var capturedValue = ""
        composeTestRule.setContent {
            AzTextBox(
                value = null, // uncontrolled mode
                onValueChange = { capturedValue = it },
                onSubmit = {},
                hint = "Hint"
            )
        }

        composeTestRule.onNodeWithTag("Hint").performTextInput("Hello")

        // Assert text displayed
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Hello").assertIsDisplayed()

        // Assert onValueChange was called correctly
        assertEquals("Hello", capturedValue)
    }

    @Test
    fun azTextBox_controlledMode_doesNotUpdateInternalText() {
        var capturedValue = ""
        composeTestRule.setContent {
            AzTextBox(
                value = "Initial", // controlled mode
                onValueChange = { capturedValue = it },
                onSubmit = {},
                hint = "Hint"
            )
        }

        composeTestRule.onNodeWithText("Initial").performTextReplacement("Updated")

        // In controlled mode, internal text shouldn't be updated by AzTextBox itself,
        // so the visual state should remain "Initial" because we aren't updating `value`
        // in our `onValueChange` callback in this test scope.
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Initial").assertIsDisplayed()

        // However, the callback SHOULD be fired with the expected new text
        assertEquals("Updated", capturedValue)
    }
}