-- Add constraint to users table if it doesn't exist
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check 
CHECK (role IN ('ADMIN', 'MODERATOR', 'USER', 'CHECKER', 'SHIPPING', 'INVENTORY'));

-- Insert users
INSERT INTO users (username, password, role) 
VALUES ('admin', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'ADMIN') 
ON CONFLICT (username) DO NOTHING;
