# Urbit Mobile

Sovereign Urbit-native phone OS. Ship runs as user-space process on Android hardware, with the 64-bit loom as the primary filesystem.

## Status: Phase 2A Deployed

Phase 1 validated that vere (msl/64 branch, 64-bit loom + demand paging) runs on a Pixel 8 Pro with GrapheneOS. Phase 2A deployed it as a system service via Magisk module — vere auto-starts on boot with OOM protection and crash recovery.

| Phase | Status | Description |
|-------|--------|-------------|
| 1 | Done | vere runs on Pixel 8 Pro via adb shell |
| 2A | **Deployed** | Magisk module: auto-start, OOM protection, crash recovery |
| 2B | Scaffolded | Full AOSP fork: custom ROM with vere as init daemon |
| 3 | Planned | Custom Urbit launcher + system UI |
| 4 | Planned | Urbit apps as native Android apps |

See [aosp/README.md](aosp/README.md) for the full deployment guide.

### What works

- **Auto-start**: Ship starts on boot via Magisk service, no manual intervention
- **Boot**: Moon boots from pill, completes OTA kernel compilation
- **Ames**: All 56 galaxies resolve, DMs send and receive
- **HTTP**: Landscape UI serves on localhost:80, Tlon app functional
- **Memory**: 16GB virtual loom maps successfully, demand-pages to ~106MB RSS at idle (from 1.3GB peak during boot)
- **Survival**: Ship survives phone disconnect, Doze mode, screen-off, reboots
- **Crash recovery**: Monitor process checks every 5 min, auto-restarts vere on crash
- **OOM protection**: oom_score_adj -900 prevents Android from killing vere

### Hardware

- Pixel 8 Pro (husky), GrapheneOS + Magisk (via avbroot)
- 12GB RAM, Tensor G3
- USB + wireless ADB

### Architecture

```
┌─────────────────────────────────────┐
│    GrapheneOS + Magisk (Android)    │
│                                     │
│  ┌─────────────────────────────┐    │
│  │     vere (aarch64-musl)     │    │
│  │     auto-start on boot      │    │
│  │     OOM protected (-900)    │    │
│  │                             │    │
│  │  ┌───────────────────────┐  │    │
│  │  │   64-bit loom (16GB)  │  │    │
│  │  │   demand-paged,       │  │    │
│  │  │   ~106MB RSS idle     │  │    │
│  │  └───────────────────────┘  │    │
│  │                             │    │
│  │  Ames ── UDP :34543         │    │
│  │  Eyre ── HTTP :80           │    │
│  └─────────────────────────────┘    │
│                                     │
│  ┌─────────────────────────────┐    │
│  │  Monitor (every 5 min)      │    │
│  │  crash restart, DNS refresh │    │
│  │  battery + RSS logging      │    │
│  └─────────────────────────────┘    │
│                                     │
└─────────────────────────────────────┘
```

## Quick Start (Phase 2A — Magisk Module)

Requires: macOS, adb, [avbroot](https://github.com/chenxiaolong/avbroot/releases), a Pixel 8 Pro with GrapheneOS.

```bash
# 1. Check prerequisites
./aosp/scripts/bootstrap.sh

# 2. Generate signing keys (one-time)
./aosp/avbroot/setup-keys.sh keys

# 3. Download GrapheneOS OTA + factory image + Magisk APK
#    OTA + factory: https://grapheneos.org/releases (husky)
#    Magisk:        https://github.com/topjohnwu/Magisk/releases

# 4. Patch OTA with Magisk
./aosp/avbroot/patch-ota.sh <ota.zip> <Magisk.apk>

# 5. Extract patched images for fastboot
avbroot ota extract --input <ota-magisk.zip> --directory extracted --fastboot

# 6. Flash stock GrapheneOS factory image (WIPES DEVICE)
#    Backup pier first: ./aosp/scripts/backup-pier.sh pier-backup
unzip husky-install-*.zip && cd husky-install-*/ && ./flash-all.sh

# 7. Flash patched Magisk images on top
adb reboot bootloader
ANDROID_PRODUCT_OUT=extracted fastboot flashall --skip-reboot
fastboot erase avb_custom_key
fastboot flash avb_custom_key keys/avb_pkmd.bin
fastboot reboot

# 8. Set up Magisk (open app, let it finish, reboot)

# 9. Build and install vere module
cp /path/to/urbit-patched aosp/magisk-module/common/urbit
./aosp/scripts/build-module.sh
adb push urbit-vere-module.zip /sdcard/Download/
# Magisk app → Modules → Install from storage

# 10. Restore pier + reboot
./aosp/scripts/restore-pier.sh pier-backup
adb reboot
# Ship starts automatically
```

See [aosp/README.md](aosp/README.md) for the detailed step-by-step with troubleshooting.

## Build

[![Build](https://github.com/dalhec-banler/urbit-mobile/actions/workflows/build.yml/badge.svg)](https://github.com/dalhec-banler/urbit-mobile/actions/workflows/build.yml)

### Download pre-built binaries

Every commit to `main` or `develop` produces artifacts via GitHub Actions:
- **urbit-aarch64-linux-musl** — patched vere binary ready for Android
- **urbit-vere-module.zip** — Magisk module with binary included

Download from [Actions](https://github.com/dalhec-banler/urbit-mobile/actions) or [Releases](https://github.com/dalhec-banler/urbit-mobile/releases).

### Build locally

#### Linux x86_64 (recommended)

Zig cross-compiles natively — no Docker or emulation needed.

```bash
# 1. Download Zig 0.15.2
curl -LO https://ziglang.org/download/0.15.2/zig-x86_64-linux-0.15.2.tar.xz
tar -xf zig-x86_64-linux-0.15.2.tar.xz
export PATH="$PWD/zig-x86_64-linux-0.15.2:$PATH"

# 2. Clone vere source (msl/64 branch)
git clone -b msl/64 https://github.com/urbit/vere.git

# 3. Build
cd vere
zig build -Dtarget=aarch64-linux-musl -Doptimize=ReleaseFast
cd ..

# 4. Patch DNS for Android (musl hardcodes /etc/resolv.conf which doesn't exist)
python3 scripts/patch-dns.py vere/zig-out/aarch64-linux-musl/urbit

# 5. (Optional) Strip debug symbols
llvm-strip vere/zig-out/aarch64-linux-musl/urbit-patched
```

#### macOS (Apple Silicon)

Use Docker via Colima to run Zig in an aarch64 container:

```bash
# 1. Start Colima (aarch64 VM for Docker)
colima start --arch aarch64 --cpu 4 --memory 8

# 2. Download zig 0.15.2 for Linux aarch64 and build the Docker image
curl -LO https://ziglang.org/download/0.15.2/zig-aarch64-linux-0.15.2.tar.xz
docker build -t vere-builder .

# 3. Clone vere source (msl/64 branch)
git clone -b msl/64 https://github.com/urbit/vere.git

# 4. Build
./scripts/build.sh ./vere

# 5. Patch DNS for Android
python3 scripts/patch-dns.py vere/zig-out/aarch64-linux-musl/urbit
```

Output: ~23MB statically-linked ELF aarch64 binary.

### Deploy to phone (Phase 1 — manual)

```bash
# Push binary, keyfile, and pill to phone
./scripts/deploy.sh \
    vere/zig-out/aarch64-linux-musl/urbit-patched \
    path/to/your-moon.key \
    path/to/urbit-v4.3.pill
```

### Boot (Phase 1 — manual)

```bash
# First boot (new moon)
adb shell
cd /data/local/tmp
./urbit --no-dock --no-conn -t -p 34543 \
    -B /data/local/tmp/urbit-v4.3.pill \
    -w your-moon-name \
    -k /data/local/tmp/your-moon-name.key \
    /data/local/tmp/urbit-pier

# Resume existing pier
adb shell '/data/local/tmp/urbit --no-dock --no-conn -t -p 34543 /data/local/tmp/urbit-pier'
```

### Required flags

| Flag | Reason |
|------|--------|
| `--no-dock` | Android /data/local/tmp doesn't support hard links |
| `--no-conn` | Unix socket for control plane can't bind on Android |
| `-t` | No-TTY mode for adb shell |
| `-p 34543` | Explicit Ames port |

### DNS setup

Before running vere (Phase 1 only — Phase 2 handles this automatically):

```bash
adb shell "echo 'nameserver 192.168.2.254
nameserver 8.8.8.8' > /tmp/resolv.conf"
```

Adjust nameservers for your network. WiFi required (cellular IPv6/NAT64 doesn't work for shell-started native binaries).

## Scripts

Utility scripts for managing urbit-mobile. All require ADB with port forwarding set up (`adb forward tcp:12321 tcp:12321`).

| Script | Purpose |
|--------|---------|
| `scripts/status.sh` | Full system status report |
| `scripts/health.sh` | Quick health check |
| `scripts/lens.sh` | Reliable lens API client (auto-cancels stuck jobs) |
| `scripts/deploy-binary.sh` | Deploy new vere binary to phone |
| `scripts/build.sh` | Cross-compile vere for aarch64-musl |

```bash
# Full status report
./scripts/status.sh

# Quick health check
./scripts/health.sh

# Send dojo command via lens (handles stuck jobs)
./scripts/lens.sh "(add 1 1)"

# Deploy new binary
./scripts/deploy-binary.sh /path/to/urbit

# Check logs
adb shell "su -c 'tail -20 /data/vere/urbit.log'"
adb shell "su -c 'cat /data/vere/monitor.log'"
```

## Known issues

- ~~**`shm_open` fails**~~ **FIXED**: Patch 0001 checks for /dev/shm before calling shm_open.
- ~~**`http.c: failed to open spin stack`**~~ **FIXED**: Same patch removes the noisy error message.
- **Lens 500 errors after stuck job**: Lens agent is single-threaded. If a command fails mid-stream or client disconnects, the job stays stuck and all subsequent requests return 500. Workaround: send `{"source":{"cancel":null},"sink":{"stdout":null}}` to clear.
- **Monitor killed by Doze (Phase 1 only)**: Shell scripts get killed after ~1.5 hours of screen-off. Phase 2 Magisk service survives because it runs as a boot service.
- **Pill can't download on-device**: vere's curl can't fetch the pill during first boot. Download on host and push via adb.
- **"Your device is corrupt" warning**: Expected with custom AVB keys. Select "boot anyway." Does not affect functionality.

## Phase 2: System Service

Phase 2 makes vere a proper system service that auto-starts on boot. Two paths:

**Path A (Magisk module)** — Deployed and tested. Patch existing GrapheneOS with Magisk via avbroot, deploy vere as a module. See [aosp/README.md](aosp/README.md).

**Path B (AOSP fork)** — The long-term product vision. Build a custom ROM from source with vere as a native init daemon, custom Urbit launcher, Urbit branding, Urbit apps as system apps. Device overlay scaffolded at `aosp/device-overlay/`.

```bash
# Quick start (Path A)
./aosp/scripts/bootstrap.sh    # check prerequisites + print full guide
```

## Upstream

- Vere source: [urbit/vere](https://github.com/urbit/vere), branch `msl/64`
- Zig 0.15.2 required (0.16.0 has breaking build.zig.zon changes)
- macOS 26 (Tahoe) breaks zig 0.15.2's linker, hence the Docker build container
- avbroot: [chenxiaolong/avbroot](https://github.com/chenxiaolong/avbroot) (download binary, not on crates.io)

## License

MIT
