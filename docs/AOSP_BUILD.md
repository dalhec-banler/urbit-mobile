# AOSP Build Guide

Complete walkthrough for building a custom Android ROM with vere as a native system service.

This is Phase 2B of urbit-mobile: a full AOSP fork with vere integrated at the system level.

## Overview

What you get:

- Vere runs as a native init daemon (not via Magisk)
- Dedicated system user (AID 2901) with full SELinux policy
- Custom Urbit launcher (future)
- Urbit apps as system apps (future)
- Your own OTA update server (future)

## System Requirements

| Resource | Minimum | Recommended |
|----------|---------|-------------|
| RAM | 32 GB | 64 GB |
| Storage | 300 GB SSD | 500 GB+ NVMe |
| OS | Ubuntu 22.04 or 24.04 LTS | Ubuntu 24.04 LTS |
| CPU | 8 cores | 16+ cores |
| Network | Fast connection | 100+ Mbps |

Build times:

- **First sync**: 1-3 hours (downloads ~100GB)
- **Full build**: 1-4 hours depending on CPU
- **Incremental build**: 10-30 minutes

## Required Dependencies

Install build dependencies on Ubuntu:

```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Essential build tools
sudo apt install -y \
    git \
    git-lfs \
    gnupg \
    flex \
    bison \
    build-essential \
    zip \
    unzip \
    curl \
    wget \
    zlib1g-dev \
    gcc-multilib \
    g++-multilib \
    libc6-dev-i386 \
    libncurses5 \
    lib32ncurses5-dev \
    x11proto-core-dev \
    libx11-dev \
    lib32z1-dev \
    libgl1-mesa-dev \
    libxml2-utils \
    xsltproc \
    fontconfig \
    libssl-dev \
    python3 \
    python3-pip \
    rsync

# Repo tool for managing AOSP sources
mkdir -p ~/.local/bin
curl https://storage.googleapis.com/git-repo-downloads/repo > ~/.local/bin/repo
chmod a+x ~/.local/bin/repo
echo 'export PATH="$HOME/.local/bin:$PATH"' >> ~/.bashrc
source ~/.bashrc

# Verify
repo --version
```

### Critical: rsync

The `rsync` package is required for vendor blob extraction. Without it:

```
Error: rsync not found
```

Install: `sudo apt install rsync`

### Critical: zip

Required for creating flashable images:

```bash
sudo apt install zip
```

## Directory Structure

```
~/aosp/
├── .repo/              # Repo metadata (don't delete)
├── build/              # Build system
├── device/
│   ├── google/         # Pixel device configs
│   └── urbit/
│       └── vere/       # Our device overlay (copied here)
├── external/           # External libraries
├── frameworks/         # Android framework
├── kernel/             # Linux kernel
├── out/                # Build output (~150GB)
├── packages/           # AOSP apps
├── system/             # Core system
└── vendor/
    └── google/         # Pixel vendor blobs
```

## Build Steps

### Step 1: Initialize AOSP Source

```bash
# Create working directory
mkdir -p ~/aosp && cd ~/aosp

# Initialize repo with Android 15 (latest with Pixel 8 Pro support)
repo init -u https://android.googlesource.com/platform/manifest \
    -b android-15.0.0_r29 \
    --partial-clone \
    --clone-filter=blob:limit=10M

# Sync sources (1-3 hours, ~100GB)
repo sync -c -j$(nproc) --no-tags --optimized-fetch
```

**Tip**: Use `--partial-clone` to reduce initial download. Full checkout happens on demand.

### Step 2: Extract Vendor Blobs

Pixel 8 Pro requires proprietary vendor blobs (drivers, firmware). Two options:

**Option A: From connected device**

```bash
# Connect Pixel 8 Pro via USB with ADB debugging
adb root
adb remount

# Clone extraction tool
cd ~
git clone https://github.com/anestisb/android-prepare-vendor

# Extract (requires rsync)
./android-prepare-vendor/execute-all.sh -d husky -b <BUILD_ID> -o vendor-out

# BUILD_ID example: AP3A.241005.015 (from Settings > About > Build number)
```

**Option B: From factory image**

```bash
# Download factory image from https://developers.google.com/android/images#husky
# Look for "husky" (Pixel 8 Pro)

cd ~
git clone https://github.com/anestisb/android-prepare-vendor
./android-prepare-vendor/execute-all.sh \
    -d husky \
    -b AP3A.241005.015 \
    -f /path/to/husky-ap3a.241005.015-factory.zip \
    -o vendor-out
```

Then copy to AOSP source:

```bash
cp -r vendor-out/husky/<BUILD_ID>/vendor/google ~/aosp/vendor/google
cp -r vendor-out/husky/<BUILD_ID>/vendor/google_devices ~/aosp/vendor/google_devices
```

### Step 3: Add Vere Device Overlay

Copy the urbit-mobile device overlay:

