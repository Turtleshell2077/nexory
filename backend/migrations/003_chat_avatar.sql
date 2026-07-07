-- Миграция 003: аватар чата (для event/group чатов)
ALTER TABLE chats
    ADD COLUMN IF NOT EXISTS avatar_url TEXT;
