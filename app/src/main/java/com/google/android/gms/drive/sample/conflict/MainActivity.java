/**
 * Copyright 2014 Google Inc. All Rights Reserved.
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

package com.google.android.gms.drive.sample.conflict;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.ExecutionOptions;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * Main Activity of the application where "Grocery List" is displayed, edited and saved.
 */
public class MainActivity extends BaseDemoActivity {

    private static final String TAG = "MainActivity";

    protected EditText groceryListEditText;
    protected Button updateGroceryListButton;

    // Instance variables used for DriveFile and DriveContents to help initiate file conflicts.
    protected DriveFile groceryListFile;
    protected DriveContents groceryListContents;

    // Receiver used to update the EditText once conflicts have been resolved.
    protected BroadcastReceiver broadcastReceiver;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);

        groceryListEditText = (EditText) findViewById(R.id.editText);
        updateGroceryListButton = (Button) findViewById(R.id.button);

        updateGroceryListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (groceryListContents != null) {
                    groceryListContents.reopenForWrite(getGoogleApiClient())
                            .setResultCallback(updateDriveContensCallback);
                    // Disable update button to prevent double taps.
                    updateGroceryListButton.setEnabled(false);
                }
            }
        });

        // When conflicts are resolved, update the EditText with the resolved list
        // then open the contents so it contains the resolved list.
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(ConflictResolver.CONFLICT_RESOLVED)) {
                    Log.d(TAG, "Received intent to update edit text.");
                    String resolvedStr = intent.getStringExtra("conflictResolution");
                    groceryListEditText.setText(resolvedStr);
                    // Open {@code groceryListFile} in read only mode to update
                    // {@code groceryListContents} to current base state.
                    groceryListFile.open(getGoogleApiClient(), DriveFile.MODE_READ_ONLY, null)
                            .setResultCallback(driveContentsCallback);
                }
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(ConflictResolver.CONFLICT_RESOLVED));
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        super.onStop();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        super.onConnected(connectionHint);
        // Syncing to help devices use the same file.
        Drive.DriveApi.requestSync(getGoogleApiClient()).setResultCallback(syncCallback);
    }

    // Callback when requested sync returns.
    private ResultCallback<Status> syncCallback = new ResultCallback<Status>() {
        @Override
        public void onResult(Status status) {
            if (!status.isSuccess()) {
                Log.e(TAG, "Unable to sync.");
            }
            Query query = new Query.Builder()
                    .addFilter(Filters.eq(SearchableField.TITLE,
                            getResources().getString(R.string.groceryListFileName)))
                    .build();
            Drive.DriveApi.query(getGoogleApiClient(), query).setResultCallback(metadataCallback);
        }
    };

    // Callback when search for the grocery list file returns. It sets {@code groceryListFile} if
    // it exists or initiates the creation of a new file if no file is found.
    private ResultCallback<DriveApi.MetadataBufferResult> metadataCallback =
            new ResultCallback<DriveApi.MetadataBufferResult>() {
        @Override
        public void onResult(DriveApi.MetadataBufferResult metadataBufferResult) {
            if (!metadataBufferResult.getStatus().isSuccess()) {
                showMessage("Problem while retrieving results.");
                return;
            }
            int results = metadataBufferResult.getMetadataBuffer().getCount();
            if (results > 0) {
                // If the file exists then use it.
                DriveId driveId = metadataBufferResult.getMetadataBuffer().get(0).getDriveId();
                groceryListFile = Drive.DriveApi.getFile(getGoogleApiClient(), driveId);
                groceryListFile.open(getGoogleApiClient(), DriveFile.MODE_READ_ONLY, null)
                        .setResultCallback(driveContentsCallback);
            } else {
                // If the file does not exist then create one.
                Drive.DriveApi.newDriveContents(getGoogleApiClient())
                        .setResultCallback(newContentsCallback);
            }
        }
    };

    // Callback when {@code groceryListContents} is reopened for writing.
    private ResultCallback<DriveApi.DriveContentsResult> updateDriveContensCallback =
            new ResultCallback<DriveApi.DriveContentsResult>() {
        @Override
        public void onResult(DriveApi.DriveContentsResult driveContentsResult) {
            if (!driveContentsResult.getStatus().isSuccess()) {
                Log.e(TAG, "Unable to updated grocery list.");
                return;
            }
            DriveContents driveContents = driveContentsResult.getDriveContents();
            OutputStream outputStream = driveContents.getOutputStream();
            Writer writer = new OutputStreamWriter(outputStream);
            try {
                writer.write(groceryListEditText.getText().toString());
                writer.close();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }

            // ExecutionOptions define the conflict strategy to be used.
            ExecutionOptions executionOptions = new ExecutionOptions.Builder()
                    .setNotifyOnCompletion(true)
                    .setConflictStrategy(ExecutionOptions.CONFLICT_STRATEGY_KEEP_REMOTE)
                    .build();
            driveContents.commit(getGoogleApiClient(), null, executionOptions)
                    .setResultCallback(fileWrittenCallback);

            Log.d(TAG, "Saving file.");
        }
    };

    // Callback when file has been written locally.
    private ResultCallback<Status> fileWrittenCallback = new ResultCallback<Status>() {
        @Override
        public void onResult(Status status) {
            if (!status.isSuccess()) {
                Log.e(TAG, "Unable to write grocery list.");
            }
            Log.d(TAG, "File saved locally.");
            groceryListFile.open(getGoogleApiClient(), DriveFile.MODE_READ_ONLY, null)
                    .setResultCallback(driveContentsCallback);
        }
    };

    // Callback when {@code DriveApi.DriveContentsResult} for the creation of a new
    // {@code DriveContents} has been returned.
    private ResultCallback<DriveApi.DriveContentsResult> newContentsCallback =
            new ResultCallback<DriveApi.DriveContentsResult>() {
        @Override
        public void onResult(DriveApi.DriveContentsResult driveContentsResult) {
            if (!driveContentsResult.getStatus().isSuccess()) {
                Log.e(TAG, "Unable to create grocery list file contents.");
                return;
            }
            Log.d(TAG, "grocery_list new file contents returned.");
            groceryListContents = driveContentsResult.getDriveContents();

            MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                    .setTitle(getResources().getString(R.string.groceryListFileName))
                    .setMimeType("text/plain")
                    .build();
            // create a file on root folder
            Drive.DriveApi.getRootFolder(getGoogleApiClient())
                    .createFile(getGoogleApiClient(), changeSet, groceryListContents)
                    .setResultCallback(groceryListFileCallback);
        }
    };

    // Callback when request to create grocery list file is returned.
    private ResultCallback<DriveFolder.DriveFileResult> groceryListFileCallback =
            new ResultCallback<DriveFolder.DriveFileResult>() {
        @Override
        public void onResult(DriveFolder.DriveFileResult driveFileResult) {
            if (!driveFileResult.getStatus().isSuccess()) {
                Log.e(TAG, "Unable to create grocery list file.");
                return;
            }
            Log.d(TAG, "Grocery list file returned.");
            groceryListFile = driveFileResult.getDriveFile();
            // Open {@code groceryListFile} in read only mode to update
            // {@code groceryListContents} to current base state.
            groceryListFile.open(getGoogleApiClient(), DriveFile.MODE_READ_ONLY, null)
                    .setResultCallback(driveContentsCallback);
        }
    };

    // Callback when request to open {@code groceryListFile} in read only mode is returned.
    private ResultCallback<DriveApi.DriveContentsResult> driveContentsCallback =
            new ResultCallback<DriveApi.DriveContentsResult>() {
        @Override
        public void onResult(DriveApi.DriveContentsResult driveContentsResult) {
            if (!driveContentsResult.getStatus().isSuccess()) {
                Log.e(TAG, "Unable to load grocery list data.");

                // Try to open {@code groceryListFile} again.
                groceryListFile.open(getGoogleApiClient(), DriveFile.MODE_READ_ONLY, null)
                        .setResultCallback(driveContentsCallback);
                return;
            }
            groceryListContents = driveContentsResult.getDriveContents();
            InputStream inputStream = groceryListContents.getInputStream();
            String groceryListStr = ConflictUtil.getStringFromInputStream(inputStream);

            // Only update {@code groceryListEditText} initially when {@code groceryListFile}
            // is opened.
            if (groceryListEditText.getText().toString().equals("Loading...")) {
                groceryListEditText.setText(groceryListStr);
            }

            // The text in {@code groceryListEditText} should be the same as text in
            // {@code groceryListContents} to enable {@code updateGroceryListButton}.
            if (groceryListEditText.getText().toString().trim().equals(groceryListStr.trim())) {
                updateGroceryListButton.setEnabled(true);
            }
        }
    };

}
