#!/bin/bash
set -euo pipefail

# Health check for urbit-mobile
# Run locally with ADB port forwarding or on-device
#
# Usage: ./scripts/health.sh

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "=== Urbit Mobile Health Check ==="
echo ""

# Check if lens is available
LENS_HOST="${LENS_HOST:-127.0.0.1}"
LENS_PORT="${LENS_PORT:-12321}"

echo "1. Lens API ($LENS_HOST:$LENS_PORT)"
if IDENTITY=$("$SCRIPT_DIR/lens.sh" "our" 2>/dev/null); then
    echo "   ✓ Ship: $IDENTITY"
else
    echo "   ✗ Lens not responding"
    echo "   Try: adb forward tcp:12321 tcp:12321"
    exit 1
fi

# Get ship time
if NOW=$("$SCRIPT_DIR/lens.sh" "now" 2>/dev/null); then
    echo "   ✓ Time: $NOW"
fi

# Check process via ADB
echo ""
echo "2. Process Status"
if command -v adb &>/dev/null; then
    PROCS=$(adb shell "ps -ef | grep 'urbit' | grep -v grep | wc -l" 2>/dev/null || echo "0")
    if [ "$PROCS" -ge 2 ]; then
        echo "   ✓ $PROCS urbit processes running"

        # Memory stats
        PID=$(adb shell "ps -ef | grep 'urbit work' | grep -v grep | head -1 | awk '{print \$2}'" 2>/dev/null)
        if [ -n "$PID" ]; then
            MEM=$(adb shell "su -c 'cat /proc/$PID/status'" 2>/dev/null | grep -E "VmRSS" | awk '{print $2}')
            if [ -n "$MEM" ]; then
                MEM_MB=$((MEM / 1024))
                echo "   ✓ Memory: ${MEM_MB}MB RSS"
            fi
        fi
    else
        echo "   ✗ urbit not running"
    fi
else
    echo "   - ADB not available, skipping process check"
fi

# Check uptime (simple arithmetic works reliably)
echo ""
echo "3. Runtime Check"
if RESULT=$("$SCRIPT_DIR/lens.sh" "(mul 6 7)" 2>/dev/null); then
    if [ "$RESULT" = "42" ]; then
        echo "   ✓ Dojo working (6*7=$RESULT)"
    else
        echo "   ! Dojo returned unexpected: $RESULT"
    fi
else
    echo "   - Dojo unavailable"
fi

# Check recent log for errors
echo ""
echo "5. Recent Log"
if command -v adb &>/dev/null; then
    ERRORS=$(adb shell "su -c 'tail -100 /data/vere/urbit.log'" 2>/dev/null | grep -iE "error|fail|crash" | wc -l)
    if [ "$ERRORS" -eq 0 ]; then
        echo "   ✓ No errors in recent logs"
    else
        echo "   ! $ERRORS error lines in recent logs"
        echo "   Run: adb shell \"su -c 'tail -20 /data/vere/urbit.log'\""
    fi
else
    echo "   - ADB not available"
fi

echo ""
echo "=== Health check complete ==="
