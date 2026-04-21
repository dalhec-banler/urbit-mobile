#!/bin/bash
set -euo pipefail

# Restore pier to phone after re-flashing with Magisk.
# Phase 2 pier location: /data/vere/pier (moved from /data/local/tmp/urbit-pier)
#
# Usage:
#   ./aosp/scripts/restore-pier.sh <backup-dir> [pier-name]

BACKUP_DIR="${1:?Usage: $0 <backup-dir> [pier-name]}"
PIER_NAME="${2:-urbit-pier}"
DEST="/data/vere"

echo "=== Restoring pier to phone ==="
echo "  Source: $BACKUP_DIR/$PIER_NAME"
echo "  Destination: $DEST/pier"
echo ""

# Validate
adb devices | grep -q "device$" || { echo "ERROR: No device connected via ADB"; exit 1; }
[ -d "$BACKUP_DIR/$PIER_NAME" ] || { echo "ERROR: Backup not found at $BACKUP_DIR/$PIER_NAME"; exit 1; }

# Create destination
adb shell "mkdir -p $DEST"

# Push pier
echo "Pushing pier (this may take several minutes)..."
adb push "$BACKUP_DIR/$PIER_NAME" "$DEST/pier"

# Push binary if present in backup
if [ -f "$BACKUP_DIR/urbit" ]; then
    echo "Pushing vere binary..."
    adb push "$BACKUP_DIR/urbit" "$DEST/urbit"
    adb shell "chmod +x $DEST/urbit"
fi

echo ""
echo "=== Restore complete ==="
echo "  Pier:   $DEST/pier"
echo "  Binary: $DEST/urbit"
echo ""
echo "Reboot the phone to start vere automatically via Magisk module."
