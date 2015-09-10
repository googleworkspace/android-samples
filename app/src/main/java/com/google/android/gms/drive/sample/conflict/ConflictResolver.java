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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveApi.DriveContentsResult;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.ExecutionOptions;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.events.CompletionEvent;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * ConflictResolver handles a CompletionEvent with a conflict status.
 */
public class ConflictResolver implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private class HandleConflictTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            // Get base contents.
            InputStream baseInputStream = mConflictedCompletionEvent
                    .getBaseContentsInputStream();
            String baseStr = ConflictUtil.getStringFromInputStream(baseInputStream);

            // Get modified contents.
            InputStream modifiedInputStream = mConflictedCompletionEvent
                    .getModifiedContentsInputStream();
            String modifiedStr = ConflictUtil.getStringFromInputStream(modifiedInputStream);

            // Get modified metadata.
            MetadataChangeSet modifiedMetadataChangeSet = mConflictedCompletionEvent
                    .getModifiedMetadataChangeSet();

            // Get current contents.
            DriveId driveId = mConflictedCompletionEvent.getDriveId();
            DriveFile currentFile = driveId.asDriveFile();
            DriveContentsResult currentDriveContentsResult = currentFile.open(mGoogleApiClient,
                    DriveFile.MODE_READ_ONLY, null).await();
            String serverStr;
            DriveContents currentDriveContents;
            if (currentDriveContentsResult.getStatus().isSuccess()) {
                currentDriveContents = currentDriveContentsResult.getDriveContents();
                InputStream serverInputStream = currentDriveContents.getInputStream();
                serverStr = ConflictUtil.getStringFromInputStream(serverInputStream);
            } else {
                // The current contents cannot be opened at this point, probably due to
                // connectivity, so by snoozing the event we will get it again later.
                Log.d(TAG, "Unable to retrieve current content, snoozing completion event.");
                mConflictedCompletionEvent.snooze();
                return null;
            }

            // Resolve conflict.
            String resolvedItems = ConflictUtil.resolveConflict(baseStr, serverStr,
                    modifiedStr);

            // Commit resolved contents.
            DriveContentsResult driveContentsResult = currentDriveContents
                    .reopenForWrite(mGoogleApiClient).await();
            if (driveContentsResult.getStatus().isSuccess()) {
                DriveContents writingDriveContents = driveContentsResult.getDriveContents();

                // Modified MetadataChangeSet is supplied here to be reapplied.
                writeItems(writingDriveContents, resolvedItems, modifiedMetadataChangeSet);
            } else {
                // The contents cannot be reopened at this point, probably due to
                // connectivity, so by snoozing the event we will get it again later.
                Log.d(TAG, "Unable to write resolved content, snoozing completion event.");
                mConflictedCompletionEvent.snooze();
            }
            return null;
        }

        /**
         * Write items to driveContents.
         *
         * @param driveContents DriveContents of the grocery list.
         * @param items String of items to be written to the grocery list.
         */
        private void writeItems(DriveContents driveContents, String items,
                MetadataChangeSet metadataChangeSet) {

            OutputStream outputStream = driveContents.getOutputStream();
            Writer writer = new OutputStreamWriter(outputStream);
            try {
                writer.write(items);
            } catch (IOException e) {
                Log.e(TAG, "Unable to write modifiedContents to OutputStream\n" + e.getMessage());
                mConflictedCompletionEvent.snooze();
                return;
            }
            try {
                writer.close();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }

            // It is not likely that resolving a conflict will result in another conflict, but it can
            // happen if the file changed again while this conflict was resolved. Since we already
            // implemented conflict resolution and we never want to miss user data, we commit here with
            // execution options in conflict-aware mode (otherwise we would overwrite server content).
            ExecutionOptions executionOptions = new ExecutionOptions.Builder()
                    .setNotifyOnCompletion(true)
                    .setConflictStrategy(ExecutionOptions.CONFLICT_STRATEGY_KEEP_REMOTE)
                    .build();

            // The modified MetadataChangeSet must be reapplied here since it would not
            // have been applied when the conflict occurred.
            com.google.android.gms.common.api.Status writeStatus = driveContents.commit(mGoogleApiClient, metadataChangeSet,
                    executionOptions).await();

            if (writeStatus.isSuccess()) {
                Log.d(TAG, "resolved list: " + items);
                mConflictedCompletionEvent.dismiss();
                sendResult(items);
            } else {
                // The contents cannot be committed at this point, probably due to
                // connectivity, so by snoozing the event we will get it again later.
                Log.d(TAG, "Unable to write resolved content, snoozing completion event.");
                mConflictedCompletionEvent.snooze();
            }

        }

        /**
         * Notify the UI that the list should be updated
         *
         * @param resolution Resolved grocery list.
         */
        private void sendResult(String resolution) {
            Intent intent = new Intent(CONFLICT_RESOLVED);
            intent.putExtra("conflictResolution", resolution);
            mBroadcaster.sendBroadcast(intent);
        }
    }

    private static final String TAG = "ConflictResolver";

    public static final String CONFLICT_RESOLVED =
            "com.google.android.gms.drive.sample.conflict.CONFLICT_RESOLVED";

    private LocalBroadcastManager mBroadcaster;
    private GoogleApiClient mGoogleApiClient;
    private CompletionEvent mConflictedCompletionEvent;
    private boolean mResolutionStarted;

    public ConflictResolver(CompletionEvent mConflictedCompletionEvent, Context mContext) {
        this.mConflictedCompletionEvent = mConflictedCompletionEvent;

        // Scopes Drive.SCOPE_FILE and Drive.SCOPE_APPFOLDER are added here to ensure we can
        // resolve conflicts on any file touched by our app.
        // A new GoogleApiClient should be created to handle each new CompletionEvent since each
        // event is tied to a specific user account. Any DriveFile action taken must be done using
        // the correct account. GoogleApiClients should not be reused for CompletionEvents.
        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addScope(Drive.SCOPE_APPFOLDER)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                // Ensuring that the GoogleApiClient uses the correct account when resolving
                // conflict.
                .setAccountName(mConflictedCompletionEvent.getAccountName())
                .build();

        mBroadcaster = LocalBroadcastManager.getInstance(mContext);
    }

    /**
     * Initiate the resolution process by connecting the GoogleApiClient.
     */
    public void resolve() {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "connected");
        if (!mResolutionStarted) {
            mResolutionStarted = true;
            new HandleConflictTask().execute();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "GoogleApiClient connection suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
        mConflictedCompletionEvent.snooze();
    }
}
