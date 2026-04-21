#!/system/bin/sh
# vere-wrapper.sh — service wrapper for init-started vere
#
# Called by init via vere.rc. Handles DNS setup before exec'ing vere.

VERE_HOME=/data/vere
PIER=$VERE_HOME/pier
BINARY=/system/bin/urbit
LOG=$VERE_HOME/urbit.log
PORT=34543

# Write DNS config (musl reads /tmp/resolv.conf via binary patch)
DNS1=$(getprop net.dns1 2>/dev/null)
DNS2=$(getprop net.dns2 2>/dev/null)
[ -z "$DNS1" ] && DNS1="8.8.8.8"
[ -z "$DNS2" ] && DNS2="8.8.4.8"
echo "nameserver $DNS1" > /tmp/resolv.conf
echo "nameserver $DNS2" >> /tmp/resolv.conf

# Read port from system property if set
PROP_PORT=$(getprop persist.vere.port 2>/dev/null)
[ -n "$PROP_PORT" ] && PORT=$PROP_PORT

# Read pier path from system property if set
PROP_PIER=$(getprop persist.vere.pier 2>/dev/null)
[ -n "$PROP_PIER" ] && PIER=$PROP_PIER

echo "$(date) [..] Starting vere: port=$PORT pier=$PIER" >> $LOG

# exec replaces wrapper with vere — init tracks the vere PID directly
exec $BINARY --no-dock --no-conn -t -p $PORT $PIER >> $LOG 2>&1
