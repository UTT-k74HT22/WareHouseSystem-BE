#!/bin/bash
# =============================================================================
# WHS - Race Condition Test Runner
# Chạy concurrent inventory reserve requests để test race condition
#
# USAGE:
#   bash docs/newman/run-concurrent-inventory.sh [CONCURRENCY] [ORDER_IDS_FILE]
#
# EXAMPLES:
#   # Test với 15 concurrent confirms (requires order IDs to be set)
#   bash docs/newman/run-concurrent-inventory.sh 15
#
#   # Test increase/decrease concurrent
#   bash docs/newman/run-concurrent-inventory.sh increase 20
#
# FLOW:
#   1. Setup: Add 10 units stock
#   2. Concurrent: 15 processes cùng lúc confirm different orders
#   3. Verify: Chỉ 10 succeed, 5 conflict, stock consistent
# =============================================================================

BASE_DIR="$(cd "$(dirname "$0")/.." && pwd)"
POSTMAN_DIR="$BASE_DIR/postman"
REPORT_DIR="$BASE_DIR/newman/reports"
ENV_FILE="$POSTMAN_DIR/WHS_Performance_Env.postman_environment.json"
COLLECTION="$POSTMAN_DIR/02_Inventory_ConcurrentTest.postman_collection.json"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

CONCURRENCY="${1:-15}"
MODE="${2:-reserve}"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}================================================${NC}"
echo -e "${BLUE}  Inventory Race Condition Test                 ${NC}"
echo -e "${BLUE}  Mode: $MODE | Concurrency: $CONCURRENCY       ${NC}"
echo -e "${BLUE}================================================${NC}"

mkdir -p "$REPORT_DIR"

# =============================================================================
# PHASE 1: Sequential Setup
# =============================================================================
echo -e "\n${YELLOW}[PHASE 1] Sequential Setup...${NC}"

newman run "$COLLECTION" \
    -e "$ENV_FILE" \
    --folder "00 - Setup & Auth" \
    --folder "01 - Setup Initial Stock" \
    --reporters cli \
    --timeout-request 30000 \
    || { echo -e "${RED}Setup failed!${NC}"; exit 1; }

echo -e "${GREEN}Setup complete. Initial stock set.${NC}"

# =============================================================================
# PHASE 2: Sequential Order Creation
# =============================================================================
echo -e "\n${YELLOW}[PHASE 2] Creating $CONCURRENCY sales orders sequentially...${NC}"

newman run "$COLLECTION" \
    -e "$ENV_FILE" \
    --folder "02 - Create Sales Orders for Race Condition" \
    --iteration-count "$CONCURRENCY" \
    --delay-request 100 \
    --reporters cli \
    --timeout-request 30000 \
    || { echo -e "${RED}Order creation failed!${NC}"; exit 1; }

echo -e "${GREEN}Created $CONCURRENCY sales orders.${NC}"

# =============================================================================
# PHASE 3: Concurrent Confirm (Race Condition)
# =============================================================================
echo -e "\n${YELLOW}[PHASE 3] Starting $CONCURRENCY concurrent confirms (RACE CONDITION TEST)...${NC}"
echo "This tests whether distributed locks prevent overselling."
echo ""

PIDS=()
SUCCESS_COUNT=0
FAIL_COUNT=0

# Lưu kết quả mỗi process vào temp file
TEMP_DIR=$(mktemp -d)

for i in $(seq 1 "$CONCURRENCY"); do
    {
        local_report="$TEMP_DIR/result_$i.txt"

        # Mỗi process confirm order thứ i
        newman run "$POSTMAN_DIR/03_SalesOrder_ConcurrentTest.postman_collection.json" \
            -e "$ENV_FILE" \
            --folder "03 - Concurrent Confirms (Race Condition for Limited Stock)" \
            --env-var "confirm_loop_counter=$((i - 1))" \
            --iteration-count 1 \
            --reporters cli \
            --timeout-request 30000 \
            > "$local_report" 2>&1

        exit_code=$?
        echo "$exit_code" > "$TEMP_DIR/exit_$i.txt"
    } &

    PIDS+=($!)
done

echo "Waiting for all $CONCURRENCY concurrent processes to complete..."

# Đợi tất cả hoàn thành
for pid in "${PIDS[@]}"; do
    wait "$pid"
done

echo -e "${GREEN}All concurrent processes completed.${NC}"

# Đếm kết quả
for i in $(seq 1 "$CONCURRENCY"); do
    exit_file="$TEMP_DIR/exit_$i.txt"
    if [ -f "$exit_file" ]; then
        code=$(cat "$exit_file")
        if [ "$code" = "0" ]; then
            SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
        else
            FAIL_COUNT=$((FAIL_COUNT + 1))
        fi
    fi
done

rm -rf "$TEMP_DIR"

# =============================================================================
# PHASE 4: Verify Consistency
# =============================================================================
echo -e "\n${YELLOW}[PHASE 4] Verifying inventory consistency...${NC}"

newman run "$COLLECTION" \
    -e "$ENV_FILE" \
    --folder "06 - Verify Consistency" \
    --reporters cli,htmlextra \
    --reporter-htmlextra-export "$REPORT_DIR/${TIMESTAMP}_RaceCondition_Verify.html" \
    --timeout-request 30000

# =============================================================================
# SUMMARY
# =============================================================================
echo -e "\n${BLUE}================================================${NC}"
echo -e "${BLUE}  RACE CONDITION TEST RESULTS                   ${NC}"
echo -e "${BLUE}================================================${NC}"
echo -e "Concurrency level:    $CONCURRENCY processes"
echo -e "Stock available:      10 units (from setup)"
echo ""
echo -e "Process results:"
echo -e "  ${GREEN}Successes: $SUCCESS_COUNT${NC}"
echo -e "  ${RED}Failures: $FAIL_COUNT${NC}"
echo ""

# Validation
if [ "$SUCCESS_COUNT" -le 10 ] && [ "$FAIL_COUNT" -ge 5 ]; then
    echo -e "${GREEN}✓ RACE CONDITION PROTECTION WORKING${NC}"
    echo "  No overselling detected. At most 10 succeeded (stock limit)."
    OVERALL="PASS"
else
    echo -e "${RED}✗ POTENTIAL RACE CONDITION ISSUE${NC}"
    echo "  Expected: <= 10 successes, >= 5 failures (for $CONCURRENCY concurrent with 10 stock)"
    echo "  Got: $SUCCESS_COUNT successes, $FAIL_COUNT failures"
    OVERALL="FAIL"
fi

echo ""
echo "Verification report: $REPORT_DIR/${TIMESTAMP}_RaceCondition_Verify.html"
echo ""

if [ "$OVERALL" = "FAIL" ]; then
    exit 1
fi
