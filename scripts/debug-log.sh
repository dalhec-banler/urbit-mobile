#!/bin/bash
set -euo pipefail

# Pull and analyze vere logs from device
# Useful for debugging crashes and issues

OUTPUT_DIR="${1:-.}"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
LOG_DIR="$OUTPUT_DIR/logs-$TIMESTAMP"

mkdir -p "$LOG_DIR"

echo "=== Pulling Debug Logs ==="
echo "Output: $LOG_DIR"
echo ""

# Pull vere logs
echo "1. Vere logs..."
adb shell "su -c 'cat /data/vere/urbit.log'" > "$LOG_DIR/urbit.log" 2>/dev/null || echo "   (not found)"
adb shell "su -c 'cat /data/vere/monitor.log'" > "$LOG_DIR/monitor.log" 2>/dev/null || echo "   (not found)"

# Pull process info
echo "2. Process info..."
adb shell "ps -ef | grep urbit" > "$LOG_DIR/processes.txt" 2>/dev/null

# Pull system info
echo "3. System info..."
{
    echo "=== Device ==="
    adb shell getprop ro.product.model
    adb shell getprop ro.build.display.id
    echo ""
    echo "=== Memory ==="
    adb shell "cat /proc/meminfo | head -10"
    echo ""
    echo "=== Battery ==="
    adb shell "dumpsys battery"
} > "$LOG_DIR/system.txt" 2>/dev/null

# Pull Magisk module logs
echo "4. Magisk module..."
adb shell "su -c 'cat /data/adb/modules/urbit-vere/module.prop'" > "$LOG_DIR/module.prop" 2>/dev/null || true

# Pull logcat (last 1000 lines)
echo "5. Logcat..."
adb logcat -d -t 1000 > "$LOG_DIR/logcat.txt" 2>/dev/null

# Analyze
echo ""
echo "=== Analysis ==="

# Count errors
ERRORS=$(grep -ciE "error|fail|crash|panic|abort" "$LOG_DIR/urbit.log" 2>/dev/null || echo "0")
echo "Errors in urbit.log: $ERRORS"

# Check for OOM
if grep -qiE "killed|oom|lowmem" "$LOG_DIR/logcat.txt" 2>/dev/null; then
    echo "⚠ OOM/kill events detected in logcat"
    grep -iE "killed|oom|lowmem" "$LOG_DIR/logcat.txt" | tail -5
fi

# Check for crashes
if grep -qiE "sigsegv|sigabrt|fatal" "$LOG_DIR/urbit.log" 2>/dev/null; then
    echo "⚠ Crash signals detected"
    grep -iE "sigsegv|sigabrt|fatal" "$LOG_DIR/urbit.log" | tail -5
fi

# Recent activity
echo ""
echo "=== Recent urbit.log (last 20 lines) ==="
tail -20 "$LOG_DIR/urbit.log" 2>/dev/null || echo "(empty)"

echo ""
echo "Logs saved to: $LOG_DIR"
echo "To investigate: less $LOG_DIR/urbit.log"
