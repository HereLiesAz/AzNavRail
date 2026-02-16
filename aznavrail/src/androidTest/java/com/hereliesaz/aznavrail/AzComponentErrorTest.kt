package com.hereliesaz.aznavrail

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AzComponentErrorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun azTextBox_withBothMultilineAndSecret_throwsException() {
        try {
            composeTestRule.setContent {
                AzTextBox(
                    hint = "Error",
                    multiline = true,
                    secret = true,
                    onSubmit = {}
                )
            }
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // Success
        }
    }

    @Test
    fun azNavRail_invalidConfig_throwsException() {
        try {
            val scope = AzNavRailScopeImpl()
            scope.azTheme(expandedWidth = 50.dp, collapsedWidth = 100.dp, showFooter = true)
            fail("Expected IllegalArgumentException for expandedWidth < collapsedWidth")
        } catch (e: IllegalArgumentException) {
            // Success
        }
    }
}
