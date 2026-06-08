package com.hereliesaz.aznavrail.internal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.bottomsheet.AzSheetController
import com.hereliesaz.aznavrail.model.AzSheetDetent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AzSheetGesturesTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun downwardDrag_stepsDownOneDetent_notStraightToHidden() {
        val controller = AzSheetController(initial = AzSheetDetent.FULL)
        composeTestRule.setContent {
            val density = LocalDensity.current
            Box(
                Modifier
                    .size(200.dp)
                    .testTag("drag")
                    .azSheetVerticalDrag(controller, density, thresholdDp = 24.dp),
            )
        }

        composeTestRule.onNodeWithTag("drag").performTouchInput { swipeDown() }
        composeTestRule.waitForIdle()

        assertEquals(
            "A downward drag from FULL must descend exactly one detent (FULL -> HALF), mirroring " +
                "the up-drag's stepUp — azSheetVerticalDrag must call controller.stepDown(), " +
                "not snapTo(HIDDEN).",
            AzSheetDetent.HALF,
            controller.detent,
        )
    }

    @Test
    fun upwardDrag_stepsUpOneDetent() {
        val controller = AzSheetController(initial = AzSheetDetent.PEEK)
        composeTestRule.setContent {
            val density = LocalDensity.current
            Box(
                Modifier
                    .size(200.dp)
                    .testTag("drag")
                    .azSheetVerticalDrag(controller, density, thresholdDp = 24.dp),
            )
        }

        composeTestRule.onNodeWithTag("drag").performTouchInput { swipeUp() }
        composeTestRule.waitForIdle()

        assertEquals(
            "An upward drag from PEEK must ascend exactly one detent (PEEK -> HALF).",
            AzSheetDetent.HALF,
            controller.detent,
        )
    }
}
