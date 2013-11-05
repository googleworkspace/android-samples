package com.google.android.gms.drive.sample.demo;

import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveFolder.DriveFolderResult;
import com.google.android.gms.drive.DriveFolder.OnCreateFolderCallback;
import com.google.android.gms.drive.MetadataChangeSet;

import android.os.Bundle;

public class CreateFolderActivity extends BaseDemoActivity implements OnCreateFolderCallback {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        super.onConnected(connectionHint);
        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                .setTitle("New folder")
                .build();
        Drive.DriveApi.getRootFolder().createFolder(
                getGoogleApiClient(), changeSet).addResultCallback(this);
    }

    @Override
    public void onCreateFolder(DriveFolderResult result) {
        if (!result.getStatus().isSuccess()) {
            showMessage("Error while trying to create the folder");
            return;
        }
        showMessage("Created a folder: " + result.getDriveFolder().getDriveId());
    }
}
