# Lumi-Chat Comprehensive Test Plan

Generated: 2026-01-28
Companion to: COMPREHENSIVE_IMPROVEMENT_PLAN.md

---

## Test Strategy Overview

### Coverage Goals
- **Backend API:** 95% line coverage, 90% branch coverage
- **Backend IM Server:** 90% line coverage, 85% branch coverage
- **Frontend Components:** 90% line coverage
- **Frontend Stores:** 95% line coverage
- **Frontend Services:** 90% line coverage
- **E2E Critical Paths:** 80% user flow coverage
- **Integration Tests:** 100% API endpoint coverage

### Test Pyramid
```
              /\
             /E2E\         <- 10% (Critical user flows)
            /------\
           /  Integ \      <- 20% (API + Component integration)
          /----------\
         /   Unit     \    <- 70% (Functions, components, services)
        /--------------\
```

---

## 1. Backend API Tests

### 1.1 AuthController Tests

**File:** `services/api/src/test/java/com/lumichat/controller/AuthControllerIntegrationTest.java`

**Test Cases:**

```java
// Registration Tests
@Test void shouldRegisterNewUser()
@Test void shouldFailRegistrationWithDuplicateEmail()
@Test void shouldFailRegistrationWithInvalidEmail()
@Test void shouldFailRegistrationWithShortPassword()
@Test void shouldFailRegistrationWithMissingFields()
@Test void shouldGenerateUniqueUID()
@Test void shouldSendWelcomeEmail()

// Login Tests
@Test void shouldLoginSuccessfully()
@Test void shouldLoginWithUID()
@Test void shouldLoginWithEmail()
@Test void shouldFailWithWrongPassword()
@Test void shouldFailWithNonExistentUser()
@Test void shouldFailForInactiveUser()
@Test void shouldFailForBannedUser()
@Test void shouldRegisterDeviceOnLogin()
@Test void shouldUpdateLastLoginTime()

// Token Refresh Tests
@Test void shouldRefreshTokenSuccessfully()
@Test void shouldFailWithInvalidRefreshToken()
@Test void shouldFailWithExpiredRefreshToken()
@Test void shouldFailForInactiveUserOnRefresh()
@Test void shouldUpdateDeviceLastActive()

// Logout Tests
@Test void shouldLogoutSuccessfully()
@Test void shouldMarkDeviceOffline()
@Test void shouldClearPushToken()
@Test void shouldFailWithoutAuthentication()

// Password Reset Tests
@Test void shouldSendPasswordResetEmail()
@Test void shouldResetPasswordWithValidToken()
@Test void shouldFailResetWithInvalidToken()
@Test void shouldFailResetWithExpiredToken()
@Test void shouldFailResetWithAccessToken()
@Test void shouldPreventEmailEnumeration()

// Multi-Device Tests
@Test void shouldAllowMultipleDeviceLogin()
@Test void shouldGenerateUniqueTokensPerDevice()
@Test void shouldMaintainSeparateSessions()
@Test void shouldLogoutSpecificDevice()
```

**Coverage Target:** 95%+

---

### 1.2 UserController Tests

**File:** `services/api/src/test/java/com/lumichat/controller/UserControllerIntegrationTest.java`

**Test Cases:**

```java
// Profile Tests
@Test void shouldGetCurrentUserProfile()
@Test void shouldUpdateNickname()
@Test void shouldUpdateGender()
@Test void shouldUpdateSignature()
@Test void shouldUpdateDescription()
@Test void shouldUpdateMultipleFields()
@Test void shouldRejectTooLongNickname()
@Test void shouldRejectInvalidGender()

// Avatar Tests
@Test void shouldUploadAvatar()
@Test void shouldRejectOversizedAvatar()
@Test void shouldRejectInvalidImageFormat()
@Test void shouldReplaceExistingAvatar()

// UID Management Tests
@Test void shouldCheckUIDAvailability()
@Test void shouldUpdateUID()
@Test void shouldPreventDuplicateUID()
@Test void shouldRejectInvalidUIDFormat()

// Search Tests
@Test void shouldSearchUserByUID()
@Test void shouldSearchUserByEmail()
@Test void shouldSearchUserByPartialMatch()
@Test void shouldReturnEmptyForNoMatch()

// Password Change Tests
@Test void shouldChangePassword()
@Test void shouldFailWithWrongOldPassword()
@Test void shouldFailWithWeakNewPassword()
@Test void shouldRequireAuthentication()
```

