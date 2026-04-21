#!/bin/bash
set -euo pipefail

# Patch a GrapheneOS OTA image with Magisk using avbroot.
# This injects root access while preserving verified boot with your own keys.
#
# Prerequisites:
#   - avbroot binary (download from https://github.com/chenxiaolong/avbroot/releases)
#   - Keys generated via setup-keys.sh
#   - GrapheneOS OTA zip (download from https://grapheneos.org/releases)
#   - Magisk APK (download from https://github.com/topjohnwu/Magisk/releases)
#
# Usage:
#   ./aosp/avbroot/patch-ota.sh <grapheneos-ota.zip> <magisk.apk> [keys-dir]
#
# After patching, extract images for fastboot:
#   avbroot ota extract --input <output-magisk.zip> --directory extracted --fastboot

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

# --magisk-preinit-device super is required for Pixel 8 Pro (husky)
# with Magisk v25211+. The "super" partition holds dynamic partitions.
avbroot ota patch \
    --input "$OTA_ZIP" \
    --output "$OUTPUT" \
    --key-avb "$KEYS_DIR/avb.key" \
    --key-ota "$KEYS_DIR/ota.key" \
    --cert-ota "$KEYS_DIR/ota.crt" \
    --magisk "$MAGISK_APK" \
    --magisk-preinit-device super

SIZE=$(du -h "$OUTPUT" | awk '{print $1}')
echo ""
echo "=== Patched OTA ready: $OUTPUT ($SIZE) ==="
echo ""
echo "Next: extract images for fastboot flashing:"
echo ""
echo "  mkdir -p extracted"
echo "  avbroot ota extract \\"
echo "      --input $OUTPUT \\"
echo "      --directory extracted \\"
echo "      --fastboot"
echo ""
echo "Then flash:"
echo "  adb reboot bootloader"
echo "  ANDROID_PRODUCT_OUT=extracted fastboot flashall --skip-reboot"
echo "  fastboot erase avb_custom_key"
echo "  fastboot flash avb_custom_key $KEYS_DIR/avb_pkmd.bin"
echo "  fastboot reboot"
