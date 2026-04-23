#!/bin/bash
# =============================================================================
# WHS - Load & Performance Test
# Test response times và system behavior dưới sustained load
#
# USAGE:
#   bash docs/newman/run-load-test.sh [CONCURRENT_USERS] [DURATION_SECONDS]
#
# EXAMPLES:
#   bash docs/newman/run-load-test.sh          # 30 users, 60 giây
#   bash docs/newman/run-load-test.sh 50 120   # 50 users, 120 giây
# =============================================================================

BASE_DIR="$(cd "$(dirname "$0")/.." && pwd)"
POSTMAN_DIR="$BASE_DIR/postman"
REPORT_DIR="$BASE_DIR/newman/reports"
ENV_FILE="$POSTMAN_DIR/WHS_Performance_Env.postman_environment.json"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

CONCURRENT_USERS="${1:-30}"
DURATION_SECONDS="${2:-60}"
ITERATIONS_PER_USER=$(( DURATION_SECONDS / 2 ))  # 1 request mỗi 2 giây

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}================================================${NC}"
echo -e "${BLUE}  WHS Load Test                                 ${NC}"
echo -e "${BLUE}  Users: $CONCURRENT_USERS | Duration: ${DURATION_SECONDS}s   ${NC}"
echo -e "${BLUE}================================================${NC}"

mkdir -p "$REPORT_DIR"

# Kiểm tra server
if ! curl -s --max-time 5 http://localhost:8080/actuator/health > /dev/null; then
    echo -e "${RED}ERROR: Server not responding!${NC}"
    exit 1
fi

# Login trước để lấy token
echo "Getting auth token..."
TOKEN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"Admin@123"}')

ACCESS_TOKEN=$(echo "$TOKEN_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['access_token'])" 2>/dev/null || \
               echo "$TOKEN_RESPONSE" | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)

if [ -z "$ACCESS_TOKEN" ]; then
    echo -e "${YELLOW}WARNING: Could not extract token. Check admin credentials.${NC}"
fi

echo -e "${GREEN}Token obtained. Starting load test...${NC}"

# =============================================================================
# CONCURRENT LOAD TEST
# =============================================================================

echo -e "\n${YELLOW}Starting $CONCURRENT_USERS concurrent users for ${DURATION_SECONDS}s...${NC}"

PIDS=()
RESPONSE_TIMES_FILE="$REPORT_DIR/${TIMESTAMP}_response_times.txt"
echo "" > "$RESPONSE_TIMES_FILE"

START_TIME=$(date +%s)

for user_id in $(seq 1 "$CONCURRENT_USERS"); do
    {
        user_iter=0
        while true; do
            current_time=$(date +%s)
            elapsed=$((current_time - START_TIME))

            if [ $elapsed -ge $DURATION_SECONDS ]; then
                break
            fi

            user_iter=$((user_iter + 1))

            # GET inventories - Read load
            response=$(curl -s -o /dev/null \
                -w "%{http_code},%{time_total}" \
                --max-time 10 \
                "http://localhost:8080/api/v1/inventories?page=0&size=10" \
                -H "Authorization: Bearer $ACCESS_TOKEN" \
                2>/dev/null)

            http_code=$(echo "$response" | cut -d',' -f1)
            time_total=$(echo "$response" | cut -d',' -f2)
            time_ms=$(echo "$time_total * 1000" | bc 2>/dev/null || echo "$time_total")

            echo "user=$user_id,iter=$user_iter,http=$http_code,time=${time_ms}ms" >> "$RESPONSE_TIMES_FILE"

            sleep 1
        done
    } &
    PIDS+=($!)
done

echo "Load test running... (${DURATION_SECONDS}s)"

# Monitor trong khi test
for i in $(seq 1 $((DURATION_SECONDS / 10))); do
    sleep 10
    # Lấy HikariCP metrics
    POOL_METRICS=$(curl -s "http://localhost:8080/actuator/metrics/hikaricp.connections.active" \
        -H "Authorization: Bearer $ACCESS_TOKEN" 2>/dev/null)
    ACTIVE_CONNS=$(echo "$POOL_METRICS" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['measurements'][0]['value'])" 2>/dev/null || echo "N/A")
    echo "  [${i}0s] Active DB connections: $ACTIVE_CONNS"
