/**
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

package com.google.android.gms.drive.sample.demo;

import android.content.Intent;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;

import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi.ContentsResult;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFile.DownloadProgressListener;
import com.google.android.gms.drive.DriveFile.OnContentsOpenedCallback;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.OpenFileActivityBuilder;

/**
 * An activity to illustrate how to open contents and listen
 * the download progress if the file is not already sync'ed.
 */
public class RetrieveContentsWithProgressDialogActivity extends BaseDemoActivity
        implements OnContentsOpenedCallback {

    private static final String TAG = "RetrieveFileWithProgressDialogActivity";

    private static final int REQUEST_CODE_OPENER = 1;

    public enum WaitingOp { RETRIEVE }

    private ProgressBar mProgressBar;

    private WaitingOp mWaitingOp;

    private DriveId mSelectedDriveId;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_progress);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mProgressBar.setMax(100);
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        super.onConnected(connectionHint);
        if (mWaitingOp == WaitingOp.RETRIEVE) {
            open();
            return;
        }

        // let the user pick any file
        IntentSender intentSender = Drive.DriveApi
                .newOpenFileActivityBuilder()
                .setMimeType(new String[]{ "video/mp4", "image/jpeg" })
                .build(getGoogleApiClient());
        try {
            startIntentSenderForResult(intentSender, REQUEST_CODE_OPENER, null, 0, 0, 0);
            mWaitingOp = WaitingOp.RETRIEVE;
        } catch (SendIntentException e) {
          Log.w(TAG, "Unable to send intent", e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_OPENER && resultCode == RESULT_OK) {
            mSelectedDriveId = (DriveId) data.getParcelableExtra(
                    OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void open() {
        mWaitingOp = null;
        // reset progress dialog
        mProgressBar.setProgress(0);
        DownloadProgressListener listener = new DownloadProgressListener() {
            @Override
            public void onProgress(long bytesDownloaded, long bytesExpected) {
                // update progress dialog with the latest status
                int progress = (int)(bytesDownloaded*100/bytesExpected);
                Log.d(TAG, String.format("Loading progress: %d percent", progress));
                mProgressBar.setProgress(progress);
            }
        };
        Drive.DriveApi.getFile(getGoogleApiClient(), mSelectedDriveId)
            .openContents(getGoogleApiClient(), DriveFile.MODE_READ_ONLY, listener)
            .addResultCallback(this);
    }

    @Override
    public void onOpen(ContentsResult result) {
        if (!result.getStatus().isSuccess()) {
            showMessage("Error while opening the file contents");
            return;
        }
        showMessage("File contents opened");
    }
}
