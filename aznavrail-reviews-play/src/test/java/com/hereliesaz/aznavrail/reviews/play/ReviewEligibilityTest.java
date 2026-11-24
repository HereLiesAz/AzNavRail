package com.hereliesaz.aznavrail.reviews.play;

import org.junit.Test;

import static org.junit.Assert.*;

public class ReviewEligibilityTest {
    private static final long DAY = 86_400_000L;
    private static final ReviewEligibility.State EMPTY = new ReviewEligibility.State(-1, 0, -1);

    @Test public void onlyFocusedReleasePlayInstalls() {
        assertTrue(ReviewEligibility.allowed(false, "com.android.vending", true));
        assertFalse(ReviewEligibility.allowed(true, "com.android.vending", true));
        assertFalse(ReviewEligibility.allowed(false, "com.android.vending", false));
        assertFalse(ReviewEligibility.allowed(false, null, true));
        assertFalse(ReviewEligibility.allowed(false, "com.android.shell", true));
        assertFalse(ReviewEligibility.allowed(false, "com.amazon.venezia", true));
    }

    @Test public void distinctDaysNotCallsAndAttemptReserved() {
        ReviewEligibility.Decision first = ReviewEligibility.evaluate(EMPTY, DAY, 3, 90);
        assertFalse(first.request);
        assertEquals(1, first.state.usageDays);
        ReviewEligibility.Decision same = ReviewEligibility.evaluate(first.state, DAY + 1000, 3, 90);
        assertFalse(same.request);
        assertSame(first.state, same.state);
        ReviewEligibility.Decision second = ReviewEligibility.evaluate(same.state, 5 * DAY, 3, 90);
        assertFalse(second.request);
        assertEquals(2, second.state.usageDays);
        ReviewEligibility.Decision third = ReviewEligibility.evaluate(second.state, 9 * DAY, 3, 90);
        assertTrue(third.request);
        assertEquals(3, third.state.usageDays);
        assertEquals(777_600_000L, third.state.lastAttempt);
        assertFalse(ReviewEligibility.evaluate(third.state, 9 * DAY + 1, 3, 90).request);
    }

    @Test public void cooldownSurvivesReloadAndDoesNotResetOnUpgrade() {
        ReviewEligibility.State restored = new ReviewEligibility.State(9, 3, 777_600_000L);
        assertFalse(ReviewEligibility.evaluate(restored, 8_553_599_999L, 3, 90).request);
        ReviewEligibility.Decision next = ReviewEligibility.evaluate(restored, 8_553_600_000L, 3, 90);
        assertTrue(next.request);
        assertEquals(8_553_600_000L, next.state.lastAttempt);
    }

    @Test public void rollbackDoesNotCreateNewOpportunitiesOrBypassCooldown() {
        ReviewEligibility.State state = new ReviewEligibility.State(10, 2, -1);
        assertSame(state, ReviewEligibility.evaluate(state, 9 * DAY, 3, 90).state);
        ReviewEligibility.State attempted = new ReviewEligibility.State(10, 3, 10 * DAY + 500);
        assertFalse(ReviewEligibility.evaluate(attempted, 10 * DAY + 499, 3, 90).request);
    }

    @Test public void configurableThresholdAndCooldown() {
        ReviewEligibility.Decision first = ReviewEligibility.evaluate(EMPTY, 0, 1, 2);
        assertTrue(first.request);
        assertFalse(ReviewEligibility.evaluate(first.state, 172_799_999L, 1, 2).request);
        assertTrue(ReviewEligibility.evaluate(first.state, 172_800_000L, 1, 2).request);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidThresholdRejected() {
        ReviewEligibility.evaluate(EMPTY, DAY, 0, 90);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidCooldownRejected() {
        ReviewEligibility.evaluate(EMPTY, DAY, 3, 0);
    }
}
