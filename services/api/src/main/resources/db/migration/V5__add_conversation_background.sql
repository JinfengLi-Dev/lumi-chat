-- Add background_url field to user_conversations table for custom chat backgrounds
ALTER TABLE user_conversations ADD COLUMN background_url VARCHAR(500);
