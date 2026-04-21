#!/system/bin/sh
# Urbit vere service management
# Sourced by service.sh — provides start/stop/monitor functions

VERE_HOME=/data/vere
PIER=$VERE_HOME/pier
BINARY=$VERE_HOME/urbit
LOG=$VERE_HOME/urbit.log
PIDFILE=$VERE_HOME/vere.pid
MONITOR_LOG=$VERE_HOME/monitor.log
PORT=34543

write_dns() {
    # Use Android's active DNS servers, fall back to Google DNS
    local dns1=$(getprop net.dns1 2>/dev/null)
    local dns2=$(getprop net.dns2 2>/dev/null)
    [ -z "$dns1" ] && dns1="8.8.8.8"
    [ -z "$dns2" ] && dns2="8.8.4.8"
    echo "nameserver $dns1" > /tmp/resolv.conf
    echo "nameserver $dns2" >> /tmp/resolv.conf
}

is_running() {
    local pid=$(cat $PIDFILE 2>/dev/null)
    [ -n "$pid" ] && kill -0 $pid 2>/dev/null
}

start_vere() {
    if [ ! -f "$BINARY" ]; then
        echo "$(date) [ERROR] vere binary not found at $BINARY" >> $LOG
        return 1
    fi

    if [ ! -d "$PIER" ]; then
        echo "$(date) [ERROR] pier not found at $PIER" >> $LOG
        return 1
    fi

    if is_running; then
        echo "$(date) [ok] vere already running (PID $(cat $PIDFILE))" >> $LOG
        return 0
    fi

    echo "$(date) [..] Starting vere on port $PORT..." >> $LOG
    nohup $BINARY --no-dock --no-conn -t -p $PORT $PIER >> $LOG 2>&1 &
    local pid=$!
    echo $pid > $PIDFILE

    # OOM protection — tell Android not to kill vere
    echo -900 > /proc/$pid/oom_score_adj 2>/dev/null

    echo "$(date) [ok] vere started (PID $pid)" >> $LOG

    # Wait for HTTP to come up
    for i in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30; do
        sleep 2
        if grep -q "http: web interface live" $LOG 2>/dev/null; then
            echo "$(date) [ok] HTTP live on localhost:8080" >> $LOG
            return 0
        fi
    done
    echo "$(date) [!!] HTTP didn't come up in 60s (vere may still be booting)" >> $LOG
}

stop_vere() {
    local pid=$(cat $PIDFILE 2>/dev/null)
    if [ -z "$pid" ] || ! kill -0 $pid 2>/dev/null; then
        echo "$(date) [ok] vere not running" >> $LOG
        rm -f $PIDFILE
        return 0
    fi

    echo "$(date) [..] Stopping vere (PID $pid), waiting for checkpoint..." >> $LOG
    kill -TERM $pid 2>/dev/null

    for i in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15; do
        sleep 2
        if ! kill -0 $pid 2>/dev/null; then
            echo "$(date) [ok] vere stopped gracefully" >> $LOG
            rm -f $PIDFILE
            return 0
        fi
    done

    echo "$(date) [!!] Force killing vere after 30s" >> $LOG
    kill -KILL $pid 2>/dev/null
    rm -f $PIDFILE
}

monitor_vere() {
    # Health monitor — checks every 5 minutes, restarts if dead
    echo "$(date) [ok] Monitor started" >> $MONITOR_LOG

    while true; do
        sleep 300

        # Refresh DNS in case network changed
        write_dns

        # Check battery
        local batt=$(dumpsys battery 2>/dev/null | grep level | head -1 | tr -d ' ')
        local temp=$(dumpsys battery 2>/dev/null | grep temperature | head -1 | tr -d ' ')
        local charge=$(dumpsys battery 2>/dev/null | grep 'AC powered\|USB powered' | tr -d ' ' | tr '\n' ',')

        if is_running; then
            local pid=$(cat $PIDFILE)
            local rss=$(ps -eo pid,rss 2>/dev/null | grep "^[ ]*$pid " | awk '{print $2}')
            echo "$(date) | $batt | $charge $temp | vere=ALIVE pid=$pid rss_kb=$rss" >> $MONITOR_LOG
        else
            echo "$(date) | $batt | $charge $temp | vere=DEAD — restarting" >> $MONITOR_LOG
            # Capture why it died
            dmesg 2>/dev/null | grep -i "kill\|oom\|urbit" | tail -5 >> $MONITOR_LOG 2>/dev/null

            # Restart
            write_dns
            start_vere
        fi
    done
}