**Coverage Target:** 95%+

---

### 1.3 ConversationController Tests

**File:** `services/api/src/test/java/com/lumichat/controller/ConversationControllerIntegrationTest.java`

**Test Cases:**

```java
// Get Conversations Tests
@Test void shouldGetAllUserConversations()
@Test void shouldSortByLastMessageTime()
@Test void shouldIncludeUnreadCounts()
@Test void shouldIncludeLastMessage()
@Test void shouldPaginateConversations()
@Test void shouldFilterHiddenConversations()

// Create Conversation Tests
@Test void shouldCreatePrivateConversation()
@Test void shouldCreateGroupConversation()
@Test void shouldReturnExistingPrivateConversation()
@Test void shouldFailWithInvalidParticipant()
@Test void shouldFailWithSelfConversation()

// Update Conversation Tests
@Test void shouldPinConversation()
@Test void shouldUnpinConversation()
@Test void shouldMuteConversation()
@Test void shouldUnmuteConversation()
@Test void shouldMarkAsRead()
@Test void shouldResetUnreadCount()

// Delete Conversation Tests
@Test void shouldDeleteConversation()
@Test void shouldHideConversation()
@Test void shouldPreserveForOtherParticipants()
@Test void shouldCascadeDeleteMessages()
```

**Coverage Target:** 95%+

---

### 1.4 MessageController Tests

**File:** `services/api/src/test/java/com/lumichat/controller/MessageControllerIntegrationTest.java`

**Test Cases:**

```java
// Send Message Tests
@Test void shouldSendTextMessage()
@Test void shouldSendImageMessage()
@Test void shouldSendFileMessage()
@Test void shouldSendVoiceMessage()
@Test void shouldSendVideoMessage()
@Test void shouldSendLocationMessage()
@Test void shouldSendUserCardMessage()
@Test void shouldSendGroupCardMessage()
@Test void shouldIncrementUnreadCount()
@Test void shouldUpdateConversationLastMessage()

// Get Messages Tests
@Test void shouldGetMessagesForConversation()
@Test void shouldPaginateMessages()
@Test void shouldSortByTimestamp()
@Test void shouldIncludeQuotedMessages()
@Test void shouldIncludeRecalledMessages()

// Quote/Reply Tests
@Test void shouldQuoteMessage()
@Test void shouldIncludeOriginalContent()
@Test void shouldHandleRecalledOriginal()
@Test void shouldPreventCircularQuotes()

// Recall Tests
@Test void shouldRecallOwnMessage()
@Test void shouldFailRecallAfterTimeLimit()
@Test void shouldFailRecallOthersMessage()
@Test void shouldUpdateQuotedReferences()
@Test void shouldNotifyAllDevices()

// Forward Tests
@Test void shouldForwardToFriend()
@Test void shouldForwardToGroup()
@Test void shouldForwardMultipleMessages()
@Test void shouldPreserveOriginalContent()

// Delete Tests
@Test void shouldDeleteOwnMessage()
@Test void shouldDeleteOnlyForSelf()
@Test void shouldNotDeleteForOthers()

// Read Status Tests
@Test void shouldMarkMessagesAsRead()
@Test void shouldUpdateReadPosition()
@Test void shouldSyncReadStatusToDevices()
```

**Coverage Target:** 95%+

---

### 1.5 FriendController Tests

**File:** `services/api/src/test/java/com/lumichat/controller/FriendControllerIntegrationTest.java`

**Test Cases:**

