package com.mowtiie.messecure.util;

import android.app.Activity;
import android.view.WindowManager;

import androidx.preference.PreferenceManager;

public class SecurityHelper {

    public static void applyScreenshotBlock(Activity activity) {
        if (activity == null) return;

        boolean enabled = PreferenceManager
                .getDefaultSharedPreferences(activity)
                .getBoolean("screenshot_block", true);

        if (enabled) {
            activity.getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }
}