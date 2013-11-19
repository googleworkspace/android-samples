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

import android.os.Bundle;

import com.google.android.gms.Batch;
import com.google.android.gms.Batch.BatchCallback;
import com.google.android.gms.PendingResult;
import com.google.android.gms.Status;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi.ContentsResult;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFile.OnContentsOpenedCallback;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResource.MetadataResult;
import com.google.android.gms.drive.DriveResource.OnMetadataRetrievedCallback;

/**
 * An activity to illustrate how to make batch requests to Drive
 * service backend.
 */
public class BatchRequestsActivity extends BaseDemoActivity {

    /**
     * Handles the Drive service initialization. Batches two requests to read
     * a file's metadata and contents, and handles the batch request's response.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        super.onConnected(connectionHint);
        // retrieve meta data and open contents of a file.
        DriveFile file = Drive.DriveApi.getFile(
                DriveId.createFromResourceId("0ByfSjdPVs9MZcEE3bzJCc3NsRkE"));
        final PendingResult<MetadataResult, OnMetadataRetrievedCallback> metadataResult = file
                .getMetadata(getGoogleApiClient());
        final PendingResult<ContentsResult, OnContentsOpenedCallback> contentsResult = file
                .openContents(getGoogleApiClient(), DriveFile.MODE_READ_ONLY, null);
        new Batch(metadataResult, contentsResult).addResultCallback(new BatchCallback() {
            @Override
            public void onBatchComplete(Status status) {
                if (!status.getStatus().isSuccess()) {
                    showMessage("Error occured while trying to retrieve metadata and contents");
                    return;
                }
                showMessage("Metadata is retrieved, and opened contents.");
            }
        });
    }
}
