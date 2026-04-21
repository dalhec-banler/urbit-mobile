# Phase 2: System-Level Vere

Two paths to making vere a proper system service on the Pixel 8 Pro.

## Path A: Magisk Module (deploy this week)

Patches the existing GrapheneOS install with Magisk (root), then deploys vere as a module that auto-starts on boot. No OS rebuild required.

### What you get

- **Auto-start on boot** — vere launches after boot_completed, no manual adb needed
- **OOM protection** — oom_score_adjust -900, Android won't kill vere
- **Graceful shutdown** — SIGTERM triggers loom checkpoint before power-off
- **Auto-restart** — monitor detects crashes and restarts vere
- **Dynamic DNS** — reads Android's active DNS servers, falls back to Google DNS
- **Still GrapheneOS** — all upstream security updates, just re-patch each OTA

### Prerequisites

| Tool | Install |
|------|---------|
| adb | `brew install android-platform-tools` |
| avbroot | `cargo install avbroot` |
| Rust/cargo | `curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs \| sh` |

### Quick start

```bash
# Check prerequisites
./aosp/scripts/bootstrap.sh

# The bootstrap script prints the full step-by-step.
# Summary:

# 1. Backup pier (bootloader unlock wipes device!)
./aosp/scripts/backup-pier.sh pier-backup

# 2. Generate signing keys (one-time)
./aosp/avbroot/setup-keys.sh keys

# 3. Download GrapheneOS OTA for husky + Magisk APK
#    OTA:    https://grapheneos.org/releases
#    Magisk: https://github.com/topjohnwu/Magisk/releases

# 4. Patch OTA with Magisk
./aosp/avbroot/patch-ota.sh <ota.zip> <Magisk.apk>

# 5. Unlock bootloader, flash AVB key, sideload patched OTA
adb reboot bootloader
fastboot flashing unlock
fastboot flash avb_custom_key keys/avb_pkmd.bin
# Reboot to recovery → Apply update from ADB
adb sideload <ota-magisk.zip>
# Relock bootloader
fastboot flashing lock

# 6. Build and install vere module
cp /path/to/urbit-patched aosp/magisk-module/common/urbit
./aosp/scripts/build-module.sh
adb push urbit-vere-module.zip /sdcard/Download/
# Magisk app → Modules → Install from storage

# 7. Restore pier
./aosp/scripts/restore-pier.sh pier-backup

# 8. Reboot — vere starts automatically
adb reboot
```

### Updating GrapheneOS

When a new GrapheneOS release comes out:

```bash
# Download new OTA, re-patch, sideload
./aosp/avbroot/patch-ota.sh <new-ota.zip> <Magisk.apk>
adb sideload <new-ota-magisk.zip>
```

Pier data and Magisk modules survive OTA updates.

### File layout on phone

```
/data/vere/
├── urbit              # vere binary (patched for /tmp/resolv.conf)
├── pier/              # ship pier data
├── urbit.log          # vere stdout/stderr
├── monitor.log        # health monitor output
├── vere.pid           # PID file for service management
└── vere-service.sh    # service wrapper (installed by module)
```

### Module structure

```
aosp/magisk-module/
├── META-INF/          # Magisk installer bootstrap
├── module.prop        # Module metadata
├── customize.sh       # Installation: creates /data/vere/, copies binary
├── service.sh         # Boot service: writes DNS, starts vere, starts monitor
├── post-fs-data.sh    # Early boot: ensures /data/vere/ exists
├── uninstall.sh       # Cleanup: graceful stop (pier data preserved)
├── common/
│   ├── vere-service.sh  # Service wrapper (start/stop/monitor functions)
│   └── urbit            # Patched vere binary (gitignored, add before building)
└── sepolicy.rule      # SELinux policy patches (placeholder — magisk domain is permissive)
```

---

## Path B: Full AOSP Fork (the real product)

Build a custom Android ROM from source with vere as a first-class native daemon. This is the path to a shippable Urbit OS with custom launcher, branding, and Urbit apps as system apps.

### What you get (beyond Path A)