done

# Đợi tất cả users hoàn thành
for pid in "${PIDS[@]}"; do
    wait "$pid"
done

# =============================================================================
# ANALYZE RESULTS
# =============================================================================
echo -e "\n${YELLOW}Analyzing results...${NC}"

if [ -f "$RESPONSE_TIMES_FILE" ] && [ -s "$RESPONSE_TIMES_FILE" ]; then
    TOTAL_REQUESTS=$(wc -l < "$RESPONSE_TIMES_FILE")
    SUCCESS_200=$(grep -c ",http=200," "$RESPONSE_TIMES_FILE" 2>/dev/null || echo 0)
    FAIL_429=$(grep -c ",http=429," "$RESPONSE_TIMES_FILE" 2>/dev/null || echo 0)
    FAIL_5XX=$(grep -c ",http=5" "$RESPONSE_TIMES_FILE" 2>/dev/null || echo 0)
    FAIL_000=$(grep -c ",http=000," "$RESPONSE_TIMES_FILE" 2>/dev/null || echo 0)

    ERROR_RATE=$(echo "scale=2; ($FAIL_5XX + $FAIL_000) * 100 / $TOTAL_REQUESTS" | bc 2>/dev/null || echo "N/A")

    echo -e "\n${BLUE}================================================${NC}"
    echo -e "${BLUE}  LOAD TEST RESULTS                             ${NC}"
    echo -e "${BLUE}================================================${NC}"
    echo "Total requests:   $TOTAL_REQUESTS"
    echo "HTTP 200:         $SUCCESS_200"
    echo "HTTP 429:         $FAIL_429 (rate limited - expected)"
    echo "HTTP 5xx:         $FAIL_5XX"
    echo "Timeout (000):    $FAIL_000"
    echo "Error rate:       ${ERROR_RATE}%"
    echo ""

    # Tính response time stats nếu có bc
    if command -v python3 &> /dev/null; then
        python3 << 'EOF'
import re, sys

times = []
with open(sys.argv[1] if len(sys.argv) > 1 else '/dev/stdin') as f:
    for line in f:
        m = re.search(r'time=(\d+(?:\.\d+)?)ms', line)
        if m:
            times.append(float(m.group(1)))

if times:
    times.sort()
    n = len(times)
    print(f"Response Times (ms):")
    print(f"  Min:  {min(times):.0f}ms")
    print(f"  P50:  {times[n//2]:.0f}ms")
    print(f"  P90:  {times[int(n*0.9)]:.0f}ms")
    print(f"  P95:  {times[int(n*0.95)]:.0f}ms")
    print(f"  P99:  {times[int(n*0.99)]:.0f}ms")
    print(f"  Max:  {max(times):.0f}ms")

    p95 = times[int(n*0.95)]
    if p95 < 300:
        print(f"\n  P95={p95:.0f}ms < 300ms → EXCELLENT")
    elif p95 < 1000:
        print(f"\n  P95={p95:.0f}ms < 1000ms → ACCEPTABLE")
    else:
        print(f"\n  P95={p95:.0f}ms > 1000ms → NEEDS IMPROVEMENT")
EOF
        "$RESPONSE_TIMES_FILE"
    fi

    # Pass/Fail determination
    echo ""
    if [ "$FAIL_5XX" -gt 0 ] || [ "$FAIL_000" -gt 0 ]; then
        echo -e "${RED}✗ LOAD TEST FAILED: ${FAIL_5XX} server errors, ${FAIL_000} timeouts${NC}"
        OVERALL="FAIL"
    else
        echo -e "${GREEN}✓ LOAD TEST PASSED: No server errors under ${CONCURRENT_USERS} concurrent users${NC}"
        OVERALL="PASS"
    fi

    echo ""
    echo "Raw data: $RESPONSE_TIMES_FILE"
else
    echo -e "${RED}No response data collected${NC}"
    OVERALL="FAIL"
fi

if [ "$OVERALL" = "FAIL" ]; then
    exit 1
fi
