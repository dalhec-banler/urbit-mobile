#!/system/bin/sh
# Urbit Vere — early boot setup
# Runs during post-fs-data (data partition mounted, before boot_completed)

VERE_HOME=/data/vere

# Ensure data directory exists
mkdir -p $VERE_HOME
chmod 700 $VERE_HOME

# Set OOM protection for any existing vere process
# (shouldn't be running yet at post-fs-data, but just in case)
VERE_PID=$(cat $VERE_HOME/vere.pid 2>/dev/null)
if [ -n "$VERE_PID" ] && [ -d "/proc/$VERE_PID" ]; then
    echo -900 > /proc/$VERE_PID/oom_score_adj 2>/dev/null
fi
