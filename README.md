# Noisequest Webwrapper

A simple Android WebView wrapper for Quest Board instances with configurable URL and QR code scanning.

## Features

- **User-configurable instance URL** - Set any Quest Board instance URL
- **QR Code scanning** - Quickly scan QR codes containing instance URLs
- **Persistent storage** - URL is saved locally
- **WebView with proper cookie support** - Maintains sessions
- **Back button navigation** - Proper WebView history handling
- **Progress indicator** - Shows loading progress
- **Secure by default** - No backups, no cleartext traffic

## Setup

1. Open the project in Android Studio
2. Sync Gradle files
3. Build and run on a device or emulator

## Usage

1. On first launch, you'll be prompted to enter an instance URL
2. You can either type it manually or tap "Scan QR Code" to scan a QR code
3. Tap "Save and Load" to connect
4. To change instances later, clear app data or modify SharedPreferences

## Default Instance

You can pre-populate by modifying the code, but it's designed to ask the user on first launch.

## Building

```bash
./gradlew assembleDebug
```

The APK will be in `app/build/outputs/apk/debug/`
