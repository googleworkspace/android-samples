/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.samples.drive.trash;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SortOrder;
import com.google.android.gms.drive.query.SortableField;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;

/**
 * Google Drive Android API Trash sample.
 * This sample demonstrates how DriveFiles and DriveFolders can be trashed and untrashed.
 */
public class MainActivity extends FragmentActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_SIGN_IN = 0;

    // Handles access to files in Drive
    private DriveResourceClient mDriveResourceClient;

    private Stack<DriveId> mNavigationPath;
    private FileFolderAdapter mFileFolderAdapter;

    private TextView mCurrentFolderNameTextView;
    private Button mAddFileButton;
    private Button mAddFolderButton;
    private Button mPreviousFolderButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNavigationPath = new Stack<>();
        mCurrentFolderNameTextView = findViewById(R.id.folderNameTextView);

        // An adapter for accessing and displaying resources (files and folders).
        mFileFolderAdapter = new FileFolderAdapter(
                this, R.layout.resource_item, Collections.<Metadata>emptyList());

        ListView fileFolderListView = findViewById(R.id.fileListView);
        // Trash or untrash a resource with a long click.
        fileFolderListView.setOnItemLongClickListener((parent, view, position, id) -> {
            // Metadata provides access to a DriveFile or DriveFolder's trash state.
            Metadata metadata = mFileFolderAdapter.getItem(position);
            toggleTrashStatus(metadata);
            return true;
        });

        // Show a view of folder contents when a folder is clicked on.
        fileFolderListView.setOnItemClickListener((parent, view, position, id) -> {
            Metadata metadata = mFileFolderAdapter.getItem(position);
            if (metadata != null && metadata.isFolder()) {
                DriveId folderId = metadata.getDriveId();
                navigateToFolder(folderId);
            }
        });
        fileFolderListView.setAdapter(mFileFolderAdapter);
        fileFolderListView.setEmptyView(findViewById(R.id.emptyMessageView));

        mAddFileButton = findViewById(R.id.addFileButton);
        mAddFileButton.setEnabled(false);
        mAddFileButton.setOnClickListener(v -> createFile());

        mAddFolderButton = findViewById(R.id.addFolderButton);
        mAddFolderButton.setEnabled(false);
        mAddFolderButton.setOnClickListener(view -> createFolder());

        mPreviousFolderButton = findViewById(R.id.previousButton);
        mPreviousFolderButton.setEnabled(false);
        mPreviousFolderButton.setOnClickListener(v -> navigateBack());

        if (savedInstanceState != null) {
            ArrayList<DriveId> driveIds =
                    savedInstanceState.getParcelableArrayList("navigationPath");
            if (driveIds != null && !driveIds.isEmpty()) {
                mPreviousFolderButton.setEnabled(true);
                mNavigationPath.addAll(driveIds);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        signIn();
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putParcelableArrayList(
                "navigationPath", new ArrayList<>(mNavigationPath));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SIGN_IN) {
            if (resultCode != RESULT_OK) {
                // Sign-in may fail or be cancelled by the user. For this sample, sign-in is
                // required and is fatal. For apps where sign-in is optional, handle appropriately
                Log.e(TAG, "Sign-in failed.");
                finish();
                return;
            }
            Task<GoogleSignInAccount> getAccountTask =
                    GoogleSignIn.getSignedInAccountFromIntent(data);
            if (getAccountTask.isSuccessful()) {
                initializeDriveResourceClient(getAccountTask.getResult());
            } else {
                Log.e(TAG, "Sign-in failed.");
                finish();
            }
        }
    }

    /**
     * Starts the sign in process to get the current user and authorize access to Drive.
     */
    private void signIn() {
        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestScopes(Drive.SCOPE_FILE)
                        .build();
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, signInOptions);
        startActivityForResult(googleSignInClient.getSignInIntent(), REQUEST_CODE_SIGN_IN);
    }

    /**
     * Continues the sign-in process, initializing the DriveResourceClient with the current
     * user's account.
     *
     * @param signInAccount Authorized google account
     */
    private void initializeDriveResourceClient(GoogleSignInAccount signInAccount) {
        mDriveResourceClient = Drive.getDriveResourceClient(getApplicationContext(), signInAccount);
        onDriveClientReady();
    }

    /**
     * Load initial data once the client is ready.
     */
    private void onDriveClientReady() {
        Task<Void> initFolderTask = initializeFolderView();
        handleTaskError(initFolderTask, R.string.unexpected_error);
    }

    /**
     * Initializes the folder view after the given task completes.
     *
     * @return Task which resolves after the view has been initialized
     */
    private Task<Void> initializeFolderView() {
        Task<DriveFolder> folderTask;
        if (mNavigationPath.isEmpty()) {
            folderTask = mDriveResourceClient.getRootFolder();
        } else {
            folderTask = Tasks.forResult(mNavigationPath.peek().asDriveFolder());
        }
        Task<Void> initFolderTask = folderTask.continueWith(task -> {
            DriveId id = task.getResult().getDriveId();
            if (mNavigationPath.isEmpty()) {
                mNavigationPath.push(id);
            }
            return null;
        });
        return updateUiAfterTask(initFolderTask);
    }

    /**
     * Trashes or untrashes the given item.
     *
     * @param metadata Item to (un)trash
     */
    private void toggleTrashStatus(Metadata metadata) {
        // [START trash]
        if (!metadata.isTrashable()) {
            showMessage(R.string.trashable_error);
            return;
        }
        DriveResource driveResource = metadata.getDriveId().asDriveResource();
        Task<Void> toggleTrashTask;
        if (metadata.isTrashed()) {
            toggleTrashTask = mDriveResourceClient.untrash(driveResource);
        } else {
            toggleTrashTask = mDriveResourceClient.trash(driveResource);
        }
        toggleTrashTask = updateUiAfterTask(toggleTrashTask);
        handleTaskError(toggleTrashTask, R.string.unexpected_error);
        // [END trash]
    }

    /**
     * Creates an empty file in the current folder.
     */
    private void createFile() {
        setUiInteractionsEnabled(false);
        int fileCount = mFileFolderAdapter.getCount();
        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                .setTitle("sample file " + (fileCount + 1))
                .setMimeType("text/plain")
                .build();
        DriveFolder driveFolder = mNavigationPath.peek().asDriveFolder();
        Task<DriveFile> createFileTask =
                mDriveResourceClient.createFile(driveFolder, changeSet, null);
        Task<Void> updateTask = updateUiAfterTask(createFileTask);
        handleTaskError(updateTask, R.string.unexpected_error);
    }

    /**
     * Creates an empty folder in the current folder.
     */
    private void createFolder() {
        setUiInteractionsEnabled(false);
        int fileCount = mFileFolderAdapter.getCount();
        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                .setTitle("sample folder " + (fileCount + 1))
                .build();
        DriveFolder driveFolder = mNavigationPath.peek().asDriveFolder();
        Task<DriveFolder> createFolderTask =
                mDriveResourceClient.createFolder(driveFolder, changeSet);
        Task<Void> updateTask = updateUiAfterTask(createFolderTask);
        handleTaskError(updateTask, R.string.unexpected_error);
    }

    private void navigateToFolder(DriveId folderId) {
        setUiInteractionsEnabled(false);
        mNavigationPath.push(folderId);
        Task<Void> navigationTask = updateUiAfterTask(Tasks.forResult(null));
        handleTaskError(navigationTask, R.string.change_folder_error);
    }

    /**
     * Returns to previous folder view if available.
     */
    private void navigateBack() {
        setUiInteractionsEnabled(false);
        mNavigationPath.pop();
        Task<Void> navigationTask = updateUiAfterTask(Tasks.forResult(null));
        handleTaskError(navigationTask, R.string.change_folder_error);
    }

    private Task<Void> loadFolderNameAndContents(DriveFolder folder) {
        return Tasks.whenAll(loadFolderName(folder), loadFolderContents(folder));
    }

    private Task<Void> loadFolderContents(DriveFolder folder) {
        Query folderQuery = new Query.Builder()
                .setSortOrder(new SortOrder.Builder()
                        .addSortAscending(SortableField.CREATED_DATE)
                        .build())
                .build();

        Task<MetadataBuffer> task = mDriveResourceClient.queryChildren(folder, folderQuery);
        return task.continueWith(task1 -> {
            MetadataBuffer items = task1.getResult();
            List<Metadata> children = new ArrayList<>(items.getCount());
            try {
                for (int i = 0; i < items.getCount(); i++) {
                    children.add(items.get(i).freeze());
                }
                // Show folders first
                Collections.sort(children, new Comparator<Metadata>() {
                    @Override
                    public int compare(Metadata a, Metadata b) {
                        int aVal = a.isFolder() ? 1 : 0;
                        int bVal = b.isFolder() ? 1 : 0;
                        return bVal - aVal;
                    }
                });
                mFileFolderAdapter.setFiles(children);
                mFileFolderAdapter.notifyDataSetChanged();
            } finally {
                items.release();
            }
            return null;
        });
    }

    /**
     * Update the UI with the current folder name
     *
     * @param folder currently selected folder
     */
    private Task<Void> loadFolderName(DriveFolder folder) {
        Log.i(TAG, "Fetching folder metadata for " + folder.getDriveId().encodeToString());
        Task<Metadata> getMetadataTask = mDriveResourceClient.getMetadata(folder);
        return getMetadataTask.continueWith(task -> {
            mCurrentFolderNameTextView.setText(task.getResult().getTitle());
            return null;
        });
    }

    private <T> Task<Void> updateUiAfterTask(Task<T> task) {
        Task<Void> loadFolderTask = task.continueWithTask(task1 -> {
            DriveFolder currentFolder = mNavigationPath.peek().asDriveFolder();
            return loadFolderNameAndContents(currentFolder);
        });
        // Wait till Metadata is loaded to allow user interaction.
        loadFolderTask.addOnCompleteListener(task12 -> setUiInteractionsEnabled(true));
        return loadFolderTask;
    }

    /**
     * Display error if task fails.
     *
     * @param task the task
     * @param messageResourceId the error message to show
     */
    private <T> Task<T> handleTaskError(Task<T> task, final Integer messageResourceId) {
        return task.addOnFailureListener(e -> {
            Log.e(TAG, "Unexpected error", e);
            if (messageResourceId != null) {
                showMessage(messageResourceId);
            }
        });
    }

    private void showMessage(int resourceId) {
        String message = getResources().getString(resourceId);
        Log.i(TAG, message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void setUiInteractionsEnabled(boolean enabled) {
        mAddFileButton.setEnabled(enabled);
        mAddFolderButton.setEnabled(enabled);
        mFileFolderAdapter.setEnabled(enabled);
        mPreviousFolderButton.setEnabled(enabled && mNavigationPath.size() > 1);
    }
}