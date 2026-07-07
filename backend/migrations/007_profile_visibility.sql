-- Миграция 007: гибкая видимость профиля/контактов
-- profile_visibility: 'all' (всем) | 'friends' (друзьям) | 'selected' (выбранным друзьям)
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS profile_visibility VARCHAR(20) DEFAULT 'friends';

-- Белый список «выбранных друзей», которым виден профиль с личными данными
CREATE TABLE IF NOT EXISTS profile_allowed (
    owner_id   UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    allowed_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (owner_id, allowed_id)
);
