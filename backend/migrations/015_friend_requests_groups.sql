-- Миграция 015.
--
-- 1) Заявки в друзья: индекс для регулярной уборки просроченных заявок
--    (живут 90 дней, см. REQUEST_TTL_DAYS в otherControllers.js).
-- 2) Групповые чаты: тип 'group', название и создатель.

CREATE INDEX IF NOT EXISTS idx_friendships_pending
    ON friendships(status, created_at)
    WHERE status = 'pending';

-- Чаты: снимаем ограничение типа и добавляем 'group'.
-- Имя ограничения задаётся автоматически (chats_type_check) — на всякий случай
-- пробуем оба варианта записи.
ALTER TABLE chats DROP CONSTRAINT IF EXISTS chats_type_check;
ALTER TABLE chats
    ADD CONSTRAINT chats_type_check
    CHECK (type IN ('direct', 'event', 'group', 'support'));

-- Название группы и её создатель. Для direct/event остаются NULL.
ALTER TABLE chats ADD COLUMN IF NOT EXISTS title      VARCHAR(120);
ALTER TABLE chats ADD COLUMN IF NOT EXISTS creator_id UUID REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_chats_creator ON chats(creator_id);
