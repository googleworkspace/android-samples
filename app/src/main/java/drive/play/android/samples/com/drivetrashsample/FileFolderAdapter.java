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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.drive.Metadata;

import java.util.List;

/**
 * Adapter for displaying files and folders in the ListView.
 */
public class FileFolderAdapter extends ArrayAdapter<Metadata> {

    /**
     * ViewHolder to store each of the component views that need to be updated by the Adapter.
     */
    private class ViewHolder {
        TextView filenameTextView;
        TextView trashStatusTextView;
        ImageView fileFolderImageView;
    }

    private List<Metadata> fileMetadata;
    private boolean enabled;

    public FileFolderAdapter(Context context, int resource, List<Metadata> fileMetadata) {
        super(context, resource);
        this.fileMetadata = fileMetadata;
        enabled = true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rootView = inflater.inflate(R.layout.resource_item, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.filenameTextView = (TextView) rootView.findViewById(R.id.filenameTextView);
            viewHolder.trashStatusTextView = (TextView) rootView.findViewById(R.id.trashStatusTextView);
            viewHolder.fileFolderImageView = (ImageView) rootView.findViewById(R.id.imageView);

            convertView = rootView;
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final Metadata metadata = getItem(position);

        viewHolder.filenameTextView.setText(metadata.getTitle());
        viewHolder.trashStatusTextView.setText(metadata.isTrashed() ?
                getContext().getResources().getString(R.string.trashed_status) :
                getContext().getResources().getString(R.string.not_trashed_status));
        if (metadata.isFolder()) {
            viewHolder.fileFolderImageView.setImageDrawable(getContext().getResources()
                    .getDrawable(R.drawable.ic_folder_black_18dp));
        } else {
            viewHolder.fileFolderImageView.setImageDrawable(getContext().getResources()
                    .getDrawable(R.drawable.ic_description_black_18dp));
        }

        return convertView;
    }

    @Override
    public int getCount() {
        return fileMetadata.size();
    }

    @Override
    public Metadata getItem(int position) {
        return fileMetadata.get(position);
    }

    @Override
    public boolean isEnabled(int position) {
        return enabled;
    }

    public void setFiles(List<Metadata> fileMetadata) {
        this.fileMetadata = fileMetadata;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
