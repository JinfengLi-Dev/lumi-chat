# Lumi-Chat Verified Issues Report

**Date:** 2025-11-27
**Verification Method:** Code review + Build testing
**Verified By:** Claude Code Assistant

---

## Executive Summary

After thorough verification of all implemented code, I found that **most features are "shells"** - they have frontend UI but the backend APIs don't exist. The application will NOT work as a complete system until the missing backend endpoints are implemented.

### Build Status
| Component | Build Status | Runtime Status |
|-----------|-------------|----------------|
| Backend API | BUILD SUCCESS | **Cannot test - no database running** |
| IM Server | BUILD SUCCESS | **Cannot test - no configuration** |
| Frontend | BUILD SUCCESS | **Will fail at runtime - missing APIs** |

---

## CRITICAL ISSUES (Application Breaking)

### Issue 1: Missing Backend Controllers

**Severity:** CRITICAL
**Impact:** Frontend will receive 404 errors for most API calls

The frontend makes calls to these endpoints that **DO NOT EXIST**:

| Frontend Calls | Backend Status | File |
|----------------|----------------|------|
| `GET /users/me` | MISSING | userApi.getCurrentUser() |
| `PUT /users/me` | MISSING | userApi.updateProfile() |
| `POST /users/me/avatar` | MISSING | userApi.uploadAvatar() |
| `GET /devices` | MISSING | userApi.getDevices() |
| `DELETE /devices/{id}` | MISSING | userApi.logoutDevice() |
| `DELETE /devices` | MISSING | userApi.logoutAllDevices() |
| `GET /users/search` | MISSING | userApi.searchUsers() |
| `GET /users/{uid}` | MISSING | userApi.getUserByUid() |
| `POST /auth/reset-password` | MISSING | authApi.resetPassword() |
| `POST /auth/change-password` | MISSING | authApi.changePassword() |

**Backend Controllers that exist:**
- `AuthController` - /auth/login, /auth/register, /auth/refresh, /auth/logout, /auth/forgot-password
- `HealthController` - /health

**Backend Controllers that are MISSING:**
- `UserController` - All user profile and device management
- `FriendController` - All friend operations
- `GroupController` - All group operations
- `ConversationController` - All conversation operations
- `MessageController` - All message operations
- `FileController` - All file upload/download operations

---

### Issue 2: Frontend API Calls Will Fail

**Severity:** CRITICAL
**Impact:** Application unusable after login

**Verified flow analysis:**

1. User opens Login page - **WORKS** (static page)
2. User submits login - **WORKS** (API exists)
3. After login, frontend calls `fetchCurrentUser()` - **FAILS** (API missing)
4. User tries to view profile - **FAILS** (API missing)
5. User tries to view devices - **FAILS** (API missing)
6. User tries chat - **FAILS** (Conversation API missing)
7. User tries to send message - **FAILS** (Message API missing)

---

### Issue 3: Forgot Password Not Implemented

**Severity:** HIGH
**Impact:** Users cannot recover accounts

