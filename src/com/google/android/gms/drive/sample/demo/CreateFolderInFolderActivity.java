
package com.google.android.gms.drive.sample.demo;

import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveFolder.DriveFolderResult;
import com.google.android.gms.drive.DriveFolder.OnCreateFolderCallback;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.MetadataChangeSet;

import android.os.Bundle;

/**
 * An activity to create a folder inside a folder.
 */
public class CreateFolderInFolderActivity extends BaseDemoActivity implements
        OnCreateFolderCallback {

    private static DriveId sFolderId = DriveId.createFromResourceId("0B2EEtIjPUdX6MERsWlYxN3J6RU0");

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
