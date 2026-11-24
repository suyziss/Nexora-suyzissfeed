# SUYZISS FEED

Android client for SUYZISS FEED. The feed starts empty: the app does not invent posts, followers, or likes. Photos and videos you publish are stored on the device until a backend exists.

## Build environment

| Component | Version |
|---|---:|
| JDK | 17 |
| Gradle | 8.13 |
| Android Gradle Plugin | 8.13.2 |
| Kotlin | 2.2.21 |
| Android compile SDK | 36 |
| Android build tools | 36.0.0 |
| Minimum Android version | Android 10 (API 29) |

The Gradle wrapper is included. Prefer it over a random system Gradle:

```bash
./gradlew --no-daemon :app:assembleDebug :app:lintDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

`ANDROID_HOME` must point at an SDK with `platforms;android-36` and `build-tools;36.0.0`.

## What works on the client

- Bottom navigation, including a monochrome create button
- Profile layout that keeps the avatar and Russian stat labels from colliding on narrow screens
- Profile editing: name, username, avatar, description up to 50 characters
- Settings: private account, system/light/dark theme, Telegram support `@suyziss`
- Creating a photo or video post that shows up in your own feed and profile

There is still no account server, remote feed, or real follower graph.
