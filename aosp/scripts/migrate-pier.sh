#!/bin/bash
set -euo pipefail

# Migrate pier from Phase 1 location to Phase 2 location ON the phone.
# This copies (not moves) so the original is preserved until verified.
#
# Phase 1: /data/local/tmp/urbit-pier  (adb shell user)
# Phase 2: /data/vere/pier             (Magisk root)
#
# Usage:
#   ./aosp/scripts/migrate-pier.sh

OLD_PIER="/data/local/tmp/urbit-pier"
NEW_PIER="/data/vere/pier"

echo "=== Migrating pier to Phase 2 location ==="
echo "  From: $OLD_PIER"
echo "  To:   $NEW_PIER"
echo ""

# Check ADB
adb devices | grep -q "device$" || { echo "ERROR: No device connected"; exit 1; }

# Check old pier exists
adb shell "[ -d $OLD_PIER ]" || { echo "ERROR: No pier at $OLD_PIER"; exit 1; }

# Check vere is stopped
if adb shell "ps -eo comm 2>/dev/null" | grep -q urbit; then
    echo "ERROR: vere is still running. Stop it first:"
    echo "  adb shell /data/local/tmp/stop-ship.sh"
    exit 1
fi

# Create new directory and copy
adb shell "mkdir -p /data/vere"

echo "Copying pier (this may take a few minutes)..."
adb shell "cp -r $OLD_PIER $NEW_PIER"

# Copy binary too
if adb shell "[ -f /data/local/tmp/urbit ]" 2>/dev/null; then
    echo "Copying vere binary..."
    adb shell "cp /data/local/tmp/urbit /data/vere/urbit && chmod +x /data/vere/urbit"
fi

echo ""
echo "=== Migration complete ==="
echo "  Pier at $NEW_PIER"
echo ""
echo "The old pier at $OLD_PIER is still intact."
echo "Test the new location, then delete the old one:"
echo "  adb shell rm -rf $OLD_PIER"