```java
// Get Friends Tests
@Test void shouldGetAllFriends()
@Test void shouldSortAlphabetically()
@Test void shouldIncludeOnlineStatus()
@Test void shouldIncludeRemark()

// Friend Request Tests
@Test void shouldSendFriendRequest()
@Test void shouldIncludeRequestMessage()
@Test void shouldPreventDuplicateRequests()
@Test void shouldAutoAcceptReverseRequest()
@Test void shouldNotifyRecipient()

// Accept Request Tests
@Test void shouldAcceptFriendRequest()
@Test void shouldCreateBidirectionalFriendship()
@Test void shouldRemoveRequest()
@Test void shouldNotifyBothUsers()

// Reject Request Tests
@Test void shouldRejectFriendRequest()
@Test void shouldRemoveRequest()
@Test void shouldNotAddFriend()
@Test void shouldNotifyRequestor()

// Get Requests Tests
@Test void shouldGetReceivedRequests()
@Test void shouldGetSentRequests()
@Test void shouldIncludeTimestamps()
@Test void shouldSortByDate()

// Delete Friend Tests
@Test void shouldDeleteFriend()
@Test void shouldRemoveBothSides()
@Test void shouldDeleteSharedConversations()
@Test void shouldNotifyOtherUser()

// Update Remark Tests
@Test void shouldUpdateFriendRemark()
@Test void shouldReflectInFriendList()
@Test void shouldNotAffectOtherUser()

// Block/Unblock Tests
@Test void shouldBlockFriend()
@Test void shouldUnblockFriend()
@Test void shouldPreventMessagesWhenBlocked()
@Test void shouldHideBlockedFromOnlineList()
```

**Coverage Target:** 95%+

---

### 1.6 GroupController Tests

**File:** `services/api/src/test/java/com/lumichat/controller/GroupControllerIntegrationTest.java`

**Test Cases:**

```java
// Create Group Tests
@Test void shouldCreateGroupWithMembers()
@Test void shouldGenerateUniqueGID()
@Test void shouldSetCreatorAsOwner()
@Test void shouldAddAllMembers()
@Test void shouldCreateGroupConversation()

// Get Group Tests
@Test void shouldGetGroupInfo()
@Test void shouldIncludeMemberCount()
@Test void shouldIncludeOwnerInfo()
@Test void shouldIncludeAnnouncement()

// Get Groups Tests
@Test void shouldGetUserGroups()
@Test void shouldIncludeLastMessage()
@Test void shouldIncludeUnreadCount()
@Test void shouldSortByActivity()

// Update Group Tests
@Test void shouldUpdateGroupName()
@Test void shouldUpdateAnnouncement()
@Test void shouldRequireOwnerPermission()
@Test void shouldNotifyAllMembers()

// Get Members Tests
@Test void shouldGetAllMembers()
@Test void shouldIncludeRoles()
@Test void shouldIncludeJoinDate()
@Test void shouldIncludeCustomNicknames()

// Add Members Tests
@Test void shouldAddMembers()
@Test void shouldNotifyNewMembers()
@Test void shouldNotifyExistingMembers()
@Test void shouldCheckMaxMembers()
@Test void shouldCreateSystemMessage()

// Remove Members Tests
@Test void shouldRemoveMember()
@Test void shouldRequireOwnerOrAdmin()
@Test void shouldNotRemoveOwner()
@Test void shouldNotifyRemovedMember()
@Test void shouldCreateSystemMessage()

// Transfer Ownership Tests
@Test void shouldTransferOwnership()
@Test void shouldUpdateOldOwnerRole()
@Test void shouldUpdateNewOwnerRole()
@Test void shouldRequireOwnerPermission()
@Test void shouldNotifyAllMembers()

// Leave Group Tests
@Test void shouldLeaveGroup()
@Test void shouldRemoveMembership()
@Test void shouldNotifyOthers()
@Test void shouldFailIfOwner()

// Delete Group Tests
@Test void shouldDeleteGroup()
@Test void shouldRequireOwnerPermission()
@Test void shouldRemoveAllMemberships()
@Test void shouldDeleteConversation()
@Test void shouldNotifyAllMembers()
```

**Coverage Target:** 95%+

