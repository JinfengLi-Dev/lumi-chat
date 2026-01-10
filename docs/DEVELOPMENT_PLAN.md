# LumiChat Development Plan

## Current Status: Feature Complete (100% RainbowChat-Web Parity)

Last Updated: 2026-01-10

---

## Completed Features (47/47)

### Message Types (8/8)
- [x] Text messages with emoji picker (8 categories)
- [x] Image messages with preview/lightbox
- [x] Voice/audio messages with playback controls
- [x] Video messages with thumbnail and playback
- [x] File/document messages (PDF, Word, etc.)
- [x] Location messages with map display
- [x] Personal card messages (user contact)
- [x] Group card messages (group invitation)

### Chat Features (9/9)
- [x] Message recall (2-minute window)
- [x] Message forwarding to multiple conversations
- [x] Message quoting/replying
- [x] @mention in groups (@all, @specific users)
- [x] Typing indicators (3-second timeout)
- [x] Read receipts (double checkmarks)
- [x] Message copy
- [x] Message delete (local)
- [x] Quick reply templates

### User Profile (5/5)
- [x] View/edit personal profile
- [x] Edit signature/bio
- [x] Change avatar
- [x] Change password
- [x] Voice introduction

### Friend Management (6/6)
- [x] Search users by UID/email
- [x] Send friend request with message
- [x] Accept/reject friend requests
- [x] View pending requests list
- [x] Delete friend with confirmation
- [x] View friend's profile/info panel

### Group Features (13/13)
- [x] Create group (select members)
- [x] Group chat interface
- [x] View group info panel
- [x] Group announcement (owner editable)
- [x] Edit group name (owner)
- [x] Edit group nickname (self)
- [x] View/manage group members
- [x] Add members to group
- [x] Remove members (owner/admin)
- [x] Transfer group ownership
- [x] Dissolve/delete group (owner)
- [x] Leave group (member)
- [x] Invite to group via group card

### Stranger Chat (3/3)
- [x] Chat with non-friends
- [x] View stranger's info
- [x] Send friend request from chat

### UI Features (9/9)
- [x] Conversation list with search
- [x] Pin/unpin conversations
- [x] Mute notifications
- [x] Clear chat history
- [x] Delete conversation
- [x] Online/offline status indicator
- [x] Unread message count badges
- [x] Photo album panel
- [x] Voice recording

---

## Phase A: Stabilization (Priority: HIGH)

### A.1 Code Quality Fixes

| Task | File | Status |
|------|------|--------|
| Virtual scrolling for FriendsList | apps/web/src/components/contact/FriendsList.vue | Pending |
| Virtual scrolling for Chat | apps/web/src/views/Chat.vue | Pending |
| Debounce read receipt sending | apps/web/src/views/ChatConversation.vue | Pending |
| Fix device ID generation | apps/web/src/api/client.ts | Pending |

### A.2 Test Failures to Fix

Current test status:
- Backend: All 360+ tests passing
- Frontend: 826 passed, 33 failed

Failing test files:
- CreateGroupDialog.test.ts (error message expectations)
- websocket.test.ts (unhandled rejection)

---

## Phase B: Feature Enhancement (Priority: MEDIUM)

### B.1 Dark Mode
- Add theme toggle in Settings
- Create dark theme CSS variables
- Persist preference in localStorage
- Respect system preference

### B.2 Message Search
- Full-text search across conversations
- Search results with context
- Jump to message in conversation
- Backend search endpoint

### B.3 Additional Enhancements
- Keyboard shortcuts
- Message bubble animations
- Sound effects for notifications

---

## Phase C: Production Readiness (Priority: HIGH)

### C.1 Rate Limiting
- Login attempts: 5 per minute per IP
- API calls: 100 per minute per user
- Use Bucket4j or Spring Rate Limiter

### C.2 Health Checks
- /health endpoint for API server
- /health endpoint for IM server
- Database connectivity check
- Redis connectivity check
- MinIO connectivity check

### C.3 Monitoring
- Structured JSON logging
- Request/response logging
- Error tracking with correlation IDs

### C.4 Deployment
- Docker Compose for production
- Environment configuration
- CI/CD pipeline setup

---

## Implementation Progress

### Completed Commits

| Date | Commit | Description |
|------|--------|-------------|
| 2026-01-10 | 5963fb3 | feat: Add 4 remaining features from RainbowChat-Web UI reference |
| 2026-01-09 | d5315c7 | feat(web): Add demo accounts and fix conversation list preview |
| 2026-01-08 | 0770379 | feat: Complete Batches 5-8 code quality and security improvements |
| 2026-01-07 | 8dcfcd5 | refactor: Complete comprehensive code review optimization (Batches 1-4) |

### Current Sprint

- [ ] Phase A.1: Code quality fixes
- [ ] Phase A.2: Fix test failures (target: 95% pass rate)
- [ ] Phase B.1: Dark mode
- [ ] Phase C.1: Rate limiting
- [ ] Phase C.2: Health checks

---

## Test Coverage Requirements

Minimum coverage: 95%

| Module | Current | Target |
|--------|---------|--------|
| Backend Services | 95%+ | 95% |
| Backend Controllers | 90%+ | 95% |
| Frontend Components | 85%+ | 95% |
| Frontend Stores | 90%+ | 95% |

---

## Risk Assessment

| Risk | Impact | Mitigation |
|------|--------|------------|
| Virtual scrolling breaks existing UI | Medium | Extensive testing with large datasets |
| Dark mode affects all components | Medium | Incremental rollout, CSS variable system |
| Rate limiting blocks legitimate users | High | Start with generous limits, monitoring |

---

## Success Criteria

1. All 47 RainbowChat-Web features implemented (DONE)
2. 95%+ test pass rate
3. No critical security vulnerabilities
4. Production-ready deployment configuration
5. Comprehensive documentation

---

## Next Steps

1. Fix frontend test failures to achieve 95% pass rate
2. Implement virtual scrolling for performance
3. Add dark mode support
4. Implement rate limiting
5. Add health check endpoints
6. Create production Docker configuration