- **Custom launcher** — replace Pixel Launcher with Urbit-native launcher
- **Urbit apps as system apps** — not side-loaded, first-class citizens
- **Dedicated vere user (AID 2901)** — not running as root
- **Full SELinux policy** — minimum-privilege domain, no permissive magisk shim
- **Custom branding** — boot animation, lock screen, settings UI
- **Strip bloat** — remove unused AOSP apps and services
- **Proper init integration** — `shutdown critical` for checkpoint, cpuset for performance
- **Your own OTA server** — push updates to devices in the field

### Build requirements

| Resource | Minimum |
|----------|---------|
| RAM | 32 GB (64 GB recommended) |
| Disk | 300 GB+ SSD |
| OS | Ubuntu 24.04 LTS or Debian bookworm |
| CPU | 16+ cores recommended |
| Build time | 1-3 hours (full build) |

### Device overlay

The `device-overlay/` directory contains everything needed to add vere to an AOSP build. Copy it to `device/urbit/vere/` in the AOSP source tree:

```
device-overlay/
├── Android.bp              # Soong build: prebuilt vere binary
├── AndroidProducts.mk      # Declares the vere_husky product
├── BoardConfig.mk          # Wires in SELinux policy + AID
├── device.mk               # PRODUCT_PACKAGES + system properties
├── vere_husky.mk           # Product def: inherits aosp_husky + our additions
├── config.fs               # AID_VERE user definition (UID 2901)
├── vere.rc                 # init service: auto-start, OOM protect, shutdown critical
├── vere-wrapper.sh         # DNS setup + exec vere
└── sepolicy/
    ├── vere.te             # Type enforcement (network, mmap, file I/O)
    ├── file_contexts       # Labels for /system/bin/vere, /data/vere/
    └── property_contexts   # Labels for persist.vere.* properties
```

### Build steps

```bash
# 1. Sync AOSP source (pick tag with husky support)
repo init -u https://android.googlesource.com/platform/manifest \
    -b android-15.0.0_r29
repo sync -c -j$(nproc) --no-tags

# 2. Extract Pixel 8 Pro vendor blobs
git clone https://github.com/anestisb/android-prepare-vendor
./android-prepare-vendor/execute-all.sh -d husky -b <BUILD_ID> -o vendor-out
cp -r vendor-out/husky/<BUILD_ID>/vendor* .

# 3. Copy vere overlay into source tree
cp -r aosp/device-overlay device/urbit/vere
cp /path/to/urbit-patched device/urbit/vere/prebuilt/arm64/vere

# 4. Build
source build/envsetup.sh
lunch vere_husky-userdebug
m -j$(nproc)

# 5. Flash
adb reboot bootloader
fastboot flashall -w
```

### SELinux policy highlights

The vere domain (`vere.te`) grants minimum required permissions:

| Permission | Why |
|-----------|-----|
| `net_domain` | UDP (Ames) + TCP (Eyre HTTP) sockets |
| `execmem` | mprotect for demand-paged 64-bit loom |
| `create_dir_perms` on `vere_data_file` | Read/write pier at /data/vere/ |
| `r_dir_file` on `proc` | /proc/self/maps, /proc/meminfo for diagnostics |
| `shutdown critical` | 30s checkpoint window during power-off |

### init.rc service highlights

```rc
service vere /system/bin/vere-wrapper.sh
    class main
    user vere                    # dedicated UID 2901
    group vere inet net_raw      # network access
    seclabel u:r:vere:s0         # SELinux domain
    oom_score_adjust -900        # OOM protection
    task_profiles MaxPerformance # foreground cpuset (all cores)
    shutdown critical            # time to checkpoint on poweroff
    disabled                     # started by boot_completed trigger
```

---

## Roadmap

| Phase | Status | Description |
|-------|--------|-------------|
| 1 | Done | vere runs on Pixel 8 Pro via adb shell |
| 2A | Ready to deploy | Magisk module: auto-start, OOM protect, monitoring |
| 2B | Scaffolded | Full AOSP fork: custom ROM with vere as init daemon |
| 3 | Planned | Custom Urbit launcher + system UI |
| 4 | Planned | Urbit apps as native Android apps |
| 5 | Planned | Multi-device: support beyond Pixel 8 Pro |
