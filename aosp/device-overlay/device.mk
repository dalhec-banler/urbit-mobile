# device.mk — Urbit vere additions to the AOSP build

# Install vere binary, wrapper, and init script
PRODUCT_PACKAGES += \
    vere \
    vere-wrapper

# System properties
PRODUCT_PROPERTY_OVERRIDES += \
    persist.vere.enabled=1 \
    persist.vere.port=34543 \
    persist.vere.pier=/data/vere/pier

# Raise vm.max_map_count for the 16GB loom + hardened_malloc guard pages
PRODUCT_PROPERTY_OVERRIDES += \
    vm.max_map_count=262144
