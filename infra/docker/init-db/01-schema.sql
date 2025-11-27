-- Lumi-Chat Database Schema
-- Multi-device synchronization support

-- Users table
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
    phone VARCHAR(20),
    status VARCHAR(20) DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP,
    last_login_ip VARCHAR(50)
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_uid ON users(uid);
CREATE INDEX idx_users_status ON users(status);

-- User devices table (tracks all logged-in devices per user)
CREATE TABLE user_devices (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    device_id VARCHAR(64) UNIQUE NOT NULL,
    device_type VARCHAR(20) NOT NULL,
    device_name VARCHAR(100),
    push_token VARCHAR(500),
    push_platform VARCHAR(20),
    last_active_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_online BOOLEAN DEFAULT false,
    UNIQUE(user_id, device_id)
);

CREATE INDEX idx_user_devices_user ON user_devices(user_id);
CREATE INDEX idx_user_devices_online ON user_devices(user_id, is_online);
CREATE INDEX idx_user_devices_device_id ON user_devices(device_id);

-- Friend relationships
CREATE TABLE friendships (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    friend_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    remark VARCHAR(50),
    status VARCHAR(20) DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, friend_id)
);

CREATE INDEX idx_friendships_user ON friendships(user_id);
CREATE INDEX idx_friendships_friend ON friendships(friend_id);

-- Friend requests
CREATE TABLE friend_requests (
    id BIGSERIAL PRIMARY KEY,
    from_user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    to_user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    message VARCHAR(100),
    status VARCHAR(20) DEFAULT 'pending',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    handled_at TIMESTAMP
);

CREATE INDEX idx_friend_requests_to ON friend_requests(to_user_id, status);
CREATE INDEX idx_friend_requests_from ON friend_requests(from_user_id);

-- Groups
CREATE TABLE groups (
    id BIGSERIAL PRIMARY KEY,
    gid VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(30) NOT NULL,
    avatar VARCHAR(500),
    owner_id BIGINT REFERENCES users(id),
    creator_id BIGINT REFERENCES users(id),
    announcement TEXT,
    max_members INT DEFAULT 500,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_groups_gid ON groups(gid);
CREATE INDEX idx_groups_owner ON groups(owner_id);

-- Group members
CREATE TABLE group_members (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT REFERENCES groups(id) ON DELETE CASCADE,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    nickname VARCHAR(25),
    role VARCHAR(20) DEFAULT 'member',
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    invited_by BIGINT REFERENCES users(id),
    UNIQUE(group_id, user_id)
);

CREATE INDEX idx_group_members_group ON group_members(group_id);
CREATE INDEX idx_group_members_user ON group_members(user_id);

-- Conversations
CREATE TABLE conversations (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(20) NOT NULL,
    participant_ids BIGINT[],
    group_id BIGINT REFERENCES groups(id) ON DELETE CASCADE,
    last_msg_id BIGINT,
    last_msg_time TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_conversations_participants ON conversations USING GIN(participant_ids);
CREATE INDEX idx_conversations_group ON conversations(group_id);
CREATE INDEX idx_conversations_last_msg_time ON conversations(last_msg_time DESC);

-- Per-user conversation state (for multi-device sync)
CREATE TABLE user_conversations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    conversation_id BIGINT REFERENCES conversations(id) ON DELETE CASCADE,
    unread_count INT DEFAULT 0,
    is_muted BOOLEAN DEFAULT false,
    is_pinned BOOLEAN DEFAULT false,
    is_hidden BOOLEAN DEFAULT false,
    last_read_msg_id BIGINT,
    at_msg_ids BIGINT[],
    draft TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, conversation_id)
);

CREATE INDEX idx_user_conv_user ON user_conversations(user_id);
CREATE INDEX idx_user_conv_conversation ON user_conversations(conversation_id);

-- Messages
CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    msg_id VARCHAR(36) UNIQUE NOT NULL,
    conversation_id BIGINT REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id BIGINT REFERENCES users(id),
    sender_device_id VARCHAR(64),
    msg_type VARCHAR(20) NOT NULL,
    content TEXT,
    metadata JSONB,
    quote_msg_id VARCHAR(36),
    at_user_ids BIGINT[],
    client_created_at TIMESTAMP,
    server_created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    recalled_at TIMESTAMP
);

CREATE INDEX idx_messages_conversation ON messages(conversation_id, server_created_at DESC);
CREATE INDEX idx_messages_msg_id ON messages(msg_id);
CREATE INDEX idx_messages_server_time ON messages(server_created_at);
CREATE INDEX idx_messages_sender ON messages(sender_id);

-- Message sync status per device
CREATE TABLE message_sync_status (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    device_id VARCHAR(64),
    conversation_id BIGINT REFERENCES conversations(id) ON DELETE CASCADE,
    last_synced_msg_id BIGINT,
    last_synced_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, device_id, conversation_id)
);

CREATE INDEX idx_sync_status_device ON message_sync_status(device_id);
CREATE INDEX idx_sync_status_user ON message_sync_status(user_id);

-- Read status (shared across devices)
CREATE TABLE message_read_status (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    conversation_id BIGINT REFERENCES conversations(id) ON DELETE CASCADE,
    last_read_msg_id BIGINT,
    last_read_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, conversation_id)
);

CREATE INDEX idx_read_status_user ON message_read_status(user_id);

-- Offline message queue (per device)
CREATE TABLE offline_messages (
    id BIGSERIAL PRIMARY KEY,
    target_user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    target_device_id VARCHAR(64),
    message_id BIGINT REFERENCES messages(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    delivered_at TIMESTAMP,
    expired_at TIMESTAMP DEFAULT (CURRENT_TIMESTAMP + INTERVAL '7 days')
);

CREATE INDEX idx_offline_target ON offline_messages(target_user_id, target_device_id, delivered_at);
CREATE INDEX idx_offline_expires ON offline_messages(expired_at);

-- Files metadata
CREATE TABLE files (
    id BIGSERIAL PRIMARY KEY,
    file_id VARCHAR(36) UNIQUE NOT NULL,
    uploader_id BIGINT REFERENCES users(id),
    original_name VARCHAR(255),
    stored_name VARCHAR(255),
    mime_type VARCHAR(100),
    size_bytes BIGINT,
    bucket VARCHAR(50),
    path VARCHAR(500),
    thumbnail_path VARCHAR(500),
    width INT,
    height INT,
    duration INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_files_file_id ON files(file_id);
CREATE INDEX idx_files_uploader ON files(uploader_id);

-- System notifications
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(100),
    content TEXT,
    metadata JSONB,
    is_read BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_user ON notifications(user_id, is_read);

-- Audit log for important actions
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    action VARCHAR(50) NOT NULL,
    target_type VARCHAR(50),
    target_id BIGINT,
    metadata JSONB,
    ip_address VARCHAR(50),
    device_id VARCHAR(64),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_created ON audit_logs(created_at DESC);
