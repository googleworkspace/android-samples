/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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
package com.google.android.gms.drive.sample.demo;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

/**
 * An activity that pins a file to the device. Pinning allows
 * a file's latest version to be available locally all the time.
 * Your users should be informed about the extra bandwidth
 * and storage requirements of pinning.
 */
public class PinFileActivity extends BaseDemoActivity {
    private static final String TAG = "PinFileActivity";

    @Override
    protected void onDriveClientReady() {
        pickTextFile()
                .addOnSuccessListener(this,
                        driveId -> pinFile(driveId.asDriveFile()))
                .addOnFailureListener(this, e -> {
                    Log.e(TAG, "No file selected", e);
                    showMessage(getString(R.string.file_not_selected));
                    finish();
                });
    }

    private void pinFile(final DriveFile file) {
        // [START drive_android_pin_file]
        Task<Metadata> pinFileTask = getDriveResourceClient().getMetadata(file).continueWithTask(
                task -> {
                    Metadata metadata = task.getResult();
                    if (!metadata.isPinnable()) {
                        showMessage(getString(R.string.file_not_pinnable));
                        return Tasks.forResult(metadata);
                    }
                    if (metadata.isPinned()) {
                        showMessage(getString(R.string.file_already_pinned));
                        return Tasks.forResult(metadata);
                    }
                    MetadataChangeSet changeSet =
                            new MetadataChangeSet.Builder().setPinned(true).build();
                    return getDriveResourceClient().updateMetadata(file, changeSet);
                });
        // [END drive_android_pin_file]
        // [START drive_android_pin_file_completion]
        pinFileTask
                .addOnSuccessListener(this,
                        metadata -> {
                            showMessage(getString(R.string.metadata_updated));
                            finish();
                        })
                .addOnFailureListener(this, e -> {
                    Log.e(TAG, "Unable to update metadata", e);
                    showMessage(getString(R.string.update_failed));
                    finish();
                });
        // [END drive_android_pin_file_completion]
    }
}
