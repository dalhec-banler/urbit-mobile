#!/bin/bash
set -euo pipefail

# Deploy vere to Android phone via ADB
#
# Usage:
#   ./scripts/deploy.sh <urbit-binary> <keyfile> [pill-file]

BINARY="${1:?Usage: $0 <urbit-binary> <keyfile> [pill-file]}"
KEYFILE="${2:?Usage: $0 <urbit-binary> <keyfile> [pill-file]}"
PILL="${3:-}"
DEST="/data/local/tmp"

echo "=== Checking ADB connection ==="
adb devices | grep -q "device$" || { echo "ERROR: No device connected"; exit 1; }

echo "=== Writing DNS config ==="
adb shell "echo 'nameserver 192.168.2.254
nameserver 8.8.8.8' > /tmp/resolv.conf"

echo "=== Pushing binary ==="
adb push "$BINARY" "$DEST/urbit"
adb shell "chmod +x $DEST/urbit"

echo "=== Pushing keyfile ==="
KEYNAME=$(basename "$KEYFILE")
adb push "$KEYFILE" "$DEST/$KEYNAME"

if [ -n "$PILL" ]; then
    echo "=== Pushing pill ==="
    adb push "$PILL" "$DEST/$(basename "$PILL")"
fi

echo ""
echo "=== Deploy complete ==="
echo ""
echo "To boot a new moon:"
echo "  adb shell"
echo "  cd $DEST"
echo "  ./urbit --no-dock --no-conn -t -p 34543 -B $DEST/urbit-v4.3.pill -w \$(basename $KEYNAME .key) -k $DEST/$KEYNAME $DEST/urbit-pier"
echo ""
echo "To resume an existing pier:"
echo "  adb shell '$DEST/urbit --no-dock --no-conn -t -p 34543 $DEST/urbit-pier'"
