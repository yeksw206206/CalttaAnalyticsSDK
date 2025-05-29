/*
 * Created by yuejianzhong on 2020/11/04.
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

package com.sensorsdata.analytics.android.sdk.remote;

import android.os.CountDownTimer;
import android.os.Handler;
import android.os.SystemClock;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SAConfigOptions;
import com.sensorsdata.analytics.android.sdk.core.SACoreHelper;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.core.SAContextManager;
import com.sensorsdata.analytics.android.sdk.core.mediator.SAModuleManager;
import com.sensorsdata.analytics.android.sdk.core.event.InputData;
import com.sensorsdata.analytics.android.sdk.core.mediator.Modules;
import com.sensorsdata.analytics.android.sdk.data.adapter.DbAdapter;
import com.sensorsdata.analytics.android.sdk.internal.beans.EventType;
import com.sensorsdata.analytics.android.sdk.network.HttpCallback;
import com.sensorsdata.analytics.android.sdk.plugin.encrypt.SAStoreManager;
import com.sensorsdata.analytics.android.sdk.util.Dispatcher;

import org.json.JSONObject;

import java.security.SecureRandom;

/**
 * SDK 初始化及线上使用时，采集控制管理类
 */
public class SensorsDataRemoteManager extends BaseSensorsDataSDKRemoteManager {
    private static final String SHARED_PREF_REQUEST_TIME = "sensorsdata.request.time";
    private static final String SHARED_PREF_REQUEST_TIME_RANDOM = "sensorsdata.request.time.random";
    private static final String TAG = "SA.SensorsDataRemoteManager";

    // 每次启动 App 时，最多尝试三次
    private volatile CountDownTimer mPullSDKConfigCountDownTimer;
    private final SAStoreManager mStorageManager;
    private volatile boolean mIsInit = true;

    public SensorsDataRemoteManager(SensorsDataAPI sensorsDataAPI, SAContextManager contextManager) {
        super(sensorsDataAPI, contextManager);
        mStorageManager = SAStoreManager.getInstance();
        SALog.i(TAG, "Construct a SensorsDataRemoteManager");
    }

