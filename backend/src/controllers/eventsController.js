const { query, transaction } = require('../config/db');
const { notifyEventParticipants, notifyNewEvent } = require('../services/notificationService');
const { containsProfanity } = require('../utils/moderation');

// Собирает WHERE-условия и параметры из фильтров запроса (category, search, location).
// timeClause добавляется отдельно для upcoming/past.
function buildFilters(req, startIdx = 1) {
    const conditions = ["e.status = 'active'"];
    const params = [];
    let idx = startIdx;

    if (req.query.category) {
        conditions.push(`e.category = $${idx++}`);
        params.push(req.query.category);
    }
    if (req.query.search) {
        conditions.push(`e.search_vector @@ plainto_tsquery('russian', $${idx++})`);
        params.push(req.query.search);
    }
    if (req.query.location) {
        // Приблизительный поиск по адресу (город/место)
        conditions.push(`e.address ILIKE $${idx++}`);
        params.push(`%${req.query.location}%`);
    }
    if (req.query.freeOnly === 'true') {
        conditions.push(`COALESCE(e.price, 0) = 0`);
    }
    if (req.query.maxPrice) {
        const mp = parseFloat(req.query.maxPrice);
        if (!isNaN(mp)) {
            conditions.push(`COALESCE(e.price, 0) <= $${idx++}`);
            params.push(mp);
        }
    }
    if (req.query.level) {
        conditions.push(`e.skill_level = $${idx++}`);
        params.push(req.query.level);
    }
    return { conditions, params, idx };
}

const SELECT_COLS = `
    e.id, e.title, e.description, e.address,
    e.cover_url, e.category, e.starts_at, e.ends_at,
    e.max_participants, e.is_private, e.price, e.skill_level,
    e.event_type, e.price_description, e.created_at,
    u.id   AS creator_id,
    u.username AS creator_username,
    u.avatar_url AS creator_avatar,
    (SELECT COUNT(*) FROM event_participants ep
     WHERE ep.event_id = e.id AND ep.status = 'registered') AS participant_count
`;

// GET /events?category=&search=&location=
// Возвращает мероприятия двумя группами:
//   upcoming — предстоящие (starts_at >= сейчас), по возрастанию даты
//   past     — прошедшие   (starts_at <  сейчас), по убыванию даты
const getEvents = async (req, res) => {
    try {
        const { conditions, params } = buildFilters(req);

        // Приватные (только для друзей) события видны создателю и его друзьям
        const viewerIdx = params.length + 1;
        conditions.push(`(
            e.is_private = false
            OR e.creator_id = $${viewerIdx}
            OR EXISTS (
                SELECT 1 FROM friendships f
                WHERE f.status = 'accepted' AND (
                    (f.requester_id = e.creator_id AND f.addressee_id = $${viewerIdx}) OR
                    (f.addressee_id = e.creator_id AND f.requester_id = $${viewerIdx})
                )
            )
        )`);
        params.push(req.user.id);

        const whereBase = conditions.join(' AND ');

        // Сортировка предстоящих: 'new' — по дате создания (новые сверху),
        // иначе 'soon' — ближайшие по дате начала сверху (по умолчанию)
        const upcomingOrder = req.query.sort === 'new'
            ? 'e.created_at DESC'
            : 'e.starts_at ASC';

        const upcomingSql = `
            SELECT ${SELECT_COLS}
            FROM events e
            JOIN users u ON u.id = e.creator_id
            WHERE ${whereBase} AND e.starts_at >= NOW()
            ORDER BY ${upcomingOrder}
            LIMIT 100
        `;
        // Прошедшие показываем только за последние 2 недели — старее скрываем из ленты
        const pastSql = `
            SELECT ${SELECT_COLS}
            FROM events e
            JOIN users u ON u.id = e.creator_id
            WHERE ${whereBase} AND e.starts_at < NOW() AND e.starts_at >= NOW() - INTERVAL '14 days'
            ORDER BY e.starts_at DESC
            LIMIT 50
        `;

        const [upcomingRes, pastRes] = await Promise.all([
            query(upcomingSql, params),
            query(pastSql, params),
        ]);

        res.json({
            upcoming: upcomingRes.rows,
            past:     pastRes.rows,
        });
    } catch (err) {
        console.error('[getEvents]', err);
        res.status(500).json({ error: 'Internal server error' });
    }
};

// GET /events/my — мероприятия, на которые записан текущий пользователь
const getMyEvents = async (req, res) => {
    const userId = req.user.id;
    try {
        const result = await query(`
            SELECT e.*, u.username AS creator_username, u.avatar_url AS creator_avatar,
                (SELECT COUNT(*) FROM event_participants ep2
                 WHERE ep2.event_id = e.id AND ep2.status = 'registered') AS participant_count
            FROM events e
            JOIN event_participants ep ON ep.event_id = e.id
            JOIN users u ON u.id = e.creator_id
            WHERE ep.user_id = $1 AND ep.status = 'registered'
            ORDER BY e.starts_at ASC
        `, [userId]);

        res.json({ events: result.rows });
    } catch (err) {
        console.error('[getMyEvents]', err);
        res.status(500).json({ error: 'Internal server error' });
    }
};