```bash
cd ~/aosp

# Create device directory
mkdir -p device/urbit/vere

# Copy overlay from urbit-mobile
cp -r /path/to/urbit-mobile/aosp/device-overlay/* device/urbit/vere/

# Add the patched vere binary
mkdir -p device/urbit/vere/prebuilt/arm64
cp /path/to/urbit-patched device/urbit/vere/prebuilt/arm64/urbit
```

### Step 4: Build

```bash
cd ~/aosp

# Initialize build environment
source build/envsetup.sh

# Select target (vere_husky = Pixel 8 Pro with vere)
lunch vere_husky-userdebug

# Build (2-4 hours)
m -j$(nproc)
```

Build variants:
- `userdebug`: Development build with root access (recommended for testing)
- `user`: Production build without root
- `eng`: Engineering build with extra debugging

### Step 5: Flash

```bash
# Connect Pixel 8 Pro in bootloader mode
adb reboot bootloader

# Flash (WARNING: wipes device)
cd ~/aosp/out/target/product/husky
fastboot flashall -w
```

## Common Errors and Fixes

### Error: ucepresencelib not found

```
error: cannot find package com.google.android.ucepresencelib
```

**Cause**: Missing proprietary stub library.

**Fix**: Create a stub module:

```bash
mkdir -p ~/aosp/packages/apps/UCEPresenceLibStub

cat > ~/aosp/packages/apps/UCEPresenceLibStub/Android.bp << 'EOF'
android_library {
    name: "com.google.android.ucepresencelib",
    sdk_version: "current",
    srcs: ["src/**/*.java"],
    static_libs: [],
}
EOF

mkdir -p ~/aosp/packages/apps/UCEPresenceLibStub/src/com/google/android/ucepresence
cat > ~/aosp/packages/apps/UCEPresenceLibStub/src/com/google/android/ucepresence/Stub.java << 'EOF'
package com.google.android.ucepresence;
public class Stub {}
EOF
```

### Error: Build out of disk space

```
No space left on device
```

**Fix**: Need 300GB+ free space. The `out/` directory alone can reach 150GB.

```bash
# Clear previous build
rm -rf ~/aosp/out

# Or clean specific target
cd ~/aosp && source build/envsetup.sh && lunch vere_husky-userdebug
m clean
```

### Error: rsync not found

```
rsync: command not found
```

**Fix**: 
```bash
sudo apt install rsync
```

### Error: Java version mismatch

```
error: unsupported class file version
```

**Fix**: AOSP 15 requires JDK 17:

```bash
sudo apt install openjdk-17-jdk
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
```

### Error: Permission denied during flash

```
FAILED (remote: 'Flashing is not allowed')
```

**Fix**: Enable OEM unlocking:

1. Settings > System > Developer options
2. Enable "OEM unlocking"
3. `adb reboot bootloader`
4. `fastboot flashing unlock`
5. Confirm on device

### Error: SELinux denials

After boot, check for vere-related SELinux denials:

```bash
adb shell dmesg | grep -i vere
adb shell dmesg | grep -i denied
```

If vere is blocked, add rules to `device/urbit/vere/sepolicy/vere.te`.

### Error: Build takes forever

Ensure you have enough RAM and use parallel jobs:

```bash
# Check available memory
free -h

# Use swap if needed
sudo fallocate -l 16G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile

# Limit jobs to prevent OOM (cores - 2)
m -j$(( $(nproc) - 2 ))
```

## Verifying the Build

After flashing:

```bash
# Check vere is running
adb shell ps -ef | grep vere

# Check logs
adb logcat | grep vere

# Check init service status
adb shell getprop init.svc.vere

# Check SELinux context
adb shell ps -eZ | grep vere
# Should show: u:r:vere:s0
```

## Updating Vere Binary

To update just the vere binary without full rebuild:

```bash
# Update binary
cp /path/to/new/urbit-patched device/urbit/vere/prebuilt/arm64/urbit

# Incremental build
source build/envsetup.sh
lunch vere_husky-userdebug
m vere -j$(nproc)

# Push to device
adb root
adb remount
adb push out/target/product/husky/system/bin/vere /system/bin/
adb shell restorecon /system/bin/vere
adb shell stop vere && adb shell start vere
```

## CI/CD Builds

For automated builds, see `.github/workflows/aosp.yml`:

```bash
# Trigger manual workflow
gh workflow run aosp.yml \
    -f android_version=android-15.0.0_r29 \
    -f build_variant=userdebug
```

Requires a self-hosted runner with 300GB+ disk space.

## Next Steps

After a successful build:

1. **Test vere**: Verify ship boots and Ames connects
2. **Custom launcher**: Replace Pixel Launcher with Urbit launcher (Phase 3)
3. **System apps**: Pre-install Grove Mobile, etc. (Phase 4)
4. **OTA server**: Set up your own update infrastructure
5. **Multi-device**: Add support for other Pixel devices

## References

- [AOSP Build Documentation](https://source.android.com/docs/setup/build)
- [Pixel Factory Images](https://developers.google.com/android/images)
- [android-prepare-vendor](https://github.com/anestisb/android-prepare-vendor)
- [urbit-mobile README](../README.md)
- [Phase 2 Guide](../aosp/README.md)
