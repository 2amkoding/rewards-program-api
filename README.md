# Rewards Program API

A Spring Boot REST API for managing customer rewards program with points calculation based on transaction amounts.

## 🚀 Features

- Calculate rewards points based on transaction amounts
- Monthly and all-time rewards aggregation
- RESTful API with proper HTTP semantics
- MongoDB for scalable data storage
- Comprehensive error handling
- API documentation with Swagger UI
- Health monitoring endpoints

## 📋 Business Rules

Points are calculated as follows:
- 2 points per dollar spent over $100
- 1 point per dollar spent between $50-$100
- 0 points for amounts under $50

Example: $120 purchase = 2×20 + 1×50 = 90 points

## 🛠 Technology Stack

- **Java 17** - Programming language
- **Spring Boot 3.5.5** - Application framework
- **MongoDB 7** - NoSQL database
- **Maven** - Build tool
- **Podman/Docker** - Containerization
- **JUnit 5** - Testing framework
- **OpenAPI 3** - API documentation

## 📦 Prerequisites

- JDK 17 or higher
- Maven 3.6+
- MongoDB 7.0+ (or Podman/Docker)
- Git

## 🔧 Installation & Setup

### 1. Clone the repository

    git clone https://github.com/yourusername/rewards-program-api.git
    cd rewards-program-api

### 2. Start MongoDB

Using Podman:

    podman run -d --name rewards-mongo -p 27017:27017 mongo:7

Using Docker:

    docker run -d --name rewards-mongo -p 27017:27017 mongo:7

### 3. Configure application

The application uses environment variables for configuration. Default values are provided in `application.yml`.

To override defaults, set:

    export MONGODB_URI=mongodb://localhost:27017/rewardsdb
    export API_KEY=your-secure-api-key

### 4. Build and run

    # Build the project
    mvn clean install

    # Run the application
    mvn spring-boot:run

The application will start on `http://localhost:8080`

## 📌 API Endpoints

**🔐 Authentication Required:** All API endpoints require the `X-API-Key` header.

### Rewards Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/customers/{customerId}/rewards` | Get total rewards for a customer |
| GET | `/api/customers/{customerId}/rewards/{month}` | Get rewards for specific month |
| GET | `/api/customers/{customerId}/rewards/recent?months=N` | Get rewards for last N months |

### Transaction Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/transactions` | Create a new transaction |
| GET | `/api/transactions/customer/{customerId}` | Get all transactions for a customer |

### Health & Monitoring

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/actuator/health` | Application health status |
| GET | `/actuator/metrics` | Application metrics |
| GET | `/swagger-ui/index.html` | API documentation UI |

## 🧪 Testing

### Run unit tests

    mvn test

### Run integration tests

    mvn verify

### Test coverage report

    mvn jacoco:report
    # Report available at: target/site/jacoco/index.html

### Example API calls

Get customer rewards:

    curl -H "X-API-Key: demo-api-key-12345" \
      http://localhost:8080/api/customers/CUST001/rewards

Create transaction:

    curl -X POST http://localhost:8080/api/transactions \
      -H "X-API-Key: demo-api-key-12345" \
      -H "Content-Type: application/json" \
      -d '{
        "customerId": "CUST001",
        "amount": 120.00,
        "description": "Purchase"
      }'

## 📊 Project Structure

    src/
    ├── main/
    │   ├── java/
    │   │   └── com/portalsplatform/api/
    │   │       ├── config/         # Configuration classes
    │   │       ├── controller/     # REST controllers
    │   │       ├── exception/      # Exception handling
    │   │       ├── model/          # Domain entities
    │   │       ├── repository/     # Data access layer
    │   │       └── service/        # Business logic
    │   └── resources/
    │       └── application.yml     # Application configuration
    └── test/                       # Test classes

## 🔒 Security Features

- **API Key Authentication**: X-API-Key header required for all endpoints
- **Rate Limiting**: 100 requests per minute per API key
- **Input Validation**: Comprehensive validation on all endpoints
- **Secure Configuration**: Environment-based secrets management
- **Public Endpoints**: Health checks and documentation accessible without auth

### Default API Key (Development Only)

    X-API-Key: demo-api-key-12345

**⚠️ Important**: Change the API key in production by setting the `API_KEY` environment variable.

## 🚀 Container Deployment

### Quick Start with Podman

    # Set up environment (recommended for production)
    cp .env.podman.example .env.podman
    # Edit .env.podman with your secure values
    
    # Deploy everything (MongoDB + API)
    ./deploy-with-podman.sh

    # Check health
    ./health-check.sh

    # Run API tests
    ./test-api.sh

    # Stop services
    podman pod stop rewards-pod

### Manual Container Commands

    # Build image
    podman build -f Containerfile -t rewards-api .

    # Run with MongoDB
    podman run -d --name mongo -p 27017:27017 mongo:7
    podman run -d --name api -p 8080:8080 \
      -e MONGODB_URI=mongodb://localhost:27017/rewardsdb \
      -e API_KEY=your-api-key \
      rewards-api

See [PODMAN-DEPLOYMENT.md](PODMAN-DEPLOYMENT.md) for complete deployment guide.

## 📊 Performance

- Database indexes on frequently queried fields
- Efficient aggregation using MongoDB pipelines
- Pagination support for large datasets
- Connection pooling configured
- Container-optimized JVM settings

## 📈 Future Enhancements

- [ ] JWT-based authentication (OAuth2/OIDC)
- [ ] Redis caching for frequently accessed data
- [ ] Kubernetes deployment manifests
- [ ] GraphQL API support
- [ ] Real-time notifications for rewards milestones
- [ ] Admin dashboard with analytics
- [ ] Export functionality (CSV/PDF)
- [ ] Multi-tenant support
- [ ] Prometheus metrics integration
- [ ] Circuit breaker pattern

## 👥 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the Apache 2.0 License - see the LICENSE file for details.

## 📧 Contact

Project Link: [https://github.com/yourusername/rewards-program-api](https://github.com/yourusername/rewards-program-api)

