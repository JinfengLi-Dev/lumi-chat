# Lumi-Chat Comprehensive Improvement Plan

Generated: 2026-01-28
Based on: UI Reference Screenshots + ARCHITECTURE.md + Current Implementation Analysis

---

## Executive Summary

### Current Implementation Status: 80-85% Complete

**What's Working:**
- Backend API: 95% complete with excellent test coverage (93%+)
- Frontend Core: 80% complete with all major features implemented
- Authentication: 100% (login, register, password reset with email)
- Chat Core: 95% (messaging, conversations, real-time sync)
- Friend System: 90% (search, requests, management)
- Group System: 90% (create, manage, member operations)
- Message Types: 100% (text, image, file, voice, video, location, cards)
- Multi-Device: 85% (sync working, needs polish)

**Critical Gaps:**
1. IM Server Integration (MobileIMSDK) - Not fully integrated with frontend WebSocket
2. Initial Sync Flow - Missing on new device login
3. Push Notifications - Not configured (FCM/APNS)
4. iOS/Android Clients - Not started
5. Message Search - Not implemented
6. Advanced UI Polish - Responsive design needs refinement

---

## Phase 1: Critical Foundation (HIGH PRIORITY)

### 1.1 Complete MobileIMSDK WebSocket Integration

**Current State:** Backend IM server running, frontend WebSocket service exists but needs full integration

**Required Implementation:**
- Connect frontend `websocket.ts` service to actual IM server endpoint
- Implement proper protocol message handling (match MobileIMSDK packet structure)
- Add QoS acknowledgment handling
- Implement heartbeat mechanism
- Add auto-reconnect with exponential backoff
- Device registration on connect

**Files to Modify:**
- `apps/web/src/services/websocket.ts`
- `services/im-server/src/main/java/com/lumichat/im/server/ImServer.java`
- `services/im-server/src/main/java/com/lumichat/im/handler/MessageHandler.java`

**Success Criteria:**
- WebSocket connects to IM server successfully
- Messages sent via WebSocket appear in recipient's chat
- Connection survives network interruptions
- Heartbeat keeps connection alive
- Multiple devices receive same message (multi-device broadcast)

**Tests Required:**
```typescript
// Frontend Tests
describe('WebSocket Integration', () => {
  it('should connect to IM server', async () => {})
  it('should send and receive text messages', async () => {})
  it('should handle reconnection', async () => {})
  it('should acknowledge message delivery', async () => {})
  it('should send heartbeats every 30 seconds', async () => {})
  it('should register device on connect', async () => {})
})

// Backend Integration Tests
describe('Multi-Device Message Routing', () => {
  it('should route message to all user devices', () => {})
  it('should queue message for offline devices', () => {})
  it('should broadcast read status to all devices', () => {})
  it('should sync sent message to sender devices', () => {})
})
```

---

### 1.2 Implement Initial Sync on Login

**Current State:** Frontend loads conversations but doesn't request full sync from server

**Required Implementation:**
- Create `/api/v1/sync/initial` endpoint to return:
  - All conversations with metadata
  - Last 50 messages per conversation
  - Unread counts
  - Read status positions
  - User profile
  - Friends list
  - Groups list
- Frontend: Call sync on successful login
- Store sync cursor (last sync timestamp)
- Implement incremental sync for reconnections

**Files to Create/Modify:**
- `services/api/src/main/java/com/lumichat/controller/SyncController.java` (enhance)
- `services/api/src/main/java/com/lumichat/service/SyncService.java` (new)
- `apps/web/src/services/sync.ts` (new)
- `apps/web/src/stores/chat.ts` (add initialSync method)

**Success Criteria:**
- New device login gets full conversation history
- Messages from 6+ months ago are accessible
- Sync completes within 5 seconds for 100 conversations
- Incremental sync only fetches changes

