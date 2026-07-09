const { query, transaction } = require('../config/db');

// GET /friends
const getFriends = async (req, res) => {
    const userId = req.user.id;
    const result = await query(`
        SELECT u.id, u.username, u.avatar_url, u.bio
        FROM friendships f
        JOIN users u ON u.id = CASE
            WHEN f.requester_id = $1 THEN f.addressee_id
            ELSE f.requester_id
        END
        WHERE (f.requester_id = $1 OR f.addressee_id = $1)
          AND f.status = 'accepted'
    `, [userId]);
    res.json({ friends: result.rows });
};

// GET /friends/requests
const getFriendRequests = async (req, res) => {
    const userId = req.user.id;
    const result = await query(`
        SELECT u.id, u.username, u.avatar_url, f.created_at
        FROM friendships f
        JOIN users u ON u.id = f.requester_id
        WHERE f.addressee_id = $1 AND f.status = 'pending'
    `, [userId]);
    res.json({ requests: result.rows });
};

// POST /friends/request
const sendFriendRequest = async (req, res) => {
    const requesterId  = req.user.id;
    const { addresseeId } = req.body;

    if (requesterId === addresseeId) return res.status(400).json({ error: 'Cannot add yourself' });

    try {
        await query(`
            INSERT INTO friendships (requester_id, addressee_id)
            VALUES ($1, $2)
            ON CONFLICT DO NOTHING
        `, [requesterId, addresseeId]);
        res.status(201).json({ message: 'Friend request sent' });
    } catch (err) {
        res.status(500).json({ error: 'Internal server error' });
    }
};

// POST /friends/accept
const acceptFriendRequest = async (req, res) => {
    const userId      = req.user.id;
    const { requesterId } = req.body;

    await query(`
        UPDATE friendships SET status = 'accepted'
        WHERE requester_id = $1 AND addressee_id = $2 AND status = 'pending'
    `, [requesterId, userId]);

    res.json({ message: 'Friend request accepted' });
};

// DELETE /friends/:id
const removeFriend = async (req, res) => {
    const userId   = req.user.id;
    const friendId = req.params.id;

    await query(`
        DELETE FROM friendships
        WHERE (requester_id = $1 AND addressee_id = $2)
           OR (requester_id = $2 AND addressee_id = $1)
    `, [userId, friendId]);

    res.json({ message: 'Friend removed' });
};

// DELETE /friends/request/:id — отменить исходящую заявку
const cancelFriendRequest = async (req, res) => {
    const userId    = req.user.id;
    const addressee = req.params.id;
    await query(`
        DELETE FROM friendships
        WHERE requester_id = $1 AND addressee_id = $2 AND status = 'pending'
    `, [userId, addressee]);
    res.json({ message: 'Request cancelled' });
};

module.exports = { getFriends, getFriendRequests, sendFriendRequest, acceptFriendRequest, removeFriend, cancelFriendRequest };


// -------------------------------------------------------
// Users controller
// -------------------------------------------------------

const bcrypt = require('bcryptjs');

const getProfile = async (req, res) => {
    const userId = req.params.id || req.user.id;
    const viewerId = req.user.id;
    const isSelf = userId === viewerId;
    try {
        const userRes = await query(
            `SELECT id, username, email, phone, avatar_url, bio, display_name,
                    age, country, city, sports, looking_for, activity,
                    notifications_enabled, contacts_public, profile_visibility, role, created_at,
                    notify_messages, notify_friend_events, notify_interest_events, is_verified
             FROM users WHERE id = $1`,
            [userId]
        );
        if (userRes.rows.length === 0) return res.status(404).json({ error: 'User not found' });

        const user = userRes.rows[0];

        // Статус дружбы между смотрящим и владельцем профиля
        let friendStatus = isSelf ? 'self' : 'none';
        if (!isSelf) {
            const fr = await query(`
                SELECT requester_id, status FROM friendships
                WHERE (requester_id = $1 AND addressee_id = $2) OR (requester_id = $2 AND addressee_id = $1)
                LIMIT 1
            `, [viewerId, userId]);
            if (fr.rows.length > 0) {
                const row = fr.rows[0];
                if (row.status === 'accepted') friendStatus = 'friends';
                else if (row.status === 'pending') friendStatus = (row.requester_id === viewerId) ? 'pending_out' : 'pending_in';
            }
        }
        user.friend_status = friendStatus;

        // Видимость контактов по настройке profile_visibility
        const vis = user.profile_visibility || 'friends';
        let canSeeContacts = isSelf || vis === 'all';
        if (!canSeeContacts && vis === 'friends') {
            canSeeContacts = friendStatus === 'friends';
        }
        if (!canSeeContacts && vis === 'selected') {
            const allowed = await query(
                'SELECT 1 FROM profile_allowed WHERE owner_id = $1 AND allowed_id = $2 LIMIT 1',
                [userId, viewerId]
            );
            canSeeContacts = allowed.rows.length > 0;
        }
        if (!canSeeContacts) {
            user.email = null;
            user.phone = null;
        }

        const eventsRes = await query(`
            SELECT id, title, cover_url, starts_at, category, status
            FROM events WHERE creator_id = $1 AND status = 'active'
            ORDER BY starts_at DESC LIMIT 10
        `, [userId]);

        res.json({ user, events: eventsRes.rows });
    } catch (err) {
        console.error('[getProfile]', err);
        res.status(500).json({ error: 'Internal server error' });
    }
};

