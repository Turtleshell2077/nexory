-- Миграция 010: детальные настройки push-уведомлений
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS notify_messages        BOOLEAN DEFAULT TRUE,  -- сообщения в чатах
    ADD COLUMN IF NOT EXISTS notify_friend_events   BOOLEAN DEFAULT TRUE,  -- друг создал мероприятие
    ADD COLUMN IF NOT EXISTS notify_interest_events BOOLEAN DEFAULT TRUE;  -- мероприятие по интересам
