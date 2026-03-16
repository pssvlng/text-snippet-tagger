#!/usr/bin/env sh
set -eu

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
BUNDLE_JAR_PATH="$SCRIPT_DIR/snippet-tray-manager.jar"
DEV_JAR_PATH="$SCRIPT_DIR/build/libs/snippet-tray-manager-1.0.0-all.jar"

show_error() {
  MESSAGE="$1"
  if command -v osascript >/dev/null 2>&1; then
    osascript -e "display dialog \"$MESSAGE\" buttons {\"OK\"} default button \"OK\" with title \"Snippet Tray Manager\""
  elif command -v zenity >/dev/null 2>&1; then
    zenity --error --title="Snippet Tray Manager" --text="$MESSAGE"
  elif command -v xmessage >/dev/null 2>&1; then
    xmessage -center "$MESSAGE"
  else
    echo "Error: $MESSAGE"
  fi
}

if ! command -v java >/dev/null 2>&1; then
  show_error "Java is not installed or not on your PATH.\n\nPlease install Java 17 or newer, then run this script again.\n\nDownload: https://adoptium.net/"
  exit 1
fi

JAVA_VERSION_LINE="$(java -version 2>&1 | head -n 1)"
JAVA_VERSION_RAW="$(printf '%s' "$JAVA_VERSION_LINE" | sed -n 's/.*version "\([^"]*\)".*/\1/p')"
if [ -z "$JAVA_VERSION_RAW" ]; then
  show_error "Could not determine Java version.\n\nPlease install Java 17 or newer and try again."
  exit 1
fi

JAVA_MAJOR="$(printf '%s' "$JAVA_VERSION_RAW" | awk -F. '{if ($1 == 1) print $2; else print $1}')"
case "$JAVA_MAJOR" in
  ''|*[!0-9]*)
    show_error "Could not determine Java major version from: $JAVA_VERSION_RAW\n\nPlease install Java 17 or newer and try again."
    exit 1
    ;;
esac

if [ "$JAVA_MAJOR" -lt 17 ]; then
  show_error "Detected Java version: $JAVA_VERSION_RAW\n\nJava 17 or newer is required.\nPlease upgrade Java and run this script again."
  exit 1
fi

if [ -f "$BUNDLE_JAR_PATH" ]; then
  JAR_PATH="$BUNDLE_JAR_PATH"
elif [ -f "$DEV_JAR_PATH" ]; then
  JAR_PATH="$DEV_JAR_PATH"
else
  if [ -x "$SCRIPT_DIR/gradlew" ]; then
    echo "Jar not found. Building first..."
    "$SCRIPT_DIR/gradlew" -p "$SCRIPT_DIR" shadowJar
    JAR_PATH="$DEV_JAR_PATH"
  else
    show_error "Could not find application jar.\n\nExpected either:\n- $BUNDLE_JAR_PATH\n- $DEV_JAR_PATH\n\nAlso, Gradle wrapper was not found in this folder."
    exit 1
  fi
fi

java -jar "$JAR_PATH"
