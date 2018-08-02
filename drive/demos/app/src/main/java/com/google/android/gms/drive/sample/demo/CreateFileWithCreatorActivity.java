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

import android.content.Intent;
import android.content.IntentSender;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.drive.CreateFileActivityOptions;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.OpenFileActivityOptions;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * An activity that illustrates how to use the creator
 * intent to create a new file. The creator intent allows the user
 * to select the parent folder and the title of the newly
 * created file.
 */
public class CreateFileWithCreatorActivity extends BaseDemoActivity {
    private static final String TAG = "CreateFileWithCreator";
    private static final int REQUEST_CODE_CREATE_FILE = 2;

    @Override
    protected void onDriveClientReady() {
        createFileWithIntent();
    }

    private void createFileWithIntent() {
        // [START drive_android_create_file_with_intent]
        Task<DriveContents> createContentsTask = getDriveResourceClient().createContents();
        createContentsTask
                .continueWithTask(task -> {
                    DriveContents contents = task.getResult();
                    OutputStream outputStream = contents.getOutputStream();
                    try (Writer writer = new OutputStreamWriter(outputStream)) {
                        writer.write("Hello World!");
                    }

                    MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                                          .setTitle("New file")
                                                          .setMimeType("text/plain")
                                                          .setStarred(true)
                                                          .build();

                    CreateFileActivityOptions createOptions =
                            new CreateFileActivityOptions.Builder()
                                    .setInitialDriveContents(contents)
                                    .setInitialMetadata(changeSet)
                                    .build();
                    return getDriveClient().newCreateFileActivityIntentSender(createOptions);
                })
                .addOnSuccessListener(this,
                        intentSender -> {
                            try {
                                startIntentSenderForResult(
                                        intentSender, REQUEST_CODE_CREATE_FILE, null, 0, 0, 0);
                            } catch (IntentSender.SendIntentException e) {
                                Log.e(TAG, "Unable to create file", e);
                                showMessage(getString(R.string.file_create_error));
                                finish();
                            }
                        })
                .addOnFailureListener(this, e -> {
                    Log.e(TAG, "Unable to create file", e);
                    showMessage(getString(R.string.file_create_error));
                    finish();
                });
        // [END drive_android_create_file_with_intent]
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_CREATE_FILE) {
            if (resultCode != RESULT_OK) {
                Log.e(TAG, "Unable to create file");
                showMessage(getString(R.string.file_create_error));
            } else {
                DriveId driveId =
                        data.getParcelableExtra(OpenFileActivityOptions.EXTRA_RESPONSE_DRIVE_ID);
                showMessage(getString(R.string.file_created, "File created with ID: " + driveId));
            }
            finish();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