**Tests Required:**
```typescript
describe('Initial Sync', () => {
  it('should fetch all conversations on login', async () => {})
  it('should fetch last 50 messages per conversation', async () => {})
  it('should set correct unread counts', async () => {})
  it('should handle empty conversation list', async () => {})
  it('should store sync cursor', async () => {})
})

describe('Incremental Sync', () => {
  it('should fetch only new messages since last sync', async () => {})
  it('should update read status changes', async () => {})
  it('should handle recalled messages', async () => {})
})
```

---

### 1.3 Fix Responsive Mobile Layout

**Current State:** Basic responsive CSS exists, needs refinement for true mobile experience

**Required Implementation:**
- Refine breakpoints (mobile < 768px, tablet 768-1024px)
- Implement mobile-first navigation:
  - Bottom tab bar for main tabs (Messages/Contacts/Groups)
  - Swipe gestures for sidebar
  - Proper back navigation
- Optimize touch targets (minimum 44x44px)
- Add pull-to-refresh for conversation list
- Optimize virtual scrolling for mobile performance
- Test on real devices (iOS Safari, Android Chrome)

**Files to Modify:**
- `apps/web/src/assets/styles/main.scss`
- `apps/web/src/views/Chat.vue`
- `apps/web/src/views/ChatConversation.vue`

**Success Criteria:**
- App usable on iPhone SE (smallest modern screen)
- No horizontal scrolling
- Touch targets easy to tap
- Smooth 60fps scrolling
- Works in landscape and portrait

**Tests Required:**
```typescript
describe('Mobile Responsive Design', () => {
  it('should display mobile menu on screens < 768px', () => {})
  it('should hide sidebar on mobile when conversation open', () => {})
  it('should show back button in conversation view', () => {})
  it('should handle landscape orientation', () => {})
  it('should have touch targets >= 44x44px', () => {})
})
```

---

## Phase 2: Feature Completion (MEDIUM PRIORITY)

### 2.1 Implement Message Search

**Current State:** Not implemented

**Required Implementation:**
- Add search bar in conversation list header
- Implement full-text search API:
  - Search across all messages
  - Search within specific conversation
  - Filter by message type (images, files, etc.)
  - Search by date range
- Create SearchResults component
- Highlight search terms in results
- Jump to message in conversation on click

**Files to Create:**
- `services/api/src/main/java/com/lumichat/controller/SearchController.java`
- `services/api/src/main/java/com/lumichat/service/SearchService.java`
- `apps/web/src/components/chat/MessageSearch.vue`
- `apps/web/src/services/search.ts`

**API Endpoint:**
```
GET /api/v1/search/messages?q={query}&conversationId={id}&type={type}&before={date}&after={date}&limit=50
```

**Success Criteria:**
- Search returns results within 500ms for 10,000 messages
- Highlights search terms in results
- Supports Chinese/English/emoji search
- Shows message context (2 lines before/after)

**Tests Required:**
```typescript
describe('Message Search', () => {
  it('should search across all conversations', async () => {})
  it('should search within specific conversation', async () => {})
  it('should filter by message type', async () => {})
  it('should highlight search terms', async () => {})
  it('should support Chinese characters', async () => {})
  it('should handle empty results', async () => {})
  it('should paginate large result sets', async () => {})
})
```

---

### 2.2 Add Message Favorites/Bookmarks

**Current State:** Not implemented (mentioned in UI reference)

**Required Implementation:**
- Add "Favorite" option to message context menu
- Create favorites database table:
  ```sql
  CREATE TABLE message_favorites (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    message_id BIGINT REFERENCES messages(id),
    conversation_id BIGINT REFERENCES conversations(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, message_id)
  );
  ```
- Create Favorites view/panel
- Add API endpoints:
  - `POST /api/v1/favorites/{messageId}` - Add to favorites
  - `DELETE /api/v1/favorites/{messageId}` - Remove from favorites
  - `GET /api/v1/favorites` - List all favorites
- Show favorite indicator on messages
- Support search within favorites

