#!/bin/bash
# CA Detector - self-contained toolchain install + debug APK build.
# Sudo-free: everything lands in ~/android-toolchain. Uses Adoptium JDK 17
# (Gradle 8.2 / AGP 8.2.0 REQUIRE JDK 17; the system's Temurin 26 will NOT work),
# a direct Gradle 8.2 download, and the Android cmdline-tools.
set -e

TOOLCHAIN=$HOME/android-toolchain
JDK_DIR=$TOOLCHAIN/jdk17
SDK_DIR=$TOOLCHAIN/android-sdk
GRADLE_DIR=$TOOLCHAIN/gradle-8.2
PROJECT=/Users/Corvo/Documents/Zero/user_ca/CaReactivatorDetector

mkdir -p "$TOOLCHAIN"

# ---------- 1. JDK 17 (sudo-free, Adoptium aarch64) ----------
if [ ! -x "$JDK_DIR/Contents/Home/bin/java" ]; then
  echo "==> Downloading Temurin JDK 17"
  curl -fsSL --max-time 600 -o /tmp/jdk17.tar.gz \
    "https://api.adoptium.net/v3/binary/latest/17/ga/mac/aarch64/jdk/hotspot/normal/eclipse?project=jdk"
  tar -xzf /tmp/jdk17.tar.gz -C "$TOOLCHAIN"
  # Adoptium extracts to e.g. "jdk-17.0.19+10" (no .jdk suffix)
  mv "$TOOLCHAIN"/jdk-17* "$JDK_DIR"
  rm -f /tmp/jdk17.tar.gz
fi
export JAVA_HOME=$JDK_DIR/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
java -version

# ---------- 2. Android cmdline-tools (sudo-free) ----------
mkdir -p "$SDK_DIR/cmdline-tools"
if [ ! -x "$SDK_DIR/cmdline-tools/latest/bin/sdkmanager" ]; then
  echo "==> Downloading Android cmdline-tools"
  curl -fsSL --max-time 600 -o /tmp/cmdline-tools.zip \
    "https://dl.google.com/android/repository/commandlinetools-mac-11076708_latest.zip"
  unzip -o /tmp/cmdline-tools.zip -d "$SDK_DIR/cmdline-tools"
  mv "$SDK_DIR/cmdline-tools/cmdline-tools" "$SDK_DIR/cmdline-tools/latest"
  rm -f /tmp/cmdline-tools.zip
fi
export ANDROID_HOME=$SDK_DIR
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH

# ---------- 3. Licenses + SDK packages ----------
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# ---------- 4. Gradle 8.2 (sudo-free, direct download) ----------
if [ ! -x "$GRADLE_DIR/bin/gradle" ]; then
  echo "==> Downloading Gradle 8.2"
  curl -fsSL --max-time 600 -o /tmp/gradle.zip \
    "https://services.gradle.org/distributions/gradle-8.2-bin.zip"
  unzip -o /tmp/gradle.zip -d "$TOOLCHAIN"
  rm -f /tmp/gradle.zip
fi
export PATH=$GRADLE_DIR/bin:$PATH
gradle --version

# ---------- 5. Persist env vars ----------
grep -q 'ANDROID_TOOLCHAIN' ~/.zshrc 2>/dev/null || cat >> ~/.zshrc <<'EOF'

# CA Detector local toolchain (sudo-free, in ~/android-toolchain)
export ANDROID_HOME=$HOME/android-toolchain/android-sdk
export JAVA_HOME=$HOME/android-toolchain/jdk17/Contents/Home
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$HOME/android-toolchain/gradle-8.2/bin:$PATH
EOF

# ---------- 6. Build debug APK (direct gradle; no wrapper needed) ----------
cd "$PROJECT"
gradle assembleDebug

echo "=== DONE ==="
ls -la app/build/outputs/apk/debug/app-debug.apk
