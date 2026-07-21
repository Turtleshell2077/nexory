require('dotenv').config();

const http  = require('http');
const https = require('https');
const fs    = require('fs');
const app   = require('./app');
const { initWebSocketServer } = require('./websocket/chatServer');
const { migrate } = require('./config/migrate');

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
    .then(() => console.log('✓ Миграции применены'))
    .catch((e) => console.error('⚠️  Ошибка миграций (сервер всё равно запустится):', e.message))
    .finally(() => {
        server.listen(PORT, () => {
            console.log(`✓ Nexory server running on port ${PORT}`);
        });
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
