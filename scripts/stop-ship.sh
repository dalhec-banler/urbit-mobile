#!/system/bin/sh
# Urbit Mobile — graceful ship shutdown with checkpoint
# Push: adb push scripts/stop-ship.sh /data/local/tmp/stop-ship.sh
# Run:  adb shell /data/local/tmp/stop-ship.sh
echo "=== Urbit Mobile Stop ==="

VERE_PID=$(ps -eo pid,comm 2>/dev/null | grep urbit | grep -v grep | head -1 | awk '{print $1}')
if [ -z "$VERE_PID" ]; then
    echo "[ok] Ship not running"
    exit 0
fi

echo "[..] Sending SIGTERM to vere (PID $VERE_PID)..."
kill -TERM $VERE_PID 2>/dev/null

# Wait up to 30 seconds for graceful checkpoint
for i in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15; do
    sleep 2
    if ! ps -p $VERE_PID > /dev/null 2>&1; then
        echo "[ok] Ship shut down gracefully"
        exit 0
    fi
    echo "[..] Waiting for checkpoint... (${i})"
done

echo "[!!] Ship didn't stop in 30s, forcing..."
kill -KILL $VERE_PID 2>/dev/null
echo "[ok] Ship killed"
