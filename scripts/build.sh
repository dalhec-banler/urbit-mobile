#!/bin/bash
set -euo pipefail

# Cross-compile vere for aarch64-linux-musl using Docker
#
# Prerequisites:
#   - Colima running: colima start --arch aarch64 --cpu 4 --memory 8
#   - Docker image built: docker build -t vere-builder .
#   - Vere source cloned: git clone -b msl/64 https://github.com/urbit/vere.git
#
# Usage:
#   ./scripts/build.sh /path/to/vere/source

VERE_SRC="${1:?Usage: $0 /path/to/vere/source}"

echo "=== Building vere for aarch64-linux-musl ==="
echo "Source: $VERE_SRC"

docker run --rm -v "$VERE_SRC:/src" vere-builder \
    zig build -Dtarget=aarch64-linux-musl -Doptimize=ReleaseFast

OUTPUT="$VERE_SRC/zig-out/aarch64-linux-musl/urbit"

if [ -f "$OUTPUT" ]; then
    SIZE=$(du -h "$OUTPUT" | awk '{print $1}')
    echo "=== Build complete: $OUTPUT ($SIZE) ==="
    file "$OUTPUT"
else
    echo "ERROR: Build output not found at $OUTPUT"
    exit 1
fi
