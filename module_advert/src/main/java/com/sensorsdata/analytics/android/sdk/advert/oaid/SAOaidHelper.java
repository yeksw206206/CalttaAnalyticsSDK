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

package com.sensorsdata.analytics.android.sdk.advert.oaid;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Looper;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.advert.oaid.impl.OAIDFactory;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.core.business.SAPropertyManager;
import com.sensorsdata.analytics.android.sdk.internal.beans.LimitKey;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class SAOaidHelper {
    private static final String TAG = "SA.SAOaidHelper";
    private static String mOAID = "";
    private static String mReflectionOAID = "";
    private static CountDownLatch mCountDownLatch;
    private static Class<?> mIdentifyListener;
    private static Class<?> mIdSupplier;
    private static Class<?> jLibrary;
    private static Class<?> mMidSDKHelper;
    /**
     * 自定义 oaid 证书文件在 assets 中的路径
     */
    private static String mOidCertFilePath;

    private static final List<String> mLoadLibrary = new LinkedList<String>() {
        {
            add("msaoaidsec");                  // v1.0.30
            add("nllvm1632808251147706677");    // v1.0.29
            add("nllvm1630571663641560568");    // v1.0.27
            add("nllvm1623827671");             // v1.0.26
        }
    };

    static {
        initSDKLibrary();
    }

    /**
     * 获取 OAID 接口，注意该接口是同步接口，必须子线程中使用
     *
     * @param context Context
     * @return OAID
     */
    public static synchronized String getOpenAdIdentifier(final Context context) {
        if (!SensorsDataUtils.isOAIDEnabled()) {
            SALog.i(TAG, "Sensors getOAID disabled");
            return "";
        }
        // 默认使用客户注册的限制性属性
        if (SAPropertyManager.getInstance().isLimitKey(LimitKey.OAID)) {
            return SAPropertyManager.getInstance().getLimitValue(LimitKey.OAID);
        }
        if (Looper.getMainLooper() == Looper.myLooper()) {
            SALog.i(TAG, "function can not be called on main thread");
            return "";
        }
        if (!TextUtils.isEmpty(mOAID)) {
            return mOAID;
        }
        //获取 MSA OAID
        mOAID = getMSAOAID(context);
        SALog.i(TAG, "MSA OAID is " + mOAID);
        //校验 OAID 合法性
        //目前已知不合法 oaid 有
        //华为(00000000-0000-0000-0000-000000000000)
        //小米(00000000000000000000000000000000)
        //OPPO(64 个 0)
        if (TextUtils.isEmpty(mOAID) || allZero(mOAID)) {
            //通过反射获取 OAID
            mReflectionOAID = getROMOAID(context);
            //校验反射获取 OAID 的合法性
            if (TextUtils.isEmpty(mReflectionOAID) || allZero(mReflectionOAID)) {
                mReflectionOAID = "";
            }
            mOAID = mReflectionOAID;
            SALog.i(TAG, "Rom OAID is" + mOAID);
        }
        return mOAID;
    }

    /**
     * oaid 是否全部为 0
     *
     * @param id OAID
     * @return 是否全部为 0
     */
    private static boolean allZero(String id) {
        String replacedId = id.replace("-", "").replace("#", "").replace("_", "");
        for (int i = 0; i < replacedId.length(); i++) {
            char c = replacedId.charAt(i);
            if (c != '0') {
                return false;
            }
        }
        return true;
    }

    /**
     * 返回反射法获取的 OAID 值
     *
     * @param context context
     * @return 反射获取的 OAID 值
     */
    public static String getOpenAdIdentifierByReflection(Context context) {
        if (!SensorsDataUtils.isOAIDEnabled()) {
            SALog.i(TAG, "Sensors getOAIDReflection disabled");
            return "";
        }
        if (TextUtils.isEmpty(mOAID)) {
            getOpenAdIdentifier(context);
        }
        return mReflectionOAID;
    }

    private static String getROMOAID(Context context) {
        return OAIDFactory.create(context).getRomOAID();
    }

    private static String getMSAOAID(final Context context) {
        try {
            mCountDownLatch = new CountDownLatch(1);
            initInvokeListener();
            if (mMidSDKHelper == null || mIdentifyListener == null || mIdSupplier == null) {
                SALog.i(TAG, "OAID class create failed");
                return "";
            }
            if (TextUtils.isEmpty(mOAID)) {
                getOAIDReflect(context, 2);
            } else {
                return mOAID;
            }
            try {
                mCountDownLatch.await();
            } catch (InterruptedException e) {
                SALog.printStackTrace(e);
            }
            SALog.i(TAG, "CountDownLatch await");
            return mOAID;
        } catch (Throwable ex) {
            SALog.i(TAG, ex.getMessage());
        }
        return "";
    }

    /**
     * 通过反射获取 OAID，结果返回的 ErrorCode 如下：
     * 1008611：不支持的设备厂商
     * 1008612：不支持的设备
     * 1008613：加载配置文件出错
     * 1008614：获取接口是异步的，结果会在回调中返回，回调执行的回调可能在工作线程
     * 1008615：反射调用出错
     *
     * @param context Context
     * @param retryCount 重试次数
     */
    private static void getOAIDReflect(Context context, int retryCount) {
        try {
            if (retryCount == 0) {
                return;
            }
            final int INIT_ERROR_RESULT_DELAY = 1008614;            //获取接口是异步的，结果会在回调中返回，回调执行的回调可能在工作线程
            final int INIT_ERROR_RESULT_OK = 1008610;            //获取成功
            // 初始化证书
            initPemCert(context);
            // 初始化 Library
            if (jLibrary != null) {
                Method initEntry = jLibrary.getDeclaredMethod("InitEntry", Context.class);
                initEntry.invoke(null, context);
            }
            IdentifyListenerHandler handler = new IdentifyListenerHandler();
            Method initSDK = mMidSDKHelper.getDeclaredMethod("InitSdk", Context.class, boolean.class, mIdentifyListener);
            int errCode = (int) initSDK.invoke(null, context, true, Proxy.newProxyInstance(context.getClassLoader(), new Class[]{mIdentifyListener}, handler));
            SALog.i(TAG, "MdidSdkHelper ErrorCode : " + errCode);
            if (errCode != INIT_ERROR_RESULT_DELAY && errCode != INIT_ERROR_RESULT_OK) {
                getOAIDReflect(context, --retryCount);
                if (retryCount == 0) {
                    mCountDownLatch.countDown();
                }
            }

            /*
             * 此处是为了适配三星部分手机，根据 MSA 工作人员反馈，对于三星部分机型的支持有 bug，导致
             * 返回 1008614 错误码，但是不会触发回调。所以此处的逻辑是，两秒之后主动放弃阻塞。
             */
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        //ignore
                    }
                    mCountDownLatch.countDown();
                }
            }).start();
        } catch (Throwable ex) {
            SALog.i(TAG, ex.getMessage());
            getOAIDReflect(context, --retryCount);
            if (retryCount == 0) {
                mCountDownLatch.countDown();
            }
        }
    }

    static class IdentifyListenerHandler implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            try {
                if ("OnSupport".equalsIgnoreCase(method.getName())) {
                    Method getOAID = mIdSupplier.getDeclaredMethod("getOAID");
                    if (args.length == 1) {// v1.0.26 版本只有 1 个参数
                        mOAID = (String) getOAID.invoke(args[0]);
                    } else {
                        mOAID = (String) getOAID.invoke(args[1]);
                    }

                    SALog.i(TAG, "oaid:" + mOAID);
                    mCountDownLatch.countDown();
                }
            } catch (Throwable ex) {
                mCountDownLatch.countDown();
            }
            return null;
        }
    }

    private static void initInvokeListener() {
        try {
            mMidSDKHelper = Class.forName("com.bun.miitmdid.core.MdidSdkHelper");
        } catch (ClassNotFoundException e) {
            SALog.i(TAG, e.getMessage());
            return;
        }
        // 尝试 1.0.22 版本
        try {
            mIdentifyListener = Class.forName("com.bun.miitmdid.interfaces.IIdentifierListener");
            mIdSupplier = Class.forName("com.bun.miitmdid.interfaces.IdSupplier");
            return;
        } catch (Exception ex) {
            // ignore
        }

        // 尝试 1.0.13 - 1.0.21 版本
        try {
            mIdentifyListener = Class.forName("com.bun.supplier.IIdentifierListener");
            mIdSupplier = Class.forName("com.bun.supplier.IdSupplier");
            jLibrary = Class.forName("com.bun.miitmdid.core.JLibrary");
            return;
        } catch (Exception ex) {
            // ignore
        }

        // 尝试 1.0.5 - 1.0.13 版本
        try {
            mIdentifyListener = Class.forName("com.bun.miitmdid.core.IIdentifierListener");
            mIdSupplier = Class.forName("com.bun.miitmdid.supplier.IdSupplier");
            jLibrary = Class.forName("com.bun.miitmdid.core.JLibrary");
        } catch (Exception ex) {
            // ignore
        }
    }

    private static void initSDKLibrary() {
        for (String library : mLoadLibrary) {
            try {
                System.loadLibrary(library);  // 加载 SDK 安全库
                break;
            } catch (Throwable throwable) {
                // ignore
            }
        }
    }

    /**
     * 初始化证书
     *
     * @param context Context
     */
    private static void initPemCert(Context context) {
        try {
            String oaidCert = loadPemFromAssetFile(context);
            if (!TextUtils.isEmpty(oaidCert)) {
                Method initCert = mMidSDKHelper.getDeclaredMethod("InitCert", Context.class, String.class);
                initCert.invoke(null, context, oaidCert);
            }
        } catch (Throwable e) {
            SALog.i(TAG, e.getMessage());
        }
    }

    /**
     * 从asset文件读取证书内容
     *
     * @param context Context
     * @return 证书字符串
     */
    private static String loadPemFromAssetFile(Context context) {
        try {
            String defaultPemCert = context.getPackageName() + ".cert.pem";
            InputStream is;
            AssetManager assetManager = context.getAssets();
            if (!TextUtils.isEmpty(mOidCertFilePath)) {
                try {
                    is = assetManager.open(mOidCertFilePath);
                } catch (IOException e) {
                    is = assetManager.open(defaultPemCert);
                }
            } else {
                is = assetManager.open(defaultPemCert);
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                builder.append(line);
                builder.append('\n');
            }
            return builder.toString();
        } catch (IOException e) {
            SALog.i(TAG, "loadPemFromAssetFile failed");
            return "";
        }
    }

    /**
     * 定义 oaid 证书在 assets 中的文件相对路径
     *
     * @param filePath oaid 证书在 assets 中的相对路径，eg："file/test.cert.pom"
     */
    public static void setOaidCertFilePath(String filePath) {
        mOidCertFilePath = filePath;
    }
}