#!/bin/bash

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

API_KEY="${API_KEY:-demo-api-key-12345}"

echo -e "${YELLOW}Checking Service Health...${NC}\n"

# Check Pod
echo -n "Pod Status: "
if podman pod exists rewards-pod 2>/dev/null; then
    echo -e "${GREEN}✅ Pod exists${NC}"
    podman pod stats --no-stream rewards-pod
else
    echo -e "${RED}❌ Pod not found${NC}"
    exit 1
fi

echo -e "\n${YELLOW}Container Health:${NC}"

# Check MongoDB
echo -n "MongoDB: "
if podman exec rewards-mongo mongosh --eval "db.adminCommand({ping:1})" --quiet 2>/dev/null; then
    echo -e "${GREEN}✅ Healthy${NC}"
else
    echo -e "${RED}❌ Unhealthy${NC}"
fi

# Check API
echo -n "API: "
if curl -s -H "X-API-Key: $API_KEY" http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Healthy${NC}"

    # Display detailed health
    echo -e "\n${YELLOW}Detailed Health Info:${NC}"
    curl -s http://localhost:8080/actuator/health | python3 -m json.tool | head -20
else
    echo -e "${RED}❌ Unhealthy${NC}"
fi

# Check endpoints
echo -e "\n${YELLOW}Testing API Endpoints:${NC}"

# Test rewards endpoint
echo -n "Rewards Endpoint: "
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -H "X-API-Key: $API_KEY" http://localhost:8080/api/customers/CUST001/rewards)
if [ "$STATUS" == "200" ]; then
    echo -e "${GREEN}✅ Working (HTTP $STATUS)${NC}"
else
    echo -e "${RED}❌ Failed (HTTP $STATUS)${NC}"
fi

# Test without API key
echo -n "Security Check: "
STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/customers/CUST001/rewards)
if [ "$STATUS" == "401" ]; then
    echo -e "${GREEN}✅ Properly secured (HTTP 401 without key)${NC}"
else
    echo -e "${RED}❌ Security issue (HTTP $STATUS without key)${NC}"
fi

echo -e "\n${GREEN}Health check complete!${NC}"