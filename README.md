# Urbit Mobile

Sovereign Urbit-native phone OS. Ship runs as user-space process on Android hardware, with the 64-bit loom as the primary filesystem.

## Status: Phase 1 Complete

Validated that vere (msl/64 branch, 64-bit loom + demand paging) runs on a Pixel 8 Pro with GrapheneOS.

### What works

- **Boot**: Moon boots from pill, completes OTA kernel compilation
- **Ames**: All 56 galaxies resolve, DMs send and receive
- **HTTP**: Landscape UI serves on localhost:8080, Tlon app functional
- **Memory**: 16GB virtual loom maps successfully, demand-pages to ~106MB RSS at idle (from 1.3GB peak during boot)
- **Survival**: Ship survives phone disconnect, Doze mode, screen-off

### Hardware

- Pixel 8 Pro (husky), GrapheneOS
- 12GB RAM, Tensor G3
- USB + wireless ADB

### Architecture

```
┌─────────────────────────────────────┐
│         GrapheneOS (Android)        │
│                                     │
│  ┌─────────────────────────────┐    │
│  │     vere (aarch64-musl)     │    │
│  │                             │    │
│  │  ┌───────────────────────┐  │    │
│  │  │   64-bit loom (16GB)  │  │    │
│  │  │   demand-paged,       │  │    │
│  │  │   ~106MB RSS idle     │  │    │
│  │  └───────────────────────┘  │    │
│  │                             │    │
│  │  Ames ── UDP :34543         │    │
│  │  Eyre ── HTTP :8080         │    │
│  └─────────────────────────────┘    │
│                                     │
└─────────────────────────────────────┘
```

## Build

### Prerequisites

- macOS with Apple Silicon (or any Linux aarch64 host)
- [Colima](https://github.com/abiosoft/colima) + Docker
- ADB (`brew install android-platform-tools`)

### Cross-compile vere

```bash
# 1. Start Colima (aarch64 VM for Docker)
colima start --arch aarch64 --cpu 4 --memory 8

# 2. Download zig 0.15.2 for Linux and build the Docker image
curl -LO https://ziglang.org/download/0.15.2/zig-aarch64-linux-0.15.2.tar.xz
docker build -t vere-builder .

# 3. Clone vere source (msl/64 branch)
git clone -b msl/64 https://github.com/urbit/vere.git

# 4. Build
./scripts/build.sh ./vere

# 5. Patch DNS for Android (musl hardcodes /etc/resolv.conf which doesn't exist)
python3 scripts/patch-dns.py vere/zig-out/aarch64-linux-musl/urbit
```

Output: 23MB statically-linked ELF aarch64 binary.

### Deploy to phone

```bash
# Push binary, keyfile, and pill to phone
./scripts/deploy.sh \
    vere/zig-out/aarch64-linux-musl/urbit-patched \
    path/to/your-moon.key \
    path/to/urbit-v4.3.pill
```

### Boot

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

Before running vere, write DNS config on the phone:

```bash
adb shell "echo 'nameserver 192.168.2.254
nameserver 8.8.8.8' > /tmp/resolv.conf"
```

Adjust nameservers for your network. WiFi required (cellular IPv6/NAT64 doesn't work for shell-started native binaries).

## Monitor

```bash
# Push and start the battery/process monitor
adb push scripts/monitor.sh /data/local/tmp/monitor.sh
adb shell "chmod +x /data/local/tmp/monitor.sh"
adb shell "nohup /data/local/tmp/monitor.sh </dev/null >/dev/null 2>&1 &"

# Read logs later
adb shell cat /data/local/tmp/vere-monitor.log
```

## Known issues

- **`shm_open` fails**: No /dev/shm on Android. Cosmetic, doesn't affect operation.
- **`http.c: failed to open spin stack`**: Related to shm. Cosmetic.
- **drum decrement-underflow**: `%coup event failed` on every boot with drum.hoon stack trace. Breaks lens agent (500 errors). Ship still functions.
- **Monitor killed by Doze**: Shell scripts get killed after ~1.5 hours of screen-off. The vere process itself survives because Ames keeps active UDP sockets.
- **Pill can't download on-device**: vere's curl can't fetch the pill during first boot. Download on host and push via adb.

## Upstream

- Vere source: [urbit/vere](https://github.com/urbit/vere), branch `msl/64`
- Zig 0.15.2 required (0.16.0 has breaking build.zig.zon changes)
- macOS 26 (Tahoe) breaks zig 0.15.2's linker, hence the Docker build container

## License

MIT
