-- =====================================================
-- V5: Prompt-Konfigurationen fuer KI-gestuetzte Artikelbearbeitung
-- =====================================================

CREATE TABLE IF NOT EXISTS wm_prompt_configs (
    id              VARCHAR(36)  PRIMARY KEY,
    tenant_id       VARCHAR(36)  NOT NULL,
    name            VARCHAR(200) NOT NULL,
    description     VARCHAR(1000),
    prompt_text     TEXT         NOT NULL,
    prompt_type     VARCHAR(20)  NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_wm_prompt_configs_tenant ON wm_prompt_configs(tenant_id, prompt_type);
