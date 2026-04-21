#!/bin/bash
set -euo pipefail

# Generate signing keys for avbroot OTA patching.
# Run this ONCE, then keep the keys safe — you need them for every OTA update.
#
# Prerequisites:
#   cargo install avbroot
#
# Usage:
#   ./aosp/avbroot/setup-keys.sh [keys-directory]

KEYS_DIR="${1:-keys}"

echo "=== Generating avbroot signing keys ==="
echo "Output: $KEYS_DIR/"
echo ""

mkdir -p "$KEYS_DIR"

# AVB key (Android Verified Boot — proves the OS image is yours)
echo "[1/3] Generating AVB key..."
avbroot key generate-key -o "$KEYS_DIR/avb.key"
avbroot key encode-avb -k "$KEYS_DIR/avb.key" -o "$KEYS_DIR/avb_pkmd.bin"

# OTA signing key (signs the update package)
echo "[2/3] Generating OTA signing key..."
avbroot key generate-key -o "$KEYS_DIR/ota.key"
avbroot key generate-cert -k "$KEYS_DIR/ota.key" -o "$KEYS_DIR/ota.crt"

echo "[3/3] Done."
echo ""
echo "=== Keys generated ==="
echo ""
echo "  $KEYS_DIR/avb.key       — AVB private key (KEEP SECRET)"
echo "  $KEYS_DIR/avb_pkmd.bin  — AVB public key (flash to device)"
echo "  $KEYS_DIR/ota.key       — OTA signing private key"
echo "  $KEYS_DIR/ota.crt       — OTA signing certificate"
echo ""
echo "BACK THESE UP. If you lose them, you cannot update your device."
echo ""
echo "Next: flash the AVB public key to your device:"
echo "  adb reboot bootloader"
echo "  fastboot flash avb_custom_key $KEYS_DIR/avb_pkmd.bin"
