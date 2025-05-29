/*
 * Created by chenru on 2022/4/25 下午5:05(format year/.
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

package com.sensorsdata.analytics.android.sdk.advert.monitor;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.sensorsdata.analytics.android.sdk.SAConfigOptions;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.advert.deeplink.DeepLinkManager;
import com.sensorsdata.analytics.android.sdk.advert.utils.SAAdvertMarketHelper;
import com.sensorsdata.analytics.android.sdk.monitor.SensorsDataActivityLifecycleCallbacks;

public class SensorsDataAdvertActivityLifeCallback implements SensorsDataActivityLifecycleCallbacks.SAActivityLifecycleCallbacks {
    private SAConfigOptions mOptions;

    public SensorsDataAdvertActivityLifeCallback(SAConfigOptions options) {
        this.mOptions = options;
    }

    @Override
    public void onNewIntent(Intent intent) {

    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        try {
            SAAdvertMarketHelper.handleAdMarket(activity, mOptions.getAdvertConfig());
            DeepLinkManager.parseDeepLink(activity, mOptions.isSaveDeepLinkInfo());
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void onActivityStarted(Activity activity) {
        try {
            SAAdvertMarketHelper.handleAdMarket(activity, mOptions.getAdvertConfig());
            DeepLinkManager.parseDeepLink(activity, mOptions.isSaveDeepLinkInfo());
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
        DeepLinkManager.parseDeepLink(activity, mOptions.isSaveDeepLinkInfo());
    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }
}
