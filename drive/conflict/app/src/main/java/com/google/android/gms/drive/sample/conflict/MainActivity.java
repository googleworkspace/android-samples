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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.ExecutionOptions;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * Main Activity of the application where "Grocery List" is displayed, edited and saved.
 */
public class MainActivity extends BaseDemoActivity {
    private static final String TAG = "MainActivity";

    private EditText mEditText;
    private Button mUpdateGroceryListButton;
    // Instance variables used for DriveFile and DriveContents to help initiate file conflicts.
    private DriveFile mGroceryListFile;
    private DriveContents mDriveContents;
    // Receiver used to update the EditText once conflicts have been resolved.
    private BroadcastReceiver mBroadcastReceiver;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);

        mEditText = findViewById(R.id.editText);
        mUpdateGroceryListButton = findViewById(R.id.button);
        mUpdateGroceryListButton.setOnClickListener(view -> {
            if (mGroceryListFile != null) {
                mUpdateGroceryListButton.setEnabled(false);
                mEditText.setEnabled(false);
                saveFile()
                        .addOnCompleteListener(task -> {
                            mEditText.setEnabled(true);
                            mUpdateGroceryListButton.setEnabled(true);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Unexpected error", e);
                            showMessage(getString(R.string.unexpected_error));
                        });
            }
        });

        // When conflicts are resolved, update the EditText with the resolved list
        // then open the contents so it contains the resolved list.
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(ConflictResolver.CONFLICT_RESOLVED)) {
                    Log.d(TAG, "Received intent to update edit text.");
                    showMessage(getString(R.string.reload_after_conflict));
                    loadContents(mGroceryListFile).addOnFailureListener(e -> {
                        Log.e(TAG, "Unexpected error", e);
                        showMessage(getString(R.string.unexpected_error));
                    });
                }
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mBroadcastReceiver, new IntentFilter(ConflictResolver.CONFLICT_RESOLVED));
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        super.onStop();
    }

    @Override
    protected void onDriveClientReady() {
        getDriveClient()
                .requestSync()
                .continueWithTask(task -> initializeGroceryList())
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Unexpected error", e);
                    showMessage(getString(R.string.unexpected_error));
                });
    }

    /**
     * Retrieves the list from Drive if it exists. If not, create a new list.
     */
    private Task<Void> initializeGroceryList() {
        Log.d(TAG, "Locating grocery list file.");
        Query query = new Query.Builder()
                              .addFilter(Filters.eq(SearchableField.TITLE,
                                      getResources().getString(R.string.groceryListFileName)))
                              .build();
        return getDriveResourceClient()
                .query(query)
                .continueWithTask(task -> {
                    MetadataBuffer metadataBuffer = task.getResult();
                    try {
                        if (metadataBuffer.getCount() == 0) {
                            return createNewFile();
                        } else {
                            DriveId id = metadataBuffer.get(0).getDriveId();
                            return Tasks.forResult(id.asDriveFile());
                        }
                    } finally {
                        metadataBuffer.release();
                    }
                })
                .continueWithTask(task -> loadContents(task.getResult()));
    }

    /**
     * Gets the grocery list items.
     */
    private Task<Void> loadContents(DriveFile file) {
        mGroceryListFile = file;
        Task<DriveContents> loadTask =
                getDriveResourceClient().openFile(file, DriveFile.MODE_READ_ONLY);
        return loadTask.continueWith(task -> {
            Log.d(TAG, "Reading file contents.");
            mDriveContents = task.getResult();
            InputStream inputStream = mDriveContents.getInputStream();
            String groceryListStr = ConflictUtil.getStringFromInputStream(inputStream);

            mEditText.setText(groceryListStr);
            return null;
        });
    }

    private Task<DriveFile> createNewFile() {
        Log.d(TAG, "Creating new grocery list.");
        return getDriveResourceClient().getRootFolder().continueWithTask(
                task -> {
                    DriveFolder folder = task.getResult();
                    MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                                          .setTitle(getResources().getString(
                                                                  R.string.groceryListFileName))
                                                          .setMimeType("text/plain")
                                                          .build();

                    return getDriveResourceClient().createFile(folder, changeSet, null);
                });
    }

    private Task<Void> saveFile() {
        Log.d(TAG, "Saving file.");
        // [START drive_android_reopen_for_write]
        Task<DriveContents> reopenTask =
                getDriveResourceClient().reopenContentsForWrite(mDriveContents);
        // [END drive_android_reopen_for_write]
        return reopenTask
                .continueWithTask(task -> {
                    // [START drive_android_write_conflict_strategy]
                    DriveContents driveContents = task.getResult();
                    OutputStream outputStream = driveContents.getOutputStream();
                    try (Writer writer = new OutputStreamWriter(outputStream)) {
                        writer.write(mEditText.getText().toString());
                    }
                    // ExecutionOptions define the conflict strategy to be used.
                    // [START drive_android_execution_options]
                    ExecutionOptions executionOptions =
                            new ExecutionOptions.Builder()
                                    .setNotifyOnCompletion(true)
                                    .setConflictStrategy(
                                            ExecutionOptions.CONFLICT_STRATEGY_KEEP_REMOTE)
                                    .build();
                    return getDriveResourceClient().commitContents(
                            driveContents, null, executionOptions);
                    // [END drive_android_execution_options]
                    // [END drive_android_write_conflict_strategy]
                })
                .continueWithTask(task -> {
                    showMessage(getString(R.string.file_saved));
                    Log.d(TAG, "Reopening file for read.");
                    return loadContents(mGroceryListFile);
                });
    }
}
