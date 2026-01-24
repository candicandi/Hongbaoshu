#!/bin/bash

# Exit immediately if a command exits with a non-zero status
set -e

# Function to handle errors
die() {
  echo "$1" 1>&2
  exit 1
}

# Ensure Java environment is consistent with run_dev.sh
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

  # Fallback to system java_home if available
  if ! command -v java >/dev/null 2>&1 && [ -x /usr/libexec/java_home ]; then
    local detected_java_home
    detected_java_home=$(/usr/libexec/java_home 2>/dev/null || true)
    if [ -n "${detected_java_home:-}" ] && [ -x "$detected_java_home/bin/java" ]; then
      export JAVA_HOME="$detected_java_home"
      export PATH="$JAVA_HOME/bin:$PATH"
    fi
  fi

  if ! command -v java >/dev/null 2>&1 || ! java -version >/dev/null 2>&1; then
    die "❌ Unable to locate a Java Runtime. Please install JDK 11+ or Android Studio."
  fi
}

VERSION=""

# Parse arguments
while getopts "v:" opt; do
  case $opt in
    v) VERSION="$OPTARG" ;;
    *) echo "Usage: $0 -v <version>" >&2; exit 1 ;;
  esac
done

if [ -z "$VERSION" ]; then
    die "❌ Error: Version argument -v is required. Usage: ./build_release.sh -v 1.1.0"
fi

if [ ! -f "app/build.gradle.kts" ]; then
    die "❌ Error: app/build.gradle.kts not found. Are you in the project root?"
fi

ensure_java

echo "🚀 Preparing to build release version: $VERSION"

# Update versionName in build.gradle.kts
# Note: Using sed -i '' for macOS compatibility
sed -i '' "s/versionName = \".*\"/versionName = \"$VERSION\"/" app/build.gradle.kts

# Optional: Auto-increment versionCode to avoid upgrade issues
# Extracts the current versionCode, increments it, and replaces it.
CURRENT_CODE=$(grep 'versionCode =' app/build.gradle.kts | awk '{print $3}')
if [ -n "$CURRENT_CODE" ]; then
    NEW_CODE=$((CURRENT_CODE + 1))
    sed -i '' "s/versionCode = $CURRENT_CODE/versionCode = $NEW_CODE/" app/build.gradle.kts
    echo "ℹ️  Auto-incremented versionCode from $CURRENT_CODE to $NEW_CODE"
fi

echo "✅ Updated configuration to Version Name: $VERSION"

echo "🔨 Building Release APK..."
./gradlew assembleRelease

APK_DIR="app/build/outputs/apk/release"
SRC_APK="$APK_DIR/app-release.apk"
DEST_APK="$APK_DIR/hongbaoshu_v${VERSION}.apk"

if [ -f "$SRC_APK" ]; then
    mv "$SRC_APK" "$DEST_APK"
    echo "----------------------------------------"
    echo "🎉 Build Success!"
    echo "📂 Output: $DEST_APK"
    echo "----------------------------------------"
else
    die "❌ Build failed or APK not found at $SRC_APK"
fi
