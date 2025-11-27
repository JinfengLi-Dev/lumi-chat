# Lumi-Chat System Architecture

## Overview

Lumi-Chat is a cross-platform instant messaging system built on MobileIMSDK, providing real-time communication across Web, iOS, Android, and PC platforms with full multi-device synchronization.

Key Feature: Users can log in from multiple devices simultaneously and access all messages and history from any device.

---

## Multi-Device Synchronization Architecture

### Core Principles

1. Server-Centric Message Storage: All messages stored on server, clients sync from server
2. Multi-Session Support: One user can have multiple active sessions (devices)
3. Real-Time Sync: Messages, read status, recalls sync to all devices instantly
4. Offline Queue: Per-device offline message queues

### Sync Architecture Diagram

```
                                 ┌─────────────────────────────┐
                                 │      User Account           │
                                 │      (Single Identity)      │
                                 └──────────────┬──────────────┘
                                                │
                 ┌──────────────┬───────────────┼───────────────┬──────────────┐
                 │              │               │               │              │
                 ▼              ▼               ▼               ▼              ▼
           ┌──────────┐  ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐
           │   Web    │  │   iOS    │   │ Android  │   │    PC    │   │  Tablet  │
           │ Browser  │  │  Phone   │   │  Phone   │   │  Client  │   │   App    │
           └────┬─────┘  └────┬─────┘   └────┬─────┘   └────┬─────┘   └────┬─────┘
                │             │              │              │              │
                │   Each device has unique deviceId + session              │
                │             │              │              │              │
                └─────────────┴──────────────┴──────────────┴──────────────┘
                                             │
                                             ▼
                              ┌──────────────────────────────┐
                              │     Message Router Service    │
                              │                              │
                              │  - Route to ALL user devices │
                              │  - Track per-device delivery │
                              │  - Manage offline queues     │
                              └──────────────────────────────┘
```

### Message Delivery to Multiple Devices

```
┌─────────┐                    ┌─────────────┐                    ┌─────────────────┐
│ Sender  │                    │  IM Server  │                    │ Receiver's      │
│ Device  │                    │             │                    │ All Devices     │
└────┬────┘                    └──────┬──────┘                    └────────┬────────┘
     │                                │                                    │
     │  1. Send Message               │                                    │
     │───────────────────────────────>│                                    │
     │                                │                                    │
     │  2. Store in Database          │                                    │
     │                                │                                    │
     │  3. ACK to Sender              │                                    │
     │<───────────────────────────────│                                    │
     │                                │                                    │
     │                                │  4. Lookup ALL receiver devices    │
     │                                │     from Redis session store       │
     │                                │                                    │
     │                                │  5. Fan-out to EACH online device  │
     │                                │────────────────────────────────────>│
     │                                │     (Web, iOS, Android, PC...)     │
     │                                │                                    │
     │                                │  6. Queue for offline devices      │
     │                                │     (per-device offline queue)     │
     │                                │                                    │
     │                                │  7. Sync to Sender's other devices │
     │                                │────────────────────────────────────>│
     │                                │     (Sender sees own msg on all)   │
     │                                │                                    │
```

---

## High-Level Architecture

```
                                    ┌──────────────────┐
                                    │   Load Balancer  │
                                    │    (Nginx/ALB)   │
                                    └────────┬─────────┘
                                             │
         ┌───────────────┬───────────────────┼───────────────────┬───────────────┐
         │               │                   │                   │               │
         ▼               ▼                   ▼                   ▼               ▼
   ┌───────────┐   ┌───────────┐      ┌───────────┐      ┌───────────┐   ┌───────────┐
   │    Web    │   │    iOS    │      │  Android  │      │    PC     │   │  Tablet   │
   │  Client   │   │  Client   │      │  Client   │      │  Client   │   │  Client   │
   │  (Vue 3)  │   │  (Swift)  │      │ (Kotlin)  │      │(Electron) │   │   (RN)    │
   │ WebSocket │   │  UDP/TCP  │      │  UDP/TCP  │      │ WebSocket │   │  UDP/TCP  │
   └─────┬─────┘   └─────┬─────┘      └─────┬─────┘      └─────┬─────┘   └─────┬─────┘
         │               │                  │                  │               │
         │               └──────────────────┼──────────────────┘               │
         │                     MobileIMSDK Client Library                      │
         └──────────────────────────────────┼──────────────────────────────────┘
                                            │
                           ┌────────────────┴────────────────┐
                           │                                 │
                           ▼                                 ▼
                ┌─────────────────┐               ┌─────────────────┐
                │  MobileIMSDK    │               │   REST API      │
                │    Server       │◄─────────────►│   Server        │
                │  (Netty/Java)   │   Internal    │  (Spring Boot)  │
                │                 │     Sync      │                 │
                │  - Multi-device │               │  - Auth         │
                │    routing      │               │  - History      │
                │  - Presence     │               │  - Sync API     │
                │  - Offline Q    │               │  - Device Mgmt  │
                └────────┬────────┘               └────────┬────────┘
                         │                                 │
                         │          ┌──────────────────────┘
                         │          │
                         ▼          ▼
                ┌─────────────────────────────────────────────────────┐
                │                   Data Layer                        │
                │                                                     │
                │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐ │
                │  │ PostgreSQL  │  │    Redis    │  │    MinIO    │ │
                │  │             │  │   Cluster   │  │             │ │
                │  │ - Messages  │  │ - Sessions  │  │ - Media     │ │
                │  │   (source   │  │   (multi-   │  │ - Files     │ │
                │  │    of truth)│  │    device)  │  │             │ │
                │  │ - Users     │  │ - Presence  │  │             │ │
                │  │ - Sync logs │  │ - Pub/Sub   │  │             │ │
                │  └─────────────┘  └─────────────┘  └─────────────┘ │
                └─────────────────────────────────────────────────────┘
```

