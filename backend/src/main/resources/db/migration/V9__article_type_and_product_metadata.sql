-- Add article type to distinguish products from standard articles
ALTER TABLE wm_articles ADD COLUMN article_type VARCHAR(20) NOT NULL DEFAULT 'STANDARD';

-- Product-specific metadata (only used when article_type = 'PRODUCT')
ALTER TABLE wm_articles ADD COLUMN product_version VARCHAR(50);
ALTER TABLE wm_articles ADD COLUMN product_vendor VARCHAR(200);
ALTER TABLE wm_articles ADD COLUMN product_icon_url VARCHAR(500);
ALTER TABLE wm_articles ADD COLUMN product_documentation_url VARCHAR(500);

-- Index for fast product listing
CREATE INDEX idx_articles_type ON wm_articles(tenant_id, article_type);
