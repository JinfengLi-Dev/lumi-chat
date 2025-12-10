-- Offline message queue for devices that are offline
-- When a message is sent and the recipient device is offline,
-- the message is queued here for delivery when they come online

CREATE TABLE offline_messages (
    id BIGSERIAL PRIMARY KEY,
    target_user_id BIGINT NOT NULL,
    target_device_id VARCHAR(64),
    message_id BIGINT NOT NULL,
    conversation_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    delivered_at TIMESTAMP,
    expired_at TIMESTAMP,
    retry_count INT DEFAULT 0,
    CONSTRAINT fk_offline_target_user FOREIGN KEY (target_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_offline_message FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE,
    CONSTRAINT fk_offline_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE
);

-- Index for finding undelivered messages for a user/device
CREATE INDEX idx_offline_messages_user_device ON offline_messages(target_user_id, target_device_id);

-- Index for finding undelivered messages efficiently
CREATE INDEX idx_offline_messages_undelivered ON offline_messages(target_user_id) WHERE delivered_at IS NULL;

-- Index for cleanup of expired messages
CREATE INDEX idx_offline_messages_expired ON offline_messages(expired_at) WHERE expired_at IS NOT NULL AND delivered_at IS NULL;


-- Per-device sync tracking
-- Tracks the last message each device has synced to enable catch-up sync
CREATE TABLE device_sync_status (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    device_id VARCHAR(64) NOT NULL,
    last_synced_msg_id BIGINT,
    last_synced_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_device_sync_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_device_sync_user_device UNIQUE(user_id, device_id)
);

-- Index for efficient lookups by user
CREATE INDEX idx_device_sync_user ON device_sync_status(user_id);
