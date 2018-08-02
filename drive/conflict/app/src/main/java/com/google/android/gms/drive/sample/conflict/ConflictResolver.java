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

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.ExecutionOptions;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.events.CompletionEvent;
import com.google.android.gms.tasks.Continuation;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.concurrent.ExecutorService;

/**
 * ConflictResolver handles a CompletionEvent with a conflict status.
 */
class ConflictResolver {
    private static final String TAG = "ConflictResolver";
    static final String CONFLICT_RESOLVED =
            "com.google.android.gms.drive.sample.conflict.CONFLICT_RESOLVED";

    private LocalBroadcastManager mBroadcaster;
    private CompletionEvent mConflictedCompletionEvent;
    private Context mContext;
    private DriveResourceClient mDriveResourceClient;
    private DriveContents mDriveContents;
    private String mBaseContent;
    private String mModifiedContent;
    private String mServerContent;
    private String mResolvedContent;
    private ExecutorService mExecutorService;

    ConflictResolver(CompletionEvent conflictedCompletionEvent, Context context,
            ExecutorService executorService) {
        this.mConflictedCompletionEvent = conflictedCompletionEvent;
        mBroadcaster = LocalBroadcastManager.getInstance(context);
        mContext = context;
        mExecutorService = executorService;
    }

    /**
     * Initiate the resolution process by connecting the GoogleApiClient.
     */
    void resolve() {
        // [START drive_android_resolve_conflict]
        // A new DriveResourceClient should be created to handle each new CompletionEvent since each
        // event is tied to a specific user account. Any DriveFile action taken must be done using
        // the correct account.
        GoogleSignInOptions.Builder signInOptionsBuilder =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestScopes(Drive.SCOPE_FILE)
                        .requestScopes(Drive.SCOPE_APPFOLDER);
        if (mConflictedCompletionEvent.getAccountName() != null) {
            signInOptionsBuilder.setAccountName(mConflictedCompletionEvent.getAccountName());
        }
        GoogleSignInClient signInClient =
                GoogleSignIn.getClient(mContext, signInOptionsBuilder.build());
        signInClient.silentSignIn()
                .continueWith(mExecutorService,
                        (Continuation<GoogleSignInAccount, Void>) signInTask -> {
                            mDriveResourceClient = Drive.getDriveResourceClient(
                                    mContext, signInTask.getResult());
                            mBaseContent = ConflictUtil.getStringFromInputStream(
                                    mConflictedCompletionEvent.getBaseContentsInputStream());
                            mModifiedContent = ConflictUtil.getStringFromInputStream(
                                    mConflictedCompletionEvent
                                            .getModifiedContentsInputStream());
                            return null;
                        })
                .continueWithTask(mExecutorService,
                        task -> {
                            DriveId driveId = mConflictedCompletionEvent.getDriveId();
                            return mDriveResourceClient.openFile(
                                    driveId.asDriveFile(), DriveFile.MODE_READ_ONLY);
                        })
                .continueWithTask(mExecutorService,
                        task -> {
                            mDriveContents = task.getResult();
                            InputStream serverInputStream = task.getResult().getInputStream();
                            mServerContent =
                                    ConflictUtil.getStringFromInputStream(serverInputStream);
                            return mDriveResourceClient.reopenContentsForWrite(mDriveContents);
                        })
                .continueWithTask(mExecutorService,
                        task -> {
                            DriveContents contentsForWrite = task.getResult();
                            mResolvedContent = ConflictUtil.resolveConflict(
                                    mBaseContent, mServerContent, mModifiedContent);

                            OutputStream outputStream = contentsForWrite.getOutputStream();
                            try (Writer writer = new OutputStreamWriter(outputStream)) {
                                writer.write(mResolvedContent);
                            }

                            // It is not likely that resolving a conflict will result in another
                            // conflict, but it can happen if the file changed again while this
                            // conflict was resolved. Since we already implemented conflict
                            // resolution and we never want to miss user data, we commit here
                            // with execution options in conflict-aware mode (otherwise we would
                            // overwrite server content).
                            ExecutionOptions executionOptions =
                                    new ExecutionOptions.Builder()
                                            .setNotifyOnCompletion(true)
                                            .setConflictStrategy(
                                                    ExecutionOptions
                                                            .CONFLICT_STRATEGY_KEEP_REMOTE)
                                            .build();

                            // Commit resolved contents.
                            MetadataChangeSet modifiedMetadataChangeSet =
                                    mConflictedCompletionEvent.getModifiedMetadataChangeSet();
                            return mDriveResourceClient.commitContents(contentsForWrite,
                                    modifiedMetadataChangeSet, executionOptions);
                })
                .addOnSuccessListener(aVoid -> {
                    mConflictedCompletionEvent.dismiss();
                    Log.d(TAG, "resolved list");
                    sendResult(mModifiedContent);
                })
                .addOnFailureListener(e -> {
                    // The contents cannot be reopened at this point, probably due to
                    // connectivity, so by snoozing the event we will get it again later.
                    Log.d(TAG, "Unable to write resolved content, snoozing completion event.",
                            e);
                    mConflictedCompletionEvent.snooze();
                    if (mDriveContents != null) {
                        mDriveResourceClient.discardContents(mDriveContents);
                    }
                });
        // [END drive_android_resolve_conflict]
    }

    /**
     * Notify the UI that the list should be updated.
     *
     * @param resolution Resolved grocery list.
     */
    private void sendResult(String resolution) {
        Intent intent = new Intent(CONFLICT_RESOLVED);
        intent.putExtra("conflictResolution", resolution);
        mBroadcaster.sendBroadcast(intent);
    }
}
