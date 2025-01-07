-- Create pac table
CREATE TABLE pac (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    pouches_checked INTEGER,
    start_time TIME,
    end_time TIME,
    store VARCHAR(255) NOT NULL,
    CONSTRAINT fk_pac_user FOREIGN KEY (user_id) REFERENCES users(id)
);
-- Transfer data from user_answer to pac
INSERT INTO pac (user_id, store, start_time, end_time, pouches_checked)
SELECT ua.user_id, ua.store, ua.start_time, ua.end_time, ua.pouches_checked
FROM user_answer ua;

-- Modify user_answer table
ALTER TABLE user_answer
DROP COLUMN store,
DROP COLUMN start_time,
DROP COLUMN end_time,
DROP COLUMN pouches_checked;

-- Add submission_date column to user_answer if it doesn't exist
ALTER TABLE user_answer
ADD COLUMN IF NOT EXISTS submission_date DATE;

-- Update submission_date with the date part of start_time
UPDATE user_answer ua
SET submission_date = DATE(p.start_time)
FROM pac p
WHERE ua.id = p.user_answer_id;
