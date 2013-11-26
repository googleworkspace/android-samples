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

import java.io.IOException;
import java.io.OutputStream;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi.ContentsResult;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveId;

/**
 * An activity to illustrate how to edit contents of a Drive file.
 */
public class EditContentsActivity extends BaseDemoActivity {

    private static final String TAG = "EditContentsActivity";

    @Override
    public void onConnected(Bundle connectionHint) {
        super.onConnected(connectionHint);
        DriveFile file = Drive.DriveApi.getFile(
                getGoogleApiClient(),
                DriveId.createFromResourceId("0ByfSjdPVs9MZTHBmMVdSeWxaNTg"));
        new EditContentsAsyncTask().execute(file);
    }

    public class EditContentsAsyncTask extends AsyncTask<DriveFile, Void, Boolean> {

        @Override
        protected Boolean doInBackground(DriveFile... args) {
            DriveFile file = args[0];
            try {
                ContentsResult contentsResult = file.openContents(
                        getGoogleApiClient(), DriveFile.MODE_WRITE_ONLY, null).await();
                if (!contentsResult.getStatus().isSuccess()) {
                    return false;
                }
                OutputStream outputStream = contentsResult.getContents().getOutputStream();
                outputStream.write("Hello world".getBytes());
                com.google.android.gms.common.api.Status status = file.commitAndCloseContents(
                        getGoogleApiClient(), contentsResult.getContents()).await();
                return status.getStatus().isSuccess();
            } catch (IOException e) {
                Log.e(TAG, "IOException while appending to the output stream", e);
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result) {
                showMessage("Error while editing contents");
                return;
            }
            showMessage("Succesfully edited contents");
        }
    }
}
