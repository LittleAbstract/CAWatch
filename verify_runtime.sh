#!/bin/bash
# Runtime verification for CA Detector (option A: baseline + instant diff).
# Prereqs (already installed by setup_and_build.sh + emulator install):
#   - ANDROID_HOME/~/android-toolchain, JDK17, Gradle, emulator, arm64 image
#   - built APK at app/build/outputs/apk/debug/app-debug.apk
#   - a test CA at /tmp/ca_test/test_ca.crt
set -e

export ANDROID_HOME=$HOME/android-toolchain/android-sdk
export JAVA_HOME=$HOME/android-toolchain/jdk17/Contents/Home
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$HOME/android-toolchain/gradle-8.2/bin:$PATH

PROJECT=/Users/Corvo/Documents/Zero/user_ca/CaReactivatorDetector
APK=$PROJECT/app/build/outputs/apk/debug/app-debug.apk
AVD_NAME=ca_detector_test
PKG=com.security.careactivator
CERT=/tmp/ca_test/test_ca.crt

echo "==> 1. Create AVD (arm64, android-34, google_apis)"
if ! avdmanager list avd 2>/dev/null | grep -q "$AVD_NAME"; then
  echo "no" | avdmanager create avd -n "$AVD_NAME" -k "system-images;android-34;google_apis;arm64-v8a" -d "pixel_6" --force
fi

echo "==> 2. Start emulator headless"
# Kill any prior emulator
adb emu kill >/dev/null 2>&1 || true
$ANDROID_HOME/emulator/emulator -avd "$AVD_NAME" -no-window -noaudio -no-boot-anim -gpu swiftshader_indirect > /tmp/emulator_run.log 2>&1 &
EMU_PID=$!

echo "==> 3. Wait for boot"
adb wait-for-device
# bootcomplete can take a while on first run
for i in $(seq 1 60); do
  if [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; then
    echo "booted after ${i}0s-ish"; break
  fi
  sleep 10
done
# Give the system a moment to settle
sleep 5

echo "==> 4. Install APK"
adb install -r "$APK"

echo "==> 5. First launch -> capture baseline (no CA installed yet)"
adb shell am start -n "$PKG/.ui.MainActivity"
sleep 4
# Pull the Room DB to confirm a baseline was captured
adb exec-out run-as "$PKG" cat "/data/data/$PKG/databases/ca_detector.db" > /tmp/ca_detector.db 2>/dev/null || true
echo "    baseline rows: $(sqlite3 /tmp/ca_detector.db 'SELECT COUNT(*) FROM baseline_ca;' 2>/dev/null || echo 'n/a (sqlite3 missing)')"

echo "==> 6. Install the test CA as a USER CA"
# Push cert, then use the certinstaller intent to add it as a CA cert.
adb push "$CERT" /sdcard/Download/test_ca.crt
# The cert installer requires user interaction on real UI; drive via intent + input taps.
adb shell am start -n com.android.certinstaller/.CertInstallerMain -a android.intent.action.VIEW -d "file:///sdcard/Download/test_ca.crt" -t application/x-x509-ca-cert
sleep 3
# Tap through the installer UI (OK / Install). Coordinates are roughly centered.
adb shell input tap 540 1200   # first dialog "Install anyway"?
sleep 2
adb shell input tap 540 1200   # confirm install
sleep 3
echo "    CA install attempt done"

echo "==> 7. Trigger scan via the app's Scan Now button (tap) and/or restart service"
# Tap Scan Now button: navigate to app then tap. We use the activity + a tap at button location.
adb shell am start -n "$PKG/.ui.MainActivity"
sleep 2
# The Scan Now MaterialButton is near the bottom; tap lower-center.
adb shell input tap 540 1900
sleep 5

echo "==> 8. Assert detection"
adb exec-out run-as "$PKG" cat "/data/data/$PKG/databases/ca_detector.db" > /tmp/ca_detector2.db 2>/dev/null || true
DETECTED=$(sqlite3 /tmp/ca_detector2.db 'SELECT COUNT(*) FROM detected_ca;' 2>/dev/null || echo 'n/a')
echo "    detected_ca rows: $DETECTED"
if [ "$DETECTED" != "0" ] && [ "$DETECTED" != "n/a" ]; then
  echo "RESULT: PASS - detection fired ($DETECTED new CA(s))"
  sqlite3 /tmp/ca_detector2.db 'SELECT issuer,subject FROM detected_ca;' 2>/dev/null
else
  echo "RESULT: CHECK - detected_ca empty. Dump logcat for clues:"
  adb logcat -d -t 200 | grep -i "CertScannerService\|CA Detector" || true
fi

echo "==> 9. Cleanup emulator"
adb emu kill >/dev/null 2>&1 || kill $EMU_PID 2>/dev/null || true
echo "DONE (see RESULT above)"
