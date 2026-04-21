#!/bin/bash
set -euo pipefail

# Backup the existing pier from the phone.
# IMPORTANT: Run this BEFORE unlocking the bootloader (which wipes the device).
#
# Usage:
#   ./aosp/scripts/backup-pier.sh [backup-dir] [pier-path-on-phone]

BACKUP_DIR="${1:-pier-backup}"
PIER_PATH="${2:-/data/local/tmp/urbit-pier}"

echo "=== Backing up pier from phone ==="
echo "  Source:      $PIER_PATH"
echo "  Destination: $BACKUP_DIR/"
echo ""

# Check ADB connection
adb devices | grep -q "device$" || { echo "ERROR: No device connected via ADB"; exit 1; }

# Check pier exists on phone
adb shell "[ -d $PIER_PATH ]" || { echo "ERROR: Pier not found at $PIER_PATH"; exit 1; }

# Get pier size
SIZE=$(adb shell "du -sh $PIER_PATH 2>/dev/null" | awk '{print $1}')
echo "  Pier size: $SIZE"
echo ""
echo "This may take several minutes for a large pier..."
echo ""

# Pull the pier
mkdir -p "$BACKUP_DIR"
adb pull "$PIER_PATH" "$BACKUP_DIR/"

# Also backup the binary if present
if adb shell "[ -f /data/local/tmp/urbit ]" 2>/dev/null; then
    echo ""
    echo "Also backing up vere binary..."
    adb pull /data/local/tmp/urbit "$BACKUP_DIR/urbit"
fi

echo ""
echo "=== Backup complete ==="
echo "  Pier: $BACKUP_DIR/$(basename $PIER_PATH)/"
echo ""
echo "VERIFY before proceeding:"
echo "  ls -la $BACKUP_DIR/$(basename $PIER_PATH)/.urb/chk/"
echo ""
echo "Keep this backup safe. You'll need it after re-flashing."
