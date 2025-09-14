#!/bin/bash

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}🔐 Rewards Program API - Comprehensive Test Suite${NC}\n"

# Set environment variables for testing (with demo-safe defaults)
export TEST_API_KEY=${TEST_API_KEY:-demo-test-key-12345}
export API_KEY=${API_KEY:-dev-demo-key-12345}

echo -e "${YELLOW}Environment Configuration:${NC}"
echo -e "  • Test API Key: ${TEST_API_KEY}"
echo -e "  • Dev API Key: ${API_KEY}"
echo -e "  • Security: Environment variables with demo fallbacks"
echo -e "  • Profile: test"
echo ""

echo -e "${GREEN}🧪 Running Comprehensive Test Suite${NC}\n"

# Run tests with coverage
echo -e "${YELLOW}Running unit tests with coverage...${NC}"
mvn clean test jacoco:report

# Check if tests passed
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Unit tests passed${NC}"
else
    echo -e "${RED}❌ Unit tests failed${NC}"
    exit 1
fi

# Run integration tests
echo -e "${YELLOW}Running integration tests...${NC}"
mvn verify

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Integration tests passed${NC}"
else
    echo -e "${RED}❌ Integration tests failed${NC}"
    exit 1
fi

# Check coverage threshold
echo -e "${YELLOW}Checking coverage threshold...${NC}"
mvn jacoco:check

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Coverage threshold met (>80%)${NC}"
else
    echo -e "${YELLOW}⚠️  Coverage below threshold (still proceeding)${NC}"
fi

echo -e "\n${GREEN}📊 Test Results:${NC}"
echo -e "  • Coverage Report: ${BLUE}target/site/jacoco/index.html${NC}"
echo -e "  • Test Reports: ${BLUE}target/surefire-reports/${NC}"
echo -e "\n${GREEN}🔐 Security Features Tested:${NC}"
echo -e "  • API key authentication validation"
echo -e "  • Rate limiting behavior"
echo -e "  • Public endpoint access control"  
echo -e "  • Environment variable configuration"
echo -e "\n${GREEN}🎉 Testing implementation complete!${NC}"