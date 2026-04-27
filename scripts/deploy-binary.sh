#!/bin/bash
set -euo pipefail

# Deploy a new vere binary to a running urbit-mobile installation
#
# Usage:
#   ./scripts/deploy-binary.sh /path/to/urbit  # custom binary
#   ./scripts/deploy-binary.sh                  # use latest CI artifact
#
# Requires: adb with root access via su

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REMOTE_BINARY=/data/vere/urbit
REMOTE_TMP=/data/local/tmp/urbit-new

usage() {
    echo "Usage: $0 [binary-path]"
    echo ""
    echo "Deploy a new vere binary to the phone."
    echo "If no path given, downloads latest from GitHub Actions."
    exit 1
}

log() {
    echo "[$(date +%H:%M:%S)] $*"
}

check_adb() {
    if ! adb get-state &>/dev/null; then
        echo "Error: No device connected. Connect via USB or wireless ADB."
        exit 1
    fi
}

get_latest_artifact() {
    log "Fetching latest artifact from GitHub Actions..."

    # Get latest successful build run
    RUN_ID=$(curl -s "https://api.github.com/repos/dalhec-banler/urbit-mobile/actions/workflows/build.yml/runs?status=success&per_page=1" \
        | jq -r '.workflow_runs[0].id')

    if [ -z "$RUN_ID" ] || [ "$RUN_ID" = "null" ]; then
        echo "Error: No successful builds found"
        exit 1
    fi

    # Get artifact download URL
    ARTIFACT_URL=$(curl -s "https://api.github.com/repos/dalhec-banler/urbit-mobile/actions/runs/$RUN_ID/artifacts" \
        | jq -r '.artifacts[] | select(.name == "urbit-aarch64-linux-musl") | .archive_download_url')

    if [ -z "$ARTIFACT_URL" ] || [ "$ARTIFACT_URL" = "null" ]; then
        echo "Error: Artifact not found in run $RUN_ID"
        exit 1
    fi

    log "Downloading artifact from run $RUN_ID..."

    # Note: GitHub API requires authentication for artifact downloads
    # For now, point to releases or require local binary
    echo "Error: GitHub artifact download requires authentication."
    echo "Download manually from: https://github.com/dalhec-banler/urbit-mobile/actions/runs/$RUN_ID"
    echo "Or build locally: cd vere && zig build -Dtarget=aarch64-linux-musl -Doptimize=ReleaseFast"
    exit 1
}

stop_vere() {
    log "Stopping vere..."

    local pid
    pid=$(adb shell "su -c 'cat /data/vere/vere.pid 2>/dev/null'" | tr -d '\r')

    if [ -n "$pid" ] && adb shell "su -c 'kill -0 $pid 2>/dev/null'"; then
        adb shell "su -c 'kill -TERM $pid'"

        # Wait for graceful shutdown (up to 30s for checkpoint)
        for i in {1..15}; do
            sleep 2
            if ! adb shell "su -c 'kill -0 $pid 2>/dev/null'"; then
                log "Vere stopped"
                return 0
            fi
        done

        log "Force killing after 30s..."
        adb shell "su -c 'kill -KILL $pid 2>/dev/null'" || true
    else
        log "Vere not running"
    fi
}

deploy_binary() {
    local binary="$1"

    log "Pushing binary to phone..."
    adb push "$binary" "$REMOTE_TMP"

    log "Replacing binary..."
    adb shell "su -c 'mv $REMOTE_BINARY ${REMOTE_BINARY}.bak 2>/dev/null || true'"
    adb shell "su -c 'cp $REMOTE_TMP $REMOTE_BINARY && chmod +x $REMOTE_BINARY'"
    adb shell "su -c 'rm $REMOTE_TMP'"

    # Verify
    local size
    size=$(adb shell "su -c 'ls -l $REMOTE_BINARY'" | awk '{print $5}')
    log "Deployed: $(echo "$size/1024/1024" | bc)MB"
}

start_vere() {
    log "Starting vere..."
    adb shell "su -c 'cd /data/vere && nohup ./urbit --no-dock --no-conn -t -p 34543 /data/vere/pier >> /data/vere/urbit.log 2>&1 &'"

    # Wait for HTTP
    log "Waiting for ship to come up..."
    for i in {1..30}; do
        sleep 2
        if adb shell "su -c 'grep -q \"pier.*live\" /data/vere/urbit.log 2>/dev/null'"; then
            log "Ship is live!"
            return 0
        fi
    done

    log "Warning: Ship didn't report live within 60s (may still be booting)"
}

verify() {
    log "Verifying..."

    # Port forward if not already
    adb forward tcp:12321 tcp:12321 2>/dev/null || true

    sleep 2

    # Test lens
    local result
    result=$("$SCRIPT_DIR/lens.sh" "(add 1 1)" 2>/dev/null || echo "FAIL")

    if [ "$result" = "2" ]; then
        log "Lens API working"
    else
        log "Warning: Lens returned '$result'"
    fi

    # Show memory
    local pid
    pid=$(adb shell "ps -ef | grep 'urbit work' | grep -v grep | head -1 | awk '{print \$2}'" | tr -d '\r')
    if [ -n "$pid" ]; then
        local rss
        rss=$(adb shell "su -c 'cat /proc/$pid/status'" 2>/dev/null | grep VmRSS | awk '{print $2}')
        log "Memory: $((rss/1024))MB RSS"
    fi
}

# Main
check_adb

if [ $# -gt 0 ]; then
    BINARY="$1"
    if [ ! -f "$BINARY" ]; then
        echo "Error: File not found: $BINARY"
        exit 1
    fi
else
    get_latest_artifact
fi

stop_vere
deploy_binary "$BINARY"
start_vere
verify

log "Deploy complete!"
