Cordova Crosswalk Data Migration Plugin [![Latest Stable Version](https://img.shields.io/npm/v/cordova-plugin-crosswalk-data-migration.svg)](https://www.npmjs.com/package/cordova-plugin-crosswalk-data-migration) 
=================================

Cordova/Phonegap plugin for Android to preserve persistent webview data after removing Crosswalk from your app.  

<!-- DONATE -->
[![donate](https://www.paypalobjects.com/en_US/i/btn/btn_donateCC_LG_global.gif)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=ZRD3W47HQ3EMJ)

I dedicate a considerable amount of my free time to developing and maintaining this Cordova plugin, along with my other Open Source software.
To help ensure this plugin is kept updated/new features are added/bugfixes are implemented quickly, please donate a couple of dollars (or a little more if you can stretch) as this will help me to afford to dedicate time to its maintenance. Please consider donating if you're using this plugin in an app that makes you money, if you're being paid to make the app, or if you're asking for new features or priority bug fixes.
<!-- END DONATE -->


<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**

- [Background info](#background-info)
- [Purpose](#purpose)
- [Installation](#installation)
  - [Using the Cordova/Phonegap CLI](#using-the-cordovaphonegap-cli)
  - [PhoneGap Build](#phonegap-build)
- [Usage](#usage)
- [Migrated data](#migrated-data)
- [Implementation notes and caveats](#implementation-notes-and-caveats)
- [Continuing to support Android 4](#continuing-to-support-android-4)
  - [Final release for Android 4](#final-release-for-android-4)
  - [Conditional Crosswalk using multiple APKs](#conditional-crosswalk-using-multiple-apks)
- [Example app](#example-app)
- [License](#license)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

# Background info

Cordova/Phonegap apps have utilised the [Crosswalk project](https://crosswalk-project.org/) via plugins such as [cordova-plugin-crosswalk-webview](https://github.com/crosswalk-project/cordova-plugin-crosswalk-webview) to bring the benefits of a modern web runtime ("webview") to old Android 4.x devices, including:

- consistent, predictable behavior across old Android versions vs system webviews which are inconsistent.
- modern APIs and features available the built-in system webviews do not support.
- fixes for Chromium bugs that are present in the old Android system webviews.
- performance improvements compared with the old Android system webviews.

However, from Android 5.0 onwards, the system webview is self-updating, meaning new features and bug fixes are delivered by Play Store just like updates for any other app.
This means devices running these modern Android versions don't need Crosswalk to make use of modern web features.

In fact running Crosswalk on modern Android 5.0+ devices is actually unfavourable for the following reasons:

- The Crosswalk project [was deprecated])(https://crosswalk-project.org/blog/crosswalk-final-release.html) in February 2017.
    - This means it is no longer being actively maintained or updated, so any bugs which are present in the final (v23) release will remain and no new features will be added.
        - By contrast, the modern system webviews continues to be updated with new features and bug fixes, so in the same way that Crosswalk is more modern than old Android 4 webviews, it is actually less modern than Android 5+ webviews.
    - `cordova-plugin-crosswalk-webview` is [incompatible with the latest release of `cordova-android@6.4.0`](https://github.com/crosswalk-project/cordova-plugin-crosswalk-webview/issues/183)
        - while this issue [might get fixed](https://github.com/apache/cordova-android/pull/417) in a future `cordova-android` release, there's always the possibility that a future version of `cordova-android` becomes incompatible with Crosswalk
- Crosswalk has always had some drawbacks, all of which are unnecessary on modern Android:
    - More complex release process if using multiple APKs
        - By default Crosswalk outputs 2 APKs: one for ARMv7 device architectures and one for x86
        - This complicates the release process meaning:
            - multiple APKs need to be uploaded to the Play Store console
            - the version codes need to be carefully controlled so ARMv7 is higher than x86.
        - It's possible to configure Crosswalk to output a single combined APK, but this doubles the APK size (see below)
    - Increased APK size (about 17MB)
        - The total APK size can then double if outputting a single combined APK
    - Increased memory footprint of ~30Mb
    - Increased size on disk when installed (about 50MB)
    

# Purpose

- When using Crosswalk in an Android application, the persistent data of the webview (Local Storage, IndexedDB, etc.) is stored separately from the system webview.
- This means if you remove Crosswalk from your Android app without any precautions, all persistent data will be lost on updating from a previous version which contained Crosswalk.
- This plugin provides a migration mechanism to copy data from the Crosswalk storage location to the system webview store location, making that persistent data available to the system webview and therefore preserving it.

# Installation

The plugin is registered on [npm](https://www.npmjs.com/package/cordova-plugin-crosswalk-data-migration) as `cordova-plugin-crosswalk-data-migration`

## Using the Cordova/Phonegap [CLI](http://docs.phonegap.com/en/edge/guide_cli_index.md.html)

    $ cordova plugin add cordova-plugin-crosswalk-data-migration
    $ phonegap plugin add cordova-plugin-crosswalk-data-migration


## PhoneGap Build
Add the following xml to your config.xml to use the latest version of this plugin from [npm](https://www.npmjs.com/package/cordova-plugin-crosswalk-data-migration):

    <gap:plugin name="cordova-plugin-crosswalk-data-migration" source="npm" />

# Usage

Assuming you have an existing Cordova Android app which uses Crosswalk, you need to do the following:
- Remove Crosswalk from the app
    - **IMPORTANT**: Before releasing an update containing this plugin, Crosswalk must be removed from the Cordova app.
    - If building locally:
        - this means removing the plugin from your Cordova project
            - e.g. `cordova plugin rm cordova-plugin-crosswalk-webview`
        - you may need to re-create the `cordova-android` platform project to remove the Crosswalk files and configuration
            - `cordova platform rm android --nosave && cordova platform add android --nosave`
- Add this plugin to the app
    - If building locally, e.g. `cordova plugin add cordova-plugin-crosswalk-data-migration`
- Build the new app
    - The new build should of course have higher version code than the previous Crosswalk version.
    - This build can then be installed as an update over the previous Crosswalk version and the data migration will preserve local persistent data, making it available to the system webview with which Cordova is now running. 


# Migrated data

The plugin will migrate the following data from Crosswalk back to the system webview, dependent on the Android version: 

- Android 4.4 and above
    - Local Storage
    - Cookies
    - IndexedDB
    - WebSQL
    - Cache
- Android 4.3 and below
    - Local Storage

# Implementation notes and caveats

- On Android 4.3 and below only local storage is migrated. 
    - This is because Android 4.3 and below do not use a Chrome-powered Webview, so the storage format is entirely different to the Chrome-powered Crosswalk webview, so the data is backwardly compatible.
- Due to [Chromium dropping support for `file://` cookies](https://codereview.chromium.org/976553002/#ps80001), although the Cookie data is copied from Crosswalk to the system webview, it will probably be empty.
    - A [pull request](https://github.com/crosswalk-project/cordova-plugin-crosswalk-webview/pull/98) was made to re-enable `file://`-based cookies in the Crosswalk Cordova plugin, but it was never merged. 
- The migration mechanism is based on inverting the operations performed by Crosswalk to migrate data from the system webview to Crosswalk.
    - The Crosswalk migration can be seen in the [C++ source code](https://raw.githubusercontent.com/fujunwei/crosswalk/39e76e5644c5563067ab1fd908f1dd456aeb881f/runtime/browser/xwalk_browser_main_parts_android.cc).
- The plugin looks for Crosswalk data firstly in the app's "private" data directory (e.g. `/data/data/[package_id]`) and, if it doesn't find it there, secondarily in the "external" data directory - which is usually on internal storage also, (e.g. `/storage/emulated/0/Android/data/[package_id]`).
- After copying data from the Crosswalk directory to the system webview directory, the plugin deletes the Crosswalk data directory.
    - This is done both the avoid leaving unnecessary orphaned files and to prevent the plugin migrating the Crosswalk data every time the app runs.
- The migration operation takes place after the Cordova has initialised the webview.
    - This may lead to a visual glitch where the app is momentarily visible then disappears/re-appears.
        - This occurs because the plugin needs to restart the Cordova activity to refresh the webview data after migrating it.
        - One workaround is to slightly delay hiding of the splashscreen to hide this visual anomaly.
    - Why can't the data be migrated before Cordova initialises the webview so the Cordova activity doesn't need to be restarted?
        - I tried this approach initially by placing the migration code inside an Application class (since Cordova doesn't use one) and running the migration onCreate(), so the data was migrated before Cordova had initialised the webview.
        - The problem with this approach was:
            - when a non-Crosswalk based Cordova app is installed on an Android device, when Cordova boots up the webview for the first time, the system webview creates a set of storage folders in the application data area.
            - if a Crosswalk-based Cordova app is installed on a device, the system webview storage folders are not created.
            - so the non-Crosswalk update was installed over the Crosswalk version, the migration code ran first and migrated the data to the system webview location.
            - however, when the system webview booted up for the first time, it detected that not all the correct files were in place in the system webview location.
            - this caused the system webview to wipe the existing system webview data (containing the migrated Crosswalk data) and replace it with a new, empty version.
            - the result was that the data migrated from Crosswalk was lost.
        - By migrating the data **after** Cordova has booted the system webview for the first time, this ensures all the correct files are in place.
        - So when the migration code copies the data from the Crosswalk location to the system webview location, it isn't subsequently wiped by the system webview when the plugin restarts the Cordova activity.

# Continuing to support Android 4
- Google's [Android version stats](https://developer.android.com/about/dashboards/index.html) from 2nd to 9th Nov 2017 show that over 20% of devices are still running Android 4.x
    - This is a global statistic, so depending on your target audience, the % may be much smaller.    
- So you may still wish to support Android 4.x devices for some period of time, even though the Crosswalk project has been deprecated.  

## Final release for Android 4
- One option is to make a final release of your Crosswalk-powered Cordova app for Android 4.x
- Subsequent releases should set a [minimum supported API level](https://developer.android.com/guide/topics/manifest/uses-sdk-element.html#apilevel) of API 21.
    - This will ensure that they are only offered to devices running Android 5.0 or higher.
    - You can do this in Cordova by setting the `android-minSdkVersion` [preference in `config.xml`](https://cordova.apache.org/docs/en/latest/config_ref/#preference).
- You can then release updates without Crosswalk and that contain this plugin to migrate data from previous installations on Android 5+ which were Crosswalk-based. 

## Conditional Crosswalk using multiple APKs
- It is also possible to publish multi-APK updates to the Play Store which use Crosswalk for Android 4.x and the system webview for Android 5+.
- This approach is necessary if continued updates must also be delivered to Android 4.x.
- To do so, follow these steps:
    - Build the Crosswalk-enabled APK(s) in a Cordova project which:
        - **does** contain the Cordova Crosswalk plugin.
        - **does not** contain this migration plugin.
        - **does not** set a minimum supported API level above Android 4.x.
    - Build the system-webview APK which:
        - **does not** contain the Cordova Crosswalk plugin.
        - **does** contain this migration plugin.
        - sets a minimum supported API level of 21 (Android 5.0)
            - note: you can specify this in Cordova CLI with the `--minSdkVersion` argument
            - e.g. `cordova build --release -- --minSdkVersion=21`
        - increments the version code to be higher than in the Crosswalk APK(s)
    

# Example app
I've created an example app to demonstrate/validate this migration plugin.
- To make it easier to test, I have split it across 2 repos.
- Both contain Cordova projects with the same app logic (HTML/CSS/JSS).
- But the first is configured to use the Cordova Crosswalk plugin.
- While the second is configured to use this plugin and have a higher version code, such that it can be installed as an update over the first.

To run the migration test, do the following:
- Build & run the first part of the app:
    - Clone it: [cordova-plugin-crosswalk-data-migration-test-part1](https://github.com/dpa99c/cordova-plugin-crosswalk-data-migration-test-part1)
        - `git clone https://github.com/dpa99c/cordova-plugin-crosswalk-data-migration-test-part1 && cd cordova-plugin-crosswalk-data-migration-test-part1`
    - Add the Android platform
        - `cordova platform add android`
    - Connect a device/emulator and run the app
        - `cordova run android`
- Or install the pre-built APK:
    - [ARMv7](https://github.com/dpa99c/cordova-plugin-crosswalk-data-migration-test-part1/build/crosswalk-data-migration-test-part-1-armv7.apk)
    - [x86](https://github.com/dpa99c/cordova-plugin-crosswalk-data-migration-test-part1/build/crosswalk-data-migration-test-part-1-x86.apk)
- In the app:
    - Observe "Webview" is "Crosswalk"
    - Press "Generate data" to generate some random data.
    - Press "Save to storage" to save it use the various local storage technologies.
    - Press "Reload page" or restart the app to convince yourself the data is saved.
        - Note: Cookies will not be persisted due to the Crosswalk no longer supporting `file://` cookies.
- Build & run the second part of the app:        
    - Clone it : [cordova-plugin-crosswalk-data-migration-test-part2](https://github.com/dpa99c/cordova-plugin-crosswalk-data-migration-test-part2)
        - `git clone https://github.com/dpa99c/cordova-plugin-crosswalk-data-migration-test-part2 && cd cordova-plugin-crosswalk-data-migration-test-part2` 
    - Add the Android platform
       - `cordova platform add android`
    - Run the app
       - `cordova run android`
- Or install the pre-built APK: [ARMv7/x86](https://github.com/dpa99c/cordova-plugin-crosswalk-data-migration-test-part2/build/crosswalk-data-migration-test-part-2.apk)
- In the app:
    - Observe "Webview" is "System"
    - Observe the previous values from the Crosswalk view have been loaded back into the inputs.        

# License
================

The MIT License

Copyright (c) 2017 Dave Alden (Working Edge Ltd.)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
