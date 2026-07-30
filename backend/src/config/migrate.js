require('dotenv').config();
const fs   = require('fs');
const path = require('path');
const { pool } = require('./db');

async function migrate() {
    const migrations = [
        '001_init.sql',
        '002_user_profile_fields.sql',
        '003_chat_avatar.sql',
        '004_price_level_chat_flags.sql',
        '005_event_type_pricedesc.sql',
        '006_contacts_privacy.sql',
        '007_profile_visibility.sql',
        '008_participant_role.sql',
        '009_reports_indexes.sql',
        '010_notification_prefs.sql',
        '011_event_metro.sql',
        '012_username_unique_ci.sql',
        '013_user_status.sql',
        '014_event_ticket_url.sql',
        '015_friend_requests_groups.sql',
    ];

    // Неприменившиеся миграции возвращаем наверх. Раньше ошибка просто уходила
    // в лог, сервер стартовал с неполной схемой, и это всплывало позже случайным
    // «Ошибка на сервере» при сохранении — без единой подсказки, что дело в БД.
    const failed = [];

    for (const file of migrations) {
        const sqlPath = path.join(__dirname, '../../migrations', file);
        if (!fs.existsSync(sqlPath)) {
            console.log(`[migrate] Skipping ${file} (not found)`);
            failed.push({ file, reason: 'файл миграции отсутствует' });
            continue;
        }
        const sql = fs.readFileSync(sqlPath, 'utf8');
        console.log(`[migrate] Running ${file}...`);
        try {
            await pool.query(sql);
            console.log(`[migrate] ${file} done ✓`);
        } catch (err) {
            // Пропускаем ошибки "уже существует" при повторном запуске
            if (err.code === '42P07' || err.code === '42710' || err.code === '42701') {
                console.log(`[migrate] ${file} skipped (already exists)`);
            } else {
                console.error(`[migrate] ${file} error:`, err.message);
                failed.push({ file, reason: err.message });
            }
        }
    }

    const missing = await missingColumns();
    return { failed, missing };
}

// Колонки, без которых ломаются целые экраны приложения. Проверяем их явно:
// список миграций мог отработать «успешно» на другой базе, а прод-схема отстать.
const REQUIRED_COLUMNS = [
    ['events', 'price'], ['events', 'skill_level'], ['events', 'event_type'],
    ['events', 'price_description'], ['events', 'metro'], ['events', 'ticket_url'],
    ['users', 'status'], ['users', 'profile_visibility'], ['users', 'notify_messages'],
    ['chats', 'avatar_url'], ['chats', 'title'], ['chats', 'creator_id'],
    ['chat_members', 'muted'], ['chat_members', 'archived'],
    ['event_participants', 'role'],
];

async function missingColumns() {
    try {
        const res = await pool.query(`
            SELECT table_name, column_name
            FROM information_schema.columns
            WHERE table_schema = 'public'
        `);
        const have = new Set(res.rows.map(r => `${r.table_name}.${r.column_name}`));
        return REQUIRED_COLUMNS
            .map(([t, c]) => `${t}.${c}`)
            .filter(key => !have.has(key));
    } catch (err) {
        console.error('[migrate] не удалось проверить схему:', err.message);
        return [];
    }
}

// Запуск напрямую (`npm run migrate`) — прогоняем и закрываем пул.
// Импорт из index.js — прогоняем на старте сервера, пул не трогаем.
if (require.main === module) {
    migrate()
        .then(({ failed, missing }) => {
            if (missing.length) console.error('[migrate] НЕ ХВАТАЕТ КОЛОНОК:', missing.join(', '));
            return pool.end().then(() => process.exit(failed.length || missing.length ? 1 : 0));
        })
        .catch((e) => { console.error('[migrate] fatal:', e); process.exit(1); });
}

module.exports = { migrate };
