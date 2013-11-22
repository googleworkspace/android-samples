
package com.google.android.gms.drive.sample.demo;

import android.os.Bundle;

import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResource.MetadataResult;
import com.google.android.gms.drive.DriveResource.OnMetadataRetrievedCallback;
import com.google.android.gms.drive.Metadata;

/**
 * An activity to retrieve the metadata of a file.
 */
public class RetreiveMetadataActivity extends BaseDemoActivity implements
        OnMetadataRetrievedCallback {

    @Override
    public void onConnected(Bundle connectionHint) {
        DriveFile file = Drive.DriveApi.getFile(getGoogleApiClient(),
                DriveId.createFromResourceId("0ByfSjdPVs9MZcEE3bzJCc3NsRkE"));
        file.getMetadata(getGoogleApiClient()).addResultCallback(this);

    }

    @Override
    public void onMetadataRetrieved(MetadataResult result) {
        if (!result.getStatus().isSuccess()) {
            showMessage("Problem while trying to fetch metadata");
            return;
        }
        Metadata metadata = result.getMetadata();
        showMessage("Metadata succesfully fetched. Title: " + metadata.getTitle());
    }
}
