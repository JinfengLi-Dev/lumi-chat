-- Performance indexes for common queries
-- These indexes improve performance of message pagination and conversation list operations

-- Composite index for message pagination (filtered by conversation, ordered by time)
-- Used by: getMessages, findLatestMessagesForConversations
CREATE INDEX IF NOT EXISTS idx_messages_conversation_created
    ON messages(conversation_id, server_created_at DESC);

-- Composite index for group member existence checks
-- Used by: checking if user is member of group, batch member lookups
CREATE INDEX IF NOT EXISTS idx_group_members_group_user
    ON group_members(group_id, user_id);

-- Index for user conversation sorted by pinned status
-- The conversation list queries already join with conversations.last_msg_time
-- but we want is_pinned checks to be fast
CREATE INDEX IF NOT EXISTS idx_user_conversations_user_pinned
    ON user_conversations(user_id, is_pinned DESC);
