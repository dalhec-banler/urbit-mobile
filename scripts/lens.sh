#!/bin/bash
set -euo pipefail

# Reliable lens API client
# Automatically cancels any stuck job before sending commands
#
# Usage:
#   ./scripts/lens.sh "(add 1 1)"           # dojo command
#   ./scripts/lens.sh -r "/~mapnus-labdem-palrum-roclur"  # raw hoon
#   LENS_HOST=192.168.1.100 ./scripts/lens.sh "(add 1 1)"  # remote host

HOST="${LENS_HOST:-127.0.0.1}"
PORT="${LENS_PORT:-12321}"
URL="http://${HOST}:${PORT}"

usage() {
    echo "Usage: $0 [-r] <command>"
    echo "  -r    Raw hoon (instead of dojo)"
    echo ""
    echo "Examples:"
    echo "  $0 '(add 1 1)'                  # dojo: returns 2"
    echo "  $0 -r '/~zod'                   # raw hoon"
    echo "  $0 '.^(arch %cy /)'             # scry clay root"
    exit 1
}

RAW=false
while getopts "rh" opt; do
    case $opt in
        r) RAW=true ;;
        h) usage ;;
        *) usage ;;
    esac
done
shift $((OPTIND-1))

if [ $# -lt 1 ]; then
    usage
fi

CMD="$1"

# Cancel any stuck job first (ignore errors)
curl -s -X POST -H 'Content-Type: application/json' \
    -d '{"source":{"cancel":null},"sink":{"stdout":null}}' \
    "$URL" >/dev/null 2>&1 || true

# Build the request
if [ "$RAW" = true ]; then
    SOURCE="{\"hoon\":$(jq -n --arg c "$CMD" '$c')}"
else
    SOURCE="{\"dojo\":$(jq -n --arg c "$CMD" '$c')}"
fi

# Send the command
RESPONSE=$(curl -s -X POST -H 'Content-Type: application/json' \
    -d "{\"source\":$SOURCE,\"sink\":{\"stdout\":null}}" \
    "$URL")

# Check for error response
if echo "$RESPONSE" | grep -q "Internal Server Error"; then
    echo "Error: lens returned 500" >&2
    echo "$RESPONSE" >&2
    exit 1
fi

# Output the result (strip quotes if it's a simple string)
echo "$RESPONSE" | jq -r 'if type == "string" then . else . end' 2>/dev/null || echo "$RESPONSE"
