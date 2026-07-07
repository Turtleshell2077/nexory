const { query, transaction } = require('../config/db');
const { persistAndBroadcast } = require('../websocket/chatServer');

// GET /chats — все чаты текущего пользователя с последним сообщением и счётчиком непрочитанных
const getMyChats = async (req, res) => {
    const userId = req.user.id;
    const archived = req.query.archived === 'true';
    try {
        // Получаем все чаты пользователя.
        // last_message — подзапрос для последнего сообщения в чате.
        // unread_count — сколько сообщений после last_read_at.
        const result = await query(`
            SELECT
                c.id, c.type, c.event_id, c.avatar_url,
                cm.last_read_at,
 
                -- Для direct-чата получаем данные собеседника
                CASE WHEN c.type = 'direct' THEN (
                    SELECT json_build_object(
                        'id', u.id, 'username', u.username, 'avatar_url', u.avatar_url
                    )
                    FROM chat_members cm2
                    JOIN users u ON u.id = cm2.user_id
                    WHERE cm2.chat_id = c.id AND cm2.user_id != $1
                    LIMIT 1
                ) END AS peer,
 
                -- Для event-чата получаем название мероприятия
                CASE WHEN c.type = 'event' THEN (
                    SELECT json_build_object('id', e.id, 'title', e.title)
                    FROM events e WHERE e.id = c.event_id
                ) END AS event_info,
 
                -- Последнее сообщение
                (
                    SELECT json_build_object(
                        'id', m.id, 'content', m.content,
                        'created_at', m.created_at,
                        'sender_username', u2.username
                    )
                    FROM messages m
                    JOIN users u2 ON u2.id = m.sender_id
                    WHERE m.chat_id = c.id AND m.is_deleted = false
                    ORDER BY m.created_at DESC LIMIT 1
                ) AS last_message,
 
                -- Непрочитанные: сообщения после last_read_at не от меня
                (
                    SELECT COUNT(*) FROM messages m
                    WHERE m.chat_id = c.id
                      AND m.created_at > cm.last_read_at
                      AND m.sender_id != $1
                ) AS unread_count
 
            FROM chat_members cm
            JOIN chats c ON c.id = cm.chat_id
            WHERE cm.user_id = $1 AND COALESCE(cm.archived, false) = $2
            -- Сортируем по последней активности, а при отсутствии сообщений — по дате
            -- создания чата, чтобы event-чаты без сообщений НЕ проваливались в самый низ.
            ORDER BY COALESCE(
                (SELECT MAX(created_at) FROM messages WHERE chat_id = c.id),
                c.created_at
            ) DESC
        `, [userId, archived]);
 
        res.json({ chats: result.rows });
    } catch (err) {
        console.error('[getMyChats]', err);
        res.status(500).json({ error: 'Internal server error' });
    }
};
 
// GET /chats/:id/messages?before=<uuid>&limit=30 — история сообщений с курсорной пагинацией
// Курсорная пагинация (по ID) лучше OFFSET для чатов — она стабильна при добавлении новых сообщений
const getMessages = async (req, res) => {
    const userId = req.user.id;
    const chatId = req.params.id;
    const before = req.query.before; // ID сообщения — грузим старше него
    const limit  = Math.min(parseInt(req.query.limit) || 30, 50);
 
    // Проверяем членство
    const memberCheck = await query(
        'SELECT 1 FROM chat_members WHERE chat_id = $1 AND user_id = $2',
        [chatId, userId]
    );
    if (memberCheck.rows.length === 0) return res.status(403).json({ error: 'Forbidden' });
 
    let sql, params;
    if (before) {
        // Получить сообщения старше указанного
        sql = `
            SELECT m.id, m.content, m.type, m.created_at, m.is_deleted,
                   u.id AS sender_id, u.username AS sender_username, u.avatar_url AS sender_avatar
            FROM messages m JOIN users u ON u.id = m.sender_id
            WHERE m.chat_id = $1 AND m.created_at < (
                SELECT created_at FROM messages WHERE id = $2
            )
            ORDER BY m.created_at DESC LIMIT $3
        `;
        params = [chatId, before, limit];
    } else {
        sql = `
            SELECT m.id, m.content, m.type, m.created_at, m.is_deleted,
                   u.id AS sender_id, u.username AS sender_username, u.avatar_url AS sender_avatar
            FROM messages m JOIN users u ON u.id = m.sender_id
            WHERE m.chat_id = $1
            ORDER BY m.created_at DESC LIMIT $2
        `;
        params = [chatId, limit];
    }
 
    const result = await query(sql, params);
    // Возвращаем в хронологическом порядке (reverse)
    res.json({ messages: result.rows.reverse() });
};
 
