# Misty Image Viewer 🐱

A simple Android app to view random Misty the cat pictures.

## Features

- Fetches random Misty pics from the [starnumber.vercel.app/misty](https://starnumber.vercel.app/misty) API
- Material Design 3 theming
- Dark/Light mode support

## Compile Instructions

### Prerequisites

- Android SDK (API 21+)
- Java 17+
- Gradle 8.5+ (included via gradlew wrapper)

### Build

```bash
# Clone the repo
git clone https://github.com/sfgrdsgdsfdrfyg-code/misty-image-viewer.git
cd misty-image-viewer

# Build debug APK
./gradlew assembleDebug

# APK will be at: app/build/outputs/apk/debug/app-debug.apk
```

### Install

```bash
# Transfer APK to your Android device and install
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- Coil (image loading)
- Coroutines

## License

MIT