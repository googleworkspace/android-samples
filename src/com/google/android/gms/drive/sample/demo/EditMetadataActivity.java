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

import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResource.MetadataResult;
import com.google.android.gms.drive.DriveResource.OnMetadataUpdatedCallback;
import com.google.android.gms.drive.MetadataChangeSet;

import android.os.Bundle;

public class EditMetadataActivity extends BaseDemoActivity implements OnMetadataUpdatedCallback {

    @Override
    public void onConnected(Bundle connectionHint) {
        super.onConnected(connectionHint);
        DriveFile file = Drive.DriveApi.getFile(
                DriveId.createFromResourceId("0ByfSjdPVs9MZcEE3bzJCc3NsRkE"));
        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                .setStarred(true)
                .setTitle("A new title").build();
        file.updateMetadata(getGoogleApiClient(), changeSet).addResultCallback(this);
    }

    @Override
    public void onMetadataUpdated(MetadataResult result) {
        if (!result.getStatus().isSuccess()) {
            showMessage("Problem while trying to update metadata");
            return;
        }
        showMessage("Metadata succesfully updated");
    }
}
