/**
 * Copyright 2013 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.gms.drive.sample.quickstart;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.drive.CreateFileActivityOptions;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveClient;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Android Drive Quickstart activity. This activity takes a photo and saves it in Google Drive. The
 * user is prompted with a pre-made dialog which allows them to choose the file location.
 */
public class MainActivity extends Activity {

  private static final String TAG = "drive-quickstart";
  private static final int REQUEST_CODE_SIGN_IN = 0;
  private static final int REQUEST_CODE_CAPTURE_IMAGE = 1;
  private static final int REQUEST_CODE_CREATOR = 2;

  private DriveClient mDriveClient;
  private DriveResourceClient mDriveResourceClient;
  private Bitmap mBitmapToSave;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    signIn();
  }

  /** Start sign in activity. */
  private void signIn() {
    Log.i(TAG, "Start sign in");
    GoogleSignInClient GoogleSignInClient = buildGoogleSignInClient();
    startActivityForResult(GoogleSignInClient.getSignInIntent(), REQUEST_CODE_SIGN_IN);
  }

  /** Build a Google SignIn client. */
  private GoogleSignInClient buildGoogleSignInClient() {
    GoogleSignInOptions signInOptions =
        new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Drive.SCOPE_FILE)
            .build();
    return GoogleSignIn.getClient(this, signInOptions);
  }

  /** Create a new file and save it to Drive. */
  private void saveFileToDrive() {
    // Start by creating a new contents, and setting a callback.
    Log.i(TAG, "Creating new contents.");
    final Bitmap image = mBitmapToSave;

    mDriveResourceClient
        .createContents()
        .continueWithTask(
                task -> createFileIntentSender(task.getResult(), image))
        .addOnFailureListener(
                e -> Log.w(TAG, "Failed to create new contents.", e));
  }

  /**
   * Creates an {@link IntentSender} to start a dialog activity with configured {@link
   * CreateFileActivityOptions} for user to create a new photo in Drive.
   */
  private Task<Void> createFileIntentSender(DriveContents driveContents, Bitmap image) {
    Log.i(TAG, "New contents created.");
    // Get an output stream for the contents.
    OutputStream outputStream = driveContents.getOutputStream();
    // Write the bitmap data from it.
    ByteArrayOutputStream bitmapStream = new ByteArrayOutputStream();
    image.compress(Bitmap.CompressFormat.PNG, 100, bitmapStream);
    try {
      outputStream.write(bitmapStream.toByteArray());
    } catch (IOException e) {
      Log.w(TAG, "Unable to write file contents.", e);
    }

    // Create the initial metadata - MIME type and title.
    // Note that the user will be able to change the title later.
    MetadataChangeSet metadataChangeSet =
        new MetadataChangeSet.Builder()
            .setMimeType("image/jpeg")
            .setTitle("Android Photo.png")
            .build();
    // Set up options to configure and display the create file activity.
    CreateFileActivityOptions createFileActivityOptions =
        new CreateFileActivityOptions.Builder()
            .setInitialMetadata(metadataChangeSet)
            .setInitialDriveContents(driveContents)
            .build();

    return mDriveClient
        .newCreateFileActivityIntentSender(createFileActivityOptions)
        .continueWith(
                task -> {
                  startIntentSenderForResult(task.getResult(), REQUEST_CODE_CREATOR, null, 0, 0, 0);
                  return null;
                });
  }

  @Override
  protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    switch (requestCode) {
      case REQUEST_CODE_SIGN_IN:
        Log.i(TAG, "Sign in request code");
        // Called after user is signed in.
        if (resultCode == RESULT_OK) {
          Log.i(TAG, "Signed in successfully.");
          // Use the last signed in account here since it already have a Drive scope.
          mDriveClient = Drive.getDriveClient(this, GoogleSignIn.getLastSignedInAccount(this));
          // Build a drive resource client.
          mDriveResourceClient =
              Drive.getDriveResourceClient(this, GoogleSignIn.getLastSignedInAccount(this));
          // Start camera.
          startActivityForResult(
              new Intent(MediaStore.ACTION_IMAGE_CAPTURE), REQUEST_CODE_CAPTURE_IMAGE);
        }
        break;
      case REQUEST_CODE_CAPTURE_IMAGE:
        Log.i(TAG, "capture image request code");
        // Called after a photo has been taken.
        if (resultCode == Activity.RESULT_OK) {
          Log.i(TAG, "Image captured successfully.");
          // Store the image data as a bitmap for writing later.
          mBitmapToSave = (Bitmap) data.getExtras().get("data");
          saveFileToDrive();
        }
        break;
      case REQUEST_CODE_CREATOR:
        Log.i(TAG, "creator request code");
        // Called after a file is saved to Drive.
        if (resultCode == RESULT_OK) {
          Log.i(TAG, "Image successfully saved.");
          mBitmapToSave = null;
          // Just start the camera again for another photo.
          startActivityForResult(
              new Intent(MediaStore.ACTION_IMAGE_CAPTURE), REQUEST_CODE_CAPTURE_IMAGE);
        }
        break;
    }
  }
}
