CREATE TABLE IF NOT EXISTS app_user (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    display_name    VARCHAR(100) NOT NULL,
    is_admin        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS voice (
    id                BIGSERIAL PRIMARY KEY,
    engine_voice_id   VARCHAR(20) NOT NULL UNIQUE,
    display_name      VARCHAR(50) NOT NULL,
    gender            VARCHAR(10) NOT NULL,
    style_tag         VARCHAR(30) NOT NULL,
    is_available      BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS project (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS generation (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT REFERENCES app_user(id) ON DELETE CASCADE,
    project_id      BIGINT REFERENCES project(id) ON DELETE SET NULL,
    voice_id        BIGINT NOT NULL REFERENCES voice(id),
    input_text      TEXT NOT NULL,
    char_count      INT NOT NULL,
    audio_path      VARCHAR(500),
    duration_seconds NUMERIC(6,2),
    status          VARCHAR(20) NOT NULL DEFAULT 'pending',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS usage_daily (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    usage_date          DATE NOT NULL,
    characters_used     INT NOT NULL DEFAULT 0,
    generation_count    INT NOT NULL DEFAULT 0,
    UNIQUE(user_id, usage_date)
);

CREATE TABLE IF NOT EXISTS contact_message (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(255) NOT NULL,
    message     TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS interest_signal (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
    would_pay           VARCHAR(10) NOT NULL,
    suggested_price_inr INT,
    comment             TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
ALTER TABLE generation ADD COLUMN IF NOT EXISTS is_liked BOOLEAN NOT NULL DEFAULT FALSE;
