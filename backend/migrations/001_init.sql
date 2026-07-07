CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- -------------------------------------------------------
-- ПОЛЬЗОВАТЕЛИ
-- Центральная таблица. Все остальные таблицы ссылаются на неё.
-- -------------------------------------------------------
CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username      VARCHAR(50)  UNIQUE NOT NULL,
    email         VARCHAR(255) UNIQUE NOT NULL,
    phone         VARCHAR(20)  UNIQUE,                    -- опционально
    password_hash TEXT         NOT NULL,                  -- bcrypt hash
    avatar_url    TEXT,                                   -- ссылка на S3/хранилище
    bio           TEXT,
    fcm_token     TEXT,                                   -- токен для push-уведомлений
    is_verified   BOOLEAN      DEFAULT FALSE,             -- подтверждён ли email/телефон
    role          VARCHAR(20)  DEFAULT 'user'             -- 'user' | 'admin'
                  CHECK (role IN ('user', 'admin')),
    created_at    TIMESTAMPTZ  DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  DEFAULT NOW()
);

-- -------------------------------------------------------
-- REFRESH ТОКЕНЫ
-- JWT-система работает в паре: короткий access token (15 мин)
-- и долгий refresh token (30 дней). Refresh хранится в БД,
-- чтобы можно было его отозвать (logout, смена пароля).
-- -------------------------------------------------------
CREATE TABLE refresh_tokens (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id    UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token      TEXT         UNIQUE NOT NULL,              -- сам refresh token
    expires_at TIMESTAMPTZ  NOT NULL,
    created_at TIMESTAMPTZ  DEFAULT NOW()
);

-- -------------------------------------------------------
-- МЕРОПРИЯТИЯ
-- Основная сущность приложения.
-- -------------------------------------------------------
CREATE TABLE events (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    creator_id       UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title            VARCHAR(200) NOT NULL,
    description      TEXT,
    address          TEXT         NOT NULL,
    latitude         DECIMAL(10, 8),                     -- для карты
    longitude        DECIMAL(11, 8),
    cover_url        TEXT,                               -- обложка мероприятия
    category         VARCHAR(50),                        -- 'sport', 'music', 'food', ...
    max_participants INTEGER,                            -- NULL = без ограничений
    starts_at        TIMESTAMPTZ  NOT NULL,
    ends_at          TIMESTAMPTZ,
    is_private       BOOLEAN      DEFAULT FALSE,
    status           VARCHAR(20)  DEFAULT 'active'
                     CHECK (status IN ('active', 'cancelled', 'finished')),
    created_at       TIMESTAMPTZ  DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  DEFAULT NOW()
);

-- Индекс для быстрого поиска мероприятий по дате и статусу —
-- именно эти поля используются в лентах чаще всего
CREATE INDEX idx_events_starts_at ON events(starts_at);
CREATE INDEX idx_events_status    ON events(status);
CREATE INDEX idx_events_creator   ON events(creator_id);

-- Полнотекстовый поиск по названию и описанию мероприятия.
-- tsvector — специальный тип PostgreSQL для поискового индекса.
-- GIN-индекс позволяет делать быстрый поиск по ILIKE и @@ оператору.
ALTER TABLE events ADD COLUMN search_vector tsvector
    GENERATED ALWAYS AS (
        to_tsvector('russian', coalesce(title, '') || ' ' || coalesce(description, ''))
    ) STORED;
CREATE INDEX idx_events_search ON events USING GIN(search_vector);

-- -------------------------------------------------------
-- УЧАСТНИКИ МЕРОПРИЯТИЙ
-- Связь "пользователь записался на мероприятие".
-- -------------------------------------------------------
CREATE TABLE event_participants (
    event_id   UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    user_id    UUID NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
    status     VARCHAR(20) DEFAULT 'registered'
               CHECK (status IN ('registered', 'cancelled', 'attended')),
    joined_at  TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (event_id, user_id)
);

-- -------------------------------------------------------
-- ДРУЗЬЯ
-- Система дружбы: один отправляет запрос, другой принимает.
-- Дружба направленная: (user_a → user_b) и (user_b → user_a)
-- создаются как ОТДЕЛЬНЫЕ строки при принятии запроса.
-- Это упрощает запросы "получить всех друзей пользователя X".
-- -------------------------------------------------------
CREATE TABLE friendships (
    requester_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    addressee_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status       VARCHAR(20) DEFAULT 'pending'
                 CHECK (status IN ('pending', 'accepted', 'declined', 'blocked')),
    created_at   TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (requester_id, addressee_id)
);

CREATE INDEX idx_friendships_addressee ON friendships(addressee_id);

-- -------------------------------------------------------
-- ЧАТЫ
-- Два типа: 'direct' (личка между двумя пользователями)
-- и 'event' (чат участников конкретного мероприятия).
-- -------------------------------------------------------
CREATE TABLE chats (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    type       VARCHAR(20) NOT NULL
               CHECK (type IN ('direct', 'event', 'support')),
    event_id   UUID REFERENCES events(id) ON DELETE CASCADE, -- только для event-чатов
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Участники чата
CREATE TABLE chat_members (
    chat_id   UUID NOT NULL REFERENCES chats(id) ON DELETE CASCADE,
    user_id   UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    joined_at TIMESTAMPTZ DEFAULT NOW(),
    -- last_read_at — до какого сообщения пользователь дочитал.
    -- Используется для подсчёта непрочитанных.
    last_read_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (chat_id, user_id)
);

-- -------------------------------------------------------
-- СООБЩЕНИЯ
-- Хранятся отдельно от чатов для нормализации.
-- -------------------------------------------------------
CREATE TABLE messages (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    chat_id    UUID  NOT NULL REFERENCES chats(id) ON DELETE CASCADE,
    sender_id  UUID  NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content    TEXT  NOT NULL,
    type       VARCHAR(20) DEFAULT 'text'
               CHECK (type IN ('text', 'image', 'system')),
    is_deleted BOOLEAN     DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Индекс для быстрой загрузки истории чата (по убыванию времени)
CREATE INDEX idx_messages_chat_created ON messages(chat_id, created_at DESC);

-- -------------------------------------------------------
-- ТИКЕТЫ ПОДДЕРЖКИ
-- Когда пользователь пишет в поддержку — создаётся тикет.
-- Сервер отправляет email создателю приложения через Nodemailer.
-- -------------------------------------------------------
CREATE TABLE support_tickets (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subject    VARCHAR(300) NOT NULL,
    body       TEXT         NOT NULL,
    status     VARCHAR(20)  DEFAULT 'open'
               CHECK (status IN ('open', 'in_progress', 'closed')),
    created_at TIMESTAMPTZ  DEFAULT NOW()
);

-- -------------------------------------------------------
-- ТРИГГЕР: автоматически обновляет updated_at при изменении строки
-- -------------------------------------------------------
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER trg_events_updated_at
    BEFORE UPDATE ON events
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();