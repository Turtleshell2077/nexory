require('dotenv').config();

const http  = require('http');
const https = require('https');
const fs    = require('fs');
const app   = require('./app');
const { initWebSocketServer } = require('./websocket/chatServer');
const { migrate } = require('./config/migrate');
const { purgeExpiredFriendRequests } = require('./controllers/otherControllers');

// Предупреждение о небезопасных дефолтах в проде
if (process.env.NODE_ENV === 'production') {
    if (!process.env.JWT_ACCESS_SECRET || !process.env.JWT_REFRESH_SECRET) {
        console.warn('⚠️  JWT-секреты не заданы! Установите JWT_ACCESS_SECRET и JWT_REFRESH_SECRET в .env');
    }
    if (!process.env.DB_PASSWORD) {
        console.warn('⚠️  DB_PASSWORD не задан!');
    }
}

// HTTPS, если заданы пути к сертификатам (Let's Encrypt и т.п.), иначе HTTP.
let server;
if (process.env.SSL_KEY_PATH && process.env.SSL_CERT_PATH) {
    server = https.createServer({
        key:  fs.readFileSync(process.env.SSL_KEY_PATH),
        cert: fs.readFileSync(process.env.SSL_CERT_PATH),
    }, app);
    console.log('✓ HTTPS/WSS включён');
} else {
    server = http.createServer(app);
}

// WebSocket на том же сервере (ws:// или wss:// в зависимости от SSL)
initWebSocketServer(server);

// Защита от падения процесса
process.on('unhandledRejection', (reason) => console.error('[unhandledRejection]', reason));
process.on('uncaughtException', (err) => console.error('[uncaughtException]', err));

const PORT = process.env.PORT || 3000;

// Прогоняем миграции на старте — схема БД всегда актуальна после деплоя.
// Миграции идемпотентны (IF NOT EXISTS / пропуск «уже существует»).
migrate()
    .then(({ failed, missing }) => {
        if (!failed.length && !missing.length) {
            console.log('✓ Миграции применены');
            return;
        }
        // Раньше сбой миграции был обычной строкой в логе, сервер стартовал с
        // неполной схемой — и это проявлялось позже как «Ошибка на сервере» при
        // сохранении мероприятия. Теперь состояние схемы видно сразу при старте.
        console.error('\n' + '='.repeat(64));
        console.error('⚠️  СХЕМА БАЗЫ НЕ В ПОРЯДКЕ — часть функций будет отдавать ошибку 500');
        failed.forEach(f => console.error(`   миграция ${f.file}: ${f.reason}`));
        if (missing.length) console.error(`   отсутствуют колонки: ${missing.join(', ')}`);
        console.error('   Почините БД и перезапустите сервер: npm run migrate');
        console.error('='.repeat(64) + '\n');
    })
    .catch((e) => console.error('⚠️  Ошибка миграций (сервер всё равно запустится):', e.message))
    .finally(() => {
        server.listen(PORT, () => {
            console.log(`✓ Nexory server running on port ${PORT}`);
        });

        // Уборка просроченных заявок в друзья: на старте и раз в сутки.
        // Запросы и так фильтруют их по дате, так что корректность не зависит
        // от этой уборки — она лишь не даёт таблице расти без нужды.
        const purge = () => purgeExpiredFriendRequests().catch(
            (e) => console.error('[friends] уборка заявок не удалась:', e.message)
        );
        purge();
        setInterval(purge, 24 * 60 * 60 * 1000).unref();
    });

// Graceful shutdown — корректно закрываем соединения при остановке
function shutdown(signal) {
    console.log(`\n${signal} — завершаем работу...`);
    server.close(() => process.exit(0));
    setTimeout(() => process.exit(1), 10000).unref();
}
process.on('SIGTERM', () => shutdown('SIGTERM'));
process.on('SIGINT',  () => shutdown('SIGINT'));

module.exports = server;
