# SpotBuddy

SpotBuddy is a Kotlin/Compose Android workout tracker for planning sessions, counting completed sets, timing planks, and reviewing local historical analytics.

## Local Build

```bash
./gradlew build
./gradlew test
./gradlew assembleDebug
```

## Install Debug APK

```bash
adb devices
./gradlew installDebug
```

The normal Gradle install path updates the app without clearing local app data. Do not uninstall or clear data before installing if you want to preserve the local SQLite history database and saved workout preferences.

## CI Flow

The GitHub Actions workflow mirrors the Nabu Android CI pattern:

- checks out the repo
- installs JDK 17
- installs Android SDK tooling
- runs `./gradlew build`
- runs `./gradlew test`
- runs `./gradlew assembleDebug`
- uploads the debug APK and test-result XML artifacts

Workflow file: `.github/workflows/android-ci.yml`

