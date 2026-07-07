const WebSocket = require('ws');
const { verifyAccessToken } = require('../utils/jwt');
const { query } = require('../config/db');
const { sendPushNotification } = require('../services/notificationService');
const { censor } = require('../utils/moderation');
 
// userId → WebSocket. Хранит только ОДНО соединение на пользователя.
// Если пользователь открыл два устройства — последнее перезапишет первое.
// Для продакшена стоит использовать Redis pub/sub, но для MVP достаточно.
const connections = new Map();
 
function initWebSocketServer(httpServer) {
    const wss = new WebSocket.Server({ server: httpServer, path: '/ws' });
 
    wss.on('connection', async (ws, req) => {
        // Аутентификация через query-параметр: ws://host/ws?token=eyJ...
        const url    = new URL(req.url, 'http://localhost');
        const token  = url.searchParams.get('token');
 
        if (!token) {
            ws.close(4001, 'No token');
            return;
        }
 
        let userId;
        try {
            const payload = verifyAccessToken(token);
            userId = payload.sub;
        } catch {
            ws.close(4001, 'Invalid token');
            return;
        }
 
        // Регистрируем соединение
        connections.set(userId, ws);
        console.log(`[WS] User ${userId} connected. Total: ${connections.size}`);
 
        // Клиент отправляет JSON-сообщения.
        // Поддерживаемые типы: 'send_message', 'mark_read'
        ws.on('message', async (data) => {
            let msg;
            try {
                msg = JSON.parse(data);
            } catch {
                return; // игнорируем невалидный JSON
            }
 
            if (msg.type === 'send_message') {
                await handleSendMessage(userId, msg);
            } else if (msg.type === 'mark_read') {
                await handleMarkRead(userId, msg.chatId);
            }
        });
 
        ws.on('close', () => {
            connections.delete(userId);
            console.log(`[WS] User ${userId} disconnected`);
        });
 
        ws.on('error', (err) => {
            console.error(`[WS] Error for user ${userId}:`, err.message);
        });
 
        // Сообщаем клиенту, что он успешно подключён
        ws.send(JSON.stringify({ type: 'connected', userId }));
    });
 
    return wss;
}
 
// Обработка отправки сообщения через WebSocket
async function handleSendMessage(senderId, msg) {
    const { chatId, content, msgType = 'text' } = msg;
    if (!chatId || !content) return;
    await persistAndBroadcast(senderId, chatId, content, msgType);
}

// Сохраняет сообщение в БД и рассылает его участникам чата (WS + push).
// Используется и WebSocket-обработчиком, и REST-эндпоинтом POST /chats/:id/messages.
// Возвращает сохранённое сообщение (с данными отправителя) или null, если нет доступа.
async function persistAndBroadcast(senderId, chatId, content, msgType = 'text') {
    // Проверяем, что отправитель является участником чата
    const memberCheck = await query(
        'SELECT 1 FROM chat_members WHERE chat_id = $1 AND user_id = $2',
        [chatId, senderId]
    );
    if (memberCheck.rows.length === 0) return null;

    // Модерация: маскируем нецензурную лексику
    const cleanContent = censor(content);

    // Сохраняем сообщение в БД
    const msgRes = await query(`
        INSERT INTO messages (chat_id, sender_id, content, type)
        VALUES ($1, $2, $3, $4)
        RETURNING id, chat_id, sender_id, content, type, created_at
    `, [chatId, senderId, cleanContent, msgType]);

    const savedMsg = msgRes.rows[0];

    const senderRes = await query(
        'SELECT username, avatar_url FROM users WHERE id = $1',
        [senderId]
    );
    const sender = senderRes.rows[0];

    // Плоская форма для клиента: sender_id/sender_username/sender_avatar
    const flatMessage = {
        id:              savedMsg.id,
        chat_id:         savedMsg.chat_id,
        content:         savedMsg.content,
        type:            savedMsg.type,
        created_at:      savedMsg.created_at,
        sender_id:       senderId,
        sender_username: sender.username,
        sender_avatar:   sender.avatar_url,
    };

    const outgoing = { type: 'new_message', message: flatMessage };

    const membersRes = await query(
        'SELECT user_id FROM chat_members WHERE chat_id = $1',
        [chatId]
    );

    for (const { user_id } of membersRes.rows) {
        const recipientWs = connections.get(user_id);

        if (recipientWs && recipientWs.readyState === WebSocket.OPEN) {
            recipientWs.send(JSON.stringify(outgoing));
        } else if (user_id !== senderId) {
            // Push только если у получателя включены уведомления о сообщениях и чат не заглушён
            const uRes = await query(`
                SELECT u.fcm_token, u.notifications_enabled, u.notify_messages,
                       COALESCE(cm.muted, false) AS muted
                FROM users u
                LEFT JOIN chat_members cm ON cm.user_id = u.id AND cm.chat_id = $2
                WHERE u.id = $1
            `, [user_id, chatId]);
            const r = uRes.rows[0];
            if (r && r.fcm_token && r.notifications_enabled && r.notify_messages && !r.muted) {
                await sendPushNotification(r.fcm_token, {
                    title: sender.username,
                    body:  cleanContent.length > 100 ? cleanContent.slice(0, 97) + '...' : cleanContent,
                    data:  { chatId, type: 'message' },
                }).catch(() => {});
            }
        }
    }

    return flatMessage;
}
 
// Обновляем last_read_at — клиент открыл чат и "прочитал" все сообщения
async function handleMarkRead(userId, chatId) {
    await query(`
        UPDATE chat_members SET last_read_at = NOW()
        WHERE chat_id = $1 AND user_id = $2
    `, [chatId, userId]);
}
 
// Экспортируем connections чтобы другие сервисы могли проверить онлайн-статус,
// и persistAndBroadcast для REST-эндпоинта отправки сообщений
module.exports = { initWebSocketServer, connections, persistAndBroadcast };