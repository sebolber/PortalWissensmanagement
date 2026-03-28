-- Optionaler Organisationseinheiten-Kontext fuer Wissensmanagement-Inhalte

ALTER TABLE wm_articles ADD COLUMN organization_unit_id VARCHAR(36);
ALTER TABLE wm_categories ADD COLUMN organization_unit_id VARCHAR(36);
ALTER TABLE wm_chat_sessions ADD COLUMN organization_unit_id VARCHAR(36);

CREATE INDEX idx_wm_articles_ou ON wm_articles(organization_unit_id);
CREATE INDEX idx_wm_categories_ou ON wm_categories(organization_unit_id);
