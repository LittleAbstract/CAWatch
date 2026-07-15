# CAWatch — Build Instructions

Pure Kotlin + Room Android app. **No Rust/NDK required** (native layer was removed).

## Prerequisites

- JDK 17+
- Android SDK with `platforms;android-34` and `build-tools;34.0.0`
- `ANDROID_HOME` set

```bash
export ANDROID_HOME=$HOME/Library/Android/sdk   # macOS typical
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH
```

Or use `./setup_and_build.sh` for a sudo-free toolchain under `~/android-toolchain`.

## Build

```bash
cd CAWatch
./build.sh
# or
./gradlew assembleDebug
```

Debug APK:

```
app/build/outputs/apk/debug/app-debug.apk
```

Release bundle for Play:

```bash
./gradlew bundleRelease
```

## Install & manual test

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.security.careactivator/.ui.OnboardingActivity
adb logcat | grep -iE 'CertScanner|BootReceiver|MainActivity|CaDetector'
```

1. Complete onboarding → baseline is captured.
2. Install a test user CA (Settings → Security → Encryption & credentials → Install a certificate → CA certificate).
3. Tap **Scan now** → expect alert + list row.
4. Open the row → Open credential settings / Mark trusted / Share.
5. Optional: reboot → boot scan runs if onboarding completed.

## Project layout

```
app/src/main/java/com/security/careactivator/
  db/           Room entities + DAO
  prefs/        Onboarding + monitoring mode
  receiver/     BootReceiver
  service/      CertScannerService
  ui/           Onboarding, Main, Detail, adapter
  util/         Export + settings intents
```

## Troubleshooting

**Service killed immediately**  
Expected in **Boot + manual** mode after each scan. Use **Always-on** for a persistent notification.

**No notification on Android 13+**  
Grant notification permission during onboarding (or system settings).

**Baseline empty after clear data**  
First scan after reinstall re-captures baseline; pre-existing user CAs are not treated as “new.”
