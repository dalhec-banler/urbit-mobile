# vere_husky.mk — Product definition for Pixel 8 Pro with Urbit vere
#
# Inherits the stock AOSP husky product and adds vere as a system service.
# Build target: lunch vere_husky-ap3a-userdebug

# Override kernel version to match what we have synced
TARGET_LINUX_KERNEL_VERSION := 6.1

$(call inherit-product, device/google/shusky/aosp_husky.mk)
$(call inherit-product, device/urbit/vere/device.mk)

PRODUCT_NAME := vere_husky
PRODUCT_DEVICE := husky
PRODUCT_BRAND := Urbit
PRODUCT_MODEL := Pixel 8 Pro (Urbit)
PRODUCT_MANUFACTURER := Google
