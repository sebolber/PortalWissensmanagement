-- =====================================================
-- Wissensdatenbank Schema
-- Prefix: wm_ (um Konflikte mit PortalCore zu vermeiden)
-- =====================================================

-- Kategorien
CREATE TABLE IF NOT EXISTS wm_categories (
    id              VARCHAR(36)  PRIMARY KEY,
    tenant_id       VARCHAR(36)  NOT NULL,
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    parent_id       VARCHAR(36)  REFERENCES wm_categories(id) ON DELETE SET NULL,
    order_index     INT          DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    UNIQUE (tenant_id, name, parent_id)
);
CREATE INDEX IF NOT EXISTS idx_wm_categories_tenant ON wm_categories(tenant_id);

-- Tags
CREATE TABLE IF NOT EXISTS wm_tags (
    id              VARCHAR(36)  PRIMARY KEY,
    tenant_id       VARCHAR(36)  NOT NULL,
    name            VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, name)
);
CREATE INDEX IF NOT EXISTS idx_wm_tags_tenant ON wm_tags(tenant_id);

-- Wissensartikel
CREATE TABLE IF NOT EXISTS wm_articles (
    id                      VARCHAR(36)  PRIMARY KEY,
    tenant_id               VARCHAR(36)  NOT NULL,
    title                   VARCHAR(500) NOT NULL,
    content                 TEXT         NOT NULL,
    summary                 VARCHAR(2000),
    status                  VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    category_id             VARCHAR(36)  REFERENCES wm_categories(id) ON DELETE SET NULL,
    created_by              VARCHAR(36)  NOT NULL,
    updated_by              VARCHAR(36),
    created_at              TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP,
    version                 INT          NOT NULL DEFAULT 1,
    is_public_within_tenant BOOLEAN      NOT NULL DEFAULT TRUE,
    linked_task_id          VARCHAR(36),
    view_count              INT          NOT NULL DEFAULT 0,
    usage_count             INT          NOT NULL DEFAULT 0,
    rating_sum              DOUBLE PRECISION NOT NULL DEFAULT 0,
    rating_count            INT          NOT NULL DEFAULT 0,
    last_used_at            TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_wm_articles_tenant ON wm_articles(tenant_id);
CREATE INDEX IF NOT EXISTS idx_wm_articles_status ON wm_articles(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_wm_articles_category ON wm_articles(category_id);
CREATE INDEX IF NOT EXISTS idx_wm_articles_created ON wm_articles(tenant_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_wm_articles_task ON wm_articles(linked_task_id);

-- Volltextsuche-Index
CREATE INDEX IF NOT EXISTS idx_wm_articles_fulltext
    ON wm_articles USING gin(to_tsvector('german', coalesce(title,'') || ' ' || coalesce(content,'') || ' ' || coalesce(summary,'')));

-- Artikel-Tags (Many-to-Many)
CREATE TABLE IF NOT EXISTS wm_article_tags (
    article_id      VARCHAR(36)  NOT NULL REFERENCES wm_articles(id) ON DELETE CASCADE,
    tag_id          VARCHAR(36)  NOT NULL REFERENCES wm_tags(id) ON DELETE CASCADE,
    PRIMARY KEY (article_id, tag_id)
);

-- Artikelversionen
CREATE TABLE IF NOT EXISTS wm_article_versions (
    id              VARCHAR(36)  PRIMARY KEY,
    article_id      VARCHAR(36)  NOT NULL REFERENCES wm_articles(id) ON DELETE CASCADE,
    version         INT          NOT NULL,
    title           VARCHAR(500) NOT NULL,
    content         TEXT         NOT NULL,
    summary         VARCHAR(2000),
    changed_by      VARCHAR(36)  NOT NULL,
    changed_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    change_note     VARCHAR(500)
);
CREATE INDEX IF NOT EXISTS idx_wm_versions_article ON wm_article_versions(article_id, version DESC);

-- Wissens-Chunks (fuer Retrieval)
CREATE TABLE IF NOT EXISTS wm_chunks (
    id              VARCHAR(36)  PRIMARY KEY,
    tenant_id       VARCHAR(36)  NOT NULL,
    article_id      VARCHAR(36)  NOT NULL REFERENCES wm_articles(id) ON DELETE CASCADE,
    chunk_index     INT          NOT NULL,
    content         TEXT         NOT NULL,
    token_count     INT,
    metadata        TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_wm_chunks_article ON wm_chunks(article_id, chunk_index);
CREATE INDEX IF NOT EXISTS idx_wm_chunks_tenant ON wm_chunks(tenant_id);
CREATE INDEX IF NOT EXISTS idx_wm_chunks_fulltext
    ON wm_chunks USING gin(to_tsvector('german', content));

-- Feedback
CREATE TABLE IF NOT EXISTS wm_feedback (
    id              VARCHAR(36)  PRIMARY KEY,
    article_id      VARCHAR(36)  NOT NULL REFERENCES wm_articles(id) ON DELETE CASCADE,
    user_id         VARCHAR(36)  NOT NULL,
    tenant_id       VARCHAR(36)  NOT NULL,
    rating          INT          NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment         TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (article_id, user_id)
);
CREATE INDEX IF NOT EXISTS idx_wm_feedback_article ON wm_feedback(article_id);

-- Nutzungsmessung
CREATE TABLE IF NOT EXISTS wm_usage (
    id                      VARCHAR(36)  PRIMARY KEY,
    article_id              VARCHAR(36)  NOT NULL REFERENCES wm_articles(id) ON DELETE CASCADE,
    tenant_id               VARCHAR(36)  NOT NULL,
    context_type            VARCHAR(50)  NOT NULL,
    context_reference_id    VARCHAR(36),
    used_by                 VARCHAR(36)  NOT NULL,
    used_at                 TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_wm_usage_article ON wm_usage(article_id);
CREATE INDEX IF NOT EXISTS idx_wm_usage_tenant ON wm_usage(tenant_id, used_at DESC);

-- Chat-Sessions
CREATE TABLE IF NOT EXISTS wm_chat_sessions (
    id              VARCHAR(36)  PRIMARY KEY,
    tenant_id       VARCHAR(36)  NOT NULL,
    user_id         VARCHAR(36)  NOT NULL,
    model_config_id VARCHAR(36),
    title           VARCHAR(300),
    context_type    VARCHAR(50),
    context_ref_id  VARCHAR(36),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_wm_sessions_user ON wm_chat_sessions(tenant_id, user_id, updated_at DESC);

-- Chat-Nachrichten
CREATE TABLE IF NOT EXISTS wm_chat_messages (
    id              VARCHAR(36)  PRIMARY KEY,
    session_id      VARCHAR(36)  NOT NULL REFERENCES wm_chat_sessions(id) ON DELETE CASCADE,
    role            VARCHAR(20)  NOT NULL,
    content         TEXT         NOT NULL,
    source_refs     TEXT,
    model_id        VARCHAR(36),
    token_count     INT,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_wm_messages_session ON wm_chat_messages(session_id, created_at);

-- Wissensvorschlaege
CREATE TABLE IF NOT EXISTS wm_suggestions (
    id                      VARCHAR(36)  PRIMARY KEY,
    tenant_id               VARCHAR(36)  NOT NULL,
    context_type            VARCHAR(50)  NOT NULL,
    context_ref_id          VARCHAR(36)  NOT NULL,
    article_id              VARCHAR(36)  NOT NULL REFERENCES wm_articles(id) ON DELETE CASCADE,
    confidence              DOUBLE PRECISION,
    reason                  VARCHAR(500),
    created_at              TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_wm_suggestions_context ON wm_suggestions(tenant_id, context_type, context_ref_id);
