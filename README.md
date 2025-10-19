# SignalHub - Real-Time Communication Platform

<div align="center">

A comprehensive real-time communication platform backend similar to Discord/Zoom/WhatsApp, supporting 1-to-1 and group messaging, audio/video calls, and broadcast video.

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

</div>

---

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
  - [Running the Application](#running-the-application)
- [Project Structure](#project-structure)
- [API Documentation](#api-documentation)
- [Testing](#testing)
- [Configuration](#configuration)
- [Deployment](#deployment)
- [Contributing](#contributing)
- [License](#license)

---

## 🎯 Overview

SignalHub is a production-ready, microservice-ready real-time communication platform backend that provides:

- **Real-time messaging** with delivery tracking and read receipts
- **Audio/Video calling** using WebRTC with TURN/STUN support
- **Group communications** for team collaboration
- **Live broadcast streaming** for one-to-many scenarios
- **Push notifications** with customizable preferences
- **Admin dashboard** for user moderation and system monitoring

The platform follows **Clean Architecture** principles with **Domain-Driven Design**, ensuring maintainability, testability, and scalability. Built as a modular monolith, it's designed for easy extraction into microservices when needed.

---

## ✨ Features

### User Story 1: One-to-One Text Messaging (P1 - MVP)
- ✅ Instant text message delivery (<1 second latency)
- ✅ Typing indicators
- ✅ Message delivery and read receipts
- ✅ Persistent chat history
- ✅ Offline message queuing

### User Story 2: One-to-One Audio Calls (P2)
- 🔄 Real-time voice communication
- 🔄 Call status indicators (ringing, busy, connected)
- 🔄 Network quality monitoring
- 🔄 Audio codec optimization

### User Story 3: One-to-One Video Calls (P3)
- 🔄 HD video streaming (up to 720p)
- 🔄 Camera/microphone toggle
- 🔄 Adaptive bitrate based on bandwidth
- 🔄 Screen sharing support

### User Story 4: Group Text Messaging (P4)
- 🔄 Multi-user group chats
- 🔄 Group admin controls
- 🔄 Member management (add/remove)
- 🔄 Group typing indicators

### User Story 5: Group Audio Calls (P5)
- 📅 Multi-party audio conferencing
- 📅 Support for up to 10 participants
- 📅 Dynamic participant join/leave

### User Story 6: Group Video Calls (P6)
- 📅 Grid/gallery video layout
- 📅 Active speaker detection
- 📅 Screen sharing in groups
- 📅 Adaptive quality per participant

### User Story 7: Broadcast Video (P7)
- 📅 One-to-many live streaming
- 📅 Real-time viewer count
- 📅 Broadcast replay support
- 📅 Low-latency streaming (<3 seconds)

**Legend**: ✅ Completed | 🔄 In Progress | 📅 Planned

---

## 🏗️ Architecture

### Architectural Principles

- **Clean Architecture**: Domain-driven with clear layer separation
- **Microservice-Ready**: Modular monolith designed for service extraction
- **Event-Driven**: Kafka-based async communication between modules
- **Database-per-Service**: Logical separation enforced through schemas
- **API-First**: Contract-driven development with OpenAPI specs

### System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        Client Layer                          │
│  (Web, Mobile, Desktop - WebSocket/REST Consumers)          │
└─────────────────────┬───────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│                   API Gateway / Load Balancer                │
└─────────────────────┬───────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│                SignalHub Backend (Spring Boot)               │
├──────────────────────────────────────────────────────────────┤
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │   Auth   │  │   Chat   │  │  Group   │  │   Call   │   │
│  │  Module  │  │  Module  │  │  Module  │  │  Module  │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │Broadcast │  │Notification│ │  Admin   │  │  Shared  │   │
│  │  Module  │  │  Module   │  │  Module  │  │   Infra  │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │
└──────┬───────────┬────────────┬────────────┬───────────────┘
       │           │            │            │
┌──────▼──┐  ┌────▼────┐  ┌────▼────┐  ┌───▼────┐
│PostgreSQL│  │  Redis  │  │  Kafka  │  │ MinIO/ │
│         │  │(Cache + │  │(Events) │  │   S3   │
│         │  │Presence)│  │         │  │(Storage)│
└─────────┘  └─────────┘  └─────────┘  └────────┘
```

### Module Structure (Clean Architecture Layers)

Each module follows the same layered structure:

```
module/
├── domain/              # Enterprise Business Rules
│   ├── model/          # Entities & Aggregates
│   ├── service/        # Domain Services
│   └── event/          # Domain Events
├── application/         # Application Business Rules
│   ├── usecase/        # Use Case Implementations
│   ├── port/           # Ports (Interfaces)
│   └── dto/            # Data Transfer Objects
├── adapter/             # Interface Adapters
│   ├── in/             # Incoming Adapters
│   │   ├── rest/       # REST Controllers
│   │   └── websocket/  # WebSocket Handlers
│   └── out/            # Outgoing Adapters
│       ├── persistence/# Database Repositories
│       ├── messaging/  # Kafka Publishers
│       └── cache/      # Redis Adapters
└── infrastructure/      # Frameworks & Drivers
    ├── config/         # Module Configuration
    ├── security/       # Security Setup
    └── monitoring/     # Metrics & Health
```

---

## 🛠️ Tech Stack

### Core Technologies

| Category | Technology | Purpose |
|----------|-----------|---------|
| **Language** | Java 17+ (LTS) | Primary development language |
| **Framework** | Spring Boot 3.3.0 | Application framework |
| **Build Tool** | Maven 3.8+ | Dependency management & build |
| **Real-time** | Spring WebSocket (STOMP) | Messaging protocol |
| **WebRTC** | Custom Signaling Server | Audio/video call signaling |
| **Database** | PostgreSQL 14+ | Primary data store |
| **Cache** | Redis 7.x | Caching, presence, session management |
| **Message Queue** | Apache Kafka 3.x | Event streaming & async messaging |
| **Authentication** | Spring Security + JWT | Auth & authorization |
| **File Storage** | MinIO / Amazon S3 | Media & attachment storage |
| **TURN/STUN** | coturn | NAT traversal for WebRTC |

### Infrastructure & DevOps

| Category | Technology |
|----------|-----------|
| **Containerization** | Docker & Docker Compose |
| **Orchestration** | Kubernetes (planned) |
| **Monitoring** | Prometheus + Grafana |
| **Logging** | SLF4J + Logback (JSON format) |
| **API Docs** | SpringDoc OpenAPI (Swagger) |
| **Database Migration** | Flyway |
| **Testing** | JUnit 5, Mockito, TestContainers |

### Performance Goals

- **Concurrent Connections**: 10,000+ users
- **Message Delivery**: <1 second latency
- **Audio Latency**: <200ms
- **Throughput**: 1,000 messages/second
- **WebSocket Setup**: <100ms
- **Uptime**: 99.9%
- **Test Coverage**: 80%+

---

## 🚀 Getting Started

### Prerequisites

Before you begin, ensure you have the following installed:

- **Java Development Kit (JDK)**: 17 or higher
  ```bash
  java -version  # Should show 17 or higher
  ```

- **Maven**: 3.8 or higher
  ```bash
  mvn -version
  ```

- **Docker & Docker Compose**: Latest version
  ```bash
  docker --version
  docker-compose --version
  ```

- **Git**: For version control
  ```bash
  git --version
  ```

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-org/SignalHub.git
   cd SignalHub
   ```

2. **Start infrastructure services**
   
   The project includes a Docker Compose file that sets up all required services:
   
   ```bash
   docker-compose up -d
   ```
   
   This will start:
   - PostgreSQL (port 5432)
   - Redis (port 6379)
   - Apache Kafka (port 9092)
   - Zookeeper (port 2181)
   - MinIO (ports 9000, 9001)
   - coturn TURN/STUN server (port 3478)

3. **Verify services are running**
   ```bash
   docker-compose ps
   ```

4. **Configure the application**
   
   Copy the example configuration:
   ```bash
   copy src\main\resources\application-dev.yml.example src\main\resources\application-dev.yml
   ```
   
   Edit `application-dev.yml` with your local settings if needed.

### Running the Application

#### Option 1: Using Maven

```bash
# Run with development profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Or build and run the JAR
mvn clean package
java -jar target/communication-platform-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

#### Option 2: Using IDE

1. Import the project as a Maven project in your IDE (IntelliJ IDEA, Eclipse, VS Code)
2. Set the active profile to `dev`
3. Run `CommunicationPlatformApplication.java`

#### Verify the Application

Once started, you can access:

- **API Documentation (Swagger UI)**: http://localhost:8080/swagger-ui.html
- **Health Check**: http://localhost:8080/actuator/health
- **Metrics**: http://localhost:8080/actuator/metrics
- **MinIO Console**: http://localhost:9001 (credentials: minioadmin/minioadmin)

---

## 📁 Project Structure

```
SignalHub/
├── src/
│   ├── main/
│   │   ├── java/com/realtime/communication/
│   │   │   ├── CommunicationPlatformApplication.java
│   │   │   ├── auth/                # Authentication module
│   │   │   ├── chat/                # Messaging module
│   │   │   ├── group/               # Group management module
│   │   │   ├── call/                # WebRTC signaling module
│   │   │   ├── broadcast/           # Live streaming module
│   │   │   ├── notification/        # Notification module
│   │   │   ├── admin/               # Admin dashboard module
│   │   │   └── shared/              # Shared infrastructure
│   │   └── resources/
│   │       ├── application.yml      # Base configuration
│   │       ├── application-dev.yml  # Development config
│   │       ├── application-prod.yml # Production config
│   │       └── db/migration/        # Flyway migrations
│   └── test/
│       └── java/com/realtime/communication/
│           ├── unit/                # Unit tests
│           ├── integration/         # Integration tests
│           └── e2e/                 # End-to-end tests
├── docker-compose.yml               # Local development infrastructure
├── Dockerfile                       # Application container
├── pom.xml                          # Maven dependencies
└── README.md                        # This file
```

---

## 📚 API Documentation

### REST API

The platform provides RESTful APIs for:

- **Authentication**: `/api/v1/auth/*` - Register, login, token refresh
- **Chat**: `/api/v1/chat/*` - Message history, conversations
- **Groups**: `/api/v1/groups/*` - Group management
- **Calls**: `/api/v1/calls/*` - Call history, statistics
- **Broadcasts**: `/api/v1/broadcasts/*` - Broadcast management
- **Notifications**: `/api/v1/notifications/*` - Notification preferences
- **Admin**: `/api/v1/admin/*` - User moderation, system stats

**Interactive Documentation**: Available at `/swagger-ui.html` when running the application.

### WebSocket Endpoints

- **Chat Messages**: `/ws/chat` - Real-time messaging with STOMP
- **Call Signaling**: `/ws/call` - WebRTC SDP/ICE exchange
- **Notifications**: `/ws/notifications` - Real-time notification delivery
- **Presence**: `/ws/presence` - User online/offline status

### Event Contracts (Kafka)

The platform publishes domain events to Kafka topics:

- `user.events` - User registration, login, status changes
- `message.events` - Message sent, delivered, read
- `call.events` - Call started, ended, participant joined
- `group.events` - Group created, member added/removed
- `broadcast.events` - Broadcast started, ended, viewer joined
- `notification.events` - Notifications triggered

See `specs/001-real-time-communication/contracts/` for detailed event schemas.

---

## 🧪 Testing

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test category
mvn test -Dtest=**/*UnitTest
mvn test -Dtest=**/*IntegrationTest
mvn test -Dtest=**/*E2ETest

# Run tests with coverage
mvn test jacoco:report
# Coverage report: target/site/jacoco/index.html
```

### Test Structure

- **Unit Tests** (`src/test/java/.../unit/`): Test domain logic and use cases in isolation
- **Integration Tests** (`src/test/java/.../integration/`): Test adapters with real infrastructure (using TestContainers)
- **E2E Tests** (`src/test/java/.../e2e/`): Test complete user journeys

### Test Coverage Goals

- **Overall Coverage**: ≥80%
- **Domain Layer**: ≥90%
- **Application Layer**: ≥85%
- **Adapter Layer**: ≥70%

---

## ⚙️ Configuration

### Environment Profiles

The application supports multiple profiles:

- **dev**: Local development (uses Docker Compose services)
- **prod**: Production environment (uses managed cloud services)

### Key Configuration Properties

```yaml
# Application
server.port: 8080
spring.profiles.active: dev

# Database
spring.datasource.url: jdbc:postgresql://localhost:5432/signalhub
spring.datasource.username: postgres
spring.datasource.password: postgres

# Redis
spring.data.redis.host: localhost
spring.data.redis.port: 6379

# Kafka
spring.kafka.bootstrap-servers: localhost:9092

# JWT
jwt.secret: your-secret-key
jwt.access-token-expiration: 900000  # 15 minutes
jwt.refresh-token-expiration: 604800000  # 7 days

# File Storage
storage.type: minio  # or s3
storage.endpoint: http://localhost:9000
```

See `src/main/resources/application.yml` for complete configuration options.

---

## 🚢 Deployment

### Docker Deployment

Build the Docker image:

```bash
docker build -t signalhub:latest .
```

Run the container:

```bash
docker run -p 8080:8080 \
  --env SPRING_PROFILES_ACTIVE=prod \
  --env DATABASE_URL=jdbc:postgresql://db:5432/signalhub \
  signalhub:latest
```

### Kubernetes Deployment

Kubernetes manifests are planned for future releases. The application includes:

- Health checks (`/actuator/health`)
- Readiness probes (`/actuator/health/readiness`)
- Liveness probes (`/actuator/health/liveness`)
- Graceful shutdown support

### Environment Variables

Required environment variables for production:

```bash
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:postgresql://host:5432/db
DATABASE_USERNAME=user
DATABASE_PASSWORD=pass
REDIS_HOST=redis-host
REDIS_PORT=6379
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
JWT_SECRET=your-production-secret
S3_ENDPOINT=https://s3.amazonaws.com
S3_ACCESS_KEY=your-access-key
S3_SECRET_KEY=your-secret-key
```

---

## 🤝 Contributing

We welcome contributions! Please follow these guidelines:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Follow the Clean Architecture principles
4. Write tests for new functionality (maintain ≥80% coverage)
5. Ensure all tests pass (`mvn test`)
6. Run code quality checks (`mvn verify`)
7. Commit your changes (`git commit -m 'Add amazing feature'`)
8. Push to the branch (`git push origin feature/amazing-feature`)
9. Open a Pull Request

### Code Quality Standards

- Follow Java naming conventions
- Use Checkstyle configuration (`config/checkstyle/checkstyle.xml`)
- Pass SpotBugs analysis
- Maintain SonarQube quality gates
- Write clear commit messages

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 📞 Support

For questions, issues, or feature requests:

- **Issues**: [GitHub Issues](https://github.com/your-org/SignalHub/issues)
- **Discussions**: [GitHub Discussions](https://github.com/your-org/SignalHub/discussions)
- **Documentation**: See `specs/` directory for detailed specifications

---

## 🙏 Acknowledgments

- Spring Boot Team for the excellent framework
- WebRTC community for real-time communication standards
- Contributors and maintainers of all open-source dependencies

---

<div align="center">
Made with ❤️ by the SignalHub Team
</div>

