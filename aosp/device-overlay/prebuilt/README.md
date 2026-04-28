# Prebuilt Binaries

This directory contains prebuilt vere binaries for AOSP inclusion.

## arm64/urbit

The aarch64-linux-musl vere binary. CI builds this from the `msl/64` branch with:
- shm_open fix for Android (no /dev/shm)
- DNS patch for /tmp/resolv.conf
- Stripped for size

### Getting the binary

**From CI:**
1. Go to Actions → Build workflow
2. Download the `vere-aarch64-linux-musl` artifact
3. Rename `urbit-patched` to `urbit`
4. Place in `prebuilt/arm64/urbit`

**Build locally:**
```bash
cd /path/to/vere
zig build -Dtarget=aarch64-linux-musl -Doptimize=ReleaseFast
python3 scripts/patch-dns.py zig-out/aarch64-linux-musl/urbit
llvm-strip zig-out/aarch64-linux-musl/urbit-patched
cp zig-out/aarch64-linux-musl/urbit-patched prebuilt/arm64/urbit
```

The binary is gitignored — do not commit it.
