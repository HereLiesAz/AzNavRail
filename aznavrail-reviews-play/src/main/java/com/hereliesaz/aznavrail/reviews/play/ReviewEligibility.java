package com.hereliesaz.aznavrail.reviews.play;

import java.util.Calendar;
import java.util.TimeZone;

/** Pure local timing policy. No state represents a submitted review. */
final class ReviewEligibility {
    private ReviewEligibility() {}

    /** Only a non-debug Play installation may request from an active foreground window. */
    static boolean allowed(boolean debug, String installer, boolean active) {
        return !debug && "com.android.vending".equals(installer) && active;
    }

    /** One attempt per local calendar date, including across restarts and library upgrades. */
    static boolean canAttempt(long lastAttempt, long now, TimeZone zone) {
        if (now < 0 || lastAttempt > now) return false;
        if (lastAttempt < 0) return true;
        Calendar previous = Calendar.getInstance(zone);
        previous.setTimeInMillis(lastAttempt);
        Calendar current = Calendar.getInstance(zone);
        current.setTimeInMillis(now);
        return current.get(Calendar.YEAR) > previous.get(Calendar.YEAR)
                || (current.get(Calendar.YEAR) == previous.get(Calendar.YEAR)
                    && current.get(Calendar.DAY_OF_YEAR) > previous.get(Calendar.DAY_OF_YEAR));
    }

    /** A monotonic one-minute delay, with no repeat within the same foreground session. */
    static final class Session {
        private static final long DELAY_MS = 60_000L;
        private long openedAt = -1L;
        private long generation;
        private boolean complete;

        void open(long uptime) {
            if (openedAt < 0) {
                openedAt = uptime;
                complete = false;
                generation++;
            }
        }

        /** -1 when inactive or finished; otherwise milliseconds until the first attempt. */
        long remaining(long uptime) {
            if (openedAt < 0 || complete) return -1L;
            return Math.max(0L, DELAY_MS - Math.max(0L, uptime - openedAt));
        }

        void complete() { complete = true; }

        void close() {
            if (openedAt >= 0) generation++;
            openedAt = -1L;
            complete = false;
        }

        long generation() { return generation; }
    }
}
