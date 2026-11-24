package com.hereliesaz.aznavrail.reviews.play;

import org.junit.Test;
import java.util.TimeZone;
import static org.junit.Assert.*;

public class ReviewEligibilityTest {
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final TimeZone CENTRAL = TimeZone.getTimeZone("America/Chicago");

    @Test public void onlyFocusedReleasePlayInstalls() {
        assertTrue(ReviewEligibility.allowed(false, "com.android.vending", true));
        assertFalse(ReviewEligibility.allowed(true, "com.android.vending", true));
        assertFalse(ReviewEligibility.allowed(false, "com.android.vending", false));
        assertFalse(ReviewEligibility.allowed(false, null, true));
        assertFalse(ReviewEligibility.allowed(false, "com.android.shell", true));
        assertFalse(ReviewEligibility.allowed(false, "com.amazon.venezia", true));
    }

    @Test public void firstOpenWaitsOneMinuteAndDoesNotRepeat() {
        ReviewEligibility.Session session = new ReviewEligibility.Session();
        assertEquals(-1L, session.remaining(1000));
        session.open(1000);
        assertEquals(60_000L, session.remaining(1000));
        assertEquals(1L, session.remaining(60_999));
        assertEquals(0L, session.remaining(61_000));
        session.complete();
        assertEquals(-1L, session.remaining(120_000));
    }

    @Test public void switchingActivitiesDoesNotRestartDelay() {
        ReviewEligibility.Session session = new ReviewEligibility.Session();
        session.open(1000);
        long generation = session.generation();
        session.open(31_000);
        assertEquals(30_000L, session.remaining(31_000));
        assertEquals(generation, session.generation());
        assertEquals(0L, session.remaining(61_000));
    }

    @Test public void backgroundInvalidatesPendingFlowAndReopenRestartsDelay() {
        ReviewEligibility.Session session = new ReviewEligibility.Session();
        session.open(1000);
        long generation = session.generation();
        session.close();
        assertEquals(-1L, session.remaining(90_000));
        assertNotEquals(generation, session.generation());
        session.open(90_000);
        assertEquals(1L, session.remaining(149_999));
        assertEquals(0L, session.remaining(150_000));
    }

    @Test public void firstDayEligibleWithoutUsageThreshold() {
        assertTrue(ReviewEligibility.canAttempt(-1, 0, UTC));
    }

    @Test public void sameCalendarDaySuppressedAndMidnightAllowsNextAttempt() {
        assertFalse(ReviewEligibility.canAttempt(86_399_999L, 86_399_999L, UTC));
        assertTrue(ReviewEligibility.canAttempt(86_399_999L, 86_400_000L, UTC));
        assertFalse(ReviewEligibility.canAttempt(86_400_000L, 172_799_999L, UTC));
        assertTrue(ReviewEligibility.canAttempt(86_400_000L, 172_800_000L, UTC));
    }

    @Test public void localMidnightNotUtcMidnight() {
        // Jan 1, 1970 23:59:59.999 CST and Jan 2 midnight CST.
        assertFalse(ReviewEligibility.canAttempt(86_399_999L, 86_400_000L, CENTRAL));
        assertTrue(ReviewEligibility.canAttempt(107_999_999L, 108_000_000L, CENTRAL));
    }

    @Test public void daylightSavingAndYearBoundary() {
        // March 8 2026 midnight CST to March 9 midnight CDT is a 23-hour calendar day.
        assertTrue(ReviewEligibility.canAttempt(1772949600000L, 1773032400000L, CENTRAL));
        // 2025-12-31 23:59:59 UTC -> 2026-01-01 midnight UTC.
        assertTrue(ReviewEligibility.canAttempt(1767225599000L, 1767225600000L, UTC));
    }

    @Test public void existingTimestampSurvivesUpgradeWithoutOldCooldown() {
        assertFalse(ReviewEligibility.canAttempt(777_600_000L, 777_600_001L, UTC));
        assertTrue(ReviewEligibility.canAttempt(777_600_000L, 864_000_000L, UTC));
    }

    @Test public void backwardClockDoesNotPermitAnotherAttempt() {
        assertFalse(ReviewEligibility.canAttempt(100_000L, 99_999L, UTC));
        assertFalse(ReviewEligibility.canAttempt(-1, -2, UTC));
    }
}
