-- =====================================================
-- V2: Hierarchical articles + enhanced search
-- =====================================================

-- Add hierarchy columns to articles
ALTER TABLE wm_articles ADD COLUMN IF NOT EXISTS parent_article_id VARCHAR(36) REFERENCES wm_articles(id) ON DELETE SET NULL;
ALTER TABLE wm_articles ADD COLUMN IF NOT EXISTS sort_order INT NOT NULL DEFAULT 0;
ALTER TABLE wm_articles ADD COLUMN IF NOT EXISTS tree_path VARCHAR(4000);
ALTER TABLE wm_articles ADD COLUMN IF NOT EXISTS depth INT NOT NULL DEFAULT 0;

-- Indexes for hierarchy queries
CREATE INDEX IF NOT EXISTS idx_wm_articles_parent ON wm_articles(tenant_id, parent_article_id);
CREATE INDEX IF NOT EXISTS idx_wm_articles_tree_path ON wm_articles(tenant_id, tree_path);
CREATE INDEX IF NOT EXISTS idx_wm_articles_sort ON wm_articles(parent_article_id, sort_order);

-- Initialize tree_path for existing root articles
UPDATE wm_articles SET tree_path = '/' || id || '/', depth = 0 WHERE parent_article_id IS NULL AND tree_path IS NULL;

-- Embedding vector column placeholder for semantic search
-- When pgvector is available: ALTER TABLE wm_chunks ADD COLUMN embedding vector(1536);
-- For now, add a flag to track which chunks have embeddings
ALTER TABLE wm_chunks ADD COLUMN IF NOT EXISTS embedding_status VARCHAR(20) DEFAULT 'PENDING';

-- Search ranking cache for hybrid search
CREATE TABLE IF NOT EXISTS wm_search_cache (
    id              VARCHAR(36)  PRIMARY KEY,
    tenant_id       VARCHAR(36)  NOT NULL,
    query_hash      VARCHAR(64)  NOT NULL,
    article_id      VARCHAR(36)  NOT NULL REFERENCES wm_articles(id) ON DELETE CASCADE,
    score           DOUBLE PRECISION NOT NULL,
    search_type     VARCHAR(20)  NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMP    NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_wm_search_cache_lookup ON wm_search_cache(tenant_id, query_hash, search_type);
CREATE INDEX IF NOT EXISTS idx_wm_search_cache_expires ON wm_search_cache(expires_at);
