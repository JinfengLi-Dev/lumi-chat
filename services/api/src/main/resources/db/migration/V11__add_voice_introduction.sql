-- Add voice introduction fields to users table
ALTER TABLE users ADD COLUMN voice_intro_url VARCHAR(500);
ALTER TABLE users ADD COLUMN voice_intro_duration INT;

COMMENT ON COLUMN users.voice_intro_url IS 'URL to the user voice introduction audio file';
COMMENT ON COLUMN users.voice_intro_duration IS 'Duration of the voice introduction in seconds';
