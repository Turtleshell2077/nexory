-- Миграция 009: жалобы (модерация) + индексы для масштабирования

CREATE TABLE IF NOT EXISTS reports (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    reporter_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_type  VARCHAR(20) NOT NULL CHECK (target_type IN ('user', 'event', 'message')),
    target_id    UUID NOT NULL,
    reason       TEXT NOT NULL,
    status       VARCHAR(20) DEFAULT 'open' CHECK (status IN ('open', 'reviewed', 'dismissed')),
    created_at   TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_reports_status ON reports(status, created_at DESC);

-- Индексы под частые выборки (важно при росте числа пользователей)
CREATE INDEX IF NOT EXISTS idx_chat_members_user      ON chat_members(user_id);
CREATE INDEX IF NOT EXISTS idx_event_participants_user ON event_participants(user_id) WHERE status = 'registered';
CREATE INDEX IF NOT EXISTS idx_friendships_requester   ON friendships(requester_id);
CREATE INDEX IF NOT EXISTS idx_events_creator_status   ON events(creator_id, status);
CREATE INDEX IF NOT EXISTS idx_users_username_lower    ON users(LOWER(username));
