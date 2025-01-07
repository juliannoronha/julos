-- Create users table if not exists
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(100) NOT NULL,
    role VARCHAR(255) NOT NULL
);

-- Create sequence for user_id if not exists
CREATE SEQUENCE IF NOT EXISTS user_id_seq START WITH 1 INCREMENT BY 1;

-- Create user_answer table if not exists
DROP TABLE IF EXISTS user_answer;

CREATE TABLE user_answer (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    store VARCHAR(20) NOT NULL,
    pouches_checked INTEGER,
    submission_date DATE NOT NULL,
    user_id BIGINT REFERENCES users(id)
);

-- Create indexes on user_answer table if not exist
CREATE INDEX IF NOT EXISTS idx_user_answer_name ON user_answer(name);
CREATE INDEX IF NOT EXISTS idx_user_answer_submission_date ON user_answer(submission_date);

-- Add these indexes
CREATE INDEX IF NOT EXISTS idx_user_answer_user_id ON user_answer(user_id);
CREATE INDEX IF NOT EXISTS idx_user_answer_start_time ON user_answer(start_time);
CREATE INDEX IF NOT EXISTS idx_user_answer_end_time ON user_answer(end_time);