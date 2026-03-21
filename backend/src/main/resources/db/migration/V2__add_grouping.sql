-- Gruppierungen (article grouping)
CREATE TABLE IF NOT EXISTS wm_groupings (
    id              VARCHAR(36)  PRIMARY KEY,
    tenant_id       VARCHAR(36)  NOT NULL,
    name            VARCHAR(200) NOT NULL,
    description     VARCHAR(500),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, name)
);
CREATE INDEX IF NOT EXISTS idx_wm_groupings_tenant ON wm_groupings(tenant_id);

-- Add grouping_id to articles
ALTER TABLE wm_articles ADD COLUMN IF NOT EXISTS grouping_id VARCHAR(36) REFERENCES wm_groupings(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_wm_articles_grouping ON wm_articles(grouping_id);
