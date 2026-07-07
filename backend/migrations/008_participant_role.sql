-- Миграция 008: роль участника мероприятия ('participant' | 'moderator')
ALTER TABLE event_participants
    ADD COLUMN IF NOT EXISTS role VARCHAR(20) DEFAULT 'participant';
