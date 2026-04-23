#!/bin/bash
# =============================================================================
# WHS - Newman Test Runner
# Chạy tất cả performance & correctness tests
#
# USAGE:
#   bash docs/newman/run-all-tests.sh
#
# PREREQUISITES:
#   1. npm install -g newman newman-reporter-html newman-reporter-htmlextra
#   2. Application đang chạy tại http://localhost:8080
#   3. Điền đúng các IDs trong WHS_Performance_Env.postman_environment.json
#   4. Đặt ADMIN_PASSWORD và ADMIN_USERNAME nếu khác default
# =============================================================================

set -e  # Thoát ngay nếu có lỗi

# --- CONFIG ---
BASE_DIR="$(cd "$(dirname "$0")/.." && pwd)"
POSTMAN_DIR="$BASE_DIR/postman"
REPORT_DIR="$BASE_DIR/newman/reports"
ENV_FILE="$POSTMAN_DIR/WHS_Performance_Env.postman_environment.json"
BASE_URL="${BASE_URL:-http://localhost:8080}"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

# --- Colors ---
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=====================================================${NC}"
echo -e "${BLUE}  WHS Performance & Correctness Test Runner          ${NC}"
echo -e "${BLUE}  Timestamp: $TIMESTAMP                               ${NC}"
echo -e "${BLUE}=====================================================${NC}"

# --- Kiểm tra prerequisites ---
echo -e "\n${YELLOW}[1/6] Checking prerequisites...${NC}"

# Kiểm tra Newman
if ! command -v newman &> /dev/null; then
    echo -e "${RED}ERROR: Newman not found. Install with: npm install -g newman newman-reporter-html${NC}"
    exit 1
fi

# Kiểm tra server health
echo -n "Checking server health at $BASE_URL... "
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/actuator/health" 2>/dev/null || echo "000")
if [ "$HTTP_STATUS" = "200" ]; then
    echo -e "${GREEN}OK (HTTP $HTTP_STATUS)${NC}"
else
    echo -e "${RED}FAIL (HTTP $HTTP_STATUS)${NC}"
    echo "Make sure the application is running at $BASE_URL"
    exit 1
fi

# Kiểm tra environment file
if [ ! -f "$ENV_FILE" ]; then
    echo -e "${RED}ERROR: Environment file not found: $ENV_FILE${NC}"
    exit 1
fi

# Tạo report directory
mkdir -p "$REPORT_DIR"
echo -e "${GREEN}Prerequisites OK${NC}"

# --- Hàm chạy Newman ---
run_collection() {
    local name="$1"
    local file="$2"
    local extra_args="${3:-}"

    echo -e "\n${YELLOW}Running: $name${NC}"

    local report_file="$REPORT_DIR/${TIMESTAMP}_${name// /_}.html"

    if newman run "$file" \
        -e "$ENV_FILE" \
        --reporters cli,htmlextra \
        --reporter-htmlextra-export "$report_file" \
        --reporter-htmlextra-title "$name" \
        --reporter-htmlextra-showOnlyFails \
        --timeout-request 30000 \
        $extra_args; then
        echo -e "${GREEN}✓ $name PASSED${NC}"
        echo "  Report: $report_file"
        return 0
    else
        echo -e "${RED}✗ $name FAILED${NC}"
        echo "  Report: $report_file"
        return 1
    fi
}

FAILED_TESTS=()
PASSED_TESTS=()

# =============================================================================
# Area 3: Auth & Rate Limit
# =============================================================================
echo -e "\n${BLUE}--- Area 3: Auth & Rate Limit Tests ---${NC}"
if run_collection "Auth_RateLimit" \
    "$POSTMAN_DIR/01_Auth_RateLimit_Test.postman_collection.json"; then
    PASSED_TESTS+=("Auth & Rate Limit")
else
    FAILED_TESTS+=("Auth & Rate Limit")
fi

# Wait for rate limit window to reset (nếu cần)
echo "Waiting 5 seconds between test suites..."
sleep 5

# =============================================================================
# Area 1+2: Inventory Concurrent Tests
# =============================================================================
echo -e "\n${BLUE}--- Area 1+2: Inventory Concurrent Tests ---${NC}"
if run_collection "Inventory_Concurrent" \
    "$POSTMAN_DIR/02_Inventory_ConcurrentTest.postman_collection.json"; then
    PASSED_TESTS+=("Inventory Concurrent")
else
    FAILED_TESTS+=("Inventory Concurrent")
fi

sleep 3

# =============================================================================
# Area 4: Sales Order Concurrent Tests
# =============================================================================
echo -e "\n${BLUE}--- Area 4: Sales Order Concurrent Tests ---${NC}"
if run_collection "SalesOrder_Concurrent" \
    "$POSTMAN_DIR/03_SalesOrder_ConcurrentTest.postman_collection.json"; then
    PASSED_TESTS+=("Sales Order Concurrent")
else
    FAILED_TESTS+=("Sales Order Concurrent")
fi

sleep 3

# =============================================================================
# Area 5: RBAC Security Tests
# =============================================================================
echo -e "\n${BLUE}--- Area 5: RBAC Security Tests ---${NC}"
if run_collection "RBAC_Security" \
    "$POSTMAN_DIR/04_RBAC_SecurityTest.postman_collection.json"; then
    PASSED_TESTS+=("RBAC Security")
else
    FAILED_TESTS+=("RBAC Security")
fi

# =============================================================================
# Summary
# =============================================================================
echo -e "\n${BLUE}=====================================================${NC}"
echo -e "${BLUE}  TEST SUMMARY                                        ${NC}"
echo -e "${BLUE}=====================================================${NC}"

if [ ${#PASSED_TESTS[@]} -gt 0 ]; then
    echo -e "\n${GREEN}PASSED (${#PASSED_TESTS[@]}):${NC}"
    for t in "${PASSED_TESTS[@]}"; do
        echo -e "  ${GREEN}✓${NC} $t"
    done
fi

if [ ${#FAILED_TESTS[@]} -gt 0 ]; then
    echo -e "\n${RED}FAILED (${#FAILED_TESTS[@]}):${NC}"
    for t in "${FAILED_TESTS[@]}"; do
        echo -e "  ${RED}✗${NC} $t"
    done
fi

echo -e "\nReports saved to: $REPORT_DIR"
echo -e "Timestamp: $TIMESTAMP"

if [ ${#FAILED_TESTS[@]} -gt 0 ]; then
    echo -e "\n${RED}OVERALL: FAILED${NC}"
    exit 1
else
    echo -e "\n${GREEN}OVERALL: ALL PASSED${NC}"
    exit 0
fi
