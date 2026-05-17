package com.hereliesaz.aznavrail.bottomsheet

import com.hereliesaz.aznavrail.model.AzSheetDetent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AzSheetControllerTest {

    @Test
    fun stepUp_progressesThroughAllDetents_thenClampsAtFull() {
        val c = AzSheetController(initial = AzSheetDetent.HIDDEN)
        assertEquals(AzSheetDetent.HIDDEN, c.detent)
        c.stepUp(); assertEquals(AzSheetDetent.PEEK, c.detent)
        c.stepUp(); assertEquals(AzSheetDetent.HALF, c.detent)
        c.stepUp(); assertEquals(AzSheetDetent.FULL, c.detent)
        c.stepUp(); assertEquals(AzSheetDetent.FULL, c.detent)
    }

    @Test
    fun stepDown_progressesThroughAllDetents_thenClampsAtHidden() {
        val c = AzSheetController(initial = AzSheetDetent.FULL)
        c.stepDown(); assertEquals(AzSheetDetent.HALF, c.detent)
        c.stepDown(); assertEquals(AzSheetDetent.PEEK, c.detent)
        c.stepDown(); assertEquals(AzSheetDetent.HIDDEN, c.detent)
        c.stepDown(); assertEquals(AzSheetDetent.HIDDEN, c.detent)
    }

    @Test
    fun snapTo_jumpsDirectlyToTargetAndEmitsOnFlow() = runTest {
        val c = AzSheetController(initial = AzSheetDetent.HIDDEN)
        c.snapTo(AzSheetDetent.FULL)
        assertEquals(AzSheetDetent.FULL, c.detent)
        assertEquals(AzSheetDetent.FULL, c.detentFlow.value)
    }

    @Test
    fun disablingForcesHiddenAndBlocksStepUp() {
        val c = AzSheetController(initial = AzSheetDetent.HALF)
        c.isEnabled = false
        assertEquals(AzSheetDetent.HIDDEN, c.detent)
        c.stepUp()
        assertEquals(AzSheetDetent.HIDDEN, c.detent)
    }

    @Test
    fun detentFlowReflectsAllMutations() = runTest {
        val c = AzSheetController(initial = AzSheetDetent.HIDDEN)
        assertEquals(AzSheetDetent.HIDDEN, c.detentFlow.value)
        c.stepUp()
        assertEquals(AzSheetDetent.PEEK, c.detentFlow.value)
        c.stepUp()
        assertEquals(AzSheetDetent.HALF, c.detentFlow.value)
        c.snapTo(AzSheetDetent.HIDDEN)
        assertEquals(AzSheetDetent.HIDDEN, c.detentFlow.value)
    }

    @Test
    fun enabledFlowReflectsToggle() = runTest {
        val c = AzSheetController(initial = AzSheetDetent.PEEK)
        assertTrue(c.enabledFlow.value)
        c.isEnabled = false
        assertEquals(false, c.enabledFlow.value)
    }

    @Test
    fun snapToNonHiddenIsBlockedWhenDisabled() {
        val c = AzSheetController(initial = AzSheetDetent.HIDDEN)
        c.isEnabled = false
        c.snapTo(AzSheetDetent.FULL)
        assertEquals(AzSheetDetent.HIDDEN, c.detent)
    }
}
