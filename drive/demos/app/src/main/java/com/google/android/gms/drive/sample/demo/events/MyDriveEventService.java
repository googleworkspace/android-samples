/*
 * Copyright 2013 Google Inc. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.gms.drive.sample.demo.events;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.drive.events.ChangeEvent;
import com.google.android.gms.drive.events.DriveEventService;

// [START on_change]
/**
 * Listens for file change events when subscribed to a file.
 * [START_EXCLUDE]
 * For this sample, events are rebroadcast locally so they can be displayed in the activity.
 * [END_EXCLUDE]
 */
public class MyDriveEventService extends DriveEventService {
    private static final String TAG = "MyDriveEventService";

    // [START_EXCLUDE]
    public static final String CHANGE_EVENT =
            "com.google.android.gms.drive.sample.demo.CHANGE_EVENT";
    private LocalBroadcastManager mBroadcaster;

    @Override
    public void onCreate() {
        super.onCreate();
        mBroadcaster = LocalBroadcastManager.getInstance(getApplicationContext());
    }
    // [END_EXCLUDE]

    @Override
    public void onChange(ChangeEvent changeEvent) {
        // Handle change event..
        // [START_EXCLUDE]
        Log.d(TAG, "Received event: " + changeEvent);
        // For demo purposes, just rebroadcast so activity can display
        Intent intent = new Intent(CHANGE_EVENT);
        intent.putExtra("event", changeEvent);
        mBroadcaster.sendBroadcast(intent);
        // [END_EXCLUDE]
    }
}
// [END on_change]