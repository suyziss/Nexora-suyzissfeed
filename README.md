# SUYZISS FEED

[![Android CI](https://github.com/suyziss/Nexora-suyzissfeed/actions/workflows/android-build.yml/badge.svg)](https://github.com/suyziss/Nexora-suyzissfeed/actions/workflows/android-build.yml)

Android client for SUYZISS FEED. The feed starts empty: the app does not invent posts, followers, or likes. Photos and videos you publish are stored on the device until a backend exists.

## CI/CD

GitHub Actions runs on every push and PR to `main`, and can be triggered manually.

| Job | What it does |
|-----|----------------|
| **Build debug APK** | JDK 17 + Android SDK 36 → `assembleDebug` + `lintDebug` → uploads APK artifact |
| **Build release APK** | (on `main` / manual) unsigned release APK |

### Download the APK from CI

1. Open [Actions](https://github.com/suyziss/Nexora-suyzissfeed/actions)
2. Choose the latest successful **Android CI** run
3. In **Artifacts** download `suyziss-feed-debug-<sha>`

Artifacts are kept for 14 days.

### Run CI manually

Actions → **Android CI** → **Run workflow**

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

Prefer the Gradle wrapper:

```bash
chmod +x gradlew
./gradlew --no-daemon :app:assembleDebug :app:lintDebug
```

If `gradle/wrapper/gradle-wrapper.jar` is missing locally, generate it once:

```bash
gradle wrapper --gradle-version 8.13
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
