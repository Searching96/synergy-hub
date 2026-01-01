-- Add archived column to tasks table
ALTER TABLE tasks ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE;

-- Add index on archived column for better query performance
CREATE INDEX idx_task_archived ON tasks(archived);
