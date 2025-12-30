-- Migration: Add type column to tasks table for TaskType enum
ALTER TABLE tasks ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'TASK';
