#!/bin/bash
# Downloads the Gradle 8.4 wrapper jar required to build the project.
# Run this once before using ./gradlew

set -e

WRAPPER_JAR="gradle/wrapper/gradle-wrapper.jar"
WRAPPER_URL="https://github.com/gradle/gradle/raw/v8.4.0/gradle/wrapper/gradle-wrapper.jar"
FALLBACK_URL="https://raw.githubusercontent.com/gradle/gradle/v8.4.0/gradle/wrapper/gradle-wrapper.jar"

echo "Downloading Gradle wrapper jar..."

if command -v curl &> /dev/null; then
    curl -L -o "$WRAPPER_JAR" "$WRAPPER_URL" || curl -L -o "$WRAPPER_JAR" "$FALLBACK_URL"
elif command -v wget &> /dev/null; then
    wget -O "$WRAPPER_JAR" "$WRAPPER_URL" || wget -O "$WRAPPER_JAR" "$FALLBACK_URL"
else
    echo "ERROR: Neither curl nor wget found. Please install one and retry."
    echo "Alternatively, open the project in Android Studio which will handle this automatically."
    exit 1
fi

if [ -f "$WRAPPER_JAR" ] && [ -s "$WRAPPER_JAR" ]; then
    echo "Gradle wrapper jar downloaded successfully."
    echo "You can now run: ./gradlew assembleDebug"
else
    echo "ERROR: Download failed. Try opening the project in Android Studio instead."
    exit 1
fi
