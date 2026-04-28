#!/bin/bash
set -euo pipefail

# Deploy APKs to connected device
# Builds and installs launcher, grove-mobile, or both

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$SCRIPT_DIR/.."

usage() {
    echo "Usage: $0 {launcher|grove|all} [--no-build]"
    echo ""
    echo "Apps:"
    echo "  launcher  - urbit-mobile launcher app"
    echo "  grove     - Grove Mobile file manager"
    echo "  all       - both apps"
    echo ""
    echo "Options:"
    echo "  --no-build  Skip build, just install existing APKs"
    exit 1
}

[ $# -lt 1 ] && usage

APP="$1"
BUILD=true
[ "${2:-}" = "--no-build" ] && BUILD=false

# Check device
if ! adb get-state &>/dev/null; then
    echo "Error: No device connected"
    exit 1
fi

build_and_install() {
    local name="$1"
    local dir="$2"
    local apk_path="$3"

    echo "=== $name ==="

    if [ "$BUILD" = true ]; then
        echo "Building..."
        if [ -f "$dir/gradlew" ]; then
            (cd "$dir" && ./gradlew assembleDebug 2>&1 | tail -5)
        else
            echo "Error: No gradlew in $dir"
            return 1
        fi
    fi

    if [ -f "$apk_path" ]; then
        echo "Installing..."
        adb install -r "$apk_path" 2>&1 | grep -E "Success|Failure"
    else
        echo "Error: APK not found at $apk_path"
        return 1
    fi

    echo ""
}

case "$APP" in
    launcher)
        build_and_install "Urbit Launcher" \
            "$REPO_ROOT/launcher" \
            "$REPO_ROOT/launcher/app/build/outputs/apk/debug/app-debug.apk"
        ;;
    grove)
        GROVE_DIR="$(dirname "$REPO_ROOT")/grove-mobile"
        if [ ! -d "$GROVE_DIR" ]; then
            echo "Error: Grove Mobile not found at $GROVE_DIR"
            exit 1
        fi
        build_and_install "Grove Mobile" \
            "$GROVE_DIR" \
            "$GROVE_DIR/app/build/outputs/apk/debug/app-debug.apk"
        ;;
    all)
        build_and_install "Urbit Launcher" \
            "$REPO_ROOT/launcher" \
            "$REPO_ROOT/launcher/app/build/outputs/apk/debug/app-debug.apk"

        GROVE_DIR="$(dirname "$REPO_ROOT")/grove-mobile"
        if [ -d "$GROVE_DIR" ]; then
            build_and_install "Grove Mobile" \
                "$GROVE_DIR" \
                "$GROVE_DIR/app/build/outputs/apk/debug/app-debug.apk"
        fi
        ;;
    *)
        usage
        ;;
esac

echo "Done!"