**Files to Create:**
- `services/api/src/main/java/com/lumichat/controller/FavoriteController.java`
- `services/api/src/main/java/com/lumichat/service/FavoriteService.java`
- `services/api/src/main/java/com/lumichat/repository/MessageFavoriteRepository.java`
- `apps/web/src/views/Favorites.vue`
- `apps/web/src/stores/favorites.ts`

**Success Criteria:**
- Can favorite any message type
- Favorites persist across devices
- Can unfavorite from favorites list
- Shows original conversation context
- Supports up to 10,000 favorites per user

**Tests Required:**
```typescript
describe('Message Favorites', () => {
  it('should add message to favorites', async () => {})
  it('should remove message from favorites', async () => {})
  it('should list all user favorites', async () => {})
  it('should show favorite indicator on messages', async () => {})
  it('should prevent duplicate favorites', async () => {})
  it('should handle deleted original message', async () => {})
  it('should search within favorites', async () => {})
})
```

---

### 2.3 Implement Voice/Video Calls

**Current State:** Not implemented (mentioned in architecture but no code)

**Required Implementation:**
- Integrate WebRTC for peer-to-peer calls
- Add call initiation UI (call buttons in chat header)
- Implement call signaling via IM server
- Create call modal with:
  - Incoming call notification
  - Ringing state
  - Active call controls (mute, speaker, hang up)
  - Call duration timer
- Add call history tracking
- Support group voice calls (up to 9 participants)
- Handle call interruptions (network loss, other device answers)

**Files to Create:**
- `apps/web/src/components/call/VoiceCallModal.vue`
- `apps/web/src/components/call/VideoCallModal.vue`
- `apps/web/src/components/call/IncomingCallNotification.vue`
- `apps/web/src/services/webrtc.ts`
- `apps/web/src/stores/call.ts`
- `services/api/src/main/java/com/lumichat/controller/CallController.java`

**Success Criteria:**
- 1-on-1 voice calls work reliably
- 1-on-1 video calls work reliably
- Call quality is good on 3G/4G/WiFi
- Call notifications show on all devices
- Can switch between devices during call

**Tests Required:**
```typescript
describe('Voice/Video Calls', () => {
  it('should initiate 1-on-1 voice call', async () => {})
  it('should initiate 1-on-1 video call', async () => {})
  it('should show incoming call notification', async () => {})
  it('should accept incoming call', async () => {})
  it('should reject incoming call', async () => {})
  it('should handle call during ongoing call', async () => {})
  it('should mute/unmute audio', async () => {})
  it('should enable/disable video', async () => {})
  it('should end call', async () => {})
  it('should handle network interruption', async () => {})
})
```

---

### 2.4 Push Notification System

**Current State:** Not configured

**Required Implementation:**
- Setup Firebase Cloud Messaging (FCM) for web
- Add service worker for background notifications
- Register device push token on login
- Send push when:
  - New message arrives (device offline)
  - @mention in group
  - Friend request received
  - Call incoming
- Show notification preview with:
  - Sender name/avatar
  - Message preview (encrypted)
  - Unread count
- Handle notification click (open to conversation)
- Respect mute/DND settings

**Files to Create:**
- `apps/web/public/firebase-messaging-sw.js` (service worker)
- `apps/web/src/services/notifications.ts`
- `services/api/src/main/java/com/lumichat/service/PushNotificationService.java`
- `services/api/src/main/java/com/lumichat/config/FirebaseConfig.java`

**Success Criteria:**
- Notifications show when app in background
- Clicking notification opens correct conversation
- No notifications when app in foreground
- Respects user mute settings
- Works on Chrome, Firefox, Safari

**Tests Required:**
```typescript
describe('Push Notifications', () => {
  it('should request notification permission', async () => {})
  it('should register push token', async () => {})
  it('should show notification for new message', async () => {})
  it('should show notification for @mention', async () => {})
  it('should not show if muted', async () => {})
  it('should open to conversation on click', async () => {})
  it('should update badge count', async () => {})
})
```

---

### 2.5 Advanced Group Features

**Current State:** Basic group management works, needs admin controls

