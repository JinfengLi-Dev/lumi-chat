# CLAUDE.md - Lumi-Chat Project Guardrails

This document serves as collaborative guardrails for the Lumi-Chat project.
All development must follow these guidelines.

---

## Project Overview

Lumi-Chat is a cross-platform IM application built on MobileIMSDK.

Key References:
- UI/UX Reference: RainbowChat-Web screenshots
- Core SDK: MobileIMSDK (GitHub: JackJiang2011/MobileIMSDK)
- Documentation: docs/IMPLEMENTATION_PLAN.md, docs/ARCHITECTURE.md

---

## Repository Structure

```
lumi-chat/
├── apps/
│   ├── web/          # Vue 3 Web client
│   ├── ios/          # Swift iOS client
│   └── android/      # Kotlin Android client
├── services/
│   ├── api/          # Spring Boot REST API
│   └── im-server/    # MobileIMSDK Server
├── packages/
│   └── shared/       # Shared types, utilities
├── infra/
│   └── docker/       # Docker Compose files
├── docs/             # Project documentation
└── scripts/          # Build and utility scripts
```

---

## Development Workflow

### 1. Before Starting Any Feature
- Read docs/IMPLEMENTATION_PLAN.md for current stage
- Check the stage status and success criteria
- Update stage status to "In Progress"

### 2. Implementation
- Follow TDD: Write failing tests first
- Keep commits small and focused
- Update IMPLEMENTATION_PLAN.md status after each milestone

### 3. After Completing Feature
- Run all tests locally
- Update stage status to "Complete"
- Create PR with proper description

---

## Code Standards

### TypeScript (Web)
- Strict mode enabled
- No any types (use unknown if needed)
- Use Vue 3 Composition API
- Use Pinia for state management

### Swift (iOS)
- Follow Swift API Design Guidelines
- Use SwiftUI when possible
- MVVM architecture
- Combine for reactive patterns

### Kotlin (Android)
- Kotlin coding conventions
- Jetpack Compose for UI
- MVVM with Clean Architecture
- Coroutines for async operations

### Java (Backend)
- Java 21
- Spring Boot 3.x conventions
- Repository pattern for data access
- DTOs for API responses

---

## Testing Requirements

Minimum coverage: 95%

### Test Types
- Unit tests for all business logic
- Integration tests for API endpoints
- E2E tests for critical user flows
- Component tests for UI components

### Test Before Commit
```bash
# Web
npm run test

# iOS
xcodebuild test

# Android
./gradlew test

# Backend
./gradlew test
```

---

## Commit Message Format

```
type(scope): description

[optional body]

[optional footer]
```

Types: feat, fix, docs, style, refactor, test, chore

Examples:
- feat(web): add emoji picker component
- fix(api): correct friend request validation
- docs(arch): update message flow diagram

---

## PR Requirements

- [ ] Tests passing
- [ ] Coverage >= 95%
- [ ] No lint errors
- [ ] Documentation updated
- [ ] IMPLEMENTATION_PLAN.md status updated

---

## Key Decisions

### Message Protocol
Use MobileIMSDK's built-in protocol with custom message types.

### Database
PostgreSQL for persistence, Redis for caching/sessions.

### File Storage
MinIO for S3-compatible object storage.

### Authentication
JWT tokens with refresh token rotation.

---

## Commands for Claude

### Check Current Stage
Read docs/IMPLEMENTATION_PLAN.md and report current stage status.

### Start Stage
Update stage status to "In Progress" and begin implementation.

### Complete Stage
Run tests, update status to "Complete", prepare for next stage.

### Create Component
Follow existing patterns in the codebase, write tests first.

---

## Important Notes

1. Never commit directly to main
2. Always update IMPLEMENTATION_PLAN.md
3. Test coverage must be >= 95%
4. Follow the 3-attempt rule for stuck issues
5. Reference RainbowChat-Web screenshots for UI accuracy

---

## Formatting Rules

- NEVER use double asterisks
- Use single dash (-) for bullet points
- Use headers (# ## ###) for structure
- Use code blocks for code examples

---

Last Updated: 2025-11-27
