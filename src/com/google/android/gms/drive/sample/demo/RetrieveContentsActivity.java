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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi.ContentsResult;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveId;

/**
 * Activity to illustrate how to retrieve and read file contents.
 */
public class RetrieveContentsActivity extends BaseDemoActivity {

    private static final String TAG = "RetrieveContentsActivity";

    @Override
    public void onConnected(Bundle connectionHint) {
        super.onConnected(connectionHint);
        new RetrieveDriveFileContentsAsyncTask()
                .execute(DriveId.createFromResourceId("0ByfSjdPVs9MZcEE3bzJCc3NsRkE"));
    }

    private class RetrieveDriveFileContentsAsyncTask extends AsyncTask<DriveId, Boolean, String> {

        @Override
        protected String doInBackground(DriveId... params) {
            String contents = null;
            DriveFile file = Drive.DriveApi.getFile(getGoogleApiClient(), params[0]);
            ContentsResult contentsResult =
                    file.openContents(getGoogleApiClient(), DriveFile.MODE_READ_ONLY, null).await();
            if (!contentsResult.getStatus().isSuccess()) {
                return null;
            }
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(contentsResult.getContents().getInputStream()));
            StringBuilder builder = new StringBuilder();
            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
                contents = builder.toString();
            } catch (IOException e) {
                Log.e(TAG, "IOException while reading from the stream", e);
            }

            file.discardContents(getGoogleApiClient(), contentsResult.getContents()).await();
            return contents;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result == null) {
                showMessage("Error while reading from the file");
                return;
            }
            showMessage("File contents: " + result);
        }
    }
}
