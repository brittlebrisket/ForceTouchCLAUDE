/*
 * Android 7-15 / LSPosed compatibility helpers (added for the Android 15 port).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 */

package jp.tkgktyk.xposed.forcetouchdetector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import de.robv.android.xposed.XposedHelpers;

final class Compat {
    /** Android 7+: DecorView became a top-level class. */
    private static final String CLASS_DECOR_VIEW_N = "com.android.internal.policy.DecorView";
    /** Android 6: still nested in PhoneWindow. */
    private static final String CLASS_DECOR_VIEW_M = "com.android.internal.policy.PhoneWindow$DecorView";

    private Compat() {
    }

    static Class<?> findDecorViewClass() {
        try {
            return XposedHelpers.findClass(CLASS_DECOR_VIEW_N, null);
        } catch (Throwable t) {
            return XposedHelpers.findClass(CLASS_DECOR_VIEW_M, null);
        }
    }

    /**
     * Android 14 throws a SecurityException when an app targeting 34+ registers a receiver for
     * non-system broadcasts without saying whether it is exported. Our broadcasts come from other
     * processes (the settings app, or hooked apps), so they must be exported.
     */
    static void registerExportedReceiver(Context context, BroadcastReceiver receiver,
                                         IntentFilter filter) {
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            context.registerReceiver(receiver, filter);
        }
    }

    /**
     * Under LSPosed the module's classes are not visible to the hooked app's class loader, so the
     * Serializable settings extra can only be read with the module's own class loader.
     */
    static FTD.Settings readSettingsExtra(Intent intent) {
        try {
            intent.setExtrasClassLoader(FTD.class.getClassLoader());
            return (FTD.Settings) intent.getSerializableExtra(FTD.EXTRA_SETTINGS);
        } catch (Throwable t) {
            XposedModule.logE(t);
            return null;
        }
    }

    static boolean isPowerAction(String action) {
        return Intent.ACTION_POWER_CONNECTED.equals(action)
                || Intent.ACTION_POWER_DISCONNECTED.equals(action);
    }
}
