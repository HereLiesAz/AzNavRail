package com.hereliesaz.aznavrail.internal

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLog
import java.text.SimpleDateFormat
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class SecretScreensTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun historyList_itemClick_handlesMapException() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        var exceptionThrown = false

        val mockContext = object : ContextWrapper(baseContext) {
            override fun startActivity(intent: Intent?) {
                exceptionThrown = true
                throw ActivityNotFoundException("Simulated exception")
            }
        }

        val entries = listOf(
            SecLocEntry(
                timestamp = 1600000000000L,
                lat = 10.0,
                lng = 20.0,
                provider = "gps"
            )
        )
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides mockContext) {
                HistoryList(
                    history = entries,
                    dateFormatter = dateFormatter
                )
            }
        }

        // The item displays the provider
        composeTestRule.onNodeWithText("Provider: gps").performClick()

        // Test should pass without crashing, and our mock should have been called
        assertTrue("Mock context startActivity should have been called", exceptionThrown)

        // Verify that the error log was printed
        val logs = ShadowLog.getLogsForTag("SecretScreens")
        val hasErrorLog = logs.any { log ->
            log.type == android.util.Log.ERROR && log.msg == "Could not open map"
        }
        assertTrue("Expected error log 'Could not open map' for SecretScreens", hasErrorLog)
    }
}
