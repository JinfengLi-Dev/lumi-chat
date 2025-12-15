-- Add file expiration support for 30-day retention policy
-- Files in certain buckets (files, voice, video) will expire after 30 days
-- Avatars, images, and thumbnails do not expire

-- Add expiration timestamp to files
ALTER TABLE files ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP;

-- Add file type for easier filtering
ALTER TABLE files ADD COLUMN IF NOT EXISTS file_type VARCHAR(20);

-- Index for efficient cleanup queries
CREATE INDEX IF NOT EXISTS idx_files_expires_at ON files(expires_at) WHERE expires_at IS NOT NULL;

-- Set file_type based on bucket for existing files
UPDATE files SET file_type =
    CASE bucket
        WHEN 'avatars' THEN 'avatar'
        WHEN 'images' THEN 'image'
        WHEN 'files' THEN 'file'
        WHEN 'voice' THEN 'voice'
        WHEN 'video' THEN 'video'
        WHEN 'thumbnails' THEN 'thumbnail'
        ELSE 'file'
    END
WHERE file_type IS NULL;

-- Set default expiration for existing chat files (30 days from creation)
UPDATE files SET expires_at = created_at + INTERVAL '30 days'
WHERE bucket IN ('files', 'voice', 'video') AND expires_at IS NULL;

-- Avatars, images, and thumbnails don't expire (set to NULL explicitly)
UPDATE files SET expires_at = NULL
WHERE bucket IN ('avatars', 'images', 'thumbnails');
