// Copyright 2014 Google Inc. All Rights Reserved.

package com.google.android.gms.drive.sample.conflict;

import android.util.Log;

import com.google.android.gms.drive.events.CompletionEvent;
import com.google.android.gms.drive.events.DriveEventService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// [START on_completion]
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
// [END on_completion]