package com.hereliesaz.aznavrail

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class AzButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun azButton_displaysCorrectText() {
        val text = "Click me"
        composeTestRule.setContent {
            AzButton(onClick = {}, text = text)
        }
        composeTestRule.onNodeWithText(text).assertIsDisplayed()
    }

    @Test
    fun azToggle_switchesTextOnClick() {
        val textOn = "On"
        val textOff = "Off"
        composeTestRule.setContent {
            val (isOn, setIsOn) = remember { mutableStateOf(false) }
            AzToggle(
                textWhenOn = textOn,
                textWhenOff = textOff,
                isOn = isOn,
                onClick = { setIsOn(!isOn) }
            )
        }

        composeTestRule.onNodeWithText(textOff).assertIsDisplayed()
        composeTestRule.onNodeWithText(textOff).performClick()
        composeTestRule.onNodeWithText(textOn).assertIsDisplayed()
    }

    @Test
    fun azCycler_cyclesThroughOptions() {
        val option1 = "Option 1"
        val option2 = "Option 2"
        val option3 = "Option 3"
        composeTestRule.setContent {
            AzCycler(
                options = arrayOf(option1, option2, option3),
                onOptionSelected = {}
            )
        }

        composeTestRule.onNodeWithText(option1).assertIsDisplayed()
        composeTestRule.onNodeWithText(option1).performClick()
        composeTestRule.onNodeWithText(option2).assertIsDisplayed()
        composeTestRule.onNodeWithText(option2).performClick()
        composeTestRule.onNodeWithText(option3).assertIsDisplayed()
        composeTestRule.onNodeWithText(option3).performClick()
        composeTestRule.onNodeWithText(option1).assertIsDisplayed()
    }
}