    /**
     * 是否发起随机请求
     *
     * @return false 代表不发，true 代表发送随机请求
     */
    private boolean isRequestValid() {
        boolean isRequestValid = true;
        try {
            long lastRequestTime = mStorageManager.getLong(SHARED_PREF_REQUEST_TIME, 0);
            int randomTime = mStorageManager.getInteger(SHARED_PREF_REQUEST_TIME_RANDOM, 0);
            if (lastRequestTime != 0 && randomTime != 0) {
                float requestInterval = SystemClock.elapsedRealtime() - lastRequestTime;
                // 当前的时间减去上次请求的时间，为间隔时间，当间隔时间小于随机时间，则不请求后端
                if (requestInterval > 0 && requestInterval / 1000 < randomTime * 3600) {
                    isRequestValid = false;
                }
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
        return isRequestValid;
    }

    /**
     * 缓存远程控制随机时间
     */
    private void writeRemoteRequestRandomTime() {
        SAConfigOptions configOptions = mContextManager.getInternalConfigs().saConfigOptions;
        if (configOptions == null || !mContextManager.getInternalConfigs().isRemoteConfigEnabled) {// 此时就不保存随机时间
            return;
        }
        //默认情况下，随机请求时间为最小时间间隔
        int randomTime = configOptions.getMinRequestInterval();
        long currentTime = SystemClock.elapsedRealtime();
        //最大时间间隔大于最小时间间隔时，生成随机时间
        if (configOptions.getMaxRequestInterval() > configOptions.getMinRequestInterval()) {
            randomTime += new SecureRandom().nextInt(configOptions.getMaxRequestInterval() - configOptions.getMinRequestInterval() + 1);
        }
        mStorageManager.setLong(SHARED_PREF_REQUEST_TIME, currentTime);
        mStorageManager.setInteger(SHARED_PREF_REQUEST_TIME_RANDOM, randomTime);
    }

    /**
     * 清除远程控制随机时间的本地缓存
     */
    private void cleanRemoteRequestRandomTime() {
        mStorageManager.remove(SHARED_PREF_REQUEST_TIME);
        mStorageManager.remove(SHARED_PREF_REQUEST_TIME_RANDOM);
    }

    @Override
    public void pullSDKConfigFromServer() {
        SAConfigOptions configOptions = mContextManager.getInternalConfigs().saConfigOptions;
        if (configOptions == null || configOptions.isDisableSDK()
                || !mContextManager.getInternalConfigs().isRemoteConfigEnabled && !mContextManager.getInternalConfigs().saConfigOptions.isEnableEncrypt()) {
            return;
        }

        // 关闭随机请求或者分散的最小时间大于最大时间时，清除本地时间，请求后端
        if (configOptions.isDisableRandomTimeRequestRemoteConfig() ||
                configOptions.getMinRequestInterval() > configOptions.getMaxRequestInterval()) {
            requestRemoteConfig(RandomTimeType.RandomTimeTypeClean, true);
            SALog.i(TAG, "remote config: Request remote config because disableRandomTimeRequestRemoteConfig or minHourInterval greater than maxHourInterval");
            return;
        }

        //开启加密并且传入秘钥为空的，强制请求后端，此时请求中不带 v
        if (!isSecretKeyValid()) {
            requestRemoteConfig(RandomTimeType.RandomTimeTypeWrite, false);
            SALog.i(TAG, "remote config: Request remote config because encrypt key is null");
            return;
        }

        //满足分散请求逻辑时，请求后端
        if (isRequestValid()) {
            requestRemoteConfig(RandomTimeType.RandomTimeTypeWrite, true);
            SALog.i(TAG, "remote config: Request remote config because satisfy the random request condition");
        }
    }

    @Override
    public void requestRemoteConfig(RandomTimeType randomTimeType, final boolean enableConfigV) {
        if (mDisableDefaultRemoteConfig) {
            SALog.i(TAG, "disableDefaultRemoteConfig is true");
            return;
        }

        switch (randomTimeType) {
            case RandomTimeTypeWrite:
                writeRemoteRequestRandomTime();
                break;
            case RandomTimeTypeClean:
                cleanRemoteRequestRandomTime();
                break;
            default:
                break;
        }

        Dispatcher.getInstance().post(new Runnable() {
            @Override
            public void run() {
                try {
                    pullSDKConfigCount(enableConfigV);
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
        });
    }

    private void pullSDKConfigCount(final boolean enableConfigV) {
        if (mPullSDKConfigCountDownTimer != null) {
            mPullSDKConfigCountDownTimer.cancel();
            mPullSDKConfigCountDownTimer = null;
        }

        mPullSDKConfigCountDownTimer = new CountDownTimer(90 * 1000, 30 * 1000) {
            @Override
            public void onTick(long l) {
                if (mSensorsDataAPI != null && !mSensorsDataAPI.isNetworkRequestEnable()
                        || mContextManager.getInternalConfigs().saConfigOptions.isDisableSDK()
                        || !mContextManager.getInternalConfigs().isRemoteConfigEnabled && !mContextManager.getInternalConfigs().saConfigOptions.isEnableEncrypt()) {
                    SALog.i(TAG, "Close network request or sdk is disable");
                    return;
                }

                requestRemoteConfig(enableConfigV, new HttpCallback.StringCallback() {
                    @Override
                    public void onFailure(int code, String errorMessage) {
                        // 304 状态码为后端配置未更新，此时不需要重试
                        // 205 状态码表示后端环境未同步配置，此时需要重试，代码不需要做特殊处理
                        if (code == 304 || code == 404) {
                            resetPullSDKConfigTimer();
                        }
                        SALog.i(TAG, "Remote request failed,responseCode is " + code +
                                ",errorMessage is " + errorMessage);
                    }

                    @Override
                    public void onResponse(String response) {
                        resetPullSDKConfigTimer();
                        if (!TextUtils.isEmpty(response)) {
                            SensorsDataSDKRemoteConfig sdkRemoteConfig = toSDKRemoteConfig(response);
                            SAModuleManager.getInstance().invokeModuleFunction(Modules.Encrypt.MODULE_NAME, Modules.Encrypt.METHOD_STORE_SECRET_KEY, response);
                            if (mContextManager.getInternalConfigs().isRemoteConfigEnabled) {//开启时才保存远程配置信息
                                setSDKRemoteConfig(sdkRemoteConfig);
                            }
                        }
                        SALog.i(TAG, "Remote request was successful,response data is " + response);
                    }

                    @Override
                    public void onAfter() {

                    }
                });
            }

            @Override
            public void onFinish() {
            }
        };
        mPullSDKConfigCountDownTimer.start();
    }

    @Override
    public void resetPullSDKConfigTimer() {
        try {
            if (mPullSDKConfigCountDownTimer != null) {
                mPullSDKConfigCountDownTimer.cancel();
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        } finally {
            mPullSDKConfigCountDownTimer = null;
        }
    }

    /**
     * 更新 SensorsDataSDKRemoteConfig
     *
     * @param sdkRemoteConfig SensorsDataSDKRemoteConfig 在线控制 SDK 的配置
     */
    @Override
    protected void setSDKRemoteConfig(SensorsDataSDKRemoteConfig sdkRemoteConfig) {
        try {
            //版本号不一致时，才会返回数据，此时上报事件
            final JSONObject eventProperties = new JSONObject();
            String remoteConfigString = sdkRemoteConfig.toJson().toString();
            eventProperties.put("$app_remote_config", remoteConfigString);
            SACoreHelper.getInstance().trackQueueEvent(new Runnable() {
                @Override
                public void run() {
                    mContextManager.trackEvent(new InputData().setEventName("$AppRemoteConfigChanged").setProperties(eventProperties).setEventType(EventType.TRACK));
                }
            });
            mContextManager.getAnalyticsMessages().flush();
            DbAdapter.getInstance().commitRemoteConfig(remoteConfigString);
            SALog.i(TAG, "Save remote data");
            //值为 1 时，表示在线控制立即生效
            if (1 == sdkRemoteConfig.getEffectMode()) {
                mSDKRemoteConfig = sdkRemoteConfig;
                SALog.i(TAG, "The remote configuration takes effect immediately");
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 从本地缓存中读取最新的 SDK 配置信息
     */
    @Override
    public void applySDKConfigFromCache() {
        try {
            String remoteConfig;
            if (mIsInit) {
                remoteConfig = DbAdapter.getInstance().getRemoteConfigFromLocal();
                mIsInit = false;
            } else {
                remoteConfig = DbAdapter.getInstance().getRemoteConfig();
            }
            SensorsDataSDKRemoteConfig sdkRemoteConfig = toSDKRemoteConfig(remoteConfig);
            if (SALog.isLogEnabled()) {
                SALog.i(TAG, "Cache remote config is " + sdkRemoteConfig.toString());
            }
            if (mSensorsDataAPI != null) {
                //关闭 debug 模式
                if (sdkRemoteConfig.isDisableDebugMode()) {
                    mSensorsDataAPI.setDebugMode(SensorsDataAPI.DebugMode.DEBUG_OFF);
                    SALog.i(TAG, "Set DebugOff Mode");
                }

                if (sdkRemoteConfig.isDisableSDK()) {
                    try {
                        // note: must be SAContextManger before SensorsDataAPI init completed
                        mContextManager.getAnalyticsMessages().flush();
                        SALog.i(TAG, "DisableSDK is true");
                    } catch (Exception e) {
                        SALog.printStackTrace(e);
                    }
                }
            }
            mSDKRemoteConfig = sdkRemoteConfig;
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }
}