// POST /chats/direct — создать или получить существующий direct-чат с пользователем
const getOrCreateDirectChat = async (req, res) => {
    const userId   = req.user.id;
    const { peerId } = req.body;
 
    if (userId === peerId) return res.status(400).json({ error: 'Cannot chat with yourself' });
 
    try {
        // Проверяем, существует ли уже direct-чат между этими двумя
        const existing = await query(`
            SELECT c.id FROM chats c
            JOIN chat_members cm1 ON cm1.chat_id = c.id AND cm1.user_id = $1
            JOIN chat_members cm2 ON cm2.chat_id = c.id AND cm2.user_id = $2
            WHERE c.type = 'direct'
            LIMIT 1
        `, [userId, peerId]);
 
        if (existing.rows.length > 0) {
            return res.json({ chatId: existing.rows[0].id, isNew: false });
        }
 
        // Создаём новый
        const chatId = await transaction(async (client) => {
            const chatRes = await client.query(
                "INSERT INTO chats (type) VALUES ('direct') RETURNING id"
            );
            const id = chatRes.rows[0].id;
            await client.query(
                'INSERT INTO chat_members (chat_id, user_id) VALUES ($1,$2),($1,$3)',
                [id, userId, peerId]
            );
            return id;
        });
 
        res.status(201).json({ chatId, isNew: true });
    } catch (err) {
        res.status(500).json({ error: 'Internal server error' });
    }
};
 
// GET /chats/:id — информация о чате (заголовок, аватар, тип) для шапки экрана
const getChatInfo = async (req, res) => {
    const userId = req.user.id;
    const chatId = req.params.id;
    try {
        const memberCheck = await query(
            'SELECT 1 FROM chat_members WHERE chat_id = $1 AND user_id = $2',
            [chatId, userId]
        );
        if (memberCheck.rows.length === 0) return res.status(403).json({ error: 'Forbidden' });

        const chatRes = await query(`
            SELECT c.id, c.type, c.event_id, c.avatar_url,
                CASE WHEN c.type = 'event' THEN (
                    SELECT title FROM events e WHERE e.id = c.event_id
                ) END AS event_title,
                CASE WHEN c.type = 'direct' THEN (
                    SELECT json_build_object('id', u.id, 'username', u.username, 'avatar_url', u.avatar_url)
                    FROM chat_members cm2 JOIN users u ON u.id = cm2.user_id
                    WHERE cm2.chat_id = c.id AND cm2.user_id != $2 LIMIT 1
                ) END AS peer
            FROM chats c WHERE c.id = $1
        `, [chatId, userId]);

        if (chatRes.rows.length === 0) return res.status(404).json({ error: 'Chat not found' });

        const row = chatRes.rows[0];
        const title  = row.type === 'event' ? (row.event_title || 'Чат мероприятия')
                     : (row.peer?.username || 'Чат');
        const avatar = row.avatar_url || row.peer?.avatar_url || null;

        // Участники чата
        const membersRes = await query(`
            SELECT u.id, u.username, u.avatar_url
            FROM chat_members cm JOIN users u ON u.id = cm.user_id
            WHERE cm.chat_id = $1
            ORDER BY u.username
        `, [chatId]);

        // Детали мероприятия (для event-чата)
        let event = null;
        if (row.type === 'event' && row.event_id) {
            const evRes = await query(`
                SELECT id, title, description, address, cover_url, category,
                       starts_at, ends_at, max_participants
                FROM events WHERE id = $1
            `, [row.event_id]);
            event = evRes.rows[0] || null;
        }

        res.json({
            chat:    { id: row.id, type: row.type, title, avatar_url: avatar, can_edit_avatar: row.type !== 'direct' },
            members: membersRes.rows,
            event,
        });
    } catch (err) {
        console.error('[getChatInfo]', err);
        res.status(500).json({ error: 'Internal server error' });
    }
};