---

## 2. Backend IM Server Tests

### 2.1 Message Routing Tests

**File:** `services/im-server/src/test/java/com/lumichat/im/routing/MessageRouterTest.java`

**Test Cases:**

```java
// Single Device Routing
@Test void shouldRouteToOnlineRecipient()
@Test void shouldQueueForOfflineRecipient()
@Test void shouldAcknowledgeDelivery()

// Multi-Device Routing
@Test void shouldRouteToAllRecipientDevices()
@Test void shouldSyncToSenderDevices()
@Test void shouldHandleMixedOnlineOffline()
@Test void shouldTrackPerDeviceDelivery()

// Group Message Routing
@Test void shouldFanoutToAllGroupMembers()
@Test void shouldExcludeSender()
@Test void shouldHandleOfflineMembers()

// QoS Tests
@Test void shouldRetryFailedDelivery()
@Test void shouldRequestAcknowledgment()
@Test void shouldTimeoutUnacknowledged()
@Test void shouldDedupDuplicates()
```

**Coverage Target:** 90%+

---

### 2.2 Session Management Tests

**File:** `services/im-server/src/test/java/com/lumichat/im/session/SessionManagerTest.java`

**Test Cases:**

```java
// Session Lifecycle
@Test void shouldRegisterDevice()
@Test void shouldTrackOnlineStatus()
@Test void shouldUnregisterOnDisconnect()
@Test void shouldCleanupExpiredSessions()

// Multi-Device Sessions
@Test void shouldTrackMultipleDevices()
@Test void shouldGetAllUserDevices()
@Test void shouldRemoveSpecificDevice()
@Test void shouldPreserveOtherDevices()

// Presence Tests
@Test void shouldUpdatePresence()
@Test void shouldBroadcastPresenceChanges()
@Test void shouldAggregateMultiDevicePresence()
@Test void shouldHandleLastSeenTime()
```

**Coverage Target:** 90%+

---

### 2.3 Offline Queue Tests

**File:** `services/im-server/src/test/java/com/lumichat/im/offline/OfflineQueueTest.java`

**Test Cases:**

```java
// Queue Management
@Test void shouldEnqueueForOfflineDevice()
@Test void shouldPreserveOrder()
@Test void shouldLimitQueueSize()
@Test void shouldExpireOldMessages()

// Delivery Tests
@Test void shouldDeliverOnReconnect()
@Test void shouldClearAfterDelivery()
@Test void shouldHandlePartialDelivery()
@Test void shouldRetryFailedDelivery()

// Multi-Device Tests
@Test void shouldMaintainSeparateQueues()
@Test void shouldDeliverToCorrectDevice()
@Test void shouldNotDuplicateAcrossDevices()
```

**Coverage Target:** 90%+

---

## 3. Frontend Component Tests

### 3.1 Chat Component Tests

**File:** `apps/web/src/components/chat/__tests__/Chat.test.ts`

**Test Cases:**

```typescript
// Rendering Tests
it('should render conversation list')
it('should render selected conversation')
it('should render empty state')
it('should render loading skeletons')

// Interaction Tests
it('should select conversation on click')
it('should show context menu on right-click')
it('should pin/unpin conversation')
it('should mute/unmute conversation')
it('should delete conversation')

// Message Tests
it('should send text message')
it('should send image message')
it('should send file message')
it('should quote message')
it('should forward message')
it('should recall message')

// Real-time Updates
it('should update on new message')
it('should update on message recall')
it('should update on read status')
it('should show typing indicator')

// Keyboard Navigation
it('should navigate with arrow keys')
it('should select with Enter key')
it('should use Ctrl+Enter for newline')
```

**Coverage Target:** 90%+

---

### 3.2 Message Component Tests

**File:** `apps/web/src/components/chat/__tests__/MessageBubble.test.ts`

**Test Cases:**

