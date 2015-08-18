// Copyright 2014 Google Inc. All Rights Reserved.

package com.google.android.gms.drive.sample.conflict;

import com.google.android.gms.drive.events.CompletionEvent;
import com.google.android.gms.drive.events.DriveEventService;

public class MyDriveEventService extends DriveEventService {

    @Override
    public void onCompletion(CompletionEvent event) {
        if (event.getStatus() == CompletionEvent.STATUS_CONFLICT) {
            // Handle completion conflict.
            ConflictResolver conflictResolver = new ConflictResolver(event, this);
            conflictResolver.resolve();
        } else if (event.getStatus() == CompletionEvent.STATUS_FAILURE) {
            // Handle completion failure.

            // CompletionEvent is only dismissed here, in a real world application failure should
            // be handled before the event is dismissed.
            event.dismiss();
        } else if (event.getStatus() == CompletionEvent.STATUS_SUCCESS) {
            // Commit completed successfully.
            event.dismiss();
        }
    }
}
