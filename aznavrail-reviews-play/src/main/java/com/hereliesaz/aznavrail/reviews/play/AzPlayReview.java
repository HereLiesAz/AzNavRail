package com.hereliesaz.aznavrail.reviews.play;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

/**
 * Optional native Google Play review requests. No navigation, backend, account or review lookup.
 * Google Play alone determines whether its dialog appears; completion does not reveal whether
 * the user rated, reviewed, dismissed, or even saw the dialog.
 */
public final class AzPlayReview {
    private static final String PREFS = "az_play_review_requests";
    /** @deprecated No minimum usage-day threshold remains. */
    @Deprecated public static final int DEFAULT_MINIMUM_USAGE_DAYS = 1;
    /** @deprecated Requests are limited by local calendar day, not an elapsed-day cooldown. */
    @Deprecated public static final int DEFAULT_COOLDOWN_DAYS = 1;
    private static ForegroundRequests registered;

    private AzPlayReview() {}

    /**
     * Enable automatic review requests for the process. Call on the main thread in the launch
     * Activity's onCreate, after super.onCreate. Repeated calls are harmless. Each foreground
     * session becomes eligible after one minute; at most one Play request is made per local
     * calendar day. No request is made while the app is backgrounded or lacks window focus.
     *
     * @param activity an Activity belonging to the consuming application
     */
    public static void request(Activity activity) {
        if (activity == null || Looper.myLooper() != Looper.getMainLooper()) return;
        if (registered != null) return;
        try {
            boolean debug = (activity.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
            if (debug || !ReviewEligibility.allowed(debug, installer(activity), true)) return;
            Application app = activity.getApplication();
            ForegroundRequests requests = new ForegroundRequests(app);
            app.registerActivityLifecycleCallbacks(requests);
            registered = requests;
            // Also accept a first call from an already-focused Activity for older consumers.
            if (active(activity)) {
                requests.onActivityStarted(activity);
                requests.onActivityResumed(activity);
            }
        } catch (PackageManager.NameNotFoundException | RuntimeException ignored) {
            // Unknown installer or unavailable app state: fail closed.
        }
    }

    /**
     * @deprecated The daily policy is fixed; both legacy policy arguments are ignored.
     * Use {@link #request(Activity)} during Activity creation instead.
     */
    @Deprecated
    public static void request(Activity activity, int minimumUsageDays, int cooldownDays) {
        request(activity);
    }

    /** Tracks the app's foreground session, rather than starting a timer per Activity. */
    private static final class ForegroundRequests implements Application.ActivityLifecycleCallbacks {
        private final Application app;
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final ReviewEligibility.Session session = new ReviewEligibility.Session();
        private final Set<Activity> started =
                Collections.newSetFromMap(new IdentityHashMap<Activity, Boolean>());
        private final List<Activity> resumed = new ArrayList<>();
        private final Runnable attempt = this::attemptWhenReady;

        ForegroundRequests(Application app) {
            this.app = app;
        }

        @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}
        @Override public void onActivityStarted(Activity activity) { started.add(activity); }
        @Override public void onActivityResumed(Activity activity) {
            started.add(activity);
            resumed.remove(activity);
            resumed.add(activity);
            session.open(SystemClock.uptimeMillis());
            schedule();
        }
        @Override public void onActivityPaused(Activity activity) {
            resumed.remove(activity);
            schedule();
        }
        @Override public void onActivityStopped(Activity activity) {
            started.remove(activity);
            resumed.remove(activity);
            if (started.isEmpty() && !activity.isChangingConfigurations()) {
                session.close();
            }
            schedule();
        }
        @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
        @Override public void onActivityDestroyed(Activity activity) {
            started.remove(activity);
            resumed.remove(activity);
            schedule();
        }

        private void schedule() {
            handler.removeCallbacks(attempt);
            if (resumed.isEmpty()) return;
            long remaining = session.remaining(SystemClock.uptimeMillis());
            if (remaining >= 0) handler.postDelayed(attempt, remaining);
        }

        private void attemptWhenReady() {
            if (session.remaining(SystemClock.uptimeMillis()) != 0 || resumed.isEmpty()) return;
            Activity activity = null;
            for (int i = resumed.size() - 1; i >= 0; i--) {
                if (active(resumed.get(i))) {
                    activity = resumed.get(i);
                    break;
                }
            }
            if (activity == null) {
                // A permission dialog or transition can temporarily take focus.
                handler.postDelayed(attempt, 1000L);
                return;
            }
            session.complete();
            try {
                SharedPreferences prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
                long now = System.currentTimeMillis();
                long last = prefs.getLong("last_attempt", -1L);
                if (!ReviewEligibility.canAttempt(last, now, TimeZone.getDefault())) return;
                // Preserve the existing timestamp across upgrades; reserve before contacting Play.
                // Failed and suppressed requests count as attempts, never as submitted reviews.
                if (!prefs.edit().putLong("last_attempt", now).commit()) return;
                long generation = session.generation();
                WeakReference<Activity> reference = new WeakReference<>(activity);
                ReviewManager manager = ReviewManagerFactory.create(app);
                manager.requestReviewFlow().addOnCompleteListener(task -> {
                    Activity current = reference.get();
                    if (!task.isSuccessful() || current == null || !active(current)
                            || !resumed.contains(current) || session.generation() != generation) return;
                    try {
                        manager.launchReviewFlow(current, task.getResult());
                    } catch (RuntimeException ignored) {
                        // Play interaction failures must not interrupt the host app.
                    }
                });
            } catch (RuntimeException ignored) {
                // Missing Play services or corrupt state: fail closed.
            }
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
