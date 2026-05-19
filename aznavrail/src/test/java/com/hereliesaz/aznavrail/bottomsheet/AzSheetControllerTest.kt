package com.hereliesaz.aznavrail.bottomsheet

import androidx.compose.runtime.saveable.SaverScope
import com.hereliesaz.aznavrail.model.AzSheetDetent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AzSheetControllerTest {

    @Test
    fun defaultDetentIsHidden() {
        val c = AzSheetController()
        assertEquals(
            "AzSheetController() with no explicit `initial` must default to HIDDEN — " +
                "the launcher-pass-through gate (LogKitty parity) assumes a hidden sheet at " +
                "construction. Got ${c.detent}. Check the default in the companion `invoke`.",
            AzSheetDetent.HIDDEN,
            c.detent,
        )
    }

    @Test
    fun stepUp_progressesThroughAllDetents_thenClampsAtFull() {
        val c = AzSheetController(initial = AzSheetDetent.HIDDEN)
        assertEquals(
            "AzSheetController initial detent did not match constructor argument HIDDEN — " +
                "got ${c.detent}. The constructor should store `initial` in both _detent and _detentFlow.",
            AzSheetDetent.HIDDEN,
            c.detent,
        )

        c.stepUp()
        assertEquals(
            "stepUp() at HIDDEN should advance to PEEK — got ${c.detent}. " +
                "Order must be [HIDDEN, PEEK, HALF, FULL] in AzSheetController.stepUp().",
            AzSheetDetent.PEEK,
            c.detent,
        )

        c.stepUp()
        assertEquals(
            "stepUp() at PEEK should advance to HALF — got ${c.detent}. " +
                "Verify the PEEK branch in AzSheetController.stepUp().",
            AzSheetDetent.HALF,
            c.detent,
        )

        c.stepUp()
        assertEquals(
            "stepUp() at HALF should advance to FULL — got ${c.detent}. " +
                "Verify the HALF branch in AzSheetController.stepUp().",
            AzSheetDetent.FULL,
            c.detent,
        )

        // Saturation
        c.stepUp()
        assertEquals(
            "stepUp() at FULL should saturate at FULL (no-op past the top) — got ${c.detent}. " +
                "The FULL branch in AzSheetController.stepUp() must map to FULL, not wrap around.",
            AzSheetDetent.FULL,
            c.detent,
        )
    }

    @Test
    fun stepDown_progressesThroughAllDetents_thenClampsAtHidden() {
        val c = AzSheetController(initial = AzSheetDetent.FULL)

        c.stepDown()
        assertEquals(
            "stepDown() at FULL should reduce to HALF — got ${c.detent}.",
            AzSheetDetent.HALF,
            c.detent,
        )

        c.stepDown()
        assertEquals(
            "stepDown() at HALF should reduce to PEEK — got ${c.detent}.",
            AzSheetDetent.PEEK,
            c.detent,
        )

        c.stepDown()
        assertEquals(
            "stepDown() at PEEK should reduce to HIDDEN — got ${c.detent}.",
            AzSheetDetent.HIDDEN,
            c.detent,
        )

        // Saturation
        c.stepDown()
        assertEquals(
            "stepDown() at HIDDEN must saturate at HIDDEN (no underflow) — got ${c.detent}. " +
                "The HIDDEN branch in AzSheetController.stepDown() must map to HIDDEN.",
            AzSheetDetent.HIDDEN,
            c.detent,
        )
    }

    @Test
    fun snapTo_jumpsDirectlyToTargetWhenEnabled() = runTest {
        val c = AzSheetController(initial = AzSheetDetent.HIDDEN)
        c.snapTo(AzSheetDetent.FULL)
        assertEquals(
            "snapTo(FULL) on an enabled controller should jump directly to FULL (skipping HALF/PEEK) — " +
                "got ${c.detent}. Verify the enabled-branch of AzSheetController.snapTo().",
            AzSheetDetent.FULL,
            c.detent,
        )
        assertEquals(
            "snapTo() must propagate to detentFlow so the WindowManager collector resizes — " +
                "detentFlow.value = ${c.detentFlow.value}.",
            AzSheetDetent.FULL,
            c.detentFlow.value,
        )
    }

    @Test
    fun snapTo_nonHidden_isBlockedWhenDisabled() {
        val c = AzSheetController(initial = AzSheetDetent.HIDDEN)
        c.isEnabled = false
        c.snapTo(AzSheetDetent.FULL)
        assertEquals(
            "snapTo(FULL) must be gated when isEnabled = false (launcher-pass-through) — " +
                "got ${c.detent}, expected HIDDEN. AzSheetController.snapTo() must early-return " +
                "when `target != HIDDEN && !_isEnabled`.",
            AzSheetDetent.HIDDEN,
            c.detent,
        )
    }

    @Test
    fun snapTo_hidden_isAllowedEvenWhenDisabled() {
        // While disabled, the only legal target is HIDDEN; snapTo(HIDDEN) must NOT be blocked
        // because that's how external callers (e.g. accessibility events) force-collapse the sheet.
        val c = AzSheetController(initial = AzSheetDetent.HIDDEN)
        c.isEnabled = false
        c.snapTo(AzSheetDetent.HIDDEN)
        assertEquals(
            "snapTo(HIDDEN) must remain allowed even when disabled — got ${c.detent}. " +
                "The guard is `target != HIDDEN && !_isEnabled`.",
            AzSheetDetent.HIDDEN,
            c.detent,
        )
    }

    @Test
    fun isEnabledFalse_collapsesToHidden() {
        val c = AzSheetController(initial = AzSheetDetent.HALF)
        c.isEnabled = false
        assertEquals(
            "Setting isEnabled = false at HALF must auto-collapse to HIDDEN — got ${c.detent}. " +
                "AzSheetController.isEnabled setter must call `detent = HIDDEN` on the falling edge.",
            AzSheetDetent.HIDDEN,
            c.detent,
        )

        // stepUp should be ignored while disabled
        c.stepUp()
        assertEquals(
            "stepUp() must be ignored when isEnabled = false — got ${c.detent}. " +
                "The first line of AzSheetController.stepUp() must early-return on !_isEnabled.",
            AzSheetDetent.HIDDEN,
            c.detent,
        )
    }

    @Test
    fun detentFlow_emitsOnEveryChange() = runTest {
        val c = AzSheetController(initial = AzSheetDetent.HIDDEN)
        assertEquals(
            "detentFlow.value must be initialised from the constructor argument — got ${c.detentFlow.value}.",
            AzSheetDetent.HIDDEN,
            c.detentFlow.value,
        )
        c.stepUp()
        assertEquals(
            "detentFlow must reflect stepUp() (HIDDEN -> PEEK) — got ${c.detentFlow.value}. " +
                "The `detent` setter must push to _detentFlow.",
            AzSheetDetent.PEEK,
            c.detentFlow.value,
        )
        c.stepUp()
        assertEquals(
            "detentFlow must reflect stepUp() (PEEK -> HALF) — got ${c.detentFlow.value}.",
            AzSheetDetent.HALF,
            c.detentFlow.value,
        )
        c.snapTo(AzSheetDetent.HIDDEN)
        assertEquals(
            "detentFlow must reflect snapTo(HIDDEN) — got ${c.detentFlow.value}.",
            AzSheetDetent.HIDDEN,
            c.detentFlow.value,
        )
    }

    @Test
    fun enabledFlow_emitsOnToggle() = runTest {
        val c = AzSheetController(initial = AzSheetDetent.PEEK)
        assertTrue(
            "enabledFlow.value must start true on a freshly-constructed controller — got ${c.enabledFlow.value}.",
            c.enabledFlow.value,
        )
        c.isEnabled = false
        assertFalse(
            "enabledFlow must reflect isEnabled = false — got ${c.enabledFlow.value}. " +
                "The `isEnabled` setter must push to _enabledFlow.",
            c.enabledFlow.value,
        )
        c.isEnabled = true
        assertTrue(
            "enabledFlow must reflect isEnabled = true — got ${c.enabledFlow.value}.",
            c.enabledFlow.value,
        )
    }

    @Test
    fun saver_roundTripPreservesDetent() {
        // Simulate a configuration-change save/restore through the public Saver.
        val original = AzSheetController(initial = AzSheetDetent.HIDDEN)
        original.snapTo(AzSheetDetent.HALF)
        assertEquals(
            "Pre-save sanity: snapTo(HALF) must take effect before we exercise the Saver.",
            AzSheetDetent.HALF,
            original.detent,
        )

        val saver = AzSheetController.Saver
        // SaverScope.canBeSaved is not used by AzSheetController.Saver (we serialise a String), so a
        // throwaway implementation is sufficient to drive the save lambda.
        val scope = SaverScope { true }
        val saved = with(saver) { scope.save(original) }
        assertNotNull(
            "AzSheetController.Saver.save() must produce a non-null payload — got null. " +
                "The save lambda should return `it.detent.name`.",
            saved,
        )

        val restored = saver.restore(saved!!)
        assertNotNull(
            "AzSheetController.Saver.restore() must reconstruct a controller — got null. " +
                "The restore lambda should call `AzSheetController(AzSheetDetent.valueOf(it))`.",
            restored,
        )
        assertEquals(
            "Saver round-trip must preserve the detent across configuration change — " +
                "original = ${original.detent}, restored = ${restored!!.detent}. Verify both " +
                "save (detent.name) and restore (AzSheetDetent.valueOf) in AzSheetController.Saver.",
            AzSheetDetent.HALF,
            restored.detent,
        )
    }
}
