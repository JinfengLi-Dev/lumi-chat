-- Fix avatar URLs: convert absolute localhost URLs to relative URLs
-- Changes: http://localhost:10080/api/v1/files/{id} -> /api/v1/files/{id}

UPDATE users
SET avatar = REPLACE(avatar, 'http://localhost:10080', '')
WHERE avatar LIKE 'http://localhost:10080%';

-- Also fix any file URLs in the files table (for consistency)
-- Note: The files table stores metadata, the URL is computed dynamically
