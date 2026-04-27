#!/bin/bash
set -euo pipefail

# Reliable lens API client
# Automatically cancels any stuck job before sending commands
#
# Usage:
#   ./scripts/lens.sh "(add 1 1)"           # dojo command
#   ./scripts/lens.sh -r "/~mapnus-labdem-palrum-roclur"  # raw hoon
#   ./scripts/lens.sh -t 30 ":grove +dbug"  # with 30s timeout
#   LENS_HOST=192.168.1.100 ./scripts/lens.sh "(add 1 1)"  # remote host

HOST="${LENS_HOST:-127.0.0.1}"
PORT="${LENS_PORT:-12321}"
URL="http://${HOST}:${PORT}"
TIMEOUT="${LENS_TIMEOUT:-10}"

usage() {
    echo "Usage: $0 [-r] [-t timeout] <command>"
    echo "  -r          Raw hoon (instead of dojo)"
    echo "  -t SECONDS  Request timeout (default: 10)"
    echo ""
    echo "Examples:"
    echo "  $0 '(add 1 1)'                  # dojo: returns 2"
    echo "  $0 -r '/~zod'                   # raw hoon"
    echo "  $0 '.^(arch %cy /)'             # scry clay root"
    echo "  $0 -t 30 ':grove +dbug'         # 30s timeout for slow commands"
    exit 1
}

RAW=false
while getopts "rt:h" opt; do
    case $opt in
        r) RAW=true ;;
        t) TIMEOUT="$OPTARG" ;;
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
RESPONSE=$(curl -s --max-time "$TIMEOUT" -X POST -H 'Content-Type: application/json' \
    -d "{\"source\":$SOURCE,\"sink\":{\"stdout\":null}}" \
    "$URL" 2>&1) || {
    echo "Error: request timed out after ${TIMEOUT}s" >&2
    echo "Try increasing timeout with: -t 30" >&2
    exit 1
}

# Check for error response
if echo "$RESPONSE" | grep -q "Internal Server Error"; then
    echo "Error: lens returned 500" >&2
    echo "$RESPONSE" >&2
    exit 1
fi

# Output the result (strip quotes if it's a simple string)
echo "$RESPONSE" | jq -r 'if type == "string" then . else . end' 2>/dev/null || echo "$RESPONSE"
