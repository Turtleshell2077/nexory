-- Миграция 002: расширенные поля профиля пользователя
-- Добавляем колонки с IF NOT EXISTS для безопасного повторного запуска

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS display_name           VARCHAR(100),
    ADD COLUMN IF NOT EXISTS age                    INTEGER CHECK (age IS NULL OR (age >= 1 AND age <= 120)),
    ADD COLUMN IF NOT EXISTS country                VARCHAR(100),
    ADD COLUMN IF NOT EXISTS city                   VARCHAR(100),
    ADD COLUMN IF NOT EXISTS sports                 TEXT,
    ADD COLUMN IF NOT EXISTS looking_for            VARCHAR(100),
    ADD COLUMN IF NOT EXISTS activity               VARCHAR(100),
    ADD COLUMN IF NOT EXISTS notifications_enabled  BOOLEAN DEFAULT TRUE;