const updateProfile = async (req, res) => {
    const userId = req.user.id;
    const {
        username, bio, avatar_url, display_name,
        age, country, city, sports, looking_for, activity, notifications_enabled,
        phone, contacts_public, profile_visibility,
        notify_messages, notify_friend_events, notify_interest_events,
    } = req.body;

    const boolOrNull = (v) => v !== undefined ? String(v) : null;

    try {
        const result = await query(`
            UPDATE users SET
                username               = COALESCE($1,  username),
                bio                    = COALESCE($2,  bio),
                avatar_url             = COALESCE($3,  avatar_url),
                display_name           = COALESCE($4,  display_name),
                age                    = COALESCE($5::integer, age),
                country                = COALESCE($6,  country),
                city                   = COALESCE($7,  city),
                sports                 = COALESCE($8,  sports),
                looking_for            = COALESCE($9,  looking_for),
                activity               = COALESCE($10, activity),
                notifications_enabled  = COALESCE($11::boolean, notifications_enabled),
                phone                  = COALESCE($12, phone),
                contacts_public        = COALESCE($13::boolean, contacts_public),
                profile_visibility     = COALESCE($14, profile_visibility),
                notify_messages        = COALESCE($15::boolean, notify_messages),
                notify_friend_events   = COALESCE($16::boolean, notify_friend_events),
                notify_interest_events = COALESCE($17::boolean, notify_interest_events),
                updated_at             = NOW()
            WHERE id = $18
            RETURNING id, username, bio, avatar_url, display_name,
                      age, country, city, sports, looking_for, activity,
                      notifications_enabled, contacts_public, profile_visibility, phone, email, role, created_at,
                      notify_messages, notify_friend_events, notify_interest_events
        `, [
            username    || null,
            bio         || null,
            avatar_url  || null,
            display_name || null,
            age         ? parseInt(age) : null,
            country     || null,
            city        || null,
            sports      || null,
            looking_for || null,
            activity    || null,
            boolOrNull(notifications_enabled),
            phone       || null,
            boolOrNull(contacts_public),
            profile_visibility || null,
            boolOrNull(notify_messages),
            boolOrNull(notify_friend_events),
            boolOrNull(notify_interest_events),
            userId,
        ]);

        res.json({ user: result.rows[0] });
    } catch (err) {
        if (err.code === '23505') return res.status(409).json({ error: 'Username taken' });
        console.error('[updateProfile]', err);
        res.status(500).json({ error: 'Internal server error' });
    }
};

const changePassword = async (req, res) => {
    const userId = req.user.id;
    const { oldPassword, newPassword } = req.body;

    const userRes = await query('SELECT password_hash FROM users WHERE id = $1', [userId]);
    const valid   = await bcrypt.compare(oldPassword, userRes.rows[0].password_hash);
    if (!valid) return res.status(401).json({ error: 'Wrong password' });

    const newHash = await bcrypt.hash(newPassword, 12);
    await query('UPDATE users SET password_hash = $1 WHERE id = $2', [newHash, userId]);
    await query('DELETE FROM refresh_tokens WHERE user_id = $1', [userId]);

    res.json({ message: 'Password changed. Please login again.' });
};

const updateFcmToken = async (req, res) => {
    const userId = req.user.id;
    const { fcmToken } = req.body;
    await query('UPDATE users SET fcm_token = $1 WHERE id = $2', [fcmToken, userId]);
    res.json({ message: 'FCM token updated' });
};

