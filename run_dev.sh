#!/bin/bash

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
DEVICES=$(adb devices | grep -v "List of devices attached" | grep -v "^$" | awk '{print $1}')

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
    adb -s "$DEVICE" uninstall com.xuyutech.hongbaoshu > /dev/null 2>&1

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
