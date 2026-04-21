#!/system/bin/sh
# Urbit Mobile — one-command ship startup
# Push: adb push scripts/start-ship.sh /data/local/tmp/start-ship.sh
# Run:  adb shell /data/local/tmp/start-ship.sh
PIER=/data/local/tmp/urbit-pier
BINARY=/data/local/tmp/urbit
PORT=34543

echo "=== Urbit Mobile Start ==="

# 1. Setup DNS (wiped on every reboot)
echo "nameserver 192.168.2.254
nameserver 8.8.8.8" > /tmp/resolv.conf
echo "[ok] DNS config written"

# 2. Check if already running
EXISTING=$(ps -eo pid,comm 2>/dev/null | grep urbit | grep -v grep | head -1 | awk '{print $1}')
if [ -n "$EXISTING" ]; then
    echo "[ok] Ship already running (PID $EXISTING)"
    exit 0
fi

# 3. Start vere in background
echo "[..] Booting ship..."
nohup $BINARY --no-dock --no-conn -t -p $PORT $PIER \
    > /data/local/tmp/urbit.log 2>&1 &
VERE_PID=$!
echo "[ok] Ship starting (PID $VERE_PID)"

# 4. Wait for HTTP to come up
for i in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15; do
    sleep 2
    if grep -q "http: web interface live" /data/local/tmp/urbit.log 2>/dev/null; then
        echo "[ok] HTTP live on localhost:8080"
        break
    fi
    echo "[..] Waiting for boot... (${i})"
done

# 5. Start monitor
nohup /data/local/tmp/monitor.sh </dev/null >/dev/null 2>&1 &
echo "[ok] Monitor started"

echo ""
echo "=== Ship is running ==="
echo "  Web UI: http://localhost:8080 (on phone browser)"
echo "  Ames:   UDP :$PORT"
echo "  Logs:   adb shell tail -f /data/local/tmp/urbit.log"
