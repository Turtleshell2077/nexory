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
    ];

    for (const file of migrations) {
        const sqlPath = path.join(__dirname, '../../migrations', file);
        if (!fs.existsSync(sqlPath)) {
            console.log(`[migrate] Skipping ${file} (not found)`);
            continue;
        }
        const sql = fs.readFileSync(sqlPath, 'utf8');
        console.log(`[migrate] Running ${file}...`);
        try {
            await pool.query(sql);
            console.log(`[migrate] ${file} done ✓`);
        } catch (err) {
            // Пропускаем ошибки "уже существует" при повторном запуске
            if (err.code === '42P07' || err.code === '42710') {
                console.log(`[migrate] ${file} skipped (already exists)`);
            } else {
                console.error(`[migrate] ${file} error:`, err.message);
            }
        }
    }

    await pool.end();
}

migrate();
