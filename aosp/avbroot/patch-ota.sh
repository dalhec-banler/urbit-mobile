#!/bin/bash
set -euo pipefail

# Patch a GrapheneOS OTA image with Magisk using avbroot.
# This injects root access while preserving verified boot with your own keys.
#
# Prerequisites:
#   - cargo install avbroot
#   - Keys generated via setup-keys.sh
#   - GrapheneOS OTA zip (download from https://grapheneos.org/releases)
#   - Magisk APK (download from https://github.com/topjohnwu/Magisk/releases)
#
# Usage:
#   ./aosp/avbroot/patch-ota.sh <grapheneos-ota.zip> <magisk.apk> [keys-dir]

OTA_ZIP="${1:?Usage: $0 <grapheneos-ota.zip> <magisk.apk> [keys-dir]}"
MAGISK_APK="${2:?Usage: $0 <grapheneos-ota.zip> <magisk.apk> [keys-dir]}"
KEYS_DIR="${3:-keys}"

# Validate inputs
[ -f "$OTA_ZIP" ]    || { echo "ERROR: OTA file not found: $OTA_ZIP"; exit 1; }
[ -f "$MAGISK_APK" ] || { echo "ERROR: Magisk APK not found: $MAGISK_APK"; exit 1; }
[ -f "$KEYS_DIR/avb.key" ] || { echo "ERROR: AVB key not found. Run setup-keys.sh first."; exit 1; }

OUTPUT="${OTA_ZIP%.zip}-magisk.zip"

echo "=== Patching GrapheneOS OTA with Magisk ==="
echo ""
echo "  OTA:    $OTA_ZIP"
echo "  Magisk: $MAGISK_APK"
echo "  Keys:   $KEYS_DIR/"
echo "  Output: $OUTPUT"
echo ""

avbroot ota patch \
    --input "$OTA_ZIP" \
    --output "$OUTPUT" \
    --key-avb "$KEYS_DIR/avb.key" \
    --key-ota "$KEYS_DIR/ota.key" \
    --cert-ota "$KEYS_DIR/ota.crt" \
    --magisk "$MAGISK_APK"

SIZE=$(du -h "$OUTPUT" | awk '{print $1}')
echo ""
echo "=== Patched OTA ready: $OUTPUT ($SIZE) ==="
echo ""
echo "To flash:"
echo "  1. adb reboot recovery"
echo "  2. Select 'Apply update from ADB'"
echo "  3. adb sideload $OUTPUT"
echo "  4. Reboot"
