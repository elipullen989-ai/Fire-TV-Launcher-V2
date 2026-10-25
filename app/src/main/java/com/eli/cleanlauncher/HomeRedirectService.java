package com.eli.cleanlauncher;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.accessibility.AccessibilityEvent;

/**
 * Opens Clean Launcher whenever Amazon's home screen comes to the front, so you can use
 * Clean Launcher as your home screen without disabling Amazon's launcher.
 *
 * It only receives events from Amazon's launcher package (see accessibility_service.xml)
 * and never reads anything on screen. The "Amazon Home" tile in the launcher pauses it for a
 * few minutes so you can still reach Amazon's own home screen when you want to.
 */
public class HomeRedirectService extends AccessibilityService {

    private static final String AMAZON_HOME = "com.amazon.tv.launcher";
    private static final String PREFS = "launcher";
    private static final String PREF_PAUSE_UNTIL = "pauseUntil";
    private static final long MIN_GAP_MS = 1500;   // avoids rapid repeat launches

    private long lastLaunch = 0;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) {
            return;
        }
        if (!AMAZON_HOME.contentEquals(event.getPackageName())) {
            return;
        }
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return;
        }

        long now = System.currentTimeMillis();
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (now < prefs.getLong(PREF_PAUSE_UNTIL, 0)) {
            return;   // you asked for Amazon's home screen
        }
        if (now - lastLaunch < MIN_GAP_MS) {
            return;
        }
        lastLaunch = now;

        try {
            startActivity(new Intent(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        } catch (Exception ignored) {
            // If Android refuses, Amazon's home screen simply stays up.
        }
    }

    @Override
    public void onInterrupt() {
        // nothing to interrupt
    }
}