```typescript
// Text Message Tests
it('should render text message')
it('should render emoji')
it('should render links as clickable')
it('should sanitize HTML')

// Rich Message Tests
it('should render image with preview')
it('should render file with download link')
it('should render voice with playback')
it('should render video with thumbnail')
it('should render location with map')
it('should render user card')
it('should render group card')

// Quote Tests
it('should show quoted message')
it('should handle recalled quote')
it('should navigate to original')

// Status Tests
it('should show sending indicator')
it('should show sent checkmark')
it('should show delivered checkmarks')
it('should show read checkmarks')

// Actions Tests
it('should show context menu')
it('should copy message')
it('should quote message')
it('should forward message')
it('should recall message')
it('should delete message')
```

**Coverage Target:** 90%+

---

### 3.3 Friend Component Tests

**File:** `apps/web/src/components/contact/__tests__/FriendsList.test.ts`

**Test Cases:**

```typescript
// List Tests
it('should render friend list')
it('should sort alphabetically')
it('should show online status')
it('should show remark if set')
it('should filter by search')

// Add Friend Tests
it('should open add dialog')
it('should search users')
it('should send friend request')
it('should show request message')

// Friend Requests Tests
it('should show received requests')
it('should show sent requests')
it('should accept request')
it('should reject request')

// Actions Tests
it('should start chat')
it('should view profile')
it('should update remark')
it('should delete friend')
it('should block friend')
```

**Coverage Target:** 90%+

---

### 3.4 Group Component Tests

**File:** `apps/web/src/components/group/__tests__/GroupsList.test.ts`

**Test Cases:**

```typescript
// List Tests
it('should render group list')
it('should show member count')
it('should show last message')
it('should show unread count')
it('should filter by search')

// Create Group Tests
it('should open create dialog')
it('should select friends')
it('should set group name')
it('should create group')

// Group Info Tests
it('should show group details')
it('should show member list')
it('should edit group name')
it('should edit announcement')

// Member Management Tests
it('should invite members')
it('should remove members')
it('should transfer ownership')
it('should set custom nicknames')

// Actions Tests
it('should leave group')
it('should dissolve group')
it('should mute group')
```

**Coverage Target:** 90%+

---

## 4. Frontend Store Tests

### 4.1 Chat Store Tests

**File:** `apps/web/src/stores/__tests__/chat.test.ts`

**Test Cases:**

```typescript
// State Management
it('should initialize state')
it('should set conversations')
it('should add conversation')
it('should remove conversation')
it('should update conversation')

// Message Management
it('should add message')
it('should update message')
it('should remove message')
it('should mark as read')
it('should get unread count')

// Sorting Tests
it('should sort by timestamp')
it('should prioritize pinned')
it('should handle unread messages')

// Pagination Tests
it('should load more messages')
it('should detect hasMore')
it('should handle empty results')

// Real-time Updates
it('should handle new message')
it('should handle message recall')
it('should handle read status')
it('should handle typing indicator')

// Multi-Device Sync
it('should sync from other device')
it('should merge offline messages')
it('should update read position')
```

**Coverage Target:** 95%+

---

### 4.2 User Store Tests

**File:** `apps/web/src/stores/__tests__/user.test.ts`

**Test Cases:**

```typescript
// Authentication
it('should login successfully')
it('should logout successfully')
it('should refresh token')
it('should handle expired token')

// Profile Management
it('should get current user')
it('should update profile')
it('should upload avatar')
it('should update UID')

// Device Management
it('should get devices')
it('should logout device')
it('should track current device')

// Persistence
it('should persist token')
it('should restore session')
it('should clear on logout')
```

**Coverage Target:** 95%+

---

### 4.3 WebSocket Store Tests

**File:** `apps/web/src/stores/__tests__/websocket.test.ts`

**Test Cases:**

```typescript
// Connection
it('should connect successfully')
it('should disconnect')
it('should reconnect on failure')
it('should handle auth failure')

// Message Handling
it('should receive messages')
it('should send messages')
it('should acknowledge delivery')

// Event Handling
it('should emit message event')
it('should emit typing event')
it('should emit online status event')

// Error Handling
it('should handle connection error')
it('should handle timeout')
it('should retry with backoff')
```

**Coverage Target:** 95%+

