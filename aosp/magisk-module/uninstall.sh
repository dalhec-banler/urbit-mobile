#!/system/bin/sh
# Urbit Vere — module uninstall cleanup

VERE_HOME=/data/vere
PIDFILE=$VERE_HOME/vere.pid

# Gracefully stop vere if running
PID=$(cat $PIDFILE 2>/dev/null)
if [ -n "$PID" ] && kill -0 $PID 2>/dev/null; then
    echo "Stopping vere (PID $PID)..."
    kill -TERM $PID 2>/dev/null

    for i in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15; do
        sleep 2
        if ! kill -0 $PID 2>/dev/null; then
            break
        fi
    done

    # Force kill if still alive
    if kill -0 $PID 2>/dev/null; then
        kill -KILL $PID 2>/dev/null
    fi
fi

rm -f $PIDFILE

# NOTE: Pier data at /data/vere/ is NOT deleted.
# Remove it manually if you want to wipe the ship:
#   adb shell rm -rf /data/vere