**File:** [services/api/src/main/java/com/lumichat/controller/AuthController.java:53-57](../services/api/src/main/java/com/lumichat/controller/AuthController.java#L53-L57)

```java
@PostMapping("/forgot-password")
public ApiResponse<Void> forgotPassword(@RequestBody ForgotPasswordRequest request) {
    // TODO: Implement password reset email
    return ApiResponse.success();  // Returns success but does nothing!
}
```

The endpoint exists but:
- No email service is configured
- No password reset token generation
- No reset-password endpoint to complete the flow
- Frontend ForgotPassword.vue expects a verification code flow that doesn't exist

---

### Issue 4: Chat Store Calls Non-Existent APIs

**Severity:** CRITICAL
**Impact:** Chat functionality completely broken

**File:** [apps/web/src/stores/chat.ts](../apps/web/src/stores/chat.ts) references APIs that don't exist:

| Store Method | API Called | Backend Status |
|--------------|-----------|----------------|
| fetchConversations() | GET /conversations | MISSING |
| fetchMessages() | GET /messages | MISSING |
| sendMessage() | POST /messages | MISSING |
| recallMessage() | PUT /messages/{id}/recall | MISSING |
| forwardMessage() | POST /messages/{id}/forward | MISSING |
| deleteMessage() | DELETE /messages/{id} | MISSING |
| markAsRead() | POST /conversations/{id}/read | MISSING |

---

### Issue 5: User Store Login Flow Incomplete

**Severity:** HIGH
**Impact:** User state inconsistent after login

**File:** [apps/web/src/stores/user.ts](../apps/web/src/stores/user.ts)

After login succeeds:
1. `setAuth()` is called - stores token and user from login response - **WORKS**
2. But login response doesn't include full user data (only what AuthService returns)
3. App may try to `fetchCurrentUser()` which calls `/users/me` - **FAILS**

---

## MEDIUM ISSUES (Feature Incomplete)

### Issue 6: Chat.vue Handlers Empty

**Severity:** MEDIUM
**Impact:** Add Friend and Create Group buttons don't work

**File:** [apps/web/src/views/Chat.vue](../apps/web/src/views/Chat.vue)

```typescript
// These handlers are referenced but implementation is missing
function handleAddFriend() {
  // TODO: Show add friend dialog
}

function handleCreateGroup() {
  // TODO: Show create group dialog
}
```

---

### Issue 7: ChatConversation.vue Toolbar Non-Functional

**Severity:** MEDIUM
**Impact:** Cannot send images, files, voice, location, etc.

**File:** [apps/web/src/views/ChatConversation.vue](../apps/web/src/views/ChatConversation.vue)

All toolbar buttons are displayed but have no click handlers:
- Emoji picker - no implementation
- File upload - no implementation
- Image upload - no implementation
- Voice recording - no implementation
- Location sharing - no implementation
- Personal card - no implementation
- Group card - no implementation

---

### Issue 8: Context Menu Not Implemented

**Severity:** MEDIUM
**Impact:** Cannot recall, forward, quote, or delete messages

**File:** ChatConversation.vue likely has empty `handleContextMenu()` function.

---

### Issue 9: Settings Page Calls Non-Existent APIs

**Severity:** MEDIUM
**Impact:** Profile updates, password changes won't work

**File:** [apps/web/src/views/Settings.vue](../apps/web/src/views/Settings.vue)

- Profile update calls `userApi.updateProfile()` - API missing
- Avatar upload calls `userApi.uploadAvatar()` - API missing
- Password change calls `authApi.changePassword()` - API missing
- Device list calls `userApi.getDevices()` - API missing
- Device logout calls `userApi.logoutDevice()` - API missing

---

## LOW ISSUES (Non-Critical)

### Issue 10: No WebSocket Integration

**Severity:** LOW (for initial testing)
**Impact:** No real-time message delivery

- IM Server exists but not connected to frontend
- No WebSocket client in frontend code
- Messages would need manual refresh

---

### Issue 11: No File Storage Service

**Severity:** LOW
**Impact:** Cannot upload avatars, images, files

- MinIO is configured in application.yml
- No MinIO service class exists
- No file upload endpoint exists

---

### Issue 12: Missing Entity Mappings

**Severity:** LOW
**Impact:** Some database fields may not map correctly

Need to verify all JPA entities match the database schema exactly.

---

## Verification Steps Performed

1. **Backend Build Test**
   ```
   cd services/api && ./gradlew build -x test
   Result: BUILD SUCCESS
   ```

2. **IM Server Build Test**
   ```
   cd services/im-server && ./gradlew build -x test
   Result: BUILD SUCCESS
   ```

3. **Frontend Build Test**
   ```
   cd apps/web && npm install && npm run build
   Result: BUILD SUCCESS
   ```

4. **Controller Analysis**
   - Found only 2 controllers: AuthController, HealthController
   - Compared against frontend API calls
   - Identified 10+ missing endpoints

5. **Database Schema Review**
   - Schema file exists with 15+ tables
   - Schema is comprehensive for IM application
   - But no services/controllers use most tables

---

## Required Backend Controllers to Create

### 1. UserController.java
```
GET    /users/me           - Get current user profile
PUT    /users/me           - Update profile
POST   /users/me/avatar    - Upload avatar
PUT    /users/me/password  - Change password
GET    /users/search       - Search users
GET    /users/{uid}        - Get user by UID
```

### 2. DeviceController.java
```
GET    /devices            - List user devices
DELETE /devices/{id}       - Logout specific device
DELETE /devices            - Logout all devices
```

### 3. FriendController.java
```
GET    /friends            - List friends
POST   /friends/request    - Send friend request
GET    /friends/requests   - List friend requests
POST   /friends/requests/{id}/accept  - Accept request
POST   /friends/requests/{id}/reject  - Reject request
DELETE /friends/{id}       - Delete friend
PUT    /friends/{id}/remark - Set friend remark
POST   /friends/{id}/block - Block user
```

### 4. GroupController.java
```
GET    /groups             - List groups
POST   /groups             - Create group
GET    /groups/{id}        - Get group info
PUT    /groups/{id}        - Update group
DELETE /groups/{id}        - Delete group
GET    /groups/{id}/members - List members
POST   /groups/{id}/members - Add members
DELETE /groups/{id}/members/{uid} - Remove member
POST   /groups/{id}/transfer - Transfer ownership
POST   /groups/{id}/leave  - Leave group
```

### 5. ConversationController.java
```
GET    /conversations      - List conversations
GET    /conversations/{id} - Get conversation
DELETE /conversations/{id} - Delete conversation
POST   /conversations/{id}/read - Mark as read
PUT    /conversations/{id}/mute - Toggle mute
PUT    /conversations/{id}/pin  - Toggle pin
```

### 6. MessageController.java
```
GET    /conversations/{id}/messages - Get messages
POST   /messages           - Send message
PUT    /messages/{id}/recall - Recall message
POST   /messages/{id}/forward - Forward message
DELETE /messages/{id}      - Delete message
```

### 7. FileController.java
```
POST   /files/upload       - Upload file
GET    /files/{id}         - Download file
GET    /files/{id}/thumbnail - Get thumbnail
```

---

## Required Backend Services to Create

1. **UserService.java** - User profile management
2. **DeviceService.java** - Device management
3. **FriendService.java** - Friend operations
4. **FriendRequestService.java** - Friend request handling
5. **GroupService.java** - Group management
6. **GroupMemberService.java** - Group member operations
7. **ConversationService.java** - Conversation management
8. **MessageService.java** - Message operations
9. **FileStorageService.java** - MinIO file operations
10. **EmailService.java** - Password reset emails

---

## Recommended Fix Priority

### Phase 1: Make Login Flow Work (Priority: IMMEDIATE)
1. Create UserController with GET /users/me
2. Create DeviceController with GET /devices
3. Ensure login returns complete user data

### Phase 2: Make Profile Work (Priority: HIGH)
1. Complete UserController (update profile, avatar, password)
2. Create FileStorageService for avatar uploads
3. Complete DeviceController (logout device, logout all)

### Phase 3: Make Chat Work (Priority: HIGH)
1. Create ConversationController
2. Create ConversationService
3. Create MessageController
4. Create MessageService

### Phase 4: Make Friends Work (Priority: MEDIUM)
1. Create FriendController
2. Create FriendService
3. Create FriendRequestService

### Phase 5: Make Groups Work (Priority: MEDIUM)
1. Create GroupController
2. Create GroupService
3. Create GroupMemberService

### Phase 6: Real-time Features (Priority: LOW)
1. Integrate WebSocket client in frontend
2. Connect to IM Server
3. Implement real-time message delivery

---

## Conclusion

The Lumi-Chat application has:
- Good frontend UI scaffolding
- Good database schema
- Working authentication (login/register)
- Working build pipeline

But it is **NOT FUNCTIONAL** because:
- 90% of backend APIs are missing
- Frontend will crash with 404 errors after login
- No real functionality beyond authentication

**Estimated effort to make fully functional:**
- Backend: 3-4 weeks (10 controllers, 10 services)
- Frontend fixes: 1-2 weeks (handlers, components)
- Testing: 2 weeks
- **Total: 6-8 weeks**

---

Last Updated: 2025-11-27
