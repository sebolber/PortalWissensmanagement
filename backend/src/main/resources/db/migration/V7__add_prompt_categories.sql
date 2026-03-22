-- =====================================================
-- V7: Prompt-Kategorien fuer strukturierte Prompt-Verwaltung
-- =====================================================

CREATE TABLE IF NOT EXISTS wm_prompt_categories (
    id              VARCHAR(36)  PRIMARY KEY,
    tenant_id       VARCHAR(36)  NOT NULL,
    name            VARCHAR(200) NOT NULL,
    description     VARCHAR(500),
    sort_order      INT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_wm_prompt_categories_tenant ON wm_prompt_categories(tenant_id);
