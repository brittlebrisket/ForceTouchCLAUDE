/*
 * Copyright 2015 Takagi Katsuyuki
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.tkgktyk.xposed.forcetouchdetector;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;

/**
 * Created by tkgktyk on 2015/02/12.
 */
public class ModLongPress extends XposedModule {
    private static XSharedPreferences mPrefs;

    public static void initZygote(XSharedPreferences prefs) {
        mPrefs = prefs;
        try {
            install();
        } catch (Throwable t) {
            logE(t);
        }
    }

    private static void install() {
        // Android 15 port: "before" hooks instead of full replacements, so View.onTouchEvent
        // runs natively for every normal touch and only our synthetic long-press events
        // (deviceId == -1) are intercepted.
        XC_MethodHook injectLongClick = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                try {
                    View view = (View) methodHookParam.thisObject;
                    MotionEvent event = (MotionEvent) methodHookParam.args[0];
                    if (event.getActionMasked() == MotionEvent.ACTION_DOWN &&
                            event.getDeviceId() == -1 &&
                            view.isLongClickable()) {
                        if (view.performLongClick()) {
                            methodHookParam.setResult(true);
                        }
                    }
                } catch (Throwable t) {
                    logE(t);
                }
            }
        };
        Class<?>[] classes = new Class<?>[]{View.class, AbsListView.class};
        for (Class<?> cls : classes) {
            XposedHelpers.findAndHookMethod(cls, "onTouchEvent", MotionEvent.class, injectLongClick);
        }

        XposedHelpers.findAndHookMethod(GestureDetector.class, "onTouchEvent", MotionEvent.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                        try {
                            GestureDetector detector = (GestureDetector) methodHookParam.thisObject;
                            MotionEvent event = (MotionEvent) methodHookParam.args[0];
                            if (event.getActionMasked() == MotionEvent.ACTION_DOWN &&
                                    event.getDeviceId() == -1 &&
                                    detector.isLongpressEnabled()) {
                                GestureDetector.OnGestureListener listener
                                        = (GestureDetector.OnGestureListener) XposedHelpers
                                        .getObjectField(detector, "mListener");
                                listener.onLongPress(event);
                                methodHookParam.setResult(true);
                            }
                        } catch (Throwable t) {
                            logE(t);
                        }
                    }
                });
    }
}
