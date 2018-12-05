# Google Drive Android API Migration

This application serves as an example of how to replicate the now-deprecated
Drive Android API's functionality using equivalent Drive REST API calls. See the
[migration guide](https://developers.google.com/drive/android/deprecation) for
an overview of the migration steps.

## What does it do?

*   Creates text files in the user's My Drive folder
*   Edits file contents and metadata and saves them to Drive
*   Displays a file picker for opening Drive files in read-only mode
*   Queries the REST API for all files visible to the app

## Set Up

1.  Install the [Android SDK](https://developer.android.com/sdk/index.html).
1.  Download and configure the
    [Google Play services SDK](https://developer.android.com/google/play-services/setup.html),
    which includes the Google Drive Android API.
1.  Create a
    [Google API Console](https://console.developers.google.com/projectselector/apis/dashboard)
    project and enable the Drive API library.
1.  Register an OAuth 2.0 client for the package
    `com.google.android.gms.drive.sample.driveapimigration` with your own
    [debug keys](https://developers.google.com/drive/android/auth).

See full instructions in the
[Getting Started guide](https://developers.google.com/drive/android/get-started).
