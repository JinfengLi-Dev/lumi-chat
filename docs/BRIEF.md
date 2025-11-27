# Lumi-Chat Project Brief

## Executive Summary

Lumi-Chat is a production-ready, cross-platform instant messaging application that replicates 100% of RainbowChat-Web features and UI/UX. Built on MobileIMSDK core technology, it provides real-time communication across Web, iOS, and Android platforms.

---

## Project Goals

### Primary Objectives
1. Build a complete IM system with feature parity to RainbowChat-Web
2. Support three platforms: Web (Vue 3), iOS (Swift), Android (Kotlin)
3. Achieve 95%+ test coverage before production deployment
4. Use MobileIMSDK as the core communication layer

### Success Metrics
- Message delivery latency < 200ms
- Support 1000+ concurrent users
- 99.9% uptime in production
- Cross-platform UI consistency score > 95%

---

## Feature Scope

### Must Have (P0)
- User authentication (login, register, password recovery)
- One-on-one chat (friends and strangers)
- Group chat (create, manage, dissolve)
- Text, image, file messages
- Message recall, forward, quote
- Friend system (add, remove, requests)
- User profile management

### Should Have (P1)
- Location messages with map
- Personal/Group card sharing
- @mention functionality
- Voice/Video message display (from mobile)
- Emoji picker
- Quick replies

### Nice to Have (P2)
- Message search
- Chat history export
- Custom themes
- Push notifications (mobile)

---

## Technical Constraints

### Must Use
- MobileIMSDK for real-time communication
- WebSocket for Web client
- UDP/TCP for mobile clients

### Recommended Stack
- Web: Vue 3 + TypeScript + Element Plus
- iOS: Swift + SwiftUI
- Android: Kotlin + Jetpack Compose
- Backend: Spring Boot 3 + PostgreSQL + Redis

### Non-Functional Requirements
- Response time: < 500ms for API calls
- Message latency: < 200ms
- File upload: Max 100MB
- Image compression: Auto-resize to max 2000px

---

## Timeline Overview

| Phase | Duration | Deliverables |
|-------|----------|--------------|
| Foundation | 1 week | Project setup, Docker, CI/CD |
| Core Web | 3 weeks | Authentication, chat, messages |
| Advanced Features | 2 weeks | Groups, friends, media |
| Mobile Clients | 2 weeks | iOS and Android apps |
| Testing & QA | 1 week | Test coverage, bug fixes |
| Deployment | 1 week | Production deployment |

Total: ~10 weeks

---

## Stakeholders

- Project Owner: Luis Lee
- Development: Claude AI Assistant
- Reference: RainbowChat-Web, MobileIMSDK

---

## Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| MobileIMSDK integration complexity | High | Study documentation, start with simple flows |
| Cross-platform UI consistency | Medium | Use design system, shared tokens |
| Performance at scale | High | Load testing early, caching strategy |

---

## References

- RainbowChat-Web Screenshots: C:\Users\luis_\Desktop\RainbowChat-Web
- MobileIMSDK: https://github.com/JackJiang2011/MobileIMSDK
- Documentation: http://www.52im.net/

---

## Approval

- [ ] Project scope approved
- [ ] Timeline approved
- [ ] Technology stack approved
- [ ] Ready to begin Stage 1

---

Last Updated: 2025-11-27
