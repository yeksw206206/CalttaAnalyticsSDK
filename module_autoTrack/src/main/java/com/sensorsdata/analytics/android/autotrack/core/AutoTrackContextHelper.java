/*
 * Created by dengshiwei on 2022/07/08.
 * Copyright 2015－2022 Sensors Data Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sensorsdata.analytics.android.autotrack.core;

import android.app.Activity;
import android.app.Dialog;
import android.view.View;

import com.sensorsdata.analytics.android.autotrack.aop.FragmentTrackHelper;
import com.sensorsdata.analytics.android.autotrack.core.autotrack.ActivityLifecycleCallbacks;
import com.sensorsdata.analytics.android.autotrack.core.autotrack.FragmentViewScreenCallbacks;
import com.sensorsdata.analytics.android.autotrack.core.business.SAPageTools;
import com.sensorsdata.analytics.android.autotrack.core.impl.AutoTrackProtocolIml;
import com.sensorsdata.analytics.android.autotrack.core.pageleave.ActivityPageLeaveCallbacks;
import com.sensorsdata.analytics.android.autotrack.core.pageleave.FragmentPageLeaveCallbacks;
import com.sensorsdata.analytics.android.autotrack.core.plugins.AutoTrackEventPlugin;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.core.SAContextManager;
import com.sensorsdata.analytics.android.sdk.core.mediator.Modules;
import com.sensorsdata.analytics.android.sdk.exceptions.SensorsDataExceptionHandler;
import com.sensorsdata.analytics.android.sdk.internal.beans.InternalConfigOptions;
import com.sensorsdata.analytics.android.sdk.monitor.SensorsDataLifecycleMonitorManager;

import org.json.JSONObject;

import java.util.List;

public class AutoTrackContextHelper {
    private static final String TAG = "AutoTrackContextHelper";
    private final InternalConfigOptions mInternalConfigs;
    private final SAContextManager mSAContextManager;
    private ActivityLifecycleCallbacks mActivityLifecycleCallbacks;
    private final AutoTrackProtocolIml mProtocolImp;
    public AutoTrackContextHelper(SAContextManager contextManager) {
        this.mSAContextManager = contextManager;
        this.mInternalConfigs = mSAContextManager.getInternalConfigs();
        mProtocolImp = new AutoTrackProtocolIml(contextManager);
        registerListener();
        mSAContextManager.getPluginManager().registerPropertyPlugin(new AutoTrackEventPlugin());
        try {
            if (contextManager.getInternalConfigs().context instanceof Activity) {
                delayExecution((Activity) contextManager.getInternalConfigs().context);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public void registerListener() {
        mActivityLifecycleCallbacks = new ActivityLifecycleCallbacks(mSAContextManager);
        SensorsDataLifecycleMonitorManager.getInstance().addActivityLifeCallback(mActivityLifecycleCallbacks);
        SensorsDataExceptionHandler.addExceptionListener(mActivityLifecycleCallbacks);
        FragmentTrackHelper.addFragmentCallbacks(new FragmentViewScreenCallbacks());

        if (mInternalConfigs.saConfigOptions.isTrackPageLeave()) {
            ActivityPageLeaveCallbacks pageLeaveCallbacks = new ActivityPageLeaveCallbacks(mInternalConfigs.saConfigOptions.getIgnorePageLeave());
            SensorsDataLifecycleMonitorManager.getInstance().addActivityLifeCallback(pageLeaveCallbacks);
            SensorsDataExceptionHandler.addExceptionListener(pageLeaveCallbacks);
            if (mInternalConfigs.saConfigOptions.isTrackFragmentPageLeave()) {
                FragmentPageLeaveCallbacks fragmentPageLeaveCallbacks = new FragmentPageLeaveCallbacks(mInternalConfigs.saConfigOptions.getIgnorePageLeave());
                FragmentTrackHelper.addFragmentCallbacks(fragmentPageLeaveCallbacks);
                SensorsDataExceptionHandler.addExceptionListener(fragmentPageLeaveCallbacks);
            }
        }
    }


    /**
     * delay init sdk
     *
     * @param activity Activity
     */
    protected void delayExecution(Activity activity) {
        if (mActivityLifecycleCallbacks != null) {
            mActivityLifecycleCallbacks.onActivityCreated(activity, null); //延迟初始化监听 onActivityCreated 处理
            mActivityLifecycleCallbacks.onDelayInitStarted(activity); //延迟初始化监听 onActivityStarted 处理
        }
        if (SALog.isLogEnabled()) {
            SALog.i(TAG, "SDK init success by：" + activity.getClass().getName());
        }
    }

    public <T> T invokeModuleFunction(String methodName, Object... argv) {
        switch (methodName) {
            case Modules.AutoTrack.METHOD_ENABLE_AUTO_TRACK:
                mProtocolImp.enableAutoTrack((List<SensorsDataAPI.AutoTrackEventType>) argv[0]);
                break;
            case Modules.AutoTrack.METHOD_DISABLE_AUTO_TRACK:
                if (argv[0] instanceof SensorsDataAPI.AutoTrackEventType) {
                    mProtocolImp.disableAutoTrack((SensorsDataAPI.AutoTrackEventType) argv[0]);
                } else {
                    mProtocolImp.disableAutoTrack((List<SensorsDataAPI.AutoTrackEventType>) argv[0]);
                }
                break;
            case Modules.AutoTrack.METHOD_IS_AUTOTRACK_ENABLED:
                return (T) Boolean.valueOf(mProtocolImp.isAutoTrackEnabled());
            case Modules.AutoTrack.METHOD_IGNORE_AUTOTRACK_ACTIVITIES:
                mProtocolImp.ignoreAutoTrackActivities((List<Class<?>>) argv[0]);
                break;
            case Modules.AutoTrack.METHOD_RESUME_AUTOTRACK_ACTIVITIES:
                mProtocolImp.resumeAutoTrackActivities((List<Class<?>>) argv[0]);
                break;
            case Modules.AutoTrack.METHOD_IGNORE_AUTOTRACK_ACTIVITY:
                mProtocolImp.ignoreAutoTrackActivity((Class<?>) argv[0]);
                break;
            case Modules.AutoTrack.METHOD_RESUME_AUTOTRACK_ACTIVITY:
                mProtocolImp.resumeAutoTrackActivity((Class<?>) argv[0]);
                break;
            case Modules.AutoTrack.METHOD_IGNORE_AUTOTRACK_FRAGMENTS:
                mProtocolImp.ignoreAutoTrackFragments((List<Class<?>>) argv[0]);
                break;
            case Modules.AutoTrack.METHOD_IGNORE_AUTOTRACK_FRAGMENT:
                mProtocolImp.ignoreAutoTrackFragment((Class<?>) argv[0]);
                break;
            case Modules.AutoTrack.METHOD_RESUME_IGNORED_AUTOTRACK_FRAGMENTS:
                mProtocolImp.resumeIgnoredAutoTrackFragments((List<Class<?>>) argv[0]);
                break;
            case Modules.AutoTrack.METHOD_RESUME_IGNORED_AUTOTRACK_FRAGMENT:
                mProtocolImp.resumeIgnoredAutoTrackFragment((Class<?>) argv[0]);
                break;
            case Modules.AutoTrack.METHOD_IS_ACTIVITY_AUTOTRACK_APPVIEWSCREEN_IGNORED:
                return (T) Boolean.valueOf(mProtocolImp.isActivityAutoTrackAppViewScreenIgnored((Class<?>) argv[0]));
            case Modules.AutoTrack.METHOD_IS_ACTIVITY_AUTOTRACK_APPCLICK_IGNORED:
                return (T) Boolean.valueOf(mProtocolImp.isActivityAutoTrackAppClickIgnored((Class<?>) argv[0]));
            case Modules.AutoTrack.METHOD_IS_TRACK_FRAGMENT_APPVIEWSCREEN_ENABLED:
                return (T) Boolean.valueOf(mProtocolImp.isTrackFragmentAppViewScreenEnabled());
            case Modules.AutoTrack.METHOD_IS_FRAGMENT_AUTOTRACK_APPVIEWSCREEN:
                return (T) Boolean.valueOf(mProtocolImp.isFragmentAutoTrackAppViewScreen((Class<?>) argv[0]));
            case Modules.AutoTrack.METHOD_IS_AUTOTRACK_EVENT_TYPE_IGNORED:
                if (argv[0] instanceof Integer) {
                    return (T) Boolean.valueOf(mProtocolImp.isAutoTrackEventTypeIgnored((Integer) argv[0]));
                } else {
                    return (T) Boolean.valueOf(mProtocolImp.isAutoTrackEventTypeIgnored((SensorsDataAPI.AutoTrackEventType) argv[0]));
                }
            case Modules.AutoTrack.METHOD_SET_VIEW_ID:
                if (argv[0] instanceof View) {
                    mProtocolImp.setViewID((View) argv[0], (String)argv[1]);
                } else if (argv[0] instanceof Dialog) {
                    mProtocolImp.setViewID((Dialog) argv[0], (String)argv[1]);
                } else {
                    mProtocolImp.setViewID(argv[0], (String)argv[1]);
                }
                break;
            case Modules.AutoTrack.METHOD_SET_VIEW_ACTIVITY:
                mProtocolImp.setViewActivity((View) argv[0], (Activity) argv[1]);
                break;
            case Modules.AutoTrack.METHOD_SET_VIEW_FRAGMENT_NAME:
                mProtocolImp.setViewFragmentName((View) argv[0], (String) argv[1]);
                break;
            case Modules.AutoTrack.METHOD_TRACK_FRAGMENT_APPVIEWSCREEN:
                mProtocolImp.trackFragmentAppViewScreen();
                break;
            case Modules.AutoTrack.METHOD_ENABLE_AUTOTRACK_FRAGMENT:
                mProtocolImp.enableAutoTrackFragment((Class<?>) argv[0]);
                break;
            case Modules.AutoTrack.METHOD_ENABLE_AUTOTRACK_FRAGMENTS:
                mProtocolImp.enableAutoTrackFragments((List<Class<?>>) argv[0]);
                break;
            case Modules.AutoTrack.METHOD_IGNORE_VIEW:
                if (argv.length == 1) {
                    mProtocolImp.ignoreView((View) argv[0]);
                } else {
                    mProtocolImp.ignoreView((View) argv[0], (Boolean) argv[1]);
                }
                break;
            case Modules.AutoTrack.METHOD_SET_VIEW_PROPERTIES:
                mProtocolImp.setViewProperties((View) argv[0], (JSONObject) argv[1]);
                break;
            case Modules.AutoTrack.METHOD_GET_IGNORED_VIEW_TYPE_LIST:
                return (T) mProtocolImp.getIgnoredViewTypeList();
            case Modules.AutoTrack.METHOD_IGNORE_VIEW_TYPE:
                mProtocolImp.ignoreViewType((Class<?>) argv[0]);
                break;
            case Modules.AutoTrack.METHOD_GET_LAST_SCREENURL:
                return (T) mProtocolImp.getLastScreenUrl();
            case Modules.AutoTrack.METHOD_CLEAR_REFERRER_WHEN_APPEND:
                mProtocolImp.clearReferrerWhenAppEnd();
                break;
            case Modules.AutoTrack.METHOD_GET_LAST_SCREEN_TRACK_PROPERTIES:
                return (T) mProtocolImp.getLastScreenTrackProperties();
            case Modules.AutoTrack.METHOD_CLEAR_LAST_SCREENURL:
                mProtocolImp.clearLastScreenUrl();
                break;
            case Modules.AutoTrack.METHOD_TRACK_VIEW_SCREEN:
                if (argv.length == 1) {
                    if (argv[0] instanceof Activity) {
                        mProtocolImp.trackViewScreen((Activity) argv[0]);
                    } else {
                        mProtocolImp.trackViewScreen(argv[0]);
                    }
                } else {
                    mProtocolImp.trackViewScreen((String)argv[0], (JSONObject) argv[1]);
                }
                break;
            case Modules.AutoTrack.METHOD_TRACK_VIEW_APPCLICK:
                if (argv.length == 1) {
                    mProtocolImp.trackViewAppClick((View) argv[0]);
                } else {
                    mProtocolImp.trackViewAppClick((View) argv[0], (JSONObject) argv[1]);
                }
                break;
            case Modules.AutoTrack.METHOD_GET_REFERRER_SCREEN_TITLE:
                return (T) SAPageTools.getReferrerTitle();
        }
        return null;
    }
}
