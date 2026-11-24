package com.hereliesaz.aznavrail.reviews.play;

import java.util.concurrent.TimeUnit;

/** Pure, deterministic local request policy. No state represents a submitted review. */
final class ReviewEligibility {
    static final class State {
        final long lastDay;
        final int usageDays;
        final long lastAttempt;

        State(long lastDay, int usageDays, long lastAttempt) {
            this.lastDay = lastDay;
            this.usageDays = usageDays;
            this.lastAttempt = lastAttempt;
        }
    }

    static final class Decision {
        final State state;
        final boolean request;

        Decision(State state, boolean request) {
            this.state = state;
            this.request = request;
        }
    }

    private ReviewEligibility() {}

    /** Only a non-debug Play installation in an active foreground window is eligible. */
    static boolean allowed(boolean debug, String installer, boolean active) {
        return !debug && "com.android.vending".equals(installer) && active;
    }

    /** Counts at most one meaningful opportunity per UTC day; reserves the attempt before launch. */
    static Decision evaluate(State state, long now, int minimumUsageDays, int cooldownDays) {
        if (minimumUsageDays < 1 || cooldownDays < 1) {
            throw new IllegalArgumentException("Usage and cooldown must be positive");
        }
        if (now < 0 || state.lastAttempt > now) return new Decision(state, false);
        long day = TimeUnit.MILLISECONDS.toDays(now);
        if (day < state.lastDay) return new Decision(state, false);
        int usageDays = state.usageDays;
        if (day > state.lastDay && usageDays < Integer.MAX_VALUE) usageDays++;
        boolean eligible = usageDays >= minimumUsageDays
                && (state.lastAttempt < 0
                    || now - state.lastAttempt >= TimeUnit.DAYS.toMillis(cooldownDays));
        if (!eligible && day == state.lastDay) return new Decision(state, false);
        return new Decision(new State(day, usageDays, eligible ? now : state.lastAttempt), eligible);
    }
}
