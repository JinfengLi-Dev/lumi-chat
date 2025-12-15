-- Fix message content URLs: convert absolute localhost URLs to relative URLs
-- Changes: http://localhost:10080/api/v1/files/{id} -> /api/v1/files/{id}

-- Fix message content (for image/file messages where content is the file URL)
UPDATE messages
SET content = REPLACE(content, 'http://localhost:10080', '')
WHERE content LIKE 'http://localhost:10080%';

-- Fix metadata URLs (stored as JSONB)
-- This handles fileUrl, thumbnailUrl, and any other URL fields in metadata
UPDATE messages
SET metadata = REPLACE(metadata::text, 'http://localhost:10080', '')::jsonb
WHERE metadata IS NOT NULL
  AND metadata::text LIKE '%http://localhost:10080%';
