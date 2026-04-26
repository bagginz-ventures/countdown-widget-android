# Countdown Widget Android

A polished v1 Android home-screen countdown widget app built in Kotlin. It lets the user set a single countdown title, target date, and accent theme, then shows a large days-left number directly on a real Android widget.

## What was built

- Native Android app in **Kotlin**
- Modern single-module Android app using:
  - **Jetpack Compose** for the edit/configuration screen
  - **AppWidgetProvider + RemoteViews** for the real home-screen widget
  - **DataStore Preferences** for local device storage
- Widget design focused on:
  - large days-left number
  - premium dark card styling
  - title, status label, target date, accent chip
- Simple edit screen for:
  - countdown title
  - target date picker
  - accent theme selection
- Automatic refresh behavior:
  - daily scheduled updates via `AlarmManager`
  - refresh on date/time/timezone changes
  - refresh after reboot / app update

## Project structure

- `app/src/main/java/com/bagginzventures/countdownwidget/MainActivity.kt` – Compose app screen
- `app/src/main/java/com/bagginzventures/countdownwidget/data/` – storage + countdown logic
- `app/src/main/java/com/bagginzventures/countdownwidget/widget/` – widget provider + scheduling
- `app/src/main/res/layout/app_widget_countdown.xml` – widget UI

## Requirements

Recommended local environment:

- **JDK 17**
- **Android Studio Iguana+** or compatible
- **Android SDK 35**

## How to run

1. Open the repo in Android Studio.
2. Let Gradle sync.
3. Run the `app` configuration on an emulator or Android device.
4. Open the app and set the title/date/theme.
5. Add **Countdown Widget** from the launcher widget picker to the home screen.

## Build commands

```bash
./gradlew test
./gradlew assembleDebug
```

## Verification status

I created the full Android project structure, Gradle setup, wrapper files, widget implementation, storage layer, UI, and README.

Local verification was limited by missing Android/Java tooling in this environment at build time:

- `java` was not available
- `ANDROID_HOME` and `ANDROID_SDK_ROOT` were unset

Because of that, I could **not** run:

- Gradle sync
- unit tests
- Android build / APK generation
- emulator/device verification

## Notes / scope

- v1 stores **one shared countdown configuration** locally and all widget instances display that same countdown.
- This keeps the first version small, clean, and shippable.
- A future v2 could add per-widget configuration, richer backgrounds, multiple countdowns, and lock-screen support.
