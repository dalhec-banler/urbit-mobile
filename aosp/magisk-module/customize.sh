#!/system/bin/sh
# Urbit Vere — Magisk module installation

SKIPUNZIP=0

# Verify architecture
if [ "$ARCH" != "arm64" ]; then
    abort "This module requires arm64 (aarch64). Detected: $ARCH"
fi

# Create vere data directory
ui_print "- Creating /data/vere/"
mkdir -p /data/vere
chmod 700 /data/vere

# Install binary if included in module
if [ -f "$MODPATH/common/urbit" ]; then
    ui_print "- Installing vere binary to /data/vere/urbit"
    cp "$MODPATH/common/urbit" /data/vere/urbit
    chmod 755 /data/vere/urbit
fi

# Install service wrapper
ui_print "- Installing service wrapper"
cp "$MODPATH/common/vere-service.sh" /data/vere/vere-service.sh
chmod 755 /data/vere/vere-service.sh

ui_print ""
ui_print "  Urbit vere service installed."
ui_print "  Pier location: /data/vere/pier"
ui_print ""
if [ ! -d "/data/vere/pier" ]; then
    ui_print "  NOTE: No pier found. Push your pier before rebooting:"
    ui_print "    adb push <pier-dir> /data/vere/pier"
    ui_print ""
    ui_print "  Or migrate from Phase 1:"
    ui_print "    adb shell cp -r /data/local/tmp/urbit-pier /data/vere/pier"
fi
ui_print ""
ui_print "  After reboot, vere starts automatically."
