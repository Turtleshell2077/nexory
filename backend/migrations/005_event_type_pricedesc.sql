-- Миграция 005: тип мероприятия и описание стоимости
ALTER TABLE events
    ADD COLUMN IF NOT EXISTS event_type        VARCHAR(40),
    ADD COLUMN IF NOT EXISTS price_description  TEXT;
