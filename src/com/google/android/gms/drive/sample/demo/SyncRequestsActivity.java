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

import android.os.AsyncTask;
import android.os.Bundle;

import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi.ContentsResult;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveFolder.DriveFileResult;
import com.google.android.gms.drive.MetadataChangeSet;

/**
 * An activity to illustrate making synchronous requests to
 * the Drive service backend.
 */
public class SyncRequestsActivity extends BaseDemoActivity {

    private static String TAG = "SyncRequestsActivity";

    @Override
    public void onConnected(Bundle connectionHint) {
        super.onConnected(connectionHint);
        new CreateFileAsyncTask().execute();
    }

    /**
     * An async task that creates a new text file by
     * creating new contents and metadata entities on user's root folder.
     */
    public class CreateFileAsyncTask extends AsyncTask<Void, Void, DriveFile> {

        @Override
        protected DriveFile doInBackground(Void... arg0) {
            ContentsResult contentsResult =
                    Drive.DriveApi.newContents(getGoogleApiClient()).await();
            if (!contentsResult.getStatus().isSuccess()) {
                return null;
            }
            // create a new text file with empty contents
            MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                    .setTitle("Hello world")
                    .setMimeType("text/plain").build();
            DriveFolder rootFolder = Drive.DriveApi.getRootFolder(getGoogleApiClient());
            DriveFileResult fileResult = rootFolder.createFile(
                    getGoogleApiClient(), changeSet, contentsResult.getContents()).await();
            if (!fileResult.getStatus().isSuccess()) {
                return null;
            }
            return fileResult.getDriveFile();
        }

        @Override
        protected void onPostExecute(DriveFile result) {
            super.onPostExecute(result);
            if (result == null) {
                showMessage("Error while creating the file");
                return;
            }
            showMessage("File created: " + result.getDriveId());
        }
    }
}
