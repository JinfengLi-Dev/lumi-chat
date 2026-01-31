-- Add pinned_message_ids column to user_conversations table
ALTER TABLE user_conversations
ADD COLUMN pinned_message_ids BIGINT[] DEFAULT ARRAY[]::BIGINT[];

-- Add comment
COMMENT ON COLUMN user_conversations.pinned_message_ids IS 'Array of pinned message IDs for this conversation';
