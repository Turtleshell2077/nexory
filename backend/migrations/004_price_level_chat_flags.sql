-- Миграция 004: стоимость и уровень мероприятия + флаги чата
ALTER TABLE events
    ADD COLUMN IF NOT EXISTS price       NUMERIC(10,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS skill_level VARCHAR(30);

ALTER TABLE chat_members
    ADD COLUMN IF NOT EXISTS muted    BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS archived BOOLEAN DEFAULT FALSE;
