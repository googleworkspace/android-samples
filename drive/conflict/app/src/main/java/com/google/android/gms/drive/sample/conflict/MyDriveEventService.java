/*
 * Copyright 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.gms.drive.sample.conflict;

import android.util.Log;

import com.google.android.gms.drive.events.CompletionEvent;
import com.google.android.gms.drive.events.DriveEventService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// [START drive_android_on_completion]
public class MyDriveEventService extends DriveEventService {
    private static final String TAG = "MyDriveEventService";
    private ExecutorService mExecutorService;

    @Override
    public void onCreate() {
        super.onCreate();
        // [START_EXCLUDE]
        mExecutorService = Executors.newSingleThreadExecutor();
        // [END_EXCLUDE]
    }

    @Override
    public synchronized void onDestroy() {
        super.onDestroy();
        // [START_EXCLUDE]
        mExecutorService.shutdown();
        // [END_EXCLUDE]
    }

    @Override
    public void onCompletion(CompletionEvent event) {
        boolean eventHandled = false;
        switch (event.getStatus()) {
            case CompletionEvent.STATUS_SUCCESS:
                // Commit completed successfully.
                // Can now access the remote resource Id
                // [START_EXCLUDE]
                String resourceId = event.getDriveId().getResourceId();
                Log.d(TAG, "Remote resource ID: " + resourceId);
                eventHandled = true;
                // [END_EXCLUDE]
                break;
            case CompletionEvent.STATUS_FAILURE:
                // Handle failure....
                // Modified contents and metadata failed to be applied to the server.
                // They can be retrieved from the CompletionEvent to try to be applied later.
                // [START_EXCLUDE]
                // CompletionEvent is only dismissed here. In a real world application failure
                // should be handled before the event is dismissed.
                eventHandled = true;
                // [END_EXCLUDE]
                break;
            case CompletionEvent.STATUS_CONFLICT:
                // Handle completion conflict.
                // [START_EXCLUDE]
                ConflictResolver conflictResolver =
                        new ConflictResolver(event, this, mExecutorService);
                conflictResolver.resolve();
                eventHandled = false; // Resolver will snooze or dismiss
                // [END_EXCLUDE]
                break;
        }

        if (eventHandled) {
            event.dismiss();
        }
    }
}
// [END drive_android_on_completion]