#!/system/bin/sh
# Urbit Vere — Magisk boot service
# Runs after sys.boot_completed=1 (late_start service mode)

MODDIR=${0%/*}
VERE_HOME=/data/vere
LOG=$VERE_HOME/urbit.log

# Source the service wrapper
. $VERE_HOME/vere-service.sh

# Write DNS (uses Android network properties, falls back to Google DNS)
write_dns

# Wait for network to stabilize after boot
sleep 15

# Start vere
start_vere

# Start health monitor in background
monitor_vere &
