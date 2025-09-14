# Podman Deployment Guide

## Prerequisites

- Podman installed (`brew install podman`)
- Podman machine running (`podman machine start`)
- Java 17 and Maven installed

## Quick Start

### 1. Deploy Everything

    ./deploy-with-podman.sh

This script will:
- Check/start Podman machine
- Build the application image
- Create a pod with MongoDB and API
- Wait for services to be healthy
- Display access information

### 2. Check Health

    ./health-check.sh

### 3. View Logs

    # All logs
    podman pod logs -f rewards-pod

    # API logs only
    podman logs -f rewards-api

    # MongoDB logs only
    podman logs -f rewards-mongo

### 4. Run Tests

    ./test-api.sh

### 5. Stop Services

    podman pod stop rewards-pod

## Manual Commands

### Build Image

    podman build -f Containerfile -t localhost/rewards-api:latest .

### Create and Run Pod

    # Create pod
    podman pod create --name rewards-pod -p 8080:8080 -p 27017:27017

    # Run MongoDB
    podman run -d --pod rewards-pod --name rewards-mongo \
      -e MONGO_INITDB_ROOT_USERNAME=admin \
      -e MONGO_INITDB_ROOT_PASSWORD=secret \
      docker.io/library/mongo:7

    # Run API
    podman run -d --pod rewards-pod --name rewards-api \
      -e MONGODB_URI=mongodb://admin:secret@localhost:27017/rewardsdb?authSource=admin \
      -e API_KEY=demo-api-key-12345 \
      localhost/rewards-api:latest

### Pod Management

    # List pods
    podman pod list

    # Show pod details
    podman pod inspect rewards-pod

    # Stop pod
    podman pod stop rewards-pod

    # Remove pod (and all containers)
    podman pod rm rewards-pod

    # Stats
    podman pod stats rewards-pod

## Kubernetes Export

Generate Kubernetes manifests from running pod:

    podman generate kube rewards-pod > k8s-deployment.yaml

## Environment Variables

Edit `.env.podman` to customize:
- MongoDB credentials
- API key
- JVM settings

## API Usage

### Authentication

All API endpoints require the `X-API-Key` header:

    curl -H "X-API-Key: charter-demo-api-key-2024" \
      http://localhost:8080/api/customers/CUST001/rewards

### Available Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/actuator/health` | Health check (no auth) |
| GET | `/swagger-ui/index.html` | API documentation (no auth) |
| GET | `/api/customers/{id}/rewards` | Get total rewards |
| GET | `/api/customers/{id}/rewards/{month}` | Get monthly rewards |
| GET | `/api/customers/{id}/rewards/recent?months=N` | Get recent rewards |
| POST | `/api/transactions` | Create transaction |
| GET | `/api/transactions/customer/{id}` | List transactions |

### Example Requests

    # Get customer rewards
    curl -H "X-API-Key: charter-demo-api-key-2024" \
      http://localhost:8080/api/customers/CUST001/rewards

    # Create transaction
    curl -X POST \
      -H "X-API-Key: charter-demo-api-key-2024" \
      -H "Content-Type: application/json" \
      -d '{"customerId":"CUST001","amount":120.00,"description":"Purchase"}' \
      http://localhost:8080/api/transactions

## Troubleshooting

### Podman machine not running (macOS)

    podman machine start

### Port already in use

    # Find process using port
    lsof -i :8080

    # Kill process or change port in pod creation

### MongoDB connection issues

    # Check MongoDB logs
    podman logs rewards-mongo

    # Test MongoDB directly
    podman exec -it rewards-mongo mongosh

### API not starting

    # Check API logs
    podman logs rewards-api

    # Check environment variables
    podman inspect rewards-api | grep -A 10 Env

### Security Issues

    # Test without API key (should return 401)
    curl -i http://localhost:8080/api/customers/CUST001/rewards

    # Test with invalid API key (should return 401)
    curl -i -H "X-API-Key: invalid" http://localhost:8080/api/customers/CUST001/rewards

## Production Considerations

### Security
- Change default API keys in production
- Use environment-specific configuration
- Enable HTTPS with proper certificates
- Configure proper CORS settings

### Performance
- Adjust JVM heap settings based on load
- Monitor memory and CPU usage
- Scale horizontally with load balancer

### Monitoring
- Use `/actuator/metrics` for Prometheus monitoring
- Set up log aggregation
- Configure alerting on health check failures

### Backup
- Regular MongoDB backups
- Container registry for images
- Configuration backup