package com.hereliesaz.aznavrail.internal

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.hereliesaz.aznavrail.AzNavRailScopeImpl
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FooterTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `undock button should call onUndock but not onToggle`() {
        var onUndockCalled = false
        var onToggleCalled = false

        val scope = AzNavRailScopeImpl().apply {
            enableRailDragging = true
        }

        composeTestRule.setContent {
            Footer(
                appName = "TestApp",
                onToggle = { onToggleCalled = true },
                onUndock = { onUndockCalled = true },
                scope = scope,
                footerColor = Color.Red
            )
        }

        // Verify "Undock" is present
        composeTestRule.onNodeWithText("Undock").assertExists()

        // Click "Undock"
        composeTestRule.onNodeWithText("Undock").performClick()

        // Assertions
        assertTrue("onUndock should be called", onUndockCalled)
        assertFalse("onToggle should NOT be called for Undock", onToggleCalled)
    }

    @Test
    fun `about button should call onToggle`() {
        var onToggleCalled = false

        val scope = AzNavRailScopeImpl().apply {
            enableRailDragging = true
        }

        composeTestRule.setContent {
            Footer(
                appName = "TestApp",
                onToggle = { onToggleCalled = true },
                onUndock = { },
                scope = scope,
                footerColor = Color.Red
            )
        }

        // Click "About" (App Name acts as toggle)
        composeTestRule.onNodeWithText("TestApp").performClick()

        // Assertions
        assertTrue("onToggle should be called for App Name", onToggleCalled)
    }
}
