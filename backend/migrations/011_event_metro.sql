-- Миграция 011: ближайшая станция метро у мероприятия
ALTER TABLE events
    ADD COLUMN IF NOT EXISTS metro VARCHAR(100);
