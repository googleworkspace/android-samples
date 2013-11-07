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
