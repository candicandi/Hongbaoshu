#!/bin/bash

echo "🚀 Starting automated build and install..."

# Uninstall previous version
echo "🗑️ Uninstalling previous version..."
adb uninstall com.xuyutech.hongbaoshu

# Compile and Install
./gradlew installDebug

if [ $? -eq 0 ]; then
    echo "✅ Install successful!"
    echo "📱 Launching App..."
    adb shell am start -n com.xuyutech.hongbaoshu/com.xuyutech.hongbaoshu.MainActivity
else
    echo "❌ Install failed. Please check the logs above."
    exit 1
fi
