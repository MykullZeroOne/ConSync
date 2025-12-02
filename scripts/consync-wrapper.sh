#!/bin/bash
# Simple ConSync wrapper script
# Place this in your PATH or create an alias

# Determine the directory where this script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Path to ConSync JAR (adjust if needed)
CONSYNC_JAR="$SCRIPT_DIR/../target/consync.jar"

# Check if JAR exists
if [ ! -f "$CONSYNC_JAR" ]; then
    echo "Error: ConSync JAR not found at $CONSYNC_JAR"
    echo "Please build ConSync first: mvn clean package"
    exit 1
fi

# Run ConSync with all arguments
exec java -jar "$CONSYNC_JAR" "$@"
