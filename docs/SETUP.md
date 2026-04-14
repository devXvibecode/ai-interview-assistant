# Setup Guide

## System Requirements

| Tool | Version |
|------|---------|
| JDK | 17 |
| Android SDK | API 34 |
| AGP | 8.7.3 |
| Gradle | 8.11.1 |
| Kotlin | 1.9.24 |
| Min Android | API 26 (Android 8.0) |

---

## Build Options

### Option A — Android Studio (Desktop)

1. Install Android Studio from https://developer.android.com/studio
2. Clone the repository
3. Open the project folder in Android Studio
4. Wait for Gradle to sync automatically
5. Click Build then Build APK

### Option B — AndroidIDE (Mobile)

1. Install AndroidIDE from https://github.com/AndroidIDEOfficial/AndroidIDE
2. Clone the repository into your AndroidIDEProjects folder
3. Open the project in AndroidIDE
4. Run assembleDebug from the build menu

### Option C — Command Line

1. Clone the repository
2. Navigate into the folder
3. Run ./gradlew assembleDebug

---

## Gradle Properties

Create a file called gradle.properties in the root folder.
This file is already in .gitignore so it will never be committed.

Add this content:

org.gradle.jvmargs=-Xmx1536m -Dfile.encoding=UTF-8
org.gradle.parallel=false
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
android.defaults.buildfeatures.buildconfig=true
NVIDIA_API_KEY=

Leave NVIDIA_API_KEY empty.
Configure your API key inside the app Settings instead.

---

## Build Commands

./gradlew assembleDebug         Build a debug APK
./gradlew assembleRelease       Build a release APK
./gradlew clean assembleDebug   Clean then build
./gradlew installDebug          Build and install via ADB

---

## APK Location After Build

app/build/outputs/apk/debug/app-debug.apk

---

## Required Permissions

The app will ask for these permissions at runtime:

RECORD_AUDIO
Needed for speech recognition to work

SYSTEM_ALERT_WINDOW
Needed to display the floating overlay on top of other apps

POST_NOTIFICATIONS
Needed for the foreground service notification on Android 13 and above

FOREGROUND_SERVICE_MICROPHONE
Needed to use the microphone inside a foreground service

---

## Troubleshooting

### SDK location not found
Create local.properties in the root folder and add:
sdk.dir=/path/to/your/android-sdk

### Overlay not appearing
Go to phone Settings
Find Apps then AI Assistant
Tap Display over other apps
Enable it

### Speech recognition not working
Make sure microphone permission is granted
Make sure the Google app is installed on your phone
The Google app provides the SpeechRecognizer engine

### API connection failed
Check your API key is entered correctly in Settings
Tap the Test button to verify the connection
Make sure you have an active internet connection

### Build fails with out of memory
Increase memory in gradle.properties
Change Xmx1536m to Xmx2048m