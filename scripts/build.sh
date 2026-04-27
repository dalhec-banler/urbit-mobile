#!/bin/bash
set -euo pipefail

# Cross-compile vere for aarch64-linux-musl
#
# On Linux x86_64: uses Zig directly (cross-compilation)
# On macOS: uses Docker via Colima
#
# Prerequisites:
#   - Zig 0.15.2 in PATH, or Docker + Colima (macOS)
#   - Vere source cloned: git clone -b msl/64 https://github.com/urbit/vere.git
#
# Usage:
#   ./scripts/build.sh /path/to/vere/source

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
VERE_SRC="${1:?Usage: $0 /path/to/vere/source}"

echo "=== Building vere for aarch64-linux-musl ==="
echo "Source: $VERE_SRC"

# Apply patches
if [ -d "$SCRIPT_DIR/../patches" ]; then
    echo "=== Applying patches ==="
    for patch in "$SCRIPT_DIR/../patches"/*.patch; do
        if [ -f "$patch" ]; then
            echo "Applying: $(basename "$patch")"
            (cd "$VERE_SRC" && git apply "$patch" 2>/dev/null || git apply --check "$patch" 2>/dev/null || echo "Patch already applied or conflicts")
        fi
    done
fi

# Build
if command -v zig &>/dev/null && [ "$(uname -m)" = "x86_64" ]; then
    echo "=== Using native Zig cross-compilation ==="
    (cd "$VERE_SRC" && zig build -Dtarget=aarch64-linux-musl -Doptimize=ReleaseFast)
else
    echo "=== Using Docker build ==="
    docker run --rm -v "$VERE_SRC:/src" vere-builder \
        zig build -Dtarget=aarch64-linux-musl -Doptimize=ReleaseFast
fi

OUTPUT="$VERE_SRC/zig-out/aarch64-linux-musl/urbit"

if [ -f "$OUTPUT" ]; then
    SIZE=$(du -h "$OUTPUT" | awk '{print $1}')
    echo "=== Build complete: $OUTPUT ($SIZE) ==="
    file "$OUTPUT"
else
    echo "ERROR: Build output not found at $OUTPUT"
    exit 1
fi
