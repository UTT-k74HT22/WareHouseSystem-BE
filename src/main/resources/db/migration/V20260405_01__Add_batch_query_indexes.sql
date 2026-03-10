-- ============================================================================
-- Flyway Migration: Add Batch Query and Lifecycle Indexes
-- Version: V20260405_01
-- Description: Indexes for updated_at, created_at, and composite status tracking
-- Author: Gemini CLI
-- Date: 2026-03-10
-- ============================================================================

-- Rationale:
-- 1. idx_batch_created_at: Optimizes default pagination sorting in getAllBatches.
-- 2. idx_batch_updated_at: Supports frequent sorting/filtering by last update.
-- 3. idx_batch_status_updated_at: Optimizes lifecycle tracking queries (e.g., recently quarantined).

-- Indexes
CREATE INDEX idx_batch_created_at ON batches (created_at);
CREATE INDEX idx_batch_updated_at ON batches (updated_at);
CREATE INDEX idx_batch_status_updated_at ON batches (status, updated_at);

-- Rollback Note:
-- DROP INDEX idx_batch_created_at ON batches;
-- DROP INDEX idx_batch_updated_at ON batches;
-- DROP INDEX idx_batch_status_updated_at ON batches;

-- Risk Note:
-- Adding indexes on a large table may lock the table temporarily. 
-- For production environments with millions of rows, consider using ALGORITHM=INPLACE, LOCK=NONE if supported by the DB engine.

-- ============================================================================
-- End of Migration
-- ============================================================================
