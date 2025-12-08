-- Add clearedAt column to user_conversations table
-- This records when a user cleared messages in a conversation
-- Messages with serverCreatedAt < clearedAt should not be shown to this user

ALTER TABLE user_conversations
ADD COLUMN cleared_at TIMESTAMP;
