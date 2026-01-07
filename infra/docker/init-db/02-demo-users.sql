-- Demo users for testing
-- Passwords are bcrypt hashed 'demo123'

INSERT INTO users (uid, email, password_hash, nickname, gender, status, created_at)
VALUES
    ('DEMO001', 'alice@demo.com', '$2a$10$M0dQeufakHz2zlVnwa5NWeywNIdVtDSC2Q/S3YNVmYYaEioanSNOa', 'Alice Demo', 'female', 'active', NOW()),
    ('DEMO002', 'bob@demo.com', '$2a$10$M0dQeufakHz2zlVnwa5NWeywNIdVtDSC2Q/S3YNVmYYaEioanSNOa', 'Bob Demo', 'male', 'active', NOW())
ON CONFLICT (email) DO NOTHING;

-- Make Alice and Bob friends (bidirectional)
INSERT INTO friendships (user_id, friend_id, status, created_at)
SELECT u1.id, u2.id, 'active', NOW()
FROM users u1, users u2
WHERE u1.uid = 'DEMO001' AND u2.uid = 'DEMO002'
ON CONFLICT (user_id, friend_id) DO NOTHING;

INSERT INTO friendships (user_id, friend_id, status, created_at)
SELECT u1.id, u2.id, 'active', NOW()
FROM users u1, users u2
WHERE u1.uid = 'DEMO002' AND u2.uid = 'DEMO001'
ON CONFLICT (user_id, friend_id) DO NOTHING;