**Required Implementation:**
- Add admin role (separate from owner)
- Admin permissions:
  - Remove members
  - Mute members
  - Edit group info
  - Approve join requests
- Add group join modes:
  - Anyone can join
  - Approval required
  - Invite only
- Add member mute (prevent sending messages)
- Add member ban (remove + block rejoin)
- Add @ all permission (admin/owner only)
- Group announcement notifications
- Pin important messages in group

**Files to Modify:**
- `services/api/src/main/java/com/lumichat/entity/GroupMember.java` (add role field)
- `services/api/src/main/java/com/lumichat/entity/Group.java` (add join_mode field)
- `services/api/src/main/java/com/lumichat/service/GroupService.java`
- `apps/web/src/components/group/GroupInfoPanel.vue`

**Success Criteria:**
- Can assign admin role to members
- Admins can remove/mute members
- Can configure group join mode
- Muted members cannot send messages
- Banned members cannot rejoin

**Tests Required:**
```typescript
describe('Advanced Group Features', () => {
  it('should assign admin role', async () => {})
  it('should remove admin role', async () => {})
  it('should allow admin to remove member', async () => {})
  it('should allow admin to mute member', async () => {})
  it('should prevent muted member from sending', async () => {})
  it('should handle join approval flow', async () => {})
  it('should prevent banned user from rejoining', async () => {})
})
```

---

## Phase 3: Polish & Optimization (LOW PRIORITY)

### 3.1 Performance Optimizations

**Current State:** Good, but can be better

**Optimizations:**
- Implement IndexedDB caching for messages
- Add image lazy loading
- Optimize virtual scroll rendering
- Bundle splitting for faster initial load
- Service worker for offline support
- Compress images before upload
- Add progressive web app (PWA) support

**Files to Create/Modify:**
- `apps/web/src/services/cache.ts` (IndexedDB wrapper)
- `apps/web/vite.config.ts` (bundle optimization)
- `apps/web/public/service-worker.js`

**Success Criteria:**
- Initial load < 2 seconds on 3G
- Message list renders 1000+ messages smoothly
- Offline mode works for viewing cached messages
- App installable as PWA

---

### 3.2 Accessibility Improvements

**Current State:** Basic keyboard navigation exists

**Required Implementation:**
- Full ARIA labels for screen readers
- Keyboard navigation for all actions
- High contrast mode support
- Focus indicators for all interactive elements
- Screen reader announcements for new messages
- Reduced motion mode

**Success Criteria:**
- Passes WCAG 2.1 AA standards
- Fully navigable with keyboard only
- Screen reader can navigate entire app
- Supports Windows Narrator, NVDA, JAWS

---

### 3.3 Internationalization (i18n)

**Current State:** Hardcoded Chinese and English text

**Required Implementation:**
- Setup Vue I18n
- Extract all text to translation files
- Support languages:
  - English (en-US)
  - Simplified Chinese (zh-CN)
  - Traditional Chinese (zh-TW)
  - Japanese (ja-JP)
  - Korean (ko-KR)
- RTL support for Arabic/Hebrew
- Date/time formatting per locale
- Number formatting per locale

**Files to Create:**
- `apps/web/src/locales/en-US.json`
- `apps/web/src/locales/zh-CN.json`
- `apps/web/src/plugins/i18n.ts`

**Success Criteria:**
- Can switch language without reload
- All UI text translates
- Date/time shows in user locale
- RTL layouts work correctly

---

## Phase 4: Native Clients (FUTURE)

### 4.1 iOS Client

**Technology:** Swift + SwiftUI + MobileIMSDK-iOS

**Priority Features:**
1. Login/Register
2. Conversation list
3. Chat (text, image, voice, video)
4. Push notifications (APNS)
5. Friend management
6. Group management
7. Voice recording
8. Camera/photo picker
9. Location sharing
10. Background sync

**Timeline:** 6-8 weeks

---

### 4.2 Android Client

**Technology:** Kotlin + Jetpack Compose + MobileIMSDK-Android

