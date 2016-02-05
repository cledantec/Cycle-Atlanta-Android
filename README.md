# Cycle-Atlanta-Android

Cycle Atlanta Android is a multi-region app to collect data from bicyclists.

Cycle Atlanta Android provides:

1. Collecting bicyclists' route information
1. A list of bicyclists' previous trips
1. Ability to add notes to trips


### Prerequisites for both Android Studio and Gradle

1. Clone this repository
1. Install [Java Development Kit (JDK)](http://www.oracle.com/technetwork/java/javase/downloads/index.html)

### Building in Android Studio

1. Download, install, and run the latest version of [Android Studio](http://developer.android.com/sdk/installing/studio.html).
1. At the welcome screen select `Import Project`, browse to the location of this repository and select it then select Ok.
1. Open the Android SDK Manager (Tools->Android->SDK Manager) and add a checkmark for the necessary API level (see `compileSdkVersion` in [`onebusaway-android/build.gradle`](onebusaway-android/build.gradle)) then select OK.
1. Connect a [debugging enabled](https://developer.android.com/tools/device.html) Android device to your computer or setup an Android Virtual Device (Tools->Andorid->AVD Manager).
1. Open the "Build Variants" window (it appears as a vertical button on left side of workspace by default) & choose **obaGoogleDebug** to select the Google Play version, or **obaAmazonDebug** to select the Fire Phone.
1. Click the green play button (or Alt+Shift+F10) to build and run the project!

### Building from the command line using Gradle

1. Set the `JAVA_HOME` environmental variables to point to your JDK folder (e.g. `C:\Program Files\Java\jdk1.6.0_27`)
1. Download and install the [Android SDK](http://developer.android.com/sdk/index.html). Make sure to install the Google APIs for your API level (e.g. 17), the Android SDK Build-tools version for your `buildToolsVersion` version, the Android Support Repository and the Google Repository.
1. Set the `ANDROID_HOME` environmental variable to your Android SDK location.
1. To start the app, run `adb shell am start -n com.joulespersecond.seattlebusbot/org.onebusaway.android.ui.HomeActivity` (alternately, you can manually start the app)

### Release builds

To build a release build, you need to create a `gradle.properties` file that points to a `secure.properties` file, and a `secure.properties` file that points to your keystore and alias.

The `gradle.properties` file is located in the onebusaway-android directory and has the contents:
```
secure.properties=<full_path_to_secure_properties_file>
```

The `secure.properties` file (in the location specified in gradle.properties) has the contents:
```
key.store=<full_path_to_keystore_file>
key.alias=<key_alias_name>
```

Note that the paths in these files always use the Unix path separator `/`, even on Windows. If you use the Windows path separator `\` you will get the error `No value has been specified for property 'signingConfig.keyAlias'.`

### Deploying Cycle Atlanta in Your City

1. Set up your own server and database. See [this page](https://github.com/CUTR-at-USF/cycleatlanta.org/tree/regions) to setup server instructuions. 
2. Add your region specs to [this spreadsheet](https://docs.google.com/spreadsheets/d/1g9ROmJh-jhQxU_YfxeovIfAx9EAb3MEvpROx8Aa1-u4/edit#gid=0).