---

## 5. Integration Tests

### 5.1 Authentication Flow Tests

**File:** `apps/web/tests/integration/auth.test.ts`

**Test Cases:**

```typescript
// Registration Flow
it('should complete full registration flow')
it('should send welcome email')
it('should auto-login after registration')
it('should create user devices')

// Login Flow
it('should login with email')
it('should login with UID')
it('should register device')
it('should fetch user profile')
it('should connect websocket')

// Password Reset Flow
it('should request password reset')
it('should send reset email')
it('should reset password with token')
it('should fail with expired token')

// Multi-Device Flow
it('should login from multiple devices')
it('should show all devices')
it('should logout specific device')
```

**Coverage Target:** 100% of flows

---

### 5.2 Messaging Flow Tests

**File:** `apps/web/tests/integration/messaging.test.ts`

**Test Cases:**

```typescript
// Send/Receive Flow
it('should send and receive text message')
it('should send and receive image')
it('should send and receive file')
it('should update conversation list')
it('should increment unread count')

// Multi-Device Flow
it('should sync to sender other devices')
it('should sync to receiver all devices')
it('should sync read status')

// Offline Flow
it('should queue for offline device')
it('should deliver on reconnect')
it('should not duplicate messages')

// Group Flow
it('should send to group')
it('should deliver to all members')
it('should handle member offline')
```

**Coverage Target:** 100% of flows

---

### 5.3 Friend System Flow Tests

**File:** `apps/web/tests/integration/friend.test.ts`

**Test Cases:**

```typescript
// Add Friend Flow
it('should search user')
it('should send friend request')
it('should notify recipient')
it('should accept request')
it('should create friendship')
it('should create conversation')

// Delete Friend Flow
it('should delete friend')
it('should remove from both sides')
it('should delete conversation')
it('should notify other user')
```

**Coverage Target:** 100% of flows

---

## 6. End-to-End Tests

### 6.1 Critical User Journeys

**Framework:** Playwright

**Test Cases:**

```typescript
// New User Journey
test('should complete new user registration and first chat', async ({ page }) => {
  // 1. Register new account
  await page.goto('/register')
  await page.fill('[name="email"]', 'newuser@test.com')
  await page.fill('[name="password"]', 'password123')
  await page.fill('[name="nickname"]', 'New User')
  await page.click('[type="submit"]')

  // 2. Verify welcome page
  await expect(page).toHaveURL('/chat')

  // 3. Search for friend
  await page.click('[data-testid="add-friend"]')
  await page.fill('[placeholder="Search"]', 'friend@test.com')

  // 4. Send friend request
  await page.click('[data-testid="send-request"]')

  // 5. Wait for acceptance (simulated)

  // 6. Start chat
  await page.click('[data-testid="friend-item"]')

  // 7. Send message
  await page.fill('[placeholder="Type message"]', 'Hello!')
  await page.press('[placeholder="Type message"]', 'Enter')

  // 8. Verify message sent
  await expect(page.locator('.message-bubble')).toHaveText('Hello!')
})

// Multi-Device Journey
test('should sync messages across devices', async ({ browser }) => {
  const context1 = await browser.newContext()
  const context2 = await browser.newContext()

  const page1 = await context1.newPage()
  const page2 = await context2.newPage()

  // Login same user on two devices
  await loginAs(page1, 'user@test.com')
  await loginAs(page2, 'user@test.com')

  // Send message from device 1
  await sendMessage(page1, 'Test message')

  // Verify appears on device 2
  await expect(page2.locator('.message-bubble')).toHaveText('Test message')

  // Read on device 2
  await page2.click('.conversation-item')

  // Verify marked as read on device 1
  await expect(page1.locator('.read-indicator')).toBeVisible()
})

// Group Chat Journey
test('should create group and send message', async ({ page }) => {
  // 1. Open create group dialog
  await page.click('[data-testid="create-group"]')

  // 2. Select friends
  await page.click('[data-testid="friend-checkbox"]:nth-child(1)')
  await page.click('[data-testid="friend-checkbox"]:nth-child(2)')

  // 3. Set group name
  await page.fill('[name="groupName"]', 'Test Group')

  // 4. Create
  await page.click('[data-testid="confirm-create"]')

  // 5. Send message
  await sendMessage(page, 'Group message')

  // 6. Verify all members receive
  // (requires mock or separate user sessions)
})

// Offline Recovery Journey
test('should recover from network interruption', async ({ page, context }) => {
  await page.goto('/chat')

  // Go offline
  await context.setOffline(true)

  // Try to send message
  await sendMessage(page, 'Offline message')

  // Verify queued
  await expect(page.locator('.message-sending')).toBeVisible()

  // Go online
  await context.setOffline(false)

  // Wait for reconnection
  await page.waitForSelector('.connection-status.online')

  // Verify message sent
  await expect(page.locator('.message-sent')).toBeVisible()
})
```

