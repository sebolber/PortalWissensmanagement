-- =====================================================
-- V6: Add missing columns to wm_chunks
-- These were in V4 but never applied because DB was already at V5
-- =====================================================

ALTER TABLE wm_chunks ADD COLUMN IF NOT EXISTS heading VARCHAR(500);
ALTER TABLE wm_chunks ADD COLUMN IF NOT EXISTS article_title VARCHAR(500);

-- Recreate indexes from V4 that may also be missing
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_wm_articles_title_trgm
    ON wm_articles USING gin(title gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_wm_articles_content_trgm
    ON wm_articles USING gin(content gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_wm_articles_summary_trgm
    ON wm_articles USING gin(summary gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_wm_chunks_content_trgm
    ON wm_chunks USING gin(content gin_trgm_ops);
