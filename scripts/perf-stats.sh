#!/bin/bash
set -euo pipefail

# Performance stats for vere on Android
# Shows memory, CPU, disk I/O over time

INTERVAL="${1:-5}"
DURATION="${2:-60}"
ITERATIONS=$((DURATION / INTERVAL))

echo "=== Vere Performance Monitor ==="
echo "Interval: ${INTERVAL}s, Duration: ${DURATION}s"
echo ""

# Get vere work process PID
PID=$(adb shell "ps -ef | grep 'urbit work' | grep -v grep | head -1 | awk '{print \$2}'" 2>/dev/null | tr -d '\r')
if [ -z "$PID" ]; then
    echo "Error: vere not running"
    exit 1
fi
echo "Monitoring PID: $PID"
echo ""

printf "%-10s %-10s %-10s %-10s %-10s %-10s\n" "TIME" "RSS_MB" "CPU%" "THREADS" "FDs" "IO_R_MB"
echo "----------------------------------------------------------------------"

for i in $(seq 1 $ITERATIONS); do
    TIMESTAMP=$(date +%H:%M:%S)

    # Memory (RSS)
    RSS=$(adb shell "su -c 'cat /proc/$PID/status'" 2>/dev/null | grep VmRSS | awk '{print $2}')
    RSS_MB=$((RSS / 1024))

    # CPU (from /proc/stat - simplified)
    CPU=$(adb shell "top -b -n 1 -p $PID 2>/dev/null | tail -1 | awk '{print \$9}'" | tr -d '\r' || echo "0")

    # Threads
    THREADS=$(adb shell "su -c 'cat /proc/$PID/status'" 2>/dev/null | grep Threads | awk '{print $2}')

    # File descriptors
    FDS=$(adb shell "su -c 'ls /proc/$PID/fd 2>/dev/null | wc -l'" | tr -d '\r')

    # I/O read (from /proc/pid/io)
    IO_READ=$(adb shell "su -c 'cat /proc/$PID/io 2>/dev/null'" | grep read_bytes | awk '{print $2}' || echo "0")
    IO_READ_MB=$((IO_READ / 1024 / 1024))

    printf "%-10s %-10s %-10s %-10s %-10s %-10s\n" "$TIMESTAMP" "$RSS_MB" "$CPU" "$THREADS" "$FDS" "$IO_READ_MB"

    sleep "$INTERVAL"
done

echo ""
echo "=== Summary ==="

# Final stats
VMSIZE=$(adb shell "su -c 'cat /proc/$PID/status'" 2>/dev/null | grep VmSize | awk '{print $2}')
VMSIZE_GB=$((VMSIZE / 1024 / 1024))
PIER_SIZE=$(adb shell "su -c 'du -sm /data/vere/pier'" 2>/dev/null | awk '{print $1}')

echo "Virtual memory: ${VMSIZE_GB}GB"
echo "Pier size: ${PIER_SIZE}MB"

# Battery drain
BATT=$(adb shell "dumpsys battery" 2>/dev/null | grep "^  level:" | awk '{print $2}' | tr -d '\r')
TEMP=$(adb shell "dumpsys battery" 2>/dev/null | grep "^  temperature:" | awk '{print $2}' | tr -d '\r')
TEMP_C=$((TEMP / 10))
echo "Battery: ${BATT}% @ ${TEMP_C}°C"