**Coverage Target:** 80% of critical paths

---

## 7. Performance Tests

### 7.1 Load Tests

**Framework:** k6

**Test Scenarios:**

```javascript
// Message Load Test
export default function() {
  const token = login('user@test.com', 'password')

  // Send 1000 messages
  for (let i = 0; i < 1000; i++) {
    sendMessage(token, 'Conversation123', `Message ${i}`)
  }

  // Verify < 100ms average latency
  check(response, {
    'latency < 100ms': (r) => r.timings.duration < 100
  })
}

// Concurrent Users Test
export let options = {
  stages: [
    { duration: '1m', target: 50 },   // Ramp up to 50 users
    { duration: '5m', target: 50 },   // Stay at 50 users
    { duration: '1m', target: 100 },  // Ramp up to 100
    { duration: '5m', target: 100 },  // Stay at 100
    { duration: '1m', target: 0 },    // Ramp down
  ]
}

// Connection Test
export default function() {
  const ws = connect('ws://localhost:8080')

  ws.on('open', () => {
    ws.send(JSON.stringify({ type: 'LOGIN', token }))
  })

  ws.on('message', (msg) => {
    check(msg, {
      'receives messages': (m) => m !== null
    })
  })
}
```

**Performance Targets:**
- Message send latency: < 100ms (p95)
- Message receive latency: < 50ms (p95)
- Connection establishment: < 500ms
- Supports 100 concurrent users
- Supports 1000 messages/second throughput

---

### 7.2 Frontend Performance Tests

**Framework:** Lighthouse / WebPageTest

**Metrics:**

```javascript
// Performance Budget
const budget = {
  'first-contentful-paint': 1500,      // < 1.5s
  'largest-contentful-paint': 2500,    // < 2.5s
  'time-to-interactive': 3500,         // < 3.5s
  'cumulative-layout-shift': 0.1,      // < 0.1
  'total-blocking-time': 300,          // < 300ms
}

// Memory Tests
it('should not leak memory on navigation', async () => {
  const initialMemory = await getMemoryUsage()

  // Navigate 100 times
  for (let i = 0; i < 100; i++) {
    await navigateToChat()
    await navigateToSettings()
  }

  const finalMemory = await getMemoryUsage()

  // Memory should not grow > 20MB
  expect(finalMemory - initialMemory).toBeLessThan(20 * 1024 * 1024)
})

// Virtual Scroll Performance
it('should scroll 1000 messages smoothly', async () => {
  await loadMessages(1000)

  const fps = await measureFPS(() => {
    scrollTo('top')
    scrollTo('bottom')
  })

  // Should maintain 60fps
  expect(fps).toBeGreaterThan(55)
})
```

---

## 8. Security Tests

### 8.1 Authentication Security Tests

**Test Cases:**

```typescript
// Token Security
it('should reject expired JWT tokens')
it('should reject tampered tokens')
it('should reject tokens with wrong signature')
it('should enforce token refresh')
it('should revoke tokens on logout')

// Password Security
it('should hash passwords with bcrypt')
it('should reject weak passwords')
it('should rate limit login attempts')
it('should lock account after failures')
it('should enforce password complexity')

// Session Security
it('should use secure cookies')
it('should set httpOnly flag')
it('should set sameSite=strict')
it('should regenerate session on login')
it('should invalidate on logout')
```

