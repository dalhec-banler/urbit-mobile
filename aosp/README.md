# Phase 2: System-Level Vere

Two paths to making vere a proper system service on the Pixel 8 Pro.

## Path A: Magisk Module (tested and deployed)

Patches GrapheneOS with Magisk root via avbroot, then deploys vere as a Magisk module that auto-starts on boot. No OS rebuild required.

**Tested on**: Pixel 8 Pro (husky), GrapheneOS 2026040800, Magisk v30.7, avbroot v3.29.0.

### What you get

- **Auto-start on boot** — vere launches after boot_completed, no manual adb needed
- **OOM protection** — oom_score_adjust -900, Android won't kill vere
- **Graceful shutdown** — SIGTERM triggers loom checkpoint before power-off
- **Auto-restart** — monitor detects crashes and restarts vere every 5 min
- **Dynamic DNS** — reads Android's active DNS servers, falls back to Google DNS
- **Still GrapheneOS** — all upstream security updates, just re-patch each OTA

### Prerequisites

| Tool | Install | Notes |
|------|---------|-------|
| adb | `brew install android-platform-tools` | |
| avbroot | Download binary from [GitHub releases](https://github.com/chenxiaolong/avbroot/releases) | Not on crates.io. Get the `universal-apple-darwin.zip` for macOS |
| vere binary | Built via `scripts/build.sh` + `scripts/patch-dns.py` | 23MB aarch64-musl static binary |

### Deployment guide

This is the exact workflow tested on a Pixel 8 Pro. Read the whole thing before starting — step 5 wipes the phone.

#### Step 1: Backup your pier

If you have an existing pier on the phone, back it up first. Bootloader unlock **wipes everything**.

```bash
./aosp/scripts/backup-pier.sh pier-backup
# Default pulls from /data/local/tmp/urbit-pier
# For Phase 2 pier: ./aosp/scripts/backup-pier.sh pier-backup /data/vere/pier
```

Verify the checkpoint exists:
```bash
ls -la pier-backup/urbit-pier/.urb/chk/
# Should contain image.bin
```

#### Step 2: Generate signing keys (one-time)

```bash
./aosp/avbroot/setup-keys.sh keys
```

You'll be prompted for a passphrase. **Remember it** — you need it for every OTA update.

For scripted/non-interactive use, create a passphrase file and pass `--pass-file`:
```bash
echo "your-passphrase" > /tmp/avb-pass.txt
avbroot key generate-key -o keys/avb.key --pass-file /tmp/avb-pass.txt
avbroot key encode-avb -k keys/avb.key -o keys/avb_pkmd.bin --pass-file /tmp/avb-pass.txt
avbroot key generate-key -o keys/ota.key --pass-file /tmp/avb-pass.txt
avbroot key generate-cert -k keys/ota.key -o keys/ota.crt --pass-file /tmp/avb-pass.txt
```

**Back up your keys.** If you lose them, you cannot update your device without wiping it.

#### Step 3: Download GrapheneOS OTA + Magisk

- **GrapheneOS OTA** for husky (Pixel 8 Pro): https://grapheneos.org/releases
  - Download the `husky-ota_update-YYYY.MM.DD.HH.zip` file
- **GrapheneOS factory image**: https://grapheneos.org/releases
  - Download the `husky-install-BUILDNUMBER.zip` file (same build number as OTA)
  - URL pattern: `https://releases.grapheneos.org/husky-install-BUILDNUMBER.zip`
- **Magisk APK**: https://github.com/topjohnwu/Magisk/releases (latest stable)

#### Step 4: Patch OTA with Magisk

```bash
./aosp/avbroot/patch-ota.sh <ota.zip> <Magisk.apk>
```

This runs `avbroot ota patch` with your signing keys. The `--magisk-preinit-device super` flag is required for Pixel 8 Pro with Magisk v25211+.

Then extract the patched images for fastboot flashing:

```bash
mkdir -p extracted
avbroot ota extract \
    --input <ota-magisk.zip> \
    --directory extracted \
    --fastboot
```

The `--fastboot` flag is critical — it generates `android-info.txt` and `fastboot-info.txt` that `fastboot flashall` needs.

#### Step 5: Flash stock GrapheneOS (clean slate)

This is the safest approach. Flash the official factory image first, then layer the Magisk-patched images on top.

**WARNING: This wipes the device.**

```bash
# On phone: Settings → System → Developer options → Enable OEM unlocking
adb reboot bootloader
fastboot flashing unlock
# Confirm on phone (volume keys to select, power to confirm)
```

Extract and flash the factory image:
```bash
unzip husky-install-BUILDNUMBER.zip
cd husky-install-BUILDNUMBER/
./flash-all.sh
# Wait for it to complete — phone will reboot into stock GrapheneOS
```

Re-enable USB debugging on the phone (Settings → System → Developer options → USB debugging).

#### Step 6: Flash Magisk-patched images

Now layer the patched images on top of the clean install:

```bash
adb reboot bootloader

# Flash patched images (from the extracted/ directory)
ANDROID_PRODUCT_OUT=extracted fastboot flashall --skip-reboot

# Flash your AVB signing key
fastboot erase avb_custom_key
fastboot flash avb_custom_key keys/avb_pkmd.bin

# Boot the phone
fastboot reboot
```

The `--skip-reboot` flag prevents auto-reboot so you can flash the AVB key. The phone will show a "your device is corrupt" warning on boot — this is expected with custom AVB keys. Select "boot anyway."

#### Step 7: Set up Magisk

After the phone boots into GrapheneOS:

1. Complete initial setup (skip most, connect to WiFi)
2. Re-enable USB debugging (Settings → System → Developer options)
3. Open the **Magisk app** (pre-installed by avbroot)
4. Magisk may ask to "complete installation" — let it download and reboot
5. After reboot, open Magisk again — it may require one more reboot
6. Verify root: `adb shell su -c id` should show `uid=0(root)`

#### Step 8: Install vere module

```bash
# Option A: Include binary in module (recommended)
cp /path/to/urbit-patched aosp/magisk-module/common/urbit
./aosp/scripts/build-module.sh
adb push urbit-vere-module.zip /sdcard/Download/

# Option B: Push binary separately
./aosp/scripts/build-module.sh
adb push urbit-vere-module.zip /sdcard/Download/
adb shell "su -c 'mkdir -p /data/vere && chmod 700 /data/vere'"
adb push /path/to/urbit-patched /data/local/tmp/urbit
adb shell "su -c 'cp /data/local/tmp/urbit /data/vere/urbit && chmod 755 /data/vere/urbit'"
```

Install via Magisk app: **Modules → Install from storage → select urbit-vere-module.zip**

#### Step 9: Restore pier

```bash
# If restoring from backup:
./aosp/scripts/restore-pier.sh pier-backup

# Or manually via root shell:
adb shell "su -c 'mkdir -p /data/vere/pier'"
adb push pier-backup/urbit-pier /data/local/tmp/urbit-pier-restore
adb shell "su -c 'cp -r /data/local/tmp/urbit-pier-restore /data/vere/pier'"
```

#### Step 10: Reboot — vere starts automatically

```bash
adb reboot
```

After boot, verify:
```bash
# Check vere is running
adb shell "su -c 'cat /data/vere/urbit.log | tail -20'"
# Should show: "pier (XXXXX): live"

# Check monitor is running
adb shell "su -c 'cat /data/vere/monitor.log'"

# Check Ames
adb shell "su -c 'cat /data/vere/urbit.log | grep \"ames: live\"'"
# Should show: "ames: live on 34543"

# Check HTTP
adb shell "su -c 'cat /data/vere/urbit.log | grep \"http: web interface\"'"
# Should show: "http: web interface live on http://localhost:80"
```

### Updating GrapheneOS

When a new GrapheneOS release comes out:

```bash
# 1. Download new OTA
# 2. Patch with Magisk
./aosp/avbroot/patch-ota.sh <new-ota.zip> <Magisk.apk>

# 3. Extract for fastboot
avbroot ota extract --input <new-ota-magisk.zip> --directory extracted --fastboot

# 4. Flash
adb reboot bootloader
ANDROID_PRODUCT_OUT=extracted fastboot flashall --skip-reboot
fastboot reboot
```

Pier data and Magisk modules survive OTA updates. No need to re-flash AVB key or reinstall the module.

### Troubleshooting

| Problem | Fix |
|---------|-----|
| `fastboot flashing unlock` says "not allowed" | Enable OEM unlocking in Settings → System → Developer options first |
| OTA sideload fails with "status 2" | Don't sideload — use `fastboot flashall` with extracted images instead |
| `fastboot flash system` says "partition not found" | System is a dynamic partition. Use `fastboot flashall` which handles this automatically |
| Phone stuck on Google logo for 30+ min | Partition mismatch. Reflash stock factory image and start from step 5 |
| "Your device is corrupt" warning | Expected with custom AVB keys. Select "boot anyway" |
| `su: not found` after Magisk | Open Magisk app, let it finish setup, reboot (may need 2 reboots) |
| Recovery shows "No command" | Hold Power + tap Volume Up to access recovery menu |
| Charging icon stuck on screen | Force reboot: hold Power + Volume Down for 10s |

### File layout on phone

```
/data/vere/
├── urbit              # vere binary (23MB, patched for /tmp/resolv.conf)
├── pier/              # ship pier data (~2-4GB)
├── urbit.log          # vere stdout/stderr
├── monitor.log        # health monitor output (battery, RSS, crash restarts)
├── vere.pid           # PID file for service management
└── vere-service.sh    # service wrapper (installed by module)
```

### Module structure

```
aosp/magisk-module/
├── META-INF/          # Magisk installer bootstrap
├── module.prop        # Module metadata
├── customize.sh       # Installation: creates /data/vere/, copies binary + service wrapper
├── service.sh         # Boot service: writes DNS, waits for network, starts vere + monitor
├── post-fs-data.sh    # Early boot: ensures /data/vere/ exists
├── uninstall.sh       # Cleanup: graceful stop (pier data preserved)
├── common/
│   ├── vere-service.sh  # Service wrapper (start/stop/monitor functions)
│   └── urbit            # Patched vere binary (gitignored, add before building)
└── sepolicy.rule      # SELinux policy patches (placeholder — magisk domain is permissive)
```

---

## Path B: Full AOSP Fork (the real product)

Build a custom Android ROM from source with vere as a first-class native daemon. This is the path to a shippable Urbit OS with custom launcher, Urbit branding, and Urbit apps as system apps.

Path A (Magisk) is the POC. Path B is the product.

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

**Option A: Use the build script (recommended)**

```bash
# First, get the vere binary (download from CI or build locally)
# Place at: aosp/device-overlay/prebuilt/arm64/urbit

# Sync AOSP source (~100GB, ~2 hours on fast connection)
./aosp/scripts/build-aosp.sh sync

# Extract vendor blobs (requires device or factory image)
./aosp/scripts/build-aosp.sh vendor

# Build the ROM (~2-4 hours)
./aosp/scripts/build-aosp.sh build

# Flash to connected device
./aosp/scripts/build-aosp.sh flash
```

**Option B: Manual steps**

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
cp /path/to/urbit-patched device/urbit/vere/prebuilt/arm64/urbit

# 4. Build
source build/envsetup.sh
lunch vere_husky-userdebug
m -j$(nproc)

# 5. Flash
adb reboot bootloader
fastboot flashall -w
```

**Option C: CI workflow**

For automated builds, use the `Build AOSP` workflow (requires self-hosted runner with 300GB+ disk):

```bash
gh workflow run aosp.yml -f android_version=android-15.0.0_r29 -f build_variant=userdebug
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
| 2A | **Deployed** | Magisk module: auto-start, OOM protect, crash recovery, monitoring |
| 2B | Scaffolded | Full AOSP fork: custom ROM with vere as init daemon |
| 3 | Planned | Custom Urbit launcher + system UI |
| 4 | Planned | Urbit apps as native Android apps |
| 5 | Planned | Multi-device: support beyond Pixel 8 Pro |
