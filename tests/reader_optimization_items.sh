set -e
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
./gradlew installDebug