const searchUsers = async (req, res) => {
    // Разрешаем писать ник с "@" — убираем его перед поиском по username
    const q = (req.query.q || '').replace(/^@+/, '').trim();
    if (!q || q.length < 2) return res.json({ users: [] });

    try {
        const result = await query(`
            SELECT id, username, avatar_url, bio, display_name, city, country
            FROM users
            WHERE username ILIKE $1 OR display_name ILIKE $1 OR phone ILIKE $1
            LIMIT 20
        `, [`%${q}%`]);

        res.json({ users: result.rows });
    } catch (err) {
        console.error('[searchUsers]', err);
        res.status(500).json({ error: 'Internal server error' });
    }
};

// GET /users/me/allowed — id выбранных друзей, кому виден профиль
const getAllowed = async (req, res) => {
    try {
        const r = await query('SELECT allowed_id FROM profile_allowed WHERE owner_id = $1', [req.user.id]);
        res.json({ ids: r.rows.map(row => row.allowed_id) });
    } catch (err) {
        console.error('[getAllowed]', err);
        res.status(500).json({ error: 'Internal server error' });
    }
};

// PUT /users/me/allowed — заменить список выбранных друзей { ids: [...] }
const setAllowed = async (req, res) => {
    const userId = req.user.id;
    const ids = Array.isArray(req.body.ids) ? req.body.ids : [];
    try {
        await transaction(async (client) => {
            await client.query('DELETE FROM profile_allowed WHERE owner_id = $1', [userId]);
            for (const id of ids) {
                await client.query(
                    'INSERT INTO profile_allowed (owner_id, allowed_id) VALUES ($1, $2) ON CONFLICT DO NOTHING',
                    [userId, id]
                );
            }
        });
        res.json({ message: 'Updated', ids });
    } catch (err) {
        console.error('[setAllowed]', err);
        res.status(500).json({ error: 'Internal server error' });
    }
};

module.exports.users = { getProfile, updateProfile, changePassword, updateFcmToken, searchUsers, getAllowed, setAllowed };


// -------------------------------------------------------
// Support controller
// -------------------------------------------------------

const { sendMail } = require('../utils/mailer');

const createTicket = async (req, res) => {
    const userId  = req.user.id;
    const { subject, body } = req.body;

    try {
        await query(`
            INSERT INTO support_tickets (user_id, subject, body)
            VALUES ($1, $2, $3)
        `, [userId, subject, body]);

        const userRes = await query('SELECT username, email FROM users WHERE id = $1', [userId]);
        const user    = userRes.rows[0];

        // Дублируем на почту поддержки (если настроена). Тикет уже сохранён в БД в любом случае.
        if (process.env.SUPPORT_EMAIL) {
            sendMail({
                to: process.env.SUPPORT_EMAIL,
                subject: `[Nexory] ${subject}`,
                html: `<h3>Новое обращение</h3>
                       <p><b>От:</b> ${user.username} (${user.email})</p>
                       <p><b>Тема:</b> ${subject}</p><hr>
                       <p>${String(body).replace(/\n/g, '<br>')}</p>`,
            });
        }

        res.status(201).json({ message: 'Обращение отправлено' });
    } catch (err) {
        console.error('[support]', err);
        res.status(500).json({ error: 'Internal server error' });
    }
};

module.exports.support = { createTicket };


// -------------------------------------------------------
// Reports controller — жалобы на контент (модерация)
// -------------------------------------------------------

const createReport = async (req, res) => {
    const reporterId = req.user.id;
    const { target_type, target_id, reason } = req.body;
    try {
        await query(
            `INSERT INTO reports (reporter_id, target_type, target_id, reason) VALUES ($1, $2, $3, $4)`,
            [reporterId, target_type, target_id, reason]
        );
        res.status(201).json({ message: 'Жалоба отправлена, спасибо' });
    } catch (err) {
        console.error('[createReport]', err);
        res.status(500).json({ error: 'Internal server error' });
    }
};

// GET /reports — список жалоб (только админ)
const listReports = async (req, res) => {
    try {
        const r = await query(`
            SELECT r.*, u.username AS reporter_username
            FROM reports r JOIN users u ON u.id = r.reporter_id
            WHERE r.status = 'open'
            ORDER BY r.created_at DESC LIMIT 200
        `);
        res.json({ reports: r.rows });
    } catch (err) {
        console.error('[listReports]', err);
        res.status(500).json({ error: 'Internal server error' });
    }
};

module.exports.reports = { createReport, listReports };
