package com.google.android.gms.drive.sample.demo;

import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi.ContentsResult;
import com.google.android.gms.drive.DriveApi.OnNewContentsCallback;
import com.google.android.gms.drive.DriveFolder.DriveFileResult;
import com.google.android.gms.drive.DriveFolder.OnCreateFileCallback;
import com.google.android.gms.drive.MetadataChangeSet;

import android.os.Bundle;

public class CreateFileActivity extends BaseDemoActivity implements
        OnNewContentsCallback, OnCreateFileCallback {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_createfile);
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        super.onConnected(connectionHint);
        // create new contents resource
        Drive.DriveApi.newContents(getGoogleApiClient()).addResultCallback(this);
    }

    @Override
    public void onNewContents(ContentsResult result) {
        if (!result.getStatus().isSuccess()) {
            showMessage("Error while trying to create new file contents");
            return;
        }

        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                .setTitle("New file")
                .setMimeType("text/plain")
                .setStarred(true)
                .build();
        // create a file on root folder
        Drive.DriveApi.getRootFolder().createFile(
                getGoogleApiClient(), changeSet, result.getContents()).addResultCallback(this);
    }

    @Override
    public void onCreateFile(DriveFileResult result) {
        if (!result.getStatus().isSuccess()) {
            showMessage("Error while trying to create the file");
            return;
        }
        showMessage("Created a file: " + result.getDriveFile().getDriveId());
    }

}