**Priority Features:**
1. Login/Register
2. Conversation list
3. Chat (text, image, voice, video)
4. Push notifications (FCM)
5. Friend management
6. Group management
7. Voice recording
8. Camera/photo picker
9. Location sharing
10. Background sync via WorkManager

**Timeline:** 6-8 weeks

---

## Testing Strategy

### Test Coverage Goals

| Component | Target | Current | Gap |
|-----------|--------|---------|-----|
| Backend API | 95% | 93% | 2% |
| Backend IM Server | 90% | 85% | 5% |
| Frontend Components | 90% | 75% | 15% |
| Frontend Stores | 95% | 80% | 15% |
| Frontend Services | 90% | 70% | 20% |
| E2E Tests | 80% critical paths | 0% | 80% |

### Priority Test Suites

**1. E2E Critical Path Tests (HIGH)**
```typescript
describe('Critical User Flows', () => {
  it('should complete registration and first login', async () => {})
  it('should send and receive message', async () => {})
  it('should add friend and start chat', async () => {})
  it('should create group and invite members', async () => {})
  it('should handle multi-device sync', async () => {})
  it('should reconnect after network loss', async () => {})
})
```

**2. Load/Performance Tests (MEDIUM)**
```typescript
describe('Performance', () => {
  it('should load 100 conversations in < 1s', async () => {})
  it('should scroll 1000 messages smoothly', async () => {})
  it('should handle 50 concurrent users', async () => {})
  it('should sync 10,000 offline messages in < 5s', async () => {})
})
```

**3. Security Tests (HIGH)**
```typescript
describe('Security', () => {
  it('should reject invalid JWT tokens', async () => {})
  it('should prevent XSS in message content', async () => {})
  it('should rate limit login attempts', async () => {})
  it('should sanitize file uploads', async () => {})
  it('should enforce HTTPS only', async () => {})
})
```

---

## Implementation Timeline

### Sprint 1 (Week 1-2): Critical Foundation
- Complete MobileIMSDK integration (1.1)
- Implement initial sync (1.2)
- Fix responsive mobile layout (1.3)

### Sprint 2 (Week 3-4): Feature Completion
- Message search (2.1)
- Message favorites (2.2)
- Push notifications setup (2.4)

### Sprint 3 (Week 5-6): Advanced Features
- Voice/video calls (2.3)
- Advanced group features (2.5)
- E2E testing

### Sprint 4 (Week 7-8): Polish & Deploy
- Performance optimizations (3.1)
- Accessibility (3.2)
- Production deployment
- Monitoring setup

---

## Success Metrics

### Technical Metrics
- 95%+ test coverage
- < 2s initial load time
- < 100ms message send latency
- 99.9% uptime
- < 1% error rate

### User Metrics
- Message delivery success rate > 99.5%
- Connection success rate > 99%
- Average reconnection time < 5s
- User satisfaction > 4.5/5

---

## Risk Mitigation

### High Risk Items
1. **IM Server Integration Complexity**
   - Mitigation: Allocate 2 weeks, pair programming
2. **Multi-Device Sync Edge Cases**
   - Mitigation: Extensive testing, staged rollout
3. **Performance with Large Message History**
   - Mitigation: IndexedDB caching, lazy loading
4. **WebRTC Call Quality**
   - Mitigation: Use proven TURN servers, fallback to relay

---

## Conclusion

This plan provides a clear roadmap to complete Lumi-Chat to production quality. Focus on Phase 1 (Critical Foundation) first, as it unblocks all other features. Phase 2 adds essential features users expect. Phase 3 is polish that can be done iteratively. Phase 4 (native clients) can proceed in parallel once the web platform is stable.

**Estimated Total Timeline:** 8-10 weeks to production-ready web app

**Next Immediate Actions:**
1. Start Sprint 1, Task 1.1 (MobileIMSDK integration)
2. Setup E2E testing infrastructure (Playwright/Cypress)
3. Create performance monitoring dashboard (Sentry, Grafana)
4. Schedule code review sessions for critical components
