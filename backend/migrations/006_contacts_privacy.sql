-- Миграция 006: видимость контактов (телефон/почта)
-- contacts_public = TRUE → видно всем; FALSE → только друзьям
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS contacts_public BOOLEAN DEFAULT FALSE;
