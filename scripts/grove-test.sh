#!/bin/bash
set -euo pipefail

# Grove API test script
# Tests Grove agent via HTTP API through ADB port forwarding

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PORT="${GROVE_PORT:-8080}"
HOST="${GROVE_HOST:-localhost}"
BASE_URL="http://${HOST}:${PORT}"
COOKIE_FILE="/tmp/grove-test-cookie.txt"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

pass() { echo -e "${GREEN}✓${NC} $1"; }
fail() { echo -e "${RED}✗${NC} $1"; exit 1; }
info() { echo -e "${YELLOW}→${NC} $1"; }

echo "=== Grove API Tests ==="
echo "Base URL: $BASE_URL"
echo ""

# Check port forwarding
info "Setting up ADB port forwarding..."
adb forward tcp:$PORT tcp:80 2>/dev/null || true

# Get ship code
info "Getting ship code..."
CODE=$("$SCRIPT_DIR/lens.sh" "+code" 2>/dev/null) || fail "Could not get ship code"
echo "Code: ${CODE:0:10}..."

# Authenticate
info "Authenticating..."
AUTH_RESULT=$(curl -s -c "$COOKIE_FILE" -X POST -d "password=$CODE" "$BASE_URL/~/login" 2>/dev/null)
if [ -z "$AUTH_RESULT" ]; then
    fail "Authentication failed"
fi
pass "Authenticated"

# Get ship name
info "Getting ship name..."
SHIP_NAME=$(curl -s -b "$COOKIE_FILE" "$BASE_URL/~/name" 2>/dev/null | tr -d '"')
echo "Ship: $SHIP_NAME"

# Test Grove scries
echo ""
echo "--- Testing Scries ---"

test_scry() {
    local path=$1
    local name=$2
    local result=$(curl -s -b "$COOKIE_FILE" "$BASE_URL/~/scry/grove${path}.json" 2>/dev/null)
    if [ -n "$result" ]; then
        pass "$name: $(echo "$result" | head -c 50)..."
    else
        fail "$name"
    fi
}

test_scry "/files" "Files"
test_scry "/views" "Views"
test_scry "/shares" "Shares"
test_scry "/trusted" "Trusted"
test_scry "/catalogs" "Catalogs"
test_scry "/inbox" "Inbox"

# Test upload
echo ""
echo "--- Testing Upload ---"
CHANNEL="grove-test-$(date +%s)"
SHIP_PATP="${SHIP_NAME#\~}"

info "Uploading test file..."
curl -s -b "$COOKIE_FILE" -X PUT "$BASE_URL/~/channel/$CHANNEL" \
    -H "Content-Type: application/json" \
    -d "[{\"id\":1,\"action\":\"poke\",\"ship\":\"$SHIP_PATP\",\"app\":\"grove\",\"mark\":\"grove-action\",\"json\":{\"upload\":{\"name\":\"grove-test-$(date +%s).txt\",\"file-mark\":\"txt\",\"data\":\"VGVzdCBmaWxlIGZyb20gZ3JvdmUtdGVzdC5zaA==\",\"tags\":[\"test\",\"automated\"]}}}]"

sleep 2

# Verify upload
FILES=$(curl -s -b "$COOKIE_FILE" "$BASE_URL/~/scry/grove/files.json" 2>/dev/null)
FILE_COUNT=$(echo "$FILES" | jq 'length' 2>/dev/null || echo "0")
if [ "$FILE_COUNT" -gt 0 ]; then
    pass "Upload verified ($FILE_COUNT files total)"
else
    fail "Upload verification"
fi

# Test file download
echo ""
echo "--- Testing Download ---"
FILE_ID=$(echo "$FILES" | jq -r '.[0].id' 2>/dev/null)
if [ -n "$FILE_ID" ] && [ "$FILE_ID" != "null" ]; then
    CONTENT=$(curl -s -b "$COOKIE_FILE" "$BASE_URL/grove-file/$FILE_ID" 2>/dev/null)
    if [ -n "$CONTENT" ]; then
        pass "Download: ${CONTENT:0:30}..."
    else
        fail "Download"
    fi
fi

# Test share
echo ""
echo "--- Testing Share ---"
SHARES=$(curl -s -b "$COOKIE_FILE" "$BASE_URL/~/scry/grove/shares.json" 2>/dev/null)
SHARE_COUNT=$(echo "$SHARES" | jq 'length' 2>/dev/null || echo "0")
if [ "$SHARE_COUNT" -gt 0 ]; then
    TOKEN=$(echo "$SHARES" | jq -r '.[0].token' 2>/dev/null)
    PUBLIC=$(curl -s "$BASE_URL/grove-share/$TOKEN" 2>/dev/null)
    if [ -n "$PUBLIC" ]; then
        pass "Public share works: ${PUBLIC:0:30}..."
    else
        fail "Public share"
    fi
else
    info "No shares to test (create one first)"
fi

echo ""
echo "=== All tests passed ==="
