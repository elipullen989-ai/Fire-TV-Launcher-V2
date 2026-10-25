package com.eli.cleanlauncher;

import android.accessibilityservice.AccessibilityService;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

/**
 * Opens Clean Launcher whenever Amazon's home screen comes to the front, so you can use
 * Clean Launcher as your home screen without disabling Amazon's launcher.
 *
 * It only receives events from Amazon's launcher package (see accessibility_service.xml)
 * and never reads anything on screen. The "Amazon Home" tile in the launcher pauses it for a
 * few minutes so you can still reach Amazon's own home screen when you want to.
 *
 * Progress is written to the log under the tag "HomeRedirect":
 *   adb logcat -d -s HomeRedirect
 */
public class HomeRedirectService extends AccessibilityService {

    private static final String TAG = "HomeRedirect";
    private static final String AMAZON_HOME = "com.amazon.tv.launcher";
    private static final String PREFS = "launcher";
    private static final String PREF_PAUSE_UNTIL = "pauseUntil";
    private static final long MIN_GAP_MS = 1500;   // avoids rapid repeat launches

    private long lastLaunch = 0;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.i(TAG, "service connected");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) {
            return;
        }
        if (!AMAZON_HOME.contentEquals(event.getPackageName())) {
            return;
        }
        Log.i(TAG, "event from Amazon home, type=" + event.getEventType());
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return;
        }

        long now = System.currentTimeMillis();
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (now < prefs.getLong(PREF_PAUSE_UNTIL, 0)) {
            Log.i(TAG, "paused (Amazon Home tile), not redirecting");
            return;   // you asked for Amazon's home screen
        }
        if (now - lastLaunch < MIN_GAP_MS) {
            Log.i(TAG, "too soon after the last launch, skipping");
            return;
        }
        lastLaunch = now;
        openLauncher();
    }

    private void openLauncher() {
        Intent intent = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        try {
            Log.i(TAG, "starting Clean Launcher");
            startActivity(intent);
            return;
        } catch (Exception e) {
            Log.w(TAG, "startActivity failed: " + e);
        }
        // Second try, through a PendingIntent.
        try {
            PendingIntent.getActivity(this, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE).send();
            Log.i(TAG, "started through PendingIntent");
        } catch (Exception e) {
            Log.w(TAG, "PendingIntent failed: " + e);
            Toast.makeText(this, "Clean Launcher could not open itself", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onInterrupt() {
        // nothing to interrupt
    }
}
