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

import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveFolder.DriveFolderResult;
import com.google.android.gms.drive.DriveFolder.OnCreateFolderCallback;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.MetadataChangeSet;

/**
 * An activity to create a folder inside a folder.
 */
public class CreateFolderInFolderActivity extends BaseDemoActivity implements
        OnCreateFolderCallback {

    private static final DriveId  sFolderId =
            DriveId.createFromResourceId("0B2EEtIjPUdX6MERsWlYxN3J6RU0");

    @Override
    public void onConnected(Bundle connectionHint) {
        super.onConnected(connectionHint);
        DriveFolder folder = Drive.DriveApi.getFolder(getGoogleApiClient(), sFolderId);
        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                .setTitle("MyNewFolder").build();
        folder.createFolder(getGoogleApiClient(), changeSet).addResultCallback(this);
    }

    @Override
    public void onCreateFolder(DriveFolderResult result) {
        if (!result.getStatus().isSuccess()) {
            showMessage("Problem while trying to create a folder");
            return;
        }
        showMessage("Folder succesfully created");
    }
}
