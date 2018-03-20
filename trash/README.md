# Google Drive Android API Trash Sample.

Google Drive Android API Trash Sample app demonstrates trashing of
DriveResources via the [Google Drive Android API][1] available in
[Google Play Services][2]. Only DriveResources accessible to an app can
be trashed/untrashed. For full API details check
[Google Drive Android API Reference][3].

### How placing files in Trash works.
If the DriveResource being trashed/untrashed is a DriveFolder then all child
DriveResources will be updated to the same trash state as the DriveFolder being
updated. A successful trash/untrash will only occur if all child resources are
accessible to your app.

### Using the sample.
Add DriveFiles or DriveFolders to the currently selected DriveFolder by
clicking the appropriate "ADD" button. Long press on a DriveResource to switch
its trash status. Note that child resources of DriveFolders take on the same
trash state as their parent folder.

[1]: https://developers.google.com/drive/android/intro
[2]: http://developer.android.com/google/play-services
[3]: https://developer.android.com/reference/com/google/android/gms/drive/package-summary.html
