#!/usr/bin/env python3
"""
Patch statically-linked musl binary to use /tmp/resolv.conf instead of /etc/resolv.conf.

musl libc hardcodes /etc/resolv.conf for DNS resolution. On Android, /etc is a symlink
to /system/etc which is read-only and has no resolv.conf. This patches the string in-place
(same length, so no offset shifts).

Usage:
    python3 patch-dns.py <path-to-urbit-binary>
    # Creates <path>-patched alongside the original
"""
import sys
import shutil

if len(sys.argv) != 2:
    print(f"Usage: {sys.argv[0]} <urbit-binary>")
    sys.exit(1)

src = sys.argv[1]
dst = src + "-patched"

shutil.copy2(src, dst)

with open(dst, 'rb') as f:
    data = f.read()

old = b'/etc/resolv.conf'
new = b'/tmp/resolv.conf'

count = data.count(old)
if count == 0:
    print("ERROR: /etc/resolv.conf not found in binary")
    sys.exit(1)

data = data.replace(old, new)

with open(dst, 'wb') as f:
    f.write(data)

print(f"Patched {count} occurrence(s): {old.decode()} -> {new.decode()}")
print(f"Output: {dst}")
