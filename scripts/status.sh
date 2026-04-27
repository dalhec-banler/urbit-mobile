#!/bin/bash
set -euo pipefail

# Full status report for urbit-mobile setup
# Shows ship, launcher, and system status

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "========================================"
echo "  Urbit Mobile Status Report"
echo "  $(date)"
echo "========================================"
echo ""

# Check ADB
echo "1. Device Connection"
if adb get-state &>/dev/null; then
    DEVICE=$(adb shell getprop ro.product.model | tr -d '\r')
    OS=$(adb shell getprop ro.build.display.id | tr -d '\r')
    echo "   ✓ Connected: $DEVICE"
    echo "   ✓ OS: $OS"
else
    echo "   ✗ No device connected"
    exit 1
fi
echo ""

# Check Magisk
echo "2. Magisk Module"
if adb shell "su -c 'test -d /data/adb/modules/urbit-vere'" 2>/dev/null; then
    VERSION=$(adb shell "su -c 'cat /data/adb/modules/urbit-vere/module.prop'" 2>/dev/null | grep version= | cut -d= -f2 | tr -d '\r')
    echo "   ✓ Installed: $VERSION"
else
    echo "   ✗ Module not installed"
fi
echo ""

# Check vere binary
echo "3. Vere Binary"
if adb shell "su -c 'test -f /data/vere/urbit'" 2>/dev/null; then
    SIZE=$(adb shell "su -c 'ls -la /data/vere/urbit'" 2>/dev/null | awk '{print $5}')
    SIZE_MB=$((SIZE / 1024 / 1024))
    echo "   ✓ Binary: ${SIZE_MB}MB"
else
    echo "   ✗ Binary not found at /data/vere/urbit"
fi
echo ""

# Check pier
echo "4. Pier"
if adb shell "su -c 'test -d /data/vere/pier'" 2>/dev/null; then
    PIER_SIZE=$(adb shell "su -c 'du -sh /data/vere/pier'" 2>/dev/null | awk '{print $1}')
    echo "   ✓ Pier exists: $PIER_SIZE"
else
    echo "   ✗ Pier not found"
fi
echo ""

# Check vere process
echo "5. Vere Process"
VERE_PROCS=$(adb shell "ps -ef | grep urbit | grep -v grep | grep -v launcher | wc -l" | tr -d '\r')
if [ "$VERE_PROCS" -ge 2 ]; then
    PID=$(adb shell "ps -ef | grep 'urbit work' | grep -v grep | head -1 | awk '{print \$2}'" | tr -d '\r')
    if [ -n "$PID" ]; then
        RSS=$(adb shell "su -c 'cat /proc/$PID/status'" 2>/dev/null | grep VmRSS | awk '{print $2}')
        RSS_MB=$((RSS / 1024))
        VMSIZE=$(adb shell "su -c 'cat /proc/$PID/status'" 2>/dev/null | grep VmSize | awk '{print $2}')
        VMSIZE_GB=$((VMSIZE / 1024 / 1024))
        echo "   ✓ Running (PID $PID)"
        echo "   ✓ Memory: ${RSS_MB}MB RSS, ${VMSIZE_GB}GB virtual"
    fi
else
    echo "   ✗ Not running"
fi
echo ""

# Check launcher
echo "6. Launcher App"
LAUNCHER_PID=$(adb shell "ps -ef | grep io.nativeplanet.urbit.launcher | grep -v grep | head -1 | awk '{print \$2}'" | tr -d '\r')
if [ -n "$LAUNCHER_PID" ]; then
    echo "   ✓ Running (PID $LAUNCHER_PID)"
else
    echo "   - Not running (or not installed)"
fi
echo ""

# Check lens
echo "7. Lens API"
adb forward tcp:12321 tcp:12321 2>/dev/null || true
SHIP=$("$SCRIPT_DIR/lens.sh" "our" 2>/dev/null || echo "")
if [ -n "$SHIP" ]; then
    echo "   ✓ Responding: $SHIP"
else
    echo "   ✗ Not responding"
fi
echo ""

# Recent monitor log
echo "8. Monitor Status (last 3 entries)"
adb shell "su -c 'tail -3 /data/vere/monitor.log'" 2>/dev/null | sed 's/^/   /'
echo ""

# Battery
echo "9. Device Status"
BATT=$(adb shell "dumpsys battery" 2>/dev/null | grep "level:" | awk '{print $2}')
TEMP=$(adb shell "dumpsys battery" 2>/dev/null | grep "temperature:" | awk '{print $2}')
TEMP_C=$((TEMP / 10))
echo "   Battery: ${BATT}%"
echo "   Temperature: ${TEMP_C}°C"
echo ""

echo "========================================"
