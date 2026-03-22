-- =====================================================
-- V7: Prompt-Kategorien und erweiterte Prompt-Config-Spalten
-- =====================================================

CREATE TABLE IF NOT EXISTS wm_prompt_categories (
    id              VARCHAR(36)  PRIMARY KEY,
    tenant_id       VARCHAR(36)  NOT NULL,
    name            VARCHAR(200) NOT NULL,
    description     VARCHAR(500),
    sort_order      INTEGER      NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_wm_prompt_categories_tenant ON wm_prompt_categories(tenant_id);

-- Fehlende Spalten in wm_prompt_configs ergaenzen
ALTER TABLE wm_prompt_configs ADD COLUMN IF NOT EXISTS category_id VARCHAR(36) REFERENCES wm_prompt_categories(id);
ALTER TABLE wm_prompt_configs ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE wm_prompt_configs ADD COLUMN IF NOT EXISTS sort_order INTEGER NOT NULL DEFAULT 0;