---

### 8.2 Input Validation Tests

**Test Cases:**

```typescript
// XSS Prevention
it('should sanitize message content')
it('should escape HTML entities')
it('should strip script tags')
it('should sanitize image URLs')
it('should validate file uploads')

// SQL Injection Prevention
it('should use parameterized queries')
it('should validate user input')
it('should sanitize search queries')

// CSRF Protection
it('should validate CSRF tokens')
it('should reject requests without token')
it('should use double-submit cookies')

// File Upload Security
it('should validate file types')
it('should enforce size limits')
it('should scan for malware')
it('should strip EXIF data')
it('should prevent path traversal')
```

---

## 9. Accessibility Tests

### 9.1 WCAG 2.1 AA Compliance Tests

**Framework:** axe-core

**Test Cases:**

```typescript
// Keyboard Navigation
it('should be fully keyboard navigable')
it('should show focus indicators')
it('should support Tab navigation')
it('should support Arrow key navigation')
it('should support Enter/Space activation')
it('should trap focus in modals')

// Screen Reader Tests
it('should have ARIA labels')
it('should announce dynamic content')
it('should have proper heading structure')
it('should have alt text for images')
it('should have descriptive links')

// Color Contrast
it('should meet 4.5:1 contrast ratio')
it('should not rely on color alone')
it('should support high contrast mode')

// Responsive Design
it('should support 200% zoom')
it('should reflow content')
it('should not require horizontal scroll')
```

---

## 10. Test Execution Strategy

### 10.1 Continuous Integration

**Pipeline Stages:**

```yaml
# .github/workflows/test.yml
name: Test Suite

on: [push, pull_request]

jobs:
  unit-tests:
    - Backend unit tests
    - Frontend unit tests
    - 95% coverage requirement

  integration-tests:
    - API integration tests
    - Component integration tests

  e2e-tests:
    - Critical path tests (on main branch only)
    - Run on test environment

  performance-tests:
    - Load tests (on main branch only)
    - Lighthouse audit

  security-tests:
    - Dependency scan
    - SAST analysis
    - Secret detection
```

### 10.2 Test Environments

**Environments:**
1. **Local:** Developer machines
2. **CI:** GitHub Actions
3. **Staging:** Pre-production environment
4. **Production:** Real user environment (monitoring only)

### 10.3 Test Data Management

**Strategy:**
- Use factories for test data generation
- Seed database with realistic data
- Clean up after each test
- Use separate test database
- Mock external services (email, SMS)

---

## 11. Test Maintenance

### 11.1 Test Code Quality

**Standards:**
- Tests should be readable and maintainable
- Use descriptive test names
- Follow AAA pattern (Arrange, Act, Assert)
- Avoid test interdependencies
- Keep tests fast (< 1s per test)
- Mock external dependencies

### 11.2 Test Coverage Monitoring

**Tools:**
- Backend: JaCoCo
- Frontend: Vitest coverage
- E2E: Playwright traces

**Reporting:**
- Generate coverage reports on CI
- Fail build if coverage drops
- Track coverage trends over time
- Identify uncovered code paths

---

## Summary

This comprehensive test plan ensures Lumi-Chat is thoroughly tested at all levels:
- **6,500+ unit tests** covering backend and frontend
- **500+ integration tests** covering API and components
- **50+ E2E tests** covering critical user journeys
- **Performance tests** ensuring scalability
- **Security tests** ensuring safety
- **Accessibility tests** ensuring inclusivity

**Total Estimated Tests:** 7,000+

**Target Execution Time:**
- Unit tests: < 5 minutes
- Integration tests: < 10 minutes
- E2E tests: < 30 minutes
- Full suite: < 45 minutes

**Next Steps:**
1. Setup CI/CD pipeline
2. Implement missing test suites
3. Achieve 95% coverage
4. Run nightly E2E tests
5. Monitor test trends
