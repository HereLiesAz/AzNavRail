package com.hereliesaz.aznavrail.reviews.play;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Looper;

import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;

import java.lang.ref.WeakReference;

/**
 * Optional native Google Play review requests. No navigation, backend, account or review lookup.
 * Google Play alone determines whether its dialog appears; completion does not reveal whether
 * the user rated, reviewed, dismissed, or even saw the dialog.
 */
public final class AzPlayReview {
    private static final String PREFS = "az_play_review_requests";
    public static final int DEFAULT_MINIMUM_USAGE_DAYS = 3;
    public static final int DEFAULT_COOLDOWN_DAYS = 90;

    private AzPlayReview() {}

    /**
     * Record an opportunity at a natural stopping point on the main thread.
     * By default, require three distinct UTC usage days and wait ninety days between attempts.
     *
     * @param activity the currently focused foreground activity, not an application context
     */
    public static void request(Activity activity) {
        request(activity, DEFAULT_MINIMUM_USAGE_DAYS, DEFAULT_COOLDOWN_DAYS);
    }

    /**
     * Record an opportunity and, when locally eligible, ask Play to show its native review flow.
     * Call only after a meaningful interaction, never as a button promising to open the dialog.
     * Debug, sideloaded, unknown-store and inactive activities are ignored without recording usage.
     * A failed or suppressed Play request still consumes the local cooldown.
     *
     * @param activity the currently focused foreground activity
     * @param minimumUsageDays distinct UTC days with meaningful opportunities (positive)
     * @param cooldownDays minimum interval between attempts in 24-hour days (positive)
     */
    public static void request(Activity activity, int minimumUsageDays, int cooldownDays) {
        if (minimumUsageDays < 1 || cooldownDays < 1) {
            throw new IllegalArgumentException("Usage and cooldown must be positive");
        }
        if (activity == null || Looper.myLooper() != Looper.getMainLooper()) return;
        try {
            boolean debug = (activity.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
            if (debug || !active(activity)) return;
            if (!ReviewEligibility.allowed(debug, installer(activity), active(activity))) return;

            SharedPreferences prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            ReviewEligibility.State state = new ReviewEligibility.State(
                    prefs.getLong("last_day", -1L),
                    prefs.getInt("usage_days", 0),
                    prefs.getLong("last_attempt", -1L));
            ReviewEligibility.Decision decision = ReviewEligibility.evaluate(
                    state, System.currentTimeMillis(), minimumUsageDays, cooldownDays);
            if (decision.state == state) return;
            SharedPreferences.Editor edit = prefs.edit()
                    .putLong("last_day", decision.state.lastDay)
                    .putInt("usage_days", decision.state.usageDays)
                    .putLong("last_attempt", decision.state.lastAttempt);
            if (!decision.request) {
                edit.apply();
                return;
            }
            // Reserve the attempt durably before contacting Play, including on failure or dismissal.
            // Main-thread serialization prevents overlapping requests from multiple activities.
            if (!edit.commit()) return;
            WeakReference<Activity> reference = new WeakReference<>(activity);
            ReviewManager manager = ReviewManagerFactory.create(activity.getApplicationContext());
            manager.requestReviewFlow().addOnCompleteListener(task -> {
                Activity current = reference.get();
                if (!task.isSuccessful() || current == null || !active(current)) return;
                try {
                    manager.launchReviewFlow(current, task.getResult());
                    // Never infer a submitted review from task completion.
                } catch (RuntimeException ignored) {
                    // A failed Play interaction must not interrupt the host app.
                }
            });
        } catch (PackageManager.NameNotFoundException | RuntimeException ignored) {
            // Missing Play services, unavailable installer or corrupt local state: fail closed.
        }
    }

    private static boolean active(Activity activity) {
        return !activity.isFinishing() && !activity.isDestroyed() && activity.hasWindowFocus();
    }

    @SuppressWarnings("deprecation")
    private static String installer(Activity activity) throws PackageManager.NameNotFoundException {
        PackageManager pm = activity.getPackageManager();
        if (Build.VERSION.SDK_INT >= 30) {
            return pm.getInstallSourceInfo(activity.getPackageName()).getInstallingPackageName();
        }
        return pm.getInstallerPackageName(activity.getPackageName());
    }
}
