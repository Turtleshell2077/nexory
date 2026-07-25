-- Миграция 012: регистронезависимая уникальность никнеймов.
--
-- Зачем: на username уже есть UNIQUE, но он регистрозависимый — можно было
-- зарегистрировать «Ivan» при существующем «ivan». Поиск людей идёт только по нику,
-- поэтому двойники недопустимы: пользователь не понимал бы, кого нашёл.

-- Шаг 1. Расчистить уже существующие «двойники по регистру», иначе уникальный
-- индекс не создастся. Самому старому аккаунту в группе ник оставляем, остальным
-- добавляем короткий суффикс из его id.
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT id, username FROM (
            SELECT id, username,
                   ROW_NUMBER() OVER (
                       PARTITION BY LOWER(username)
                       ORDER BY created_at NULLS LAST, id
                   ) AS rn
            FROM users
        ) t
        WHERE rn > 1
    LOOP
        UPDATE users
        SET username = LEFT(r.username, 43) || '_' || SUBSTRING(r.id::text FROM 1 FOR 6)
        WHERE id = r.id;
        RAISE NOTICE 'Переименован дублирующийся ник: % (id=%)', r.username, r.id;
    END LOOP;
END $$;

-- Шаг 2. Уникальный индекс по нижнему регистру.
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username_lower
    ON users (LOWER(username));

-- Шаг 3. Индекс для поиска по нику по префиксу/подстроке (ILIKE 'abc%').
-- Ускоряет поиск людей, который теперь идёт только по username.
CREATE INDEX IF NOT EXISTS idx_users_username_lower_text
    ON users (LOWER(username) text_pattern_ops);
