#!/bin/bash
set -euo pipefail

# build-aosp.sh — Build Urbit AOSP ROM for Pixel 8 Pro (husky)
#
# Prerequisites:
#   - Ubuntu 24.04 or Debian bookworm
#   - 64GB RAM recommended (32GB minimum)
#   - 300GB+ free disk space
#   - repo tool installed
#
# Usage:
#   ./build-aosp.sh sync      # First time: sync AOSP source (~100GB)
#   ./build-aosp.sh vendor    # Extract vendor blobs from device or factory image
#   ./build-aosp.sh build     # Build the ROM
#   ./build-aosp.sh flash     # Flash to connected device
#   ./build-aosp.sh all       # Do everything

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
AOSP_DIR="${AOSP_ROOT:-$HOME/aosp}"
DEVICE="husky"
PRODUCT="vere_husky"
VARIANT="${BUILD_VARIANT:-userdebug}"
ANDROID_VERSION="android-15.0.0_r29"  # Android 15 with husky support
OVERLAY_DIR="$SCRIPT_DIR/../device-overlay"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

info() { echo -e "${GREEN}[INFO]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

check_deps() {
    info "Checking dependencies..."

    # Check for repo
    if ! command -v repo &>/dev/null; then
        error "repo not found. Install: https://source.android.com/setup/build/downloading"
    fi

    # Check disk space
    AVAIL=$(df -BG "$AOSP_DIR" 2>/dev/null | tail -1 | awk '{print $4}' | tr -d 'G' || echo "0")
    if [ "$AVAIL" -lt 200 ]; then
        warn "Only ${AVAIL}GB available. AOSP needs ~300GB."
    fi

    # Check RAM
    RAM=$(free -g | awk '/^Mem:/{print $2}')
    if [ "$RAM" -lt 32 ]; then
        warn "Only ${RAM}GB RAM. Build may be slow or fail. 64GB recommended."
    fi

    # Check for vere binary
    if [ ! -f "$OVERLAY_DIR/prebuilt/arm64/urbit" ]; then
        warn "Vere binary not found at $OVERLAY_DIR/prebuilt/arm64/urbit"
        warn "Download from CI or build locally. See prebuilt/README.md"
    fi
}

cmd_sync() {
    info "Syncing AOSP source to $AOSP_DIR..."
    mkdir -p "$AOSP_DIR"
    cd "$AOSP_DIR"

    if [ ! -d ".repo" ]; then
        info "Initializing repo with $ANDROID_VERSION..."
        repo init -u https://android.googlesource.com/platform/manifest \
            -b "$ANDROID_VERSION" \
            --depth=1
    fi

    info "Syncing (this will take a while)..."
    repo sync -c -j$(nproc) --no-tags --optimized-fetch

    info "AOSP source synced to $AOSP_DIR"
}

cmd_vendor() {
    info "Extracting vendor blobs for $DEVICE..."
    cd "$AOSP_DIR"

    # Check if android-prepare-vendor exists
    if [ ! -d "android-prepare-vendor" ]; then
        info "Cloning android-prepare-vendor..."
        git clone https://github.com/anestisb/android-prepare-vendor
    fi

    # Get build ID from device or use latest
    if adb devices | grep -q "device$"; then
        BUILD_ID=$(adb shell getprop ro.build.id | tr -d '\r')
        info "Device build ID: $BUILD_ID"
    else
        warn "No device connected. Using latest factory image."
        BUILD_ID="latest"
    fi

    info "Extracting vendor blobs (BUILD_ID=$BUILD_ID)..."
    ./android-prepare-vendor/execute-all.sh -d "$DEVICE" -b "$BUILD_ID" -o vendor-out

    # Copy to AOSP tree
    cp -r vendor-out/$DEVICE/*/vendor* .

    info "Vendor blobs extracted"
}

cmd_overlay() {
    info "Installing vere device overlay..."
    cd "$AOSP_DIR"

    # Create device/urbit/vere directory
    mkdir -p device/urbit/vere

    # Copy overlay files
    cp -r "$OVERLAY_DIR"/* device/urbit/vere/

    # Verify vere binary exists
    if [ ! -f "device/urbit/vere/prebuilt/arm64/urbit" ]; then
        error "Vere binary missing! Place it at device/urbit/vere/prebuilt/arm64/urbit"
    fi

    info "Device overlay installed at device/urbit/vere/"
}

cmd_build() {
    info "Building $PRODUCT-$VARIANT..."
    cd "$AOSP_DIR"

    # Install overlay first
    cmd_overlay

    # Set up build environment
    source build/envsetup.sh

    # Select target
    lunch "$PRODUCT-$VARIANT"

    # Build
    info "Starting build with $(nproc) cores..."
    m -j$(nproc)

    info "Build complete!"
    info "Output: $AOSP_DIR/out/target/product/$DEVICE/"
}

cmd_flash() {
    info "Flashing to device..."
    cd "$AOSP_DIR"

    if ! adb devices | grep -q "device$"; then
        error "No device connected via ADB"
    fi

    # Reboot to bootloader
    adb reboot bootloader
    sleep 5

    # Flash
    export ANDROID_PRODUCT_OUT="$AOSP_DIR/out/target/product/$DEVICE"
    fastboot flashall -w

    info "Flash complete!"
}

cmd_all() {
    check_deps
    cmd_sync
    cmd_vendor
    cmd_build
    cmd_flash
}

# Main
case "${1:-help}" in
    sync)   check_deps; cmd_sync ;;
    vendor) check_deps; cmd_vendor ;;
    overlay) check_deps; cmd_overlay ;;
    build)  check_deps; cmd_build ;;
    flash)  check_deps; cmd_flash ;;
    all)    cmd_all ;;
    *)
        echo "Usage: $0 {sync|vendor|overlay|build|flash|all}"
        echo ""
        echo "Commands:"
        echo "  sync    - Download AOSP source (~100GB, ~2 hours)"
        echo "  vendor  - Extract vendor blobs from device/factory image"
        echo "  overlay - Install vere device overlay into AOSP tree"
        echo "  build   - Build the ROM (~2-4 hours)"
        echo "  flash   - Flash to connected device"
        echo "  all     - Do everything"
        echo ""
        echo "Environment:"
        echo "  AOSP_ROOT      - AOSP source directory (default: ~/aosp)"
        echo "  BUILD_VARIANT  - userdebug, user, or eng (default: userdebug)"
        ;;
esac
