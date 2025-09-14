#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
POD_NAME="rewards-pod"
MONGO_CONTAINER="rewards-mongo"
API_CONTAINER="rewards-api"
API_IMAGE="localhost/rewards-api:latest"
MONGO_IMAGE="docker.io/library/mongo:7"

# Load environment variables from file if it exists
if [ -f ".env.podman" ]; then
    echo -e "${YELLOW}Loading environment from .env.podman${NC}"
    export $(grep -v "^#" .env.podman | xargs)
fi

# Environment variables with defaults (demo-safe fallbacks)
MONGO_USER="${MONGO_USER:-admin}"
MONGO_PASSWORD="${MONGO_PASSWORD:-secret}"
API_KEY="${API_KEY:-charter-demo-api-key-2024}"

echo -e "${GREEN}🚀 Starting Rewards API Deployment with Podman${NC}"
echo -e "${YELLOW}Security: Using API key from environment variables${NC}\n"

# Function to check if podman machine is running
check_podman_machine() {
    if [[ "$OSTYPE" == "darwin"* ]]; then
        echo -e "${YELLOW}Checking Podman machine status...${NC}"
        if ! podman machine list | grep -q "Currently running"; then
            echo -e "${YELLOW}Starting Podman machine...${NC}"
            podman machine start
            sleep 5
        fi
    fi
}

# Function to cleanup existing deployment
cleanup() {
    echo -e "${YELLOW}Cleaning up existing deployment...${NC}"

    # Stop and remove existing pod if it exists
    if podman pod exists $POD_NAME 2>/dev/null; then
        echo "Removing existing pod..."
        podman pod stop $POD_NAME 2>/dev/null
        podman pod rm $POD_NAME 2>/dev/null
    fi

    # Remove existing containers if they exist
    podman rm -f $MONGO_CONTAINER 2>/dev/null
    podman rm -f $API_CONTAINER 2>/dev/null
}

# Function to build the application image
build_image() {
    echo -e "${GREEN}Building application image...${NC}"
    if podman build -f Containerfile -t $API_IMAGE .; then
        echo -e "${GREEN}✅ Image built successfully${NC}"
    else
        echo -e "${RED}❌ Failed to build image${NC}"
        exit 1
    fi
}

# Function to create and start the pod
deploy_pod() {
    echo -e "${GREEN}Creating pod: $POD_NAME${NC}"

    # Create pod with port mappings
    podman pod create \
        --name $POD_NAME \
        -p 8080:8080 \
        -p 27017:27017 \
        --network bridge

    echo -e "${GREEN}Starting MongoDB container...${NC}"
    podman run -d \
        --pod $POD_NAME \
        --name $MONGO_CONTAINER \
        --health-cmd="mongosh --eval 'db.adminCommand({ping:1})' --quiet" \
        --health-interval=10s \
        --health-timeout=5s \
        --health-retries=5 \
        -e MONGO_INITDB_ROOT_USERNAME=$MONGO_USER \
        -e MONGO_INITDB_ROOT_PASSWORD=$MONGO_PASSWORD \
        -e MONGO_INITDB_DATABASE=rewardsdb \
        -v rewards-mongo-data:/data/db \
        $MONGO_IMAGE

    # Wait for MongoDB to be healthy
    echo -e "${YELLOW}Waiting for MongoDB to be ready...${NC}"
    for i in {1..30}; do
        if podman exec $MONGO_CONTAINER mongosh --eval "db.adminCommand({ping:1})" --quiet 2>/dev/null; then
            echo -e "${GREEN}✅ MongoDB is ready${NC}"
            break
        fi
        echo -n "."
        sleep 2
    done

    echo -e "${GREEN}Starting API container...${NC}"
    podman run -d \
        --pod $POD_NAME \
        --name $API_CONTAINER \
        --health-cmd="wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1" \
        --health-interval=30s \
        --health-timeout=10s \
        --health-retries=3 \
        -e SPRING_PROFILES_ACTIVE=docker \
        -e MONGODB_URI="mongodb://$MONGO_USER:$MONGO_PASSWORD@localhost:27017/rewardsdb?authSource=admin" \
        -e API_KEY=$API_KEY \
        -e JAVA_OPTS="-Xmx512m -Xms256m" \
        -v ./logs:/app/logs \
        $API_IMAGE

    # Wait for API to be healthy
    echo -e "${YELLOW}Waiting for API to be ready...${NC}"
    for i in {1..30}; do
        if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
            echo -e "${GREEN}✅ API is ready${NC}"
            break
        fi
        echo -n "."
        sleep 2
    done
}

# Function to display status
show_status() {
    echo -e "\n${GREEN}═══════════════════════════════════════════════${NC}"
    echo -e "${GREEN}🎉 Deployment Complete!${NC}"
    echo -e "${GREEN}═══════════════════════════════════════════════${NC}"
    echo -e "Pod Status:"
    podman pod ps
    echo -e "\nContainer Status:"
    podman ps --pod --filter pod=$POD_NAME
    echo -e "\n${YELLOW}Access Points:${NC}"
    echo -e "  • API Health: ${GREEN}http://localhost:8080/actuator/health${NC}"
    echo -e "  • Swagger UI: ${GREEN}http://localhost:8080/swagger-ui/index.html${NC}"
    echo -e "  • MongoDB: ${GREEN}localhost:27017${NC}"
    echo -e "\n${YELLOW}API Key:${NC} $API_KEY"
    echo -e "\n${YELLOW}Test Command:${NC}"
    echo -e "  curl -H \"X-API-Key: $API_KEY\" http://localhost:8080/api/customers/CUST001/rewards"
    echo -e "${GREEN}═══════════════════════════════════════════════${NC}"
}

# Main execution
main() {
    check_podman_machine
    cleanup
    build_image
    deploy_pod
    show_status
}

# Run main function
main