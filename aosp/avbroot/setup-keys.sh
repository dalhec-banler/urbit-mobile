#!/bin/bash
set -euo pipefail

# Generate signing keys for avbroot OTA patching.
# Run this ONCE, then keep the keys safe — you need them for every OTA update.
#
# Prerequisites:
#   avbroot binary (download from https://github.com/chenxiaolong/avbroot/releases)
#
# Usage:
#   ./aosp/avbroot/setup-keys.sh [keys-directory]
#
# Non-interactive (scripted) usage:
#   echo "your-passphrase" > /tmp/pass.txt
#   PASS_FILE=/tmp/pass.txt ./aosp/avbroot/setup-keys.sh [keys-directory]

KEYS_DIR="${1:-keys}"
PASS_ARGS=""

if [ -n "${PASS_FILE:-}" ]; then
    [ -f "$PASS_FILE" ] || { echo "ERROR: Pass file not found: $PASS_FILE"; exit 1; }
    PASS_ARGS="--pass-file $PASS_FILE"
    echo "Using passphrase from $PASS_FILE"
fi

echo "=== Generating avbroot signing keys ==="
echo "Output: $KEYS_DIR/"
echo ""

mkdir -p "$KEYS_DIR"

# AVB key (Android Verified Boot — proves the OS image is yours)
echo "[1/4] Generating AVB key..."
avbroot key generate-key -o "$KEYS_DIR/avb.key" $PASS_ARGS

echo "[2/4] Encoding AVB public key..."
avbroot key encode-avb -k "$KEYS_DIR/avb.key" -o "$KEYS_DIR/avb_pkmd.bin" $PASS_ARGS

# OTA signing key (signs the update package)
echo "[3/4] Generating OTA signing key..."
avbroot key generate-key -o "$KEYS_DIR/ota.key" $PASS_ARGS

echo "[4/4] Generating OTA signing certificate..."
avbroot key generate-cert -k "$KEYS_DIR/ota.key" -o "$KEYS_DIR/ota.crt" $PASS_ARGS

echo ""
echo "=== Keys generated ==="
echo ""
echo "  $KEYS_DIR/avb.key       — AVB private key (KEEP SECRET)"
echo "  $KEYS_DIR/avb_pkmd.bin  — AVB public key (flash to device)"
echo "  $KEYS_DIR/ota.key       — OTA signing private key"
echo "  $KEYS_DIR/ota.crt       — OTA signing certificate"
echo ""
echo "BACK THESE UP. If you lose them, you cannot update your device"
echo "without wiping it and starting over."
