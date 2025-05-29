/*
 * Created by dengshiwei on 2022/08/10.
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

package com.sensorsdata.analytics.android.sdk.encrypt.encryptor;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.encrypt.AESSecretManager;
import com.sensorsdata.analytics.android.sdk.encrypt.SAEncryptListener;
import com.sensorsdata.analytics.android.sdk.encrypt.impl.AbsSAEncrypt;
import com.sensorsdata.analytics.android.sdk.encrypt.utils.EncryptUtils;

import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;

/**
 * EC+AES 加密
 */
public class SAECEncrypt extends AbsSAEncrypt {
    /**
     * 对称密钥
     */
    byte[] aesKey;

    /**
     * 加密后的对称密钥
     */
    String mEncryptKey;

    static {
        try {
            Class<?> provider = Class.forName("org.spongycastle.jce.provider.BouncyCastleProvider");
            Security.addProvider((Provider) provider.newInstance());
        } catch (Exception e) {
            SALog.i("SA.SAECEncrypt", e.toString());
        }
    }

    @Override
    public String symmetricEncryptType() {
        return "AES";
    }

    @Override
    public String encryptEvent(byte[] event) {
        return EncryptUtils.symmetricEncrypt(aesKey, event, SymmetricEncryptMode.AES);
    }

    @Override
    public String asymmetricEncryptType() {
        return "EC";
    }

    @Override
    public String encryptSymmetricKeyWithPublicKey(String publicKey) {
        if (mEncryptKey == null) {
            try {
                aesKey = EncryptUtils.generateSymmetricKey(SymmetricEncryptMode.AES);
                mEncryptKey = EncryptUtils.encryptAESKey(publicKey, aesKey, "EC");
            } catch (NoSuchAlgorithmException e) {
                SALog.printStackTrace(e);
                return null;
            }
        }
        return mEncryptKey;
    }

    @Override
    public String encryptEventRecord(String eventJson) {
        return AESSecretManager.getInstance().encryptAES(eventJson);
    }

    @Override
    public String decryptEventRecord(String encryptEvent) {
        return AESSecretManager.getInstance().decryptAES(encryptEvent);
    }
}
