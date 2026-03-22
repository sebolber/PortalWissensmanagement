-- =====================================================
-- V4: Semantic search improvements
-- Enable pg_trgm for similarity search as fallback
-- Add improved search indexes for hybrid retrieval
-- =====================================================

-- Enable trigram extension for fuzzy/similarity search
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Trigram indexes for similarity search on articles
CREATE INDEX IF NOT EXISTS idx_wm_articles_title_trgm
    ON wm_articles USING gin(title gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_wm_articles_content_trgm
    ON wm_articles USING gin(content gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_wm_articles_summary_trgm
    ON wm_articles USING gin(summary gin_trgm_ops);

-- Trigram index for similarity search on chunks
CREATE INDEX IF NOT EXISTS idx_wm_chunks_content_trgm
    ON wm_chunks USING gin(content gin_trgm_ops);

-- Add heading field to chunks for better context
ALTER TABLE wm_chunks ADD COLUMN IF NOT EXISTS heading VARCHAR(500);

-- Add article_title to chunks for better retrieval context
ALTER TABLE wm_chunks ADD COLUMN IF NOT EXISTS article_title VARCHAR(500);
