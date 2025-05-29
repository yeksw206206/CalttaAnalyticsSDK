/*
 * Created by dengshiwei on 2022/09/13.
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

package com.sensorsdata.analytics.android.sdk.jsbridge;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.webkit.JavascriptInterface;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.internal.beans.ServerUrl;
import com.sensorsdata.analytics.android.sdk.core.mediator.SAModuleManager;
import com.sensorsdata.analytics.android.sdk.core.mediator.Modules;
import com.sensorsdata.analytics.android.sdk.util.ReflectUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

public class AppWebViewInterface {
    private static final String TAG = "SA.AppWebViewInterface";
    private final Context mContext;
    private JSONObject properties;
    private final boolean enableVerify;
    private WeakReference<View> mWebView;

    public AppWebViewInterface(Context c, JSONObject p, boolean b) {
        this(c, p, b, null);
    }

    public AppWebViewInterface(Context c, JSONObject p, boolean b, View view) {
        this.mContext = c;
        this.properties = p;
        this.enableVerify = b;
        if (view != null) {
            this.mWebView = new WeakReference<>(view);
        }
    }

    @JavascriptInterface
    public String sensorsdata_call_app() {
        try {
            SALog.i(TAG, "sensorsdata_call_app");
            if (properties == null) {
                properties = new JSONObject();
            }
            properties.put("type", "Android");
            String loginId = SensorsDataAPI.sharedInstance(mContext).getLoginId();
            if (!TextUtils.isEmpty(loginId)) {
                properties.put("distinct_id", loginId);
                properties.put("is_login", true);
            } else {
                properties.put("distinct_id", SensorsDataAPI.sharedInstance(mContext).getAnonymousId());
                properties.put("is_login", false);
            }
            return properties.toString();
        } catch (JSONException e) {
            SALog.i(TAG, e.getMessage());
        }
        return null;
    }

    @JavascriptInterface
    public void sensorsdata_track(String event) {
        try {
            SALog.i(TAG, "sensorsdata_track event = " + event);
            H5Helper.trackEventFromH5(event, enableVerify);
        } catch (Exception e) {
            SALog.printStackTrace(e);
            SALog.i(TAG, "sensorsdata_track event = exception = " + event);
        }
    }

    @JavascriptInterface
    public boolean sensorsdata_verify(String event) {
        try {
            SALog.i(TAG, "sensorsdata_verify event = " + event + ", enableVerify = " + enableVerify);
            if (!enableVerify) {
                sensorsdata_track(event);
                return true;
            }
            return H5Helper.verifyEventFromH5(event);
        } catch (Exception e) {
            SALog.printStackTrace(e);
            SALog.i(TAG, "sensorsdata_verify return false,exception = " + e.getMessage());
            return false;
        }
    }

    @JavascriptInterface
    public String sensorsdata_get_server_url() {
        try {
            SALog.i(TAG, "sensorsdata_get_server_url");
            return SensorsDataAPI.getConfigOptions().isAutoTrackWebView() ? SensorsDataAPI.getConfigOptions().getServerUrl() : "";
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return "";
    }

    /**
     * 解决用户只调用了 showUpWebView 方法时，此时 App 校验 url。JS 需要拿到 App 校验结果。
     *
     * @param event 事件
     * @return 可视化校验结果
     */
    @JavascriptInterface
    public boolean sensorsdata_visual_verify(String event) {
        try {
            if (!enableVerify) {
                return true;
            }
            if (TextUtils.isEmpty(event)) {
                return false;
            }
            JSONObject eventObject = new JSONObject(event);
            String serverUrl = eventObject.optString("server_url");
            if (!TextUtils.isEmpty(serverUrl)) {
                return new ServerUrl(serverUrl).check(new ServerUrl(SensorsDataAPI.getConfigOptions().getServerUrl()));
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return false;
    }

    /**
     * 新打通方案下 js 调用 app 的通道,该接口可复用
     *
     * @param content JS 发送的消息
     */
    @JavascriptInterface
    public void sensorsdata_js_call_app(final String content) {
        try {
            SALog.i(TAG, "sensorsdata_js_call_app, content = " + content);
            if (mWebView != null) {
                H5Helper.handleJsMessage(mWebView, content);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 判断 A/B Testing 是否初始化
     *
     * @return A/B Testing SDK 是否初始化
     */
    @JavascriptInterface
    public boolean sensorsdata_abtest_module() {
        try {
            SALog.i(TAG, "sensorsdata_abtest_module");
            Class<?> sensorsABTestClass = ReflectUtil.getCurrentClass(new String[]{"com.sensorsdata.abtest.SensorsABTest"});
            Object object = ReflectUtil.callStaticMethod(sensorsABTestClass, "shareInstance");
            return object != null;
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return false;
    }

    /**
     * JS 从 App 侧获取结果的统一入口
     * 该接口的设计上和 'sensorsdata_js_call_app' 接口有所重复，'sensorsdata_js_call_app' 接口设计存在缺陷，没法有返回值。
     * 本次新增该接口适用于有返回值场景
     *
     * @return 同步返回给 JS 侧结果
     */
    @JavascriptInterface
    public String sensorsdata_get_app_visual_config() {
        try {
            SALog.i(TAG, "sensorsdata_get_app_visual_config");
            return SAModuleManager.getInstance().invokeModuleFunction(Modules.Visual.MODULE_NAME, Modules.Visual.METHOD_H5_GET_APPVISUAL_CONFIG);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return "";
    }
}
