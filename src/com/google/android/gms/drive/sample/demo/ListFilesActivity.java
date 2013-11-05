package com.google.android.gms.drive.sample.demo;

import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi.MetadataBufferResult;
import com.google.android.gms.drive.DriveFolder.OnChildrenRetrievedCallback;

import android.os.Bundle;

public class ListFilesActivity extends BaseDemoActivity implements OnChildrenRetrievedCallback {

  @Override
  protected void onCreate(Bundle b) {
    super.onCreate(b);
    setContentView(R.layout.activity_listfiles);
  }

  @Override
  public void onConnected(Bundle connectionHint) {
    super.onConnected(connectionHint);
    // list files under root folder
    Drive.DriveApi.getRootFolder().listChildren(getGoogleApiClient()).addResultCallback(this);
  }

  @Override
  public void onChildrenRetrieved(MetadataBufferResult result) {
    // list results
  }

}
