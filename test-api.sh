#!/bin/bash

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

API_KEY="${API_KEY:-demo-api-key-12345}"
BASE_URL="http://localhost:8080"

echo -e "${BLUE}═══════════════════════════════════════════════${NC}"
echo -e "${BLUE}     Rewards API Test Suite${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════${NC}\n"

# Function to test endpoint
test_endpoint() {
    local method=$1
    local endpoint=$2
    local data=$3
    local description=$4

    echo -e "${YELLOW}Testing: $description${NC}"
    echo "  Method: $method"
    echo "  Endpoint: $endpoint"

    if [ "$method" == "GET" ]; then
        response=$(curl -s -H "X-API-Key: $API_KEY" "$BASE_URL$endpoint")
    else
        response=$(curl -s -X $method -H "X-API-Key: $API_KEY" -H "Content-Type: application/json" -d "$data" "$BASE_URL$endpoint")
    fi

    echo "  Response:"
    echo "$response" | python3 -m json.tool | head -10
    echo ""
}

# Test health endpoint (no API key needed)
echo -e "${GREEN}1. Health Check${NC}"
curl -s $BASE_URL/actuator/health | python3 -m json.tool | head -15
echo ""

# Test security
echo -e "${GREEN}2. Security Test (without API key - should fail)${NC}"
curl -i $BASE_URL/api/customers/CUST001/rewards 2>/dev/null | head -1
echo -e "\n"

# Test rewards endpoints
echo -e "${GREEN}3. Rewards Endpoints${NC}\n"

test_endpoint "GET" "/api/customers/CUST001/rewards" "" "Get total rewards"
test_endpoint "GET" "/api/customers/CUST001/rewards/recent?months=3" "" "Get recent rewards"
test_endpoint "GET" "/api/customers/CUST002/rewards" "" "Get rewards for another customer"

# Test transaction creation
echo -e "${GREEN}4. Transaction Creation${NC}\n"

test_endpoint "POST" "/api/transactions" \
    '{"customerId":"CUST001","amount":250.00,"description":"Test purchase"}' \
    "Create new transaction"

# Test transactions list
test_endpoint "GET" "/api/transactions/customer/CUST001" "" "List customer transactions"

# Test rate limiting
echo -e "${GREEN}5. Rate Limiting Test${NC}"
echo "Sending 10 rapid requests..."
for i in {1..10}; do
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" -H "X-API-Key: $API_KEY" "$BASE_URL/api/customers/CUST001/rewards")
    echo -n "Request $i: HTTP $STATUS | "
done
echo ""

echo -e "\n${BLUE}═══════════════════════════════════════════════${NC}"
echo -e "${GREEN}✅ Test suite complete!${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════${NC}"