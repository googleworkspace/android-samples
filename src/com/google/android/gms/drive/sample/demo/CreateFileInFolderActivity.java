
package com.google.android.gms.drive.sample.demo;

import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi.ContentsResult;
import com.google.android.gms.drive.DriveApi.OnNewContentsCallback;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveFolder.DriveFileResult;
import com.google.android.gms.drive.DriveFolder.OnCreateFileCallback;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.MetadataChangeSet;

import android.os.Bundle;

/**
 * An activity to create a file inside a folder.
 */
public class CreateFileInFolderActivity extends BaseDemoActivity implements OnCreateFileCallback,
        OnNewContentsCallback {

    private static DriveId sFolderId = DriveId.createFromResourceId("0B2EEtIjPUdX6MERsWlYxN3J6RU0");

    @Override
    public void onConnected(Bundle connectionHint) {
        super.onConnected(connectionHint);
        Drive.DriveApi.newContents(getGoogleApiClient()).addResultCallback(this);
    }

    @Override
    public void onNewContents(ContentsResult result) {
        if (!result.getStatus().isSuccess()) {
            showMessage("Error while trying to create new file contents");
            return;
        }
        DriveFolder folder = Drive.DriveApi.getFolder(getGoogleApiClient(), sFolderId);
        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                .setTitle("New file")
                .setMimeType("text/plain")
                .setStarred(true).build();
        folder.createFile(getGoogleApiClient(), changeSet, result.getContents())
                .addResultCallback(this);
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
