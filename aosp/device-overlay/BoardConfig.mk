# BoardConfig.mk — Board-level configuration for vere overlay
#
# Include this AFTER the stock husky BoardConfig.

# Add vere SELinux policy
BOARD_SEPOLICY_DIRS += device/urbit/vere/sepolicy

# Add AID_VERE user definition
TARGET_FS_CONFIG_GEN += device/urbit/vere/config.fs
