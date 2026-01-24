#!/bin/bash

set -euo pipefail

die() {
  echo "$1" 1>&2
  exit 1
}

ensure_java() {
  if command -v java >/dev/null 2>&1; then
    if java -version >/dev/null 2>&1; then
      return 0
    fi
  fi

  local candidate_java_homes=(
    "/Applications/Android Studio.app/Contents/jbr/Contents/Home"
    "/Applications/Android Studio Preview.app/Contents/jbr/Contents/Home"
  )

  local candidate
  for candidate in "${candidate_java_homes[@]}"; do
    if [ -x "$candidate/bin/java" ]; then
      export JAVA_HOME="$candidate"
      export PATH="$JAVA_HOME/bin:$PATH"
      break
    fi
  done

  if ! command -v java >/dev/null 2>&1 && [ -x /usr/libexec/java_home ]; then
    local detected_java_home
    detected_java_home=$(/usr/libexec/java_home 2>/dev/null || true)
    if [ -n "${detected_java_home:-}" ] && [ -x "$detected_java_home/bin/java" ]; then
      export JAVA_HOME="$detected_java_home"
      export PATH="$JAVA_HOME/bin:$PATH"
    fi
  fi

  if ! command -v java >/dev/null 2>&1 || ! java -version >/dev/null 2>&1; then
    die "❌ Unable to locate a Java Runtime. Please install JDK 17 (or Android Studio) and ensure 'java' is available."
  fi
}

if [ ! -x "./gradlew" ]; then
  die "❌ ./gradlew not found or not executable."
fi

if ! command -v adb >/dev/null 2>&1; then
  die "❌ adb not found. Please install Android Platform Tools and ensure 'adb' is in PATH."
fi

ensure_java

echo "🚀 Starting automated build..."

# 1. Build the APK (Assemble only, do not install yet)
./gradlew assembleDebug

if [ $? -ne 0 ]; then
    echo "❌ Build failed. Please check the logs above."
    exit 1
fi

echo "✅ Build successful!"

# 2. Get the list of connected devices
# specific to standard ADB output: "List of devices attached" header, then "serial device"
DEVICES=$(adb devices | awk 'NR>1 && $2=="device" {print $1}')

if [ -z "$DEVICES" ]; then
    echo "⚠️ No devices connected."
    exit 0
fi

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

# Check if APK exists
if [ ! -f "$APK_PATH" ]; then
    echo "❌ APK not found at $APK_PATH"
    exit 1
fi

# 3. Iterate over each device
for DEVICE in $DEVICES; do
    echo "----------------------------------------"
    echo "📲 Processing device: $DEVICE"

    # Uninstall
    echo "   🗑️ Uninstalling previous version on $DEVICE..."
    adb -s "$DEVICE" uninstall com.xuyutech.hongbaoshu > /dev/null 2>&1 || true

    # Install
    echo "   📦 Installing new version on $DEVICE..."
    adb -s "$DEVICE" install -r "$APK_PATH"

    if [ $? -eq 0 ]; then
        echo "   ✅ Install successful on $DEVICE!"
        echo "   🚀 Launching App on $DEVICE..."
        adb -s "$DEVICE" shell am start -n com.xuyutech.hongbaoshu/com.xuyutech.hongbaoshu.MainActivity
    else
        echo "   ❌ Install failed on $DEVICE."
    fi
done

echo "----------------------------------------"
echo "✨ All devices processed."
