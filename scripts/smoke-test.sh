#!/bin/bash
set -euo pipefail

# Quick smoke test — validates the whole stack is working
# Runs in ~30 seconds, good for CI or post-deploy validation

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
FAILED=0

pass() { echo -e "\033[0;32m✓\033[0m $1"; }
fail() { echo -e "\033[0;31m✗\033[0m $1"; FAILED=$((FAILED + 1)); }
info() { echo -e "\033[1;33m→\033[0m $1"; }

echo "=== Smoke Test ==="
echo ""

# 1. Device connection
info "Checking device..."
if adb get-state &>/dev/null; then
    pass "Device connected"
else
    fail "No device"
    exit 1
fi

# 2. Root access
info "Checking root..."
if adb shell "su -c 'id'" 2>/dev/null | grep -q "uid=0"; then
    pass "Root access"
else
    fail "No root"
    exit 1
fi

# 3. Vere process
info "Checking vere process..."
PROCS=$(adb shell "ps -ef | grep 'urbit' | grep -v grep | grep -v launcher | wc -l" | tr -d '\r')
if [ "$PROCS" -ge 2 ]; then
    pass "Vere running ($PROCS processes)"
else
    fail "Vere not running"
fi

# 4. Lens connectivity
info "Checking lens API..."
adb forward tcp:12321 tcp:12321 2>/dev/null || true
if SHIP=$("$SCRIPT_DIR/lens.sh" "our" 2>/dev/null); then
    pass "Lens responding: $SHIP"
else
    fail "Lens not responding"
fi

# 5. Ames connectivity
info "Checking Ames..."
AMES_LOG=$(adb shell "su -c 'grep \"ames: live\" /data/vere/urbit.log | tail -1'" 2>/dev/null | tr -d '\r')
if echo "$AMES_LOG" | grep -q "ames: live"; then
    PORT=$(echo "$AMES_LOG" | grep -oE '[0-9]+$')
    pass "Ames live on port $PORT"
else
    fail "Ames not detected"
fi

# 6. HTTP connectivity
info "Checking HTTP..."
adb forward tcp:8080 tcp:80 2>/dev/null || true
if curl -s -o /dev/null -w "%{http_code}" http://localhost:8080 2>/dev/null | grep -qE "2[0-9][0-9]|401|302"; then
    pass "HTTP responding"
else
    fail "HTTP not responding"
fi

# 7. Dojo execution
info "Checking dojo execution..."
if RESULT=$("$SCRIPT_DIR/lens.sh" "(add 123 456)" 2>/dev/null) && [ "$RESULT" = "579" ]; then
    pass "Dojo works (123+456=$RESULT)"
else
    fail "Dojo failed"
fi

# 8. Grove agent (if installed)
info "Checking Grove..."
if "$SCRIPT_DIR/lens.sh" ":grove +dbug" -t 15 2>/dev/null | grep -q "grove"; then
    pass "Grove agent running"
else
    info "Grove not installed or not responding (skipped)"
fi

# 9. Memory check
info "Checking memory..."
PID=$(adb shell "ps -ef | grep 'urbit work' | grep -v grep | head -1 | awk '{print \$2}'" 2>/dev/null | tr -d '\r')
if [ -n "$PID" ]; then
    RSS=$(adb shell "su -c 'cat /proc/$PID/status'" 2>/dev/null | grep VmRSS | awk '{print $2}')
    RSS_MB=$((RSS / 1024))
    if [ "$RSS_MB" -lt 2000 ]; then
        pass "Memory OK (${RSS_MB}MB RSS)"
    else
        fail "Memory high (${RSS_MB}MB RSS)"
    fi
fi

# 10. Battery check
info "Checking battery..."
BATT=$(adb shell "dumpsys battery" 2>/dev/null | grep "^  level:" | awk '{print $2}' | tr -d '\r')
if [ "$BATT" -gt 20 ]; then
    pass "Battery OK (${BATT}%)"
else
    fail "Battery low (${BATT}%)"
fi

echo ""
echo "=== Results ==="
if [ "$FAILED" -eq 0 ]; then
    echo -e "\033[0;32mAll tests passed!\033[0m"
    exit 0
else
    echo -e "\033[0;31m$FAILED test(s) failed\033[0m"
    exit 1
fi
