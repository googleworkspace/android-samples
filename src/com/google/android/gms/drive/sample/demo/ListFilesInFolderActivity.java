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
import android.widget.ListView;

import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi.MetadataBufferResult;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveFolder.OnChildrenRetrievedCallback;
import com.google.android.gms.drive.DriveId;

/**
 * An activity illustrates how to list files in a folder. For an example of
 * pagination and displaying results, please see {@link ListFilesActivity}.
 */
public class ListFilesInFolderActivity extends BaseDemoActivity implements
        OnChildrenRetrievedCallback {

    private static final DriveId sFolderId =
            DriveId.createFromResourceId("0B2EEtIjPUdX6MERsWlYxN3J6RU0");

    private ListView mResultsListView;
    private ResultsAdapter mResultsAdapter;

    @Override
    public void onConnected(Bundle connectionHint) {
        super.onCreate(connectionHint);
        setContentView(R.layout.activity_listfiles);
        mResultsListView = (ListView) findViewById(R.id.listViewResults);
        mResultsAdapter = new ResultsAdapter(this);
        mResultsListView.setAdapter(mResultsAdapter);
        DriveFolder folder = Drive.DriveApi.getFolder(getGoogleApiClient(), sFolderId);
        folder.listChildren(getGoogleApiClient()).addResultCallback(this);
    }

    @Override
    public void onChildrenRetrieved(MetadataBufferResult result) {
        if (!result.getStatus().isSuccess()) {
            showMessage("Problem while retrieving files");
            return;
        }
        mResultsAdapter.clear();
        mResultsAdapter.append(result.getMetadataBuffer());
        showMessage("Successfully listed files.");
    }

}