// GET /events/:id — детальная страница мероприятия
const getEventById = async (req, res) => {
    const { id } = req.params;
    const userId = req.user.id;
    try {
        const eventRes = await query(`
            SELECT e.*,
                u.id   AS creator_id, u.username AS creator_username,
                u.avatar_url AS creator_avatar, u.bio AS creator_bio,
                (SELECT COUNT(*) FROM event_participants ep
                 WHERE ep.event_id = e.id AND ep.status = 'registered') AS participant_count
            FROM events e
            JOIN users u ON u.id = e.creator_id
            WHERE e.id = $1
        `, [id]);

        if (eventRes.rows.length === 0) {
            return res.status(404).json({ error: 'Event not found' });
        }

        const participantsRes = await query(`
            SELECT u.id, u.username, u.avatar_url, ep.role
            FROM event_participants ep
            JOIN users u ON u.id = ep.user_id
            WHERE ep.event_id = $1 AND ep.status = 'registered'
            ORDER BY ep.joined_at ASC
            LIMIT 50
        `, [id]);

        // Записан ли текущий пользователь
        const joinedRes = await query(`
            SELECT 1 FROM event_participants
            WHERE event_id = $1 AND user_id = $2 AND status = 'registered'
        `, [id, userId]);

        // ID чата мероприятия (для кнопки "Чат мероприятия")
        const chatRes = await query(`
            SELECT id FROM chats WHERE event_id = $1 AND type = 'event' LIMIT 1
        `, [id]);

        res.json({
            event:        eventRes.rows[0],
            participants: participantsRes.rows,
            isJoined:     joinedRes.rows.length > 0,
            chatId:       chatRes.rows[0]?.id || null,
        });
    } catch (err) {
        console.error('[getEventById]', err);
        res.status(500).json({ error: 'Internal server error' });
    }
};

// POST /events — создать мероприятие
const createEvent = async (req, res) => {
    const creatorId = req.user.id;
    const {
        title, description, address, latitude, longitude,
        cover_url, category, max_participants, starts_at, ends_at, is_private,
        price, skill_level, event_type, price_description
    } = req.body;

    if (containsProfanity(title) || containsProfanity(description)) {
        return res.status(400).json({ error: 'Название или описание содержит недопустимые выражения' });
    }

    try {
        const result = await transaction(async (client) => {
            const eventRes = await client.query(`
                INSERT INTO events
                    (creator_id, title, description, address, latitude, longitude,
                     cover_url, category, max_participants, starts_at, ends_at, is_private,
                     price, skill_level, event_type, price_description)
                VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15,$16)
                RETURNING *
            `, [creatorId, title, description, address, latitude, longitude,
                cover_url, category, max_participants || null, starts_at, ends_at || null,
                is_private === true || is_private === 'true',
                price ? parseFloat(price) : 0, skill_level || null,
                event_type || null, price_description || null]);

            const event = eventRes.rows[0];

            // Создатель автоматически создаёт event-чат для участников
            const chatRes = await client.query(`
                INSERT INTO chats (type, event_id) VALUES ('event', $1) RETURNING id
            `, [event.id]);

            await client.query(`
                INSERT INTO event_participants (event_id, user_id) VALUES ($1, $2)
            `, [event.id, creatorId]);

            await client.query(`
                INSERT INTO chat_members (chat_id, user_id) VALUES ($1, $2)
            `, [chatRes.rows[0].id, creatorId]);

            return event;
        });

        res.status(201).json({ event: result });

        // Уведомляем друзей и заинтересованных пользователей (не блокирует ответ)
        notifyNewEvent(result, creatorId).catch(() => {});
    } catch (err) {
        console.error('[createEvent]', err);
        res.status(500).json({ error: 'Internal server error' });
    }
};

// POST /events/:id/join — записаться на мероприятие
const joinEvent = async (req, res) => {
    const userId  = req.user.id;
    const eventId = req.params.id;

    try {
        const eventRes = await query(
            'SELECT id, max_participants, creator_id, title FROM events WHERE id = $1',
            [eventId]
        );
        if (eventRes.rows.length === 0) return res.status(404).json({ error: 'Event not found' });

        const event = eventRes.rows[0];

        if (event.max_participants) {
            const countRes = await query(
                `SELECT COUNT(*) FROM event_participants
                 WHERE event_id = $1 AND status = 'registered'`,
                [eventId]
            );
            if (parseInt(countRes.rows[0].count) >= event.max_participants) {
                return res.status(409).json({ error: 'Event is full' });
            }
        }

        await query(`
            INSERT INTO event_participants (event_id, user_id)
            VALUES ($1, $2)
            ON CONFLICT (event_id, user_id)
            DO UPDATE SET status = 'registered'
        `, [eventId, userId]);

        await query(`
            INSERT INTO chat_members (chat_id, user_id)
            SELECT c.id, $1 FROM chats c
            WHERE c.event_id = $2 AND c.type = 'event'
            ON CONFLICT DO NOTHING
        `, [userId, eventId]);

        await notifyEventParticipants(eventId, {
            title: 'Новый участник!',
            body:  `Кто-то записался на "${event.title}"`,
            targetUserId: event.creator_id,
        }).catch(() => {});

        res.json({ message: 'Joined successfully' });
    } catch (err) {
        console.error('[joinEvent]', err);
        res.status(500).json({ error: 'Internal server error' });
    }
};

