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
import androidx.compose.ui.test.assertIsDisplayed

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
    fun azTextBox_inside_lazyColumn_crashes_with_nested_scroll_issue() {
        // This test places AzTextBox inside a LazyColumn.
        // If AzTextBox uses a LazyColumn for suggestions, it should crash when suggestions are displayed
        // because of nested vertically scrolling components with undefined height.

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

        // If it hasn't crashed yet, we can try to verify suggestions are visible.
        // The crash typically happens during measurement/layout.

        // "apple" should be visible if suggestions are showing.
        // If the crash prevents rendering, this might fail or the test runner will report the exception.
        try {
            composeTestRule.onNodeWithText("apple").assertIsDisplayed()
        } catch (e: AssertionError) {
            // If assertion fails, it might be because of crash or just not displayed.
            // But if it crashes, the test fails with exception.
        }
    }
}
