#!/system/bin/sh
# Vere Mobile Monitor — tracks ship health, battery, and memory
# Push to phone: adb push scripts/monitor.sh /data/local/tmp/monitor.sh
# Run: adb shell "nohup /data/local/tmp/monitor.sh </dev/null >/dev/null 2>&1 &"
# Read: adb shell cat /data/local/tmp/vere-monitor.log

LOG=/data/local/tmp/vere-monitor.log
echo "=== VERE MOBILE MONITOR ===" > $LOG
echo "Started: $(date)" >> $LOG
echo "" >> $LOG

BATT=$(dumpsys battery 2>/dev/null | grep level | head -1 | tr -d ' ')
CHARGE=$(dumpsys battery 2>/dev/null | grep 'AC powered\|USB powered' | tr -d ' ' | tr '\n' ',')
echo "BASELINE | $(date) | $BATT | $CHARGE" >> $LOG

while true; do
  BATT=$(dumpsys battery 2>/dev/null | grep level | head -1 | tr -d ' ')
  CHARGE=$(dumpsys battery 2>/dev/null | grep 'AC powered\|USB powered' | tr -d ' ' | tr '\n' ',')
  TEMP=$(dumpsys battery 2>/dev/null | grep temperature | head -1 | tr -d ' ')

  VERE_PID=$(ps -eo pid,comm 2>/dev/null | grep urbit | grep -v grep | head -1 | awk '{print $1}')
  if [ -n "$VERE_PID" ]; then
    RSS=$(ps -eo pid,rss,comm 2>/dev/null | grep urbit | grep -v grep | awk '{sum+=$2} END {print sum}')
    STATUS="ALIVE pid=$VERE_PID rss_kb=$RSS"
  else
    STATUS="DEAD"
  fi

  UPTIME=$(cat /proc/uptime 2>/dev/null | awk '{print $1}')
  echo "$(date) | $BATT | $CHARGE $TEMP | vere=$STATUS | uptime=$UPTIME" >> $LOG

  if [ "$STATUS" = "DEAD" ]; then
    echo "!!! VERE DIED AT $(date) !!!" >> $LOG
    dmesg 2>/dev/null | grep -i "kill\|oom\|urbit" | tail -5 >> $LOG 2>/dev/null
  fi

  sleep 300
done
