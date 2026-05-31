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

## Release Signing

Release builds use the same signing pattern as Nabu:

- release keystore values can come from environment variables
- local developer values can come from `local.properties`
- if no release keystore is configured, Gradle falls back to debug signing so release packaging still exercises the pipeline

Local `local.properties` example:

```properties
RELEASE_STORE_FILE=/absolute/path/to/spotbuddy-release.keystore
RELEASE_STORE_PASSWORD=...
RELEASE_KEY_ALIAS=...
RELEASE_KEY_PASSWORD=...
```

Build a release APK:

```bash
./gradlew app:assembleRelease
```

GitHub Actions release secrets:

- `RELEASE_KEYSTORE_BASE64`
- `RELEASE_STORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

The manual release workflow decodes `RELEASE_KEYSTORE_BASE64`, assembles `app:assembleRelease`, and uploads the release APK artifact.

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

Manual release workflow: `.github/workflows/android-release.yml`