// POST /chats/:id/messages — надёжная отправка сообщения через REST.
// Сохраняет в БД и рассылает участникам (WS + push). Гарантирует персистентность,
// даже если WebSocket-соединение клиента не установлено.
const sendMessage = async (req, res) => {
    const userId = req.user.id;
    const chatId = req.params.id;
    const { content, type = 'text' } = req.body;

    if (!content || !content.trim()) {
        return res.status(400).json({ error: 'Content required' });
    }

    try {
        const message = await persistAndBroadcast(userId, chatId, content.trim(), type);
        if (!message) return res.status(403).json({ error: 'Not a chat member' });
        res.status(201).json({ message });
    } catch (err) {
        console.error('[sendMessage]', err);
        res.status(500).json({ error: 'Internal server error' });
    }
};

// PUT /chats/:id/avatar — сменить аватар чата (для event/group чатов)
const updateChatAvatar = async (req, res) => {
    const userId = req.user.id;
    const chatId = req.params.id;
    const { avatar_url } = req.body;

    try {
        const memberCheck = await query(
            'SELECT 1 FROM chat_members WHERE chat_id = $1 AND user_id = $2',
            [chatId, userId]
        );
        if (memberCheck.rows.length === 0) return res.status(403).json({ error: 'Forbidden' });

        await query('UPDATE chats SET avatar_url = $1 WHERE id = $2', [avatar_url, chatId]);
        res.json({ message: 'Chat avatar updated', avatar_url });
    } catch (err) {
        console.error('[updateChatAvatar]', err);
        res.status(500).json({ error: 'Internal server error' });
    }
};

// PATCH /chats/:id/flags — заглушить / архивировать чат для текущего пользователя
const updateChatFlags = async (req, res) => {
    const userId = req.user.id;
    const chatId = req.params.id;
    const { muted, archived } = req.body;
    try {
        await query(`
            UPDATE chat_members SET
                muted    = COALESCE($1::boolean, muted),
                archived = COALESCE($2::boolean, archived)
            WHERE chat_id = $3 AND user_id = $4
        `, [
            muted    != null ? String(muted)    : null,
            archived != null ? String(archived) : null,
            chatId, userId,
        ]);
        res.json({ message: 'Updated' });
    } catch (err) {
        console.error('[updateChatFlags]', err);
        res.status(500).json({ error: 'Internal server error' });
    }
};

// DELETE /chats/:id — удалить чат у текущего пользователя (выйти из него).
// Direct-чат: убираем себя из участников. Event-чат: тоже выходим из chat_members.
const deleteChat = async (req, res) => {
    const userId = req.user.id;
    const chatId = req.params.id;
    try {
        await query('DELETE FROM chat_members WHERE chat_id = $1 AND user_id = $2', [chatId, userId]);
        res.json({ message: 'Chat removed' });
    } catch (err) {
        console.error('[deleteChat]', err);
        res.status(500).json({ error: 'Internal server error' });
    }
};

module.exports = {
    getMyChats, getMessages, getOrCreateDirectChat, getChatInfo,
    sendMessage, updateChatAvatar, updateChatFlags, deleteChat,
};