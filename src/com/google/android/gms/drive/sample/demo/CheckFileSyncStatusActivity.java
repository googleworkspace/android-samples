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

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi.MetadataBufferResult;
import com.google.android.gms.drive.DriveFolder.OnChildrenRetrievedCallback;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.query.Query;

/**
 * An activity that checks if files are sync'ed or not.
 */
public class CheckFileSyncStatusActivity extends BaseDemoActivity
        implements OnChildrenRetrievedCallback {

    private ListView mListView;
    private SyncStatusResultsAdapter mResultsAdapter;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_listfiles);

        mListView = (ListView) findViewById(R.id.listViewResults);
        mResultsAdapter = new SyncStatusResultsAdapter(this);
        mListView.setAdapter(mResultsAdapter);
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        super.onConnected(connectionHint);
        Query query = new Query.Builder()
            .build();
        Drive.DriveApi.query(getGoogleApiClient(), query).addResultCallback(this);
    }

    @Override
    public void onChildrenRetrieved(MetadataBufferResult result) {
        if (!result.getStatus().isSuccess()) {
            return;
        }
        mResultsAdapter.clear();
        mResultsAdapter.append(result.getMetadataBuffer());
    }

    /**
     * An adapter to display sync status results.
     */
    private class SyncStatusResultsAdapter extends ResultsAdapter {

        public SyncStatusResultsAdapter(Context context) {
            super(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = View.inflate(getContext(),
                        android.R.layout.simple_list_item_2, null);
            }
            Metadata metadata = getItem(position);
            TextView titleTextView =
                    (TextView) convertView.findViewById(android.R.id.text1);
            TextView descTextView =
                    (TextView) convertView.findViewById(android.R.id.text2);
            titleTextView.setText(metadata.getTitle());
            descTextView.setText(String.format("Is synced? %b", metadata.getDriveId() != null));
            return convertView;
        }
    }

}