---

## Updated Database Schema (Multi-Device Support)

```sql
-- Users table (unchanged)
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(20) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    avatar VARCHAR(500),
    gender VARCHAR(10) DEFAULT 'unknown',
    signature VARCHAR(100),
    description TEXT,
    status VARCHAR(20) DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP,
    last_login_ip VARCHAR(50)
);

-- NEW: User devices table (tracks all logged-in devices)
CREATE TABLE user_devices (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    device_id VARCHAR(64) UNIQUE NOT NULL,      -- Unique device identifier
    device_type VARCHAR(20) NOT NULL,            -- 'web', 'ios', 'android', 'pc', 'tablet'
    device_name VARCHAR(100),                    -- "iPhone 15 Pro", "Chrome on Mac"
    push_token VARCHAR(500),                     -- FCM/APNS token for push
    last_active_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_online BOOLEAN DEFAULT false,
    UNIQUE(user_id, device_id)
);

CREATE INDEX idx_user_devices_user ON user_devices(user_id);
CREATE INDEX idx_user_devices_online ON user_devices(user_id, is_online);

-- NEW: Message sync status per device
CREATE TABLE message_sync_status (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    device_id VARCHAR(64) REFERENCES user_devices(device_id),
    conversation_id BIGINT REFERENCES conversations(id),
    last_synced_msg_id BIGINT,                   -- Last message this device received
    last_synced_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, device_id, conversation_id)
);

CREATE INDEX idx_sync_status_device ON message_sync_status(device_id);

-- NEW: Read receipts per device
CREATE TABLE message_read_status (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    conversation_id BIGINT REFERENCES conversations(id),
    last_read_msg_id BIGINT,                     -- Last message user has read
    last_read_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, conversation_id)
);

-- Messages table (updated with server timestamp)
CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    msg_id VARCHAR(36) UNIQUE NOT NULL,
    conversation_id BIGINT REFERENCES conversations(id),
    sender_id BIGINT REFERENCES users(id),
    sender_device_id VARCHAR(64),                -- Which device sent this
    msg_type VARCHAR(20) NOT NULL,
    content TEXT,
    metadata JSONB,
    quote_msg_id VARCHAR(36),
    at_user_ids BIGINT[],
    client_created_at TIMESTAMP,                 -- Client's timestamp
    server_created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- Server's timestamp (for sync ordering)
    recalled_at TIMESTAMP
);

CREATE INDEX idx_messages_conversation ON messages(conversation_id, server_created_at DESC);
CREATE INDEX idx_messages_msg_id ON messages(msg_id);
CREATE INDEX idx_messages_server_time ON messages(server_created_at);

-- NEW: Offline message queue (per device)
CREATE TABLE offline_messages (
    id BIGSERIAL PRIMARY KEY,
    target_user_id BIGINT REFERENCES users(id),
    target_device_id VARCHAR(64),                -- NULL means all devices
    message_id BIGINT REFERENCES messages(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    delivered_at TIMESTAMP,
    expired_at TIMESTAMP DEFAULT (CURRENT_TIMESTAMP + INTERVAL '7 days')
);

CREATE INDEX idx_offline_target ON offline_messages(target_user_id, target_device_id, delivered_at);

-- Conversations with per-user unread tracking
CREATE TABLE conversations (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(20) NOT NULL,
    participant_ids BIGINT[],
    group_id BIGINT REFERENCES groups(id),
    last_msg_id BIGINT,
    last_msg_time TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- NEW: Per-user conversation state
CREATE TABLE user_conversations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    conversation_id BIGINT REFERENCES conversations(id),
    unread_count INT DEFAULT 0,
    is_muted BOOLEAN DEFAULT false,
    is_pinned BOOLEAN DEFAULT false,
    is_hidden BOOLEAN DEFAULT false,
    last_read_msg_id BIGINT,
    at_msg_ids BIGINT[],                         -- Messages that @mentioned this user
    draft TEXT,                                   -- Unsent draft (synced across devices)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, conversation_id)
);

CREATE INDEX idx_user_conv_user ON user_conversations(user_id);
```

