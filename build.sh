#!/usr/bin/env bash
#
# CA Detector - Local build & verification script
#
# Runs on a machine with: JDK 17, Android SDK (cmdline-tools), and the
# sdkmanager packages below. This host did NOT have them, so the build
# step was not executed during development.
#
# Prerequisites:
#   export ANDROID_HOME=$HOME/Library/Android/sdk   # or your SDK path
#   sdkmanager "platform-tools" "platforms;android-34" \
#              "build-tools;34.0.0" "ndk;26.2.11394342"
#
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_DIR"

echo "==> Gradle wrapper sanity check"
test -x gradlew || chmod +x gradlew

echo "==> Building debug APK (no native toolchain needed)"
./gradlew assembleDebug

echo "==> Debug APK built:"
ls -la app/build/outputs/apk/debug/*.apk

echo "==> Lint check"
./gradlew lintDebug || true

echo "==> Done. Install on a device/emulator with:"
echo "    adb install -r app/build/outputs/apk/debug/app-debug.apk"

# ---------------------------------------------------------------------------
# Manual verification on device (after install):
#   1. Open the app once -> it captures the trusted baseline on first run.
#   2. adb shell am start -n com.security.careactivator/.ui.MainActivity
#   3. Install a test CA:
#        adb push test_ca.crt /sdcard/Download/
#        -> Settings > Security > Encryption & credentials >
#           Install a certificate > CA certificate
#      OR for automation:
#        adb shell ... (device policy / certinstaller)
#   4. Tap "Scan Now" -> expect "New CA installed: <issuer>" notification
#      and a new row in the list.
#   5. Reboot -> BootReceiver should re-scan and alert again.
# ---------------------------------------------------------------------------
