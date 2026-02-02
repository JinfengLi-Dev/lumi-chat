-- Add unique constraint to nickname column
-- First, handle potential duplicates by appending suffix to existing duplicates
WITH duplicates AS (
    SELECT nickname, COUNT(*) as cnt
    FROM users
    GROUP BY nickname
    HAVING COUNT(*) > 1
)
UPDATE users u
SET nickname = u.nickname || '_' || u.id
WHERE u.nickname IN (SELECT nickname FROM duplicates)
AND u.id NOT IN (
    -- Keep the first user with each nickname unchanged
    SELECT MIN(id) FROM users GROUP BY nickname
);

-- Now add the unique constraint
ALTER TABLE users ADD CONSTRAINT uk_users_nickname UNIQUE (nickname);

-- Add index for faster nickname lookups
CREATE INDEX IF NOT EXISTS idx_users_nickname ON users(nickname);
