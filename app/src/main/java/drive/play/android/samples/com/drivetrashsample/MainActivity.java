/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package drive.play.android.samples.com.drivetrashsample;

import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.google.android.gms.drive.query.SortOrder;
import com.google.android.gms.drive.query.SortableField;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Google Drive Android API Trash sample.
 *
 * This sample demonstrates how DriveFiles and DriveFolders can be trashed and untrashed.
 */
public class MainActivity extends FragmentActivity implements
        OnConnectionFailedListener, ConnectionCallbacks {

    private static final String TAG = "MainActivity";
    private static final String FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";
    private static final int REQUEST_CODE_RESOLUTION = 1;

    /**
     * GoogleApiClient wraps our service connection to Google Play Services and provides access
     * to Google APIs.
     */
    private GoogleApiClient mGoogleApiClient;

    // Keep track of folders entered to allow the user to get back to root.
    private Stack<DriveId> mPreviousFolders;
    // List to keep track of the children of the currently selected folder.
    private List<Metadata> mChildren;
    // DriveId of the currently selected folder.
    private DriveId mCurrentFolder;
    // Adapter to define how files and folders are displayed in the ListView.
    private FileFolderAdapter mFileFolderAdapter;

    private TextView mCurrentFolderNameTextView;
    private Button mAddFileButton;
    private Button mAddFolderButton;
    private Button mPreviousFolderButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView mFileFolderListView = (ListView) findViewById(R.id.fileListView);
        mFileFolderAdapter = new FileFolderAdapter(this, R.layout.resource_item,
                new ArrayList<Metadata>());
        mFileFolderListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position,
                                           long id) {
                // Metadata provides access to a DriveFile or DriveFolder's trash state.
                Metadata metadata = mFileFolderAdapter.getItem(position);
                DriveResource driveResource = metadata.getDriveId().asDriveResource();

                // If a DriveResource is a folder it will only be trashed if all of its children
                // are also accessible to this app.
                if (metadata.isTrashable()) {
                    if (metadata.isTrashed()) {
                        driveResource.untrash(mGoogleApiClient)
                                .setResultCallback(trashStatusCallback);
                    } else {
                        driveResource.trash(mGoogleApiClient)
                                .setResultCallback(trashStatusCallback);
                    }
                } else {
                    Log.d(TAG, getResources().getString(R.string.trashable_error));
                    Toast.makeText(view.getContext(),
                            getResources().getString(R.string.trashable_error),
                            Toast.LENGTH_LONG).show();
                }
                return true;
            }
        });

        mFileFolderListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Metadata metadata = mFileFolderAdapter.getItem(position);
                if (metadata.isFolder()) {
                    // Push current folder onto stack so the user can return to it later.
                    mPreviousFolders.push(mCurrentFolder);
                    mPreviousFolderButton.setEnabled(true);
                    mCurrentFolder = metadata.getDriveId();
                    mFileFolderAdapter.setEnabled(false);
                    queryFolders();
                }
            }
        });
        mFileFolderListView.setAdapter(mFileFolderAdapter);
        mFileFolderListView.setEmptyView(findViewById(R.id.emptyMessageView));

        mAddFileButton = (Button) findViewById(R.id.addFileButton);
        mAddFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addFile();
            }
        });

        mAddFolderButton = (Button) findViewById(R.id.addFolderButton);
        mAddFolderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addFolder();
            }
        });

        mPreviousFolderButton = (Button) findViewById(R.id.previousButton);
        mPreviousFolderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mPreviousFolders.isEmpty()) {
                    // Pop folder stack to query children of previous folder.
                    mCurrentFolder = mPreviousFolders.pop();
                    if (mPreviousFolders.isEmpty()) {
                        mPreviousFolderButton.setEnabled(false);
                    }
                    queryFolders();
                }
            }
        });

        mPreviousFolders = new Stack<>();
        if (savedInstanceState != null) {
            ArrayList<String> driveIds = savedInstanceState.getStringArrayList("previousFolders");
            if (driveIds.size() > 0) {
                mPreviousFolderButton.setEnabled(true);
            }
            while (driveIds != null && driveIds.size() > 0) {
                mPreviousFolders.push(DriveId.decodeFromString(driveIds
                        .remove(driveIds.size() - 1)));
            }

            String currentFolder = savedInstanceState.getString("currentFolder");
            if (currentFolder != null) {
                mCurrentFolder = DriveId.decodeFromString(currentFolder);
            }
        }

        mChildren = new ArrayList<>();
        mCurrentFolderNameTextView = (TextView) findViewById(R.id.folderNameTextView);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        ArrayList<String> driveIds = new ArrayList<>();
        while (!mPreviousFolders.isEmpty()) {
            driveIds.add(mPreviousFolders.pop().encodeToString());
        }
        savedInstanceState.putStringArrayList("previousFolders", driveIds);
        // Check here required since login pop up causes onSaveInstanceState to be called.
        if (mCurrentFolder != null) {
            savedInstanceState.putString("currentFolder", mCurrentFolder.encodeToString());
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "Google API Client connected.");
        queryFolders();
        updateActionStatus(true);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "GoogleApiClient suspended.");
        updateActionStatus(false);
    }

    /**
     * Called when {@code mGoogleApiClient} is trying to connect but fails.
     * Handle {@code connectionResult.getResolution()} if there is a resolution
     * available.
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (!connectionResult.hasResolution()) {
            GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), this, 0);
            return;
        }
        try {
            connectionResult.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Exception while starting resolution activity", e);
        }
    }

    /**
     * Handles resolution callbacks.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_RESOLUTION && resultCode == RESULT_OK) {
            mGoogleApiClient.connect();
        }
    }

    /**
     * Add file to the currently selected folder.
     */
    private void addFile() {
        mAddFileButton.setEnabled(false);

        // Counter used to create a mostly unique filename before creation.
        int fileCount = 1;
        for (Metadata metadata : mChildren) {
            if (!metadata.isFolder()) {
                fileCount++;
            }
        }

        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                .setTitle("sample file " + fileCount)
                .setMimeType("text/plain")
                .build();

        DriveFolder driveFolder = mCurrentFolder.asDriveFolder();

        driveFolder.createFile(mGoogleApiClient,
                changeSet, null).setResultCallback(addFileCallback);
    }

    /**
     * Add folder to currently selected folder.
     */
    private void addFolder() {
        mAddFolderButton.setEnabled(false);

        // Counter used to create a mostly unique folder name before creation.
        int folderCount = 1;
        for (Metadata metadata : mChildren) {
            if (metadata.isFolder()) {
                folderCount++;
            }
        }

        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                .setTitle("sample folder " + folderCount)
                .build();

        DriveFolder driveFolder = mCurrentFolder.asDriveFolder();

        driveFolder.createFolder(mGoogleApiClient, changeSet)
                .setResultCallback(addFolderCallback);
    }

    /**
     * Query for all folders in the currently selected folder.
     */
    private void queryFolders() {
        if (mCurrentFolder == null) {
            mCurrentFolder = Drive.DriveApi.getRootFolder(mGoogleApiClient).getDriveId();
        }
        DriveFolder driveFolder = mCurrentFolder.asDriveFolder();
        driveFolder.getMetadata(mGoogleApiClient).setResultCallback(folderNameCallback);

        mChildren.clear();

        SortOrder sortOrder = new SortOrder.Builder()
                .addSortAscending(SortableField.CREATED_DATE).build();

        Query folderQuery = new Query.Builder()
                .addFilter(Filters.eq(SearchableField.MIME_TYPE, FOLDER_MIME_TYPE))
                .setSortOrder(sortOrder).build();

        driveFolder.queryChildren(mGoogleApiClient, folderQuery)
                .setResultCallback(queryFoldersCallback);
    }

    /**
     * Query for all files in the currently selected folder.
     */
    private void queryFiles() {
        DriveFolder driveFolder = mCurrentFolder.asDriveFolder();

        SortOrder sortOrder = new SortOrder.Builder()
                .addSortAscending(SortableField.CREATED_DATE).build();

        // Filter out folders from results with Filters.not filter and folder mime type.
        Query fileQuery = new Query.Builder()
                .addFilter(Filters.not(Filters.eq(SearchableField.MIME_TYPE, FOLDER_MIME_TYPE)))
                .setSortOrder(sortOrder).build();

        driveFolder.queryChildren(mGoogleApiClient, fileQuery)
                .setResultCallback(queryFilesCallback);
    }

    /**
     * Enable/Disable UI elements that would require GoogleAPIClient connection.
     *
     * @param actionStatus Enable UI elements when true, disable otherwise.
     */
    private void updateActionStatus(boolean actionStatus) {
        mAddFileButton.setEnabled(actionStatus);
        mAddFolderButton.setEnabled(actionStatus);
        mFileFolderAdapter.setEnabled(actionStatus);
        if (!mPreviousFolders.isEmpty() && actionStatus) {
            mPreviousFolderButton.setEnabled(true);
        } else {
            mPreviousFolderButton.setEnabled(false);
        }
    }

    /**
     * Callback when call for folder metadata is complete.
     */
    private final ResultCallback<DriveResource.MetadataResult> folderNameCallback =
            new ResultCallback<DriveResource.MetadataResult>() {
                @Override
                public void onResult(DriveResource.MetadataResult metadataResult) {
                    if (!metadataResult.getStatus().isSuccess()) {
                        Log.e(TAG, "Unable to retrieve folder metadata.");
                        return;
                    }
                    Metadata metadata = metadataResult.getMetadata();
                    mCurrentFolderNameTextView.setText(metadata.getTitle());
                }
            };

    /**
     * Callback when call to query folders is complete.
     */
    private final ResultCallback<DriveApi.MetadataBufferResult> queryFoldersCallback =
            new ResultCallback<DriveApi.MetadataBufferResult>() {
                @Override
                public void onResult(DriveApi.MetadataBufferResult metadataBufferResult) {
                    if (!metadataBufferResult.getStatus().isSuccess()) {
                        Log.e(TAG, "Unable to retrieve queried folders.");
                        return;
                    }
                    MetadataBuffer metadatas = metadataBufferResult.getMetadataBuffer();
                    try {
                        for (int i = 0; i < metadatas.getCount(); i++) {
                            mChildren.add(metadatas.get(i).freeze());
                        }
                        metadatas.release();
                        // Query files.
                        queryFiles();
                    } finally {
                        metadatas.release();
                    }
                }
            };

    /**
     * Callback when call to query files is complete.
     */
    private final ResultCallback<DriveApi.MetadataBufferResult> queryFilesCallback =
            new ResultCallback<DriveApi.MetadataBufferResult>() {
                @Override
                public void onResult(DriveApi.MetadataBufferResult metadataBufferResult) {
                    if (!metadataBufferResult.getStatus().isSuccess()) {
                        Log.d(TAG, "Unable to retrieve queried files.");
                        return;
                    }
                    MetadataBuffer metadatas = metadataBufferResult.getMetadataBuffer();
                    try {
                        for (int i = 0; i < metadatas.getCount(); i++) {
                            mChildren.add(metadatas.get(i).freeze());
                        }

                        mFileFolderAdapter.setFiles(mChildren);
                        mFileFolderAdapter.notifyDataSetChanged();
                        mFileFolderAdapter.setEnabled(true);
                    } finally {
                        metadatas.release();
                    }
                }
            };

    /**
     * Callback when call to add a file is complete.
     */
    private final ResultCallback<DriveFolder.DriveFileResult> addFileCallback =
            new ResultCallback<DriveFolder.DriveFileResult>() {
                @Override
                public void onResult(DriveFolder.DriveFileResult driveFileResult) {
                    if (!driveFileResult.getStatus().isSuccess()) {
                        Log.e(TAG, "Unable to retrieve created file.");
                        return;
                    }
                    queryFolders();
                    mAddFileButton.setEnabled(true);
                }
            };

    /**
     * Callback when call to add a folder is complete.
     */
    private final ResultCallback<DriveFolder.DriveFolderResult> addFolderCallback =
            new ResultCallback<DriveFolder.DriveFolderResult>() {
                @Override
                public void onResult(DriveFolder.DriveFolderResult driveFolderResult) {
                    if (!driveFolderResult.getStatus().isSuccess()) {
                        Log.e(TAG, "Unable to create folder.");
                        return;
                    }
                    queryFolders();
                    mAddFolderButton.setEnabled(true);
                }
            };

    /**
     * Callback when call to trash or untrash is complete.
     */
    private final ResultCallback<Status> trashStatusCallback =
            new ResultCallback<Status>() {
                @Override
                public void onResult(Status status) {
                    if (!status.isSuccess()) {
                        Log.e(TAG, getResources().getString(R.string.unable_to_trash_error) +
                                status.getStatusMessage());
                        Toast.makeText(getBaseContext(),
                                getResources().getString(R.string.unable_to_trash_error),
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    queryFolders();
                }
            };
}