---

## Updated Redis Data Structures (Multi-Device)

```
# User Sessions - Multiple sessions per user
sessions:user:{userId} -> HASH {
    {deviceId1}: { token, deviceType, deviceName, loginAt, lastActiveAt },
    {deviceId2}: { token, deviceType, deviceName, loginAt, lastActiveAt },
    ...
}

# Device to User mapping (for quick lookup)
session:device:{deviceId} -> { userId, token, deviceType }

# User Online Devices (which devices are currently connected)
online:user:{userId} -> SET of deviceIds

# User Presence (aggregated - online if ANY device is online)
presence:{userId} -> {
    online: boolean,           # true if any device online
    lastSeen: timestamp,       # last activity across all devices
    activeDevices: count,      # number of online devices
    primaryDevice: deviceId    # most recently active device
}

# Per-device offline message queue
offline:{userId}:{deviceId} -> LIST of messageIds (FIFO)

# Unread counts (shared across devices - server tracks)
unread:{userId}:{conversationId} -> count

# Read position sync (last read message, shared)
read:{userId}:{conversationId} -> lastReadMsgId

# Sync cursor per device (for incremental sync)
sync:{userId}:{deviceId} -> lastSyncTimestamp

# Typing indicator (shows on all other devices)
typing:{conversationId}:{userId} -> { deviceId, timestamp }

# Message recall notification (broadcast to all user devices)
recall:{msgId} -> { recalledAt, recalledBy }

# Device push tokens
push:{userId} -> HASH {
    {deviceId1}: { token, platform, enabled },
    {deviceId2}: { token, platform, enabled },
    ...
}
```

---

## New API Endpoints (Multi-Device Support)

```
# Device Management
GET    /api/v1/devices                     # List all logged-in devices
DELETE /api/v1/devices/{deviceId}          # Logout specific device
DELETE /api/v1/devices                     # Logout all other devices

# Message Sync
GET    /api/v1/sync?since={timestamp}      # Get all changes since timestamp
GET    /api/v1/sync/messages?conversation={id}&since={msgId}  # Incremental sync
POST   /api/v1/sync/ack                    # Acknowledge sync position

# Read Status (synced across devices)
POST   /api/v1/conversations/{id}/read     # Mark as read (syncs to all devices)
GET    /api/v1/conversations/{id}/unread   # Get unread count

# History Pull
GET    /api/v1/conversations/{id}/messages?before={msgId}&limit=50

# Push Notification Registration
POST   /api/v1/devices/push-token          # Register push token for device
DELETE /api/v1/devices/push-token          # Unregister push token
```

---

## Multi-Device Message Flow

### 1. Send Message (Sync to Sender's Other Devices)

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Sender    │     │  IM Server  │     │  Sender's   │     │  Receiver   │
│  Device A   │     │             │     │  Device B   │     │  All Devices│
└──────┬──────┘     └──────┬──────┘     └──────┬──────┘     └──────┬──────┘
       │                   │                   │                   │
       │ 1. Send msg       │                   │                   │
       │──────────────────>│                   │                   │
       │                   │                   │                   │
       │ 2. ACK            │                   │                   │
       │<──────────────────│                   │                   │
       │                   │                   │                   │
       │                   │ 3. Sync to        │                   │
       │                   │    sender's other │                   │
       │                   │    devices        │                   │
       │                   │──────────────────>│                   │
       │                   │                   │                   │
       │                   │ 4. Deliver to     │                   │
       │                   │    all receiver   │                   │
       │                   │    devices        │                   │
       │                   │──────────────────────────────────────>│
       │                   │                   │                   │
```

### 2. Read Status Sync

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   User A    │     │  IM Server  │     │   User A    │
│  Phone      │     │             │     │   PC        │
└──────┬──────┘     └──────┬──────┘     └──────┬──────┘
       │                   │                   │
       │ 1. Mark as read   │                   │
       │──────────────────>│                   │
       │                   │                   │
       │                   │ 2. Update DB      │
       │                   │    read position  │
       │                   │                   │
       │                   │ 3. Notify other   │
       │                   │    devices        │
       │                   │──────────────────>│
       │                   │                   │
       │                   │    (PC shows      │
       │                   │     msgs as read) │
       │                   │                   │
```

