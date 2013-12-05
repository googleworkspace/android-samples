# Google Drive Android Quickstart

This application provides a
[single activity](src/com/google/android/gms/drive/sample/quickstart/MainActivity.java)
designed to get you up and running with the [Google Drive API for Android](https://developers.google.com/drive/android).

# What does it do?

* Takes photos and stores them in Drive
* Displays a file picker to the user to select where to save files
* Shows you how to write file content
* Shows you how to set file metadata including title and MIME type

# Create an OAuth 2.0 client

To run the quickstart, or access the Drive API from any other app, you must
first register your digitally signed .apk file's public certificate in the
[Google Cloud Console](https://cloud.google.com/console):

1. Go to the [Google Cloud Console](https://cloud.google.com/console).
1. Select a project, or create a new one.
1. In the sidebar on the left, select APIs & auth. In the displayed list of APIs, make sure the Drive API status is set to ON.
1. In the sidebar on the left, select Registered apps.
1. At the top of the page, select Register App.
1. Type a name for the application and select Android.
1. Choose Accessing APIs directly from Android.
1. In the Package name field, enter the app's package name `com.google.android.gms.drive.sample.quickstart`
1. In a terminal, run the the Keytool utility to get the SHA1 fingerprint of the certificate. For the debug.keystore, the password is android.

        keytool -exportcert -alias androiddebugkey -keystore ~/.android/debug.keystore -list -v

    For Eclipse, the debug keystore is typically located at
    `~/.android/debug.keystore`. Otherwise replace the path in the command above
    with the actual location of your keystore.

    The Keytool prints the fingerprint to the shell. For example:

        $ keytool -exportcert -alias androiddebugkey -keystore ~/.android/debug.keystore -list -v
        Enter keystore password: Type "android" if using debug.keystore
        Alias name: androiddebugkey
        Creation date: Aug 27, 2012
        Entry type: PrivateKeyEntry
        Certificate chain length: 1
        Certificate[1]:
        Owner: CN=Android Debug, O=Android, C=US
        Issuer: CN=Android Debug, O=Android, C=US
        Serial number: 503bd581
        Valid from: Mon Aug 27 13:16:01 PDT 2012 until: Wed Aug 20 13:16:01 PDT 2042
        Certificate fingerprints:
           MD5:  1B:2B:2D:37:E1:CE:06:8B:A0:F0:73:05:3C:A3:63:DD
           SHA1: D8:AA:43:97:59:EE:C5:95:26:6A:07:EE:1C:37:8E:F4:F0:C8:05:C8
           SHA256: F3:6F:98:51:9A:DF:C3:15:4E:48:4B:0F:91:E3:3C:6A:A0:97:DC:0A:3F:B2:D2:E1:FE:23:57:F5:EB:AC:13:30
           Signature algorithm name: SHA1withRSA
           Version: 3
        Copy the SHA1 fingerprint, which is highlighted in the example above.


1. Paste the SHA1 fingerprint into the certificate fingerprint field in the Cloud Console.
1. Click Register.
