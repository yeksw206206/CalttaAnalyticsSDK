/*
 * Created by dengshiwei on 2022/09/09.
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

package com.sensorsdata.analytics.android.sdk.visual.impl;

import com.sensorsdata.analytics.android.sdk.core.SAContextManager;
import com.sensorsdata.analytics.android.sdk.core.mediator.Modules;
import com.sensorsdata.analytics.android.sdk.core.mediator.protocol.SAModuleProtocol;
import com.sensorsdata.analytics.android.sdk.visual.property.VisualPropertiesManager;

public class SAVisualProtocolAPI implements SAModuleProtocol {
    private boolean mEnable = false;
    private static final String TAG = "SA.SAVisualProtocolImpl";
    private SAContextManager mSAContextManager;
    private VisualProtocolImpl mVisualImpl;

    @Override
    public void install(SAContextManager contextManager) {
        mSAContextManager = contextManager;
        mVisualImpl = new VisualProtocolImpl(contextManager);
        if (!contextManager.getInternalConfigs().saConfigOptions.isDisableSDK()) {
            setModuleState(true);
        }
    }

    @Override
    public void setModuleState(boolean enable) {
        if (mEnable != enable) {
            mEnable = enable;
        }
        if (enable && mSAContextManager.getInternalConfigs().saConfigOptions.isVisualizedPropertiesEnabled()) {
            // 可视化自定义属性拉取配置
            VisualPropertiesManager.getInstance().requestVisualConfig(mSAContextManager);
        }
    }

    @Override
    public String getModuleName() {
        return Modules.Visual.MODULE_NAME;
    }

    @Override
    public boolean isEnable() {
        return mEnable;
    }

    @Override
    public int getPriority() {
        return 5;
    }

    @Override
    public <T> T invokeModuleFunction(String methodName, Object... argv) {
        return mVisualImpl.invokeModuleFunction(methodName, argv);
    }
}