### 3. Message Recall (Multi-Device)

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Sender    │     │  IM Server  │     │  Sender's   │     │  Receiver   │
│  Device     │     │             │     │  Other Dev  │     │  All Devices│
└──────┬──────┘     └──────┬──────┘     └──────┬──────┘     └──────┬──────┘
       │                   │                   │                   │
       │ 1. Recall msg     │                   │                   │
       │──────────────────>│                   │                   │
       │                   │                   │                   │
       │ 2. Validate       │                   │                   │
       │                   │                   │                   │
       │ 3. Success        │                   │                   │
       │<──────────────────│                   │                   │
       │                   │                   │                   │
       │                   │ 4. Notify ALL     │                   │
       │                   │    sender devices │                   │
       │                   │──────────────────>│                   │
       │                   │                   │                   │
       │                   │ 5. Notify ALL     │                   │
       │                   │    receiver devs  │                   │
       │                   │──────────────────────────────────────>│
       │                   │                   │                   │
```

---

## Sync Strategies

### Initial Sync (New Device Login)

1. Authenticate and register device
2. Pull conversation list with last messages
3. Pull recent messages per conversation (last 50)
4. Pull unread counts
5. Subscribe to real-time updates

```
Client                              Server
   │                                  │
   │  1. Login + deviceId             │
   │─────────────────────────────────>│
   │                                  │
   │  2. Token + sync cursor          │
   │<─────────────────────────────────│
   │                                  │
   │  3. GET /sync?since=0            │
   │     (full sync for new device)   │
   │─────────────────────────────────>│
   │                                  │
   │  4. Conversations + recent msgs  │
   │<─────────────────────────────────│
   │                                  │
   │  5. Connect WebSocket            │
   │─────────────────────────────────>│
   │                                  │
   │  6. Real-time updates            │
   │<═════════════════════════════════│
   │                                  │
```

### Incremental Sync (Device Reconnects)

1. Use last sync timestamp
2. Pull only new/changed data
3. Apply updates locally

```
GET /api/v1/sync?since=1701234567890

Response:
{
    "newMessages": [...],
    "recalledMessages": [...],
    "readStatusUpdates": [...],
    "conversationUpdates": [...],
    "syncCursor": 1701234999999
}
```

---

## Push Notification Strategy

### When to Send Push

1. Message to offline device: Send push with preview
2. @mention: High-priority push
3. Message to online device: No push (real-time delivery)
4. Muted conversation: Silent push / no push

### Push Payload

```json
{
    "notification": {
        "title": "Jack Jiang",
        "body": "Hello, how are you?",
        "badge": 5
    },
    "data": {
        "type": "message",
        "conversationId": "12345",
        "messageId": "msg_abc123",
        "senderId": "user_456"
    }
}
```

---

## Updated Tech Stack

| Component | Technology | Multi-Device Notes |
|-----------|------------|-------------------|
| Communication | MobileIMSDK | Supports multi-connection per user |
| Web | Vue 3 + TypeScript | WebSocket reconnect + sync |
| iOS | Swift + SwiftUI | Background sync + push |
| Android | Kotlin + Compose | WorkManager + FCM |
| PC | Electron + Vue | Same as web, desktop wrapper |
| Backend | Spring Boot 3 | Stateless, Redis for session |
| Database | PostgreSQL | Message source of truth |
| Cache | Redis Cluster | Session, presence, pub/sub |
| Push | FCM + APNS | Per-device token management |

---

## Key Implementation Considerations

### 1. Conflict Resolution
- Server timestamp is authoritative for ordering
- Last-write-wins for user settings
- Merge strategy for drafts (or latest wins)

### 2. Bandwidth Optimization
- Delta sync (only changes since last sync)
- Message pagination
- Image/file lazy loading
- Compression for large payloads

### 3. Offline Handling
- Local SQLite/IndexedDB cache on each device
- Queue outgoing messages when offline
- Sync on reconnect

### 4. Security
- Device-specific tokens
- Revoke token on logout
- Force logout from other devices
- Login notification to other devices

---

## Updated Implementation Stages

Stage 4 now becomes more critical:

Stage 4: Multi-Device MobileIMSDK Integration (4-5 days)
- Device registration and management
- Multi-session WebSocket handling
- Message routing to all user devices
- Sync protocol implementation
- Offline queue per device

New Stage 4.5: Sync & History (2-3 days)
- Initial sync on new device
- Incremental sync on reconnect
- Message history pull
- Read status sync

---

Last Updated: 2025-11-27
