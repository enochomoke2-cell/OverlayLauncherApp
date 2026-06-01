# OverlayLauncherApp

Android floating overlay launcher app.

## What it does
- Requests `SYSTEM_ALERT_WINDOW` permission.
- Starts a floating overlay button/window.
- Lists installed launcher apps.
- Opens selected apps from the overlay.

## Android limitation
A normal Android APK cannot truly run another installed app *inside* its own view. It can launch apps or draw a floating overlay above them. Running apps inside another app requires system/privileged access or specialized device-owner/enterprise APIs.

## Build locally
Install Android Studio or Android command-line tools, then run:

```bash
gradle assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Build with GitHub Actions
Push this project to GitHub, open the **Actions** tab, run **Build Android APK**, then download the uploaded APK artifact.
