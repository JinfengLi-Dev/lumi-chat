# Lumi-Chat

Cross-platform Instant Messaging system supporting Web, iOS, Android, and PC with full multi-device synchronization.

## Features

- Multi-platform support (Web, iOS, Android, PC)
- Multi-device synchronization - access from any device with full message history
- Real-time messaging via WebSocket
- Rich message types: text, image, file, voice, video, location, user/group cards
- Message actions: recall, forward, quote, @mentions
- Friend system: add, accept/reject, delete, block
- Group chat: create, manage members, announcements
- File sharing with S3-compatible storage (MinIO)
- JWT-based authentication with device management

## Tech Stack

### Frontend
- **Web**: Vue 3 + TypeScript + Element Plus + Pinia
- **PC**: Electron + Vue 3 (planned)
- **iOS**: Swift + SwiftUI (planned)
- **Android**: Kotlin + Jetpack Compose (planned)

### Backend
- **API Server**: Spring Boot 3 + PostgreSQL + Redis
- **IM Server**: Netty WebSocket with multi-device session support
- **File Storage**: MinIO (S3-compatible)
- **Message Queue**: Redis Pub/Sub

## Project Structure

```
lumi-chat/
├── apps/
│   └── web/                    # Vue 3 web client
├── services/
│   ├── api/                    # Spring Boot REST API
│   └── im-server/              # Netty WebSocket IM server
├── infra/
│   └── docker/                 # Docker Compose configuration
└── docs/                       # Architecture & implementation docs
```

## Quick Start

### Prerequisites

- Node.js 18+
- Java 21+
- Docker & Docker Compose

### 1. Start Infrastructure

```bash
cd infra/docker
docker compose up -d
```

This starts:
- PostgreSQL (port 5432)
- Redis (port 6379)
- MinIO (ports 9000, 9001)

### 2. Start API Server

```bash
cd services/api
./gradlew bootRun
```

API server runs on http://localhost:8080/api/v1

### 3. Start IM Server

```bash
cd services/im-server
./gradlew bootRun
```

WebSocket server runs on ws://localhost:7901/ws

### 4. Start Web Client

```bash
cd apps/web
npm install
npm run dev
```

Web client runs on http://localhost:5173

## API Documentation

Once the API server is running, access Swagger UI at:
http://localhost:8080/api/v1/swagger-ui.html

## Architecture

### Multi-Device Synchronization

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Web App   │     │  iOS App    │     │ Android App │
└──────┬──────┘     └──────┬──────┘     └──────┬──────┘
       │                   │                   │
       └───────────────────┼───────────────────┘
                           │
                    ┌──────▼──────┐
                    │  IM Server  │──── Redis Pub/Sub
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │  API Server │
                    └──────┬──────┘
                           │
              ┌────────────┼────────────┐
              │            │            │
       ┌──────▼──────┐ ┌───▼───┐ ┌─────▼─────┐
       │ PostgreSQL  │ │ Redis │ │   MinIO   │
       └─────────────┘ └───────┘ └───────────┘
```

### Message Flow

1. Client sends message via WebSocket to IM Server
2. IM Server validates and publishes to Redis
3. Redis fans out to all connected devices
4. API Server persists message to PostgreSQL
5. Offline devices receive messages on reconnect

## Development Status

### Completed (Stage 1)
- [x] Project structure and build configuration
- [x] Docker Compose infrastructure
- [x] Database schema with multi-device support
- [x] Web frontend scaffolding (Vue 3)
- [x] API server with JWT authentication
- [x] IM server with WebSocket support

### In Progress (Stage 2+)
- [ ] Complete web UI implementation
- [ ] Friend system API and UI
- [ ] Group chat functionality
- [ ] File upload integration
- [ ] Message persistence and sync
- [ ] iOS and Android clients

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feat/amazing-feature`)
3. Commit your changes (`git commit -m 'feat: add amazing feature'`)
4. Push to the branch (`git push origin feat/amazing-feature`)
5. Open a Pull Request

## License

MIT License - see [LICENSE](LICENSE) for details
