/*
 * Created by dengshiwei on 2022/06/28.
 * Copyright 2015－2021 Sensors Data Inc.
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

package com.sensorsdata.sdk.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.visual.VisualizedAutoTrackService;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initLambdaButton();
        initButton();

        //VisualizedAutoTrackService.getInstance().start(this, "caltta_track_code", SA_SERVER_URL);
    }

    public void onViewClick(View view) {

    }

    private void initLambdaButton() {
        Button button = (Button) findViewById(R.id.clickButton);
        button.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ClickActivity.class));
        });
    }

    private void initButton() {
        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, FuncActivity.class));
            }
        });

        findViewById(R.id.clickInstall).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 调用激活接口触发激活事件
                SensorsDataAPI.sharedInstance().trackAppInstall();

                JSONObject properties = new JSONObject();
                try {
                    properties.put("contact_number", "13422188614");
                    properties.put("contact_name", "yeksw");
                } catch (Exception e) {
                }
                SensorsDataAPI.sharedInstance().track("save_contact", properties);
            }
        });


        findViewById(R.id.save_contact).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveContact();
            }
        });

        findViewById(R.id.clickFrament).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, FragmentTestActivity.class));
            }
        });

    }

    private final static String SA_SERVER_URL = "https://sdkdebugtest.datasink.sensorsdata.cn/sa?project=default&token=cfb8b60e42e0ae9b";

    private void saveContact() {
       // VisualizedAutoTrackService.getInstance().start(this, "caltta_track_code", SA_SERVER_URL);
    }
}
