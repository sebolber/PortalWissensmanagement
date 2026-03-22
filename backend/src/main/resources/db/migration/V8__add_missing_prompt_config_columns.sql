-- =====================================================
-- V8: Fehlende Spalten in wm_prompt_configs ergaenzen
-- =====================================================

ALTER TABLE wm_prompt_configs ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE wm_prompt_configs ADD COLUMN IF NOT EXISTS sort_order INT NOT NULL DEFAULT 0;
ALTER TABLE wm_prompt_configs ADD COLUMN IF NOT EXISTS category_id VARCHAR(36) REFERENCES wm_prompt_categories(id) ON DELETE SET NULL;
