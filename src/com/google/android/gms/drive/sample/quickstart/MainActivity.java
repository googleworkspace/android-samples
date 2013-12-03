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

package com.google.android.gms.drive.sample.quickstart;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi.ContentsResult;
import com.google.android.gms.drive.DriveApi.OnNewContentsCallback;
import com.google.android.gms.drive.MetadataChangeSet;

/**
 * Android Drive Quickstart activity. This activity takes a photo and saves it
 * in Google Drive. The user is prompted with a pre-made dialog which allows
 * them to choose the file location.
 * 
 * @author afshar@google.com
 */
public class MainActivity extends Activity implements ConnectionCallbacks,
    OnConnectionFailedListener {

  private static final String TAG = "android-drive-quickstart";
  private static final int REQUEST_CODE_CAPTURE_IMAGE = 1;
  private static final int REQUEST_CODE_CREATOR = 2;
  private static final int REQUEST_CODE_RESOLUTION = 3;

  private Bitmap mImage;
  private GoogleApiClient mGoogleApiClient;

  /**
   * Start an intent to launch the camera.
   */
  private void startCameraIntent() {
    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    startActivityForResult(cameraIntent, REQUEST_CODE_CAPTURE_IMAGE);
  }

  /**
   * Create a new file and save it to Drive.
   */
  private void saveFileToDrive() {
    Log.i(TAG, "Creating new contents.");
    // Start by creating a new contents.
    Drive.DriveApi.newContents(mGoogleApiClient).addResultCallback(new OnNewContentsCallback() {

      /*
       * Called when contents creation has completed or failed.
       * @see
       * com.google.android.gms.drive.DriveApi.OnNewContentsCallback#onNewContents
       * (com.google.android.gms.drive.DriveApi.ContentsResult)
       */
      @Override
      public void onNewContents(ContentsResult result) {
        // If the operation was not successful, we cannot do anything and must
        // fail.
        if (!result.getStatus().isSuccess()) {
          Log.i(TAG, "Failed to create new contents.");
          return;
        }

        // Otherwise, we can write our data to the new contents.
        Log.i(TAG, "New contents created.");

        // Get an output stream for the contents.
        OutputStream outputStream = result.getContents().getOutputStream();

        // Write the bitmap data from it.
        ByteArrayOutputStream bitmapStream = new ByteArrayOutputStream();
        mImage.compress(Bitmap.CompressFormat.PNG, 100, bitmapStream);
        try {
          outputStream.write(bitmapStream.toByteArray());
        } catch (IOException e1) {
          Log.i(TAG, "Unable to write file contents.");
        }

        // Create the initial metadata - MIME type and title.
        // Note that the user will be able to change the title later.
        MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
            .setMimeType("image/jpeg").setTitle("Android Photo.png").build();

        // Create an intent for the file chooser, and start it.
        IntentSender intentSender = Drive.DriveApi
            .newCreateFileActivityBuilder()
            .setInitialMetadata(metadataChangeSet)
            .setInitialContents(result.getContents())
            .build(mGoogleApiClient);
        try {
          startIntentSenderForResult(
              intentSender, REQUEST_CODE_CREATOR, null, 0, 0, 0);
        } catch (SendIntentException e) {
          Log.i(TAG, "Failed to launch file chooser.");
        }
      }
    });
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // This activity has no UI of its own. Just start the camera.
    startCameraIntent();
  }

  @Override
  protected void onResume() {
    // Activity is visible. Ensure an API client exists and is connected.
    super.onResume();
    if (mGoogleApiClient == null) {
      // Create the API client and bind it to an instance variable.
      // We use this instance as the callback for connection and connection
      // failures.
      // Since no account name is passed, the user is prompted to choose.
      mGoogleApiClient = new GoogleApiClient.Builder(this)
          .addApi(Drive.API)
          .addScope(Drive.SCOPE_FILE)
          .addConnectionCallbacks(this)
          .addOnConnectionFailedListener(this)
          .build();
    }
    // API calls don't have to wait for connection to complete, they are queued.
    mGoogleApiClient.connect();
  }

  @Override
  protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
    switch (requestCode) {
    // Called after a photo has been taken.
      case REQUEST_CODE_CAPTURE_IMAGE:
        if (resultCode == Activity.RESULT_OK) {
          // Store the image data as a bitmap for writing later.
          mImage = (Bitmap) data.getExtras().get("data");
          saveFileToDrive();
        }
        break;
      // Called after a file is saved to Drive.
      case REQUEST_CODE_CREATOR:
        if (resultCode == RESULT_OK) {
          Log.i(TAG, "Image successfully saved.");
          // Just start the camera again for another photo.
          startCameraIntent();
        }
        break;
    }
  }

  @Override
  public void onConnectionFailed(ConnectionResult result) {
    // Called whenever the API client fails to connect.
    Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
    if (!result.hasResolution()) {
      // show the localized error dialog.
      GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(), this, 0).show();
      return;
    }
    // The failure has a resolution. Resolve it.
    // Called typically when the app is not yet authorized, and an authorization
    // dialog is displayed to the user.
    try {
      result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
    } catch (SendIntentException e) {
      Log.e(TAG, "Exception while starting resolution activity", e);
    }
  }

  @Override
  public void onConnected(Bundle connectionHint) {
    Log.i(TAG, "API client connected.");
  }

  @Override
  public void onDisconnected() {
    Log.i(TAG, "API client disconnected.");
  }

}
