#!/bin/bash
set -euo pipefail

# Package the Magisk module into a flashable zip.
#
# Prerequisites:
#   - Patched vere binary at aosp/magisk-module/common/urbit
#     (build via scripts/build.sh, then patch via scripts/patch-dns.py)
#
# Usage:
#   ./aosp/scripts/build-module.sh [output-path]

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
MODULE_DIR="$SCRIPT_DIR/../magisk-module"
OUTPUT="$(cd "$SCRIPT_DIR/../.." && pwd)/urbit-vere-module.zip"
[ -n "${1:-}" ] && OUTPUT="$1"

echo "=== Building Urbit Vere Magisk Module ==="
echo ""

# Check for vere binary
if [ ! -f "$MODULE_DIR/common/urbit" ]; then
    echo "WARNING: vere binary not found at $MODULE_DIR/common/urbit"
    echo ""
    echo "The module will install without the binary."
    echo "You'll need to push the binary separately:"
    echo "  adb push <urbit-binary> /data/vere/urbit"
    echo ""
    echo "To include it in the module, copy the patched binary first:"
    echo "  cp /path/to/urbit-patched $MODULE_DIR/common/urbit"
    echo ""
    read -p "Continue without binary? [y/N] " -n 1 -r
    echo
    [[ $REPLY =~ ^[Yy]$ ]] || exit 1
fi

# Build the zip
cd "$MODULE_DIR"
rm -f "$OUTPUT"
zip -r "$OUTPUT" . \
    -x '*.DS_Store' \
    -x '*/.git/*' \
    -x '*.gitkeep' \
    -x '.gitignore'

SIZE=$(du -h "$OUTPUT" | awk '{print $1}')
echo ""
echo "=== Module built: $OUTPUT ($SIZE) ==="
echo ""
echo "Install:"
echo "  adb push $OUTPUT /sdcard/Download/"
echo "  Then: Magisk app -> Modules -> Install from storage"