// DELETE /events/:id/leave — отписаться от мероприятия
const leaveEvent = async (req, res) => {
    const userId  = req.user.id;
    const eventId = req.params.id;

    await query(`
        UPDATE event_participants SET status = 'cancelled'
        WHERE event_id = $1 AND user_id = $2
    `, [eventId, userId]);

    res.json({ message: 'Left event' });
};

// DELETE /events/:id/participants/:userId — исключить участника (только создатель)
const kickParticipant = async (req, res) => {
    const eventId = req.params.id;
    const targetId = req.params.userId;
    const userId  = req.user.id;
    try {
        const ev = await query('SELECT creator_id FROM events WHERE id = $1', [eventId]);
        if (ev.rows.length === 0) return res.status(404).json({ error: 'Event not found' });
        if (ev.rows[0].creator_id !== userId) return res.status(403).json({ error: 'Only creator' });
        if (targetId === userId) return res.status(400).json({ error: 'Cannot kick yourself' });

        await query(`UPDATE event_participants SET status = 'cancelled' WHERE event_id = $1 AND user_id = $2`, [eventId, targetId]);
        // Убираем из чата мероприятия
        await query(`
            DELETE FROM chat_members WHERE user_id = $1 AND chat_id IN (
                SELECT id FROM chats WHERE event_id = $2 AND type = 'event'
            )
        `, [targetId, eventId]);
        res.json({ message: 'Participant removed' });
    } catch (err) {
        console.error('[kickParticipant]', err);
        res.status(500).json({ error: 'Internal server error' });
    }
};

// PUT /events/:id/participants/:userId — изменить роль участника (только создатель)
const setParticipantRole = async (req, res) => {
    const eventId = req.params.id;
    const targetId = req.params.userId;
    const userId  = req.user.id;
    const role = req.body.role === 'moderator' ? 'moderator' : 'participant';
    try {
        const ev = await query('SELECT creator_id FROM events WHERE id = $1', [eventId]);
        if (ev.rows.length === 0) return res.status(404).json({ error: 'Event not found' });
        if (ev.rows[0].creator_id !== userId) return res.status(403).json({ error: 'Only creator' });

        await query(`UPDATE event_participants SET role = $1 WHERE event_id = $2 AND user_id = $3`, [role, eventId, targetId]);
        res.json({ message: 'Role updated', role });
    } catch (err) {
        console.error('[setParticipantRole]', err);
        res.status(500).json({ error: 'Internal server error' });
    }
};

// PUT /events/:id — редактировать мероприятие (только создатель)
const updateEvent = async (req, res) => {
    const userId  = req.user.id;
    const eventId = req.params.id;
    const {
        title, description, address, cover_url, category,
        max_participants, starts_at, ends_at, is_private, price, skill_level,
        event_type, price_description
    } = req.body;

    try {
        const result = await query(`
            UPDATE events SET
                title            = COALESCE($1, title),
                description      = COALESCE($2, description),
                address          = COALESCE($3, address),
                cover_url        = COALESCE($4, cover_url),
                category         = COALESCE($5, category),
                max_participants = $6,
                starts_at        = COALESCE($7, starts_at),
                ends_at          = $8,
                is_private       = COALESCE($9, is_private),
                price            = COALESCE($10::numeric, price),
                skill_level      = $11,
                event_type       = $12,
                price_description = $13,
                updated_at       = NOW()
            WHERE id = $14 AND creator_id = $15
            RETURNING *
        `, [
            title || null, description || null, address || null, cover_url || null,
            category || null,
            max_participants != null && max_participants !== '' ? parseInt(max_participants) : null,
            starts_at || null,
            ends_at || null,
            (is_private === true || is_private === 'true') ? true : (is_private === false || is_private === 'false' ? false : null),
            price != null && price !== '' ? parseFloat(price) : null,
            skill_level || null,
            event_type || null,
            price_description || null,
            eventId, userId,
        ]);

        if (result.rows.length === 0) {
            return res.status(403).json({ error: 'Not allowed or event not found' });
        }
        res.json({ event: result.rows[0] });
    } catch (err) {
        console.error('[updateEvent]', err);
        res.status(500).json({ error: 'Internal server error' });
    }
};

// DELETE /events/:id — удалить мероприятие (только создатель)
const deleteEvent = async (req, res) => {
    const userId  = req.user.id;
    const eventId = req.params.id;

    const result = await query(
        'DELETE FROM events WHERE id = $1 AND creator_id = $2 RETURNING id',
        [eventId, userId]
    );

    if (result.rows.length === 0) {
        return res.status(403).json({ error: 'Not allowed or event not found' });
    }

    res.json({ message: 'Event deleted' });
};

module.exports = {
    getEvents, getMyEvents, getEventById,
    createEvent, updateEvent, joinEvent, leaveEvent, deleteEvent,
    kickParticipant, setParticipantRole,
};
