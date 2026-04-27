#!/bin/bash
set -uo pipefail

# Test lens API functionality
# Validates basic dojo commands

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
LENS="$SCRIPT_DIR/lens.sh"
PASSED=0
FAILED=0

run_test() {
    local name="$1"
    local expected="$2"
    local cmd="$3"

    local result
    if result=$("$LENS" "$cmd" 2>/dev/null); then
        if [ "$result" = "$expected" ]; then
            echo "✓ $name"
            PASSED=$((PASSED + 1))
            return 0
        fi
    fi
    echo "✗ $name (expected: $expected, got: ${result:-ERROR})"
    FAILED=$((FAILED + 1))
    return 1
}

echo "=== Lens API Tests ==="
echo ""

run_test "Addition" "2" "(add 1 1)"
run_test "Multiplication" "42" "(mul 6 7)"
run_test "Subtraction" "5" "(sub 10 5)"

# Ship identity
echo ""
echo "Checking ship identity..."
SHIP=$("$LENS" "our" 2>/dev/null || echo "")
if [ -n "$SHIP" ]; then
    echo "✓ Ship: $SHIP"
    PASSED=$((PASSED + 1))
else
    echo "✗ Ship identity failed"
    FAILED=$((FAILED + 1))
fi

echo ""
echo "=== Results: $PASSED passed, $FAILED failed ==="

[ "$FAILED" -eq 0 ]
