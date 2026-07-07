const fs   = require('fs');
const path = require('path');
 
// Для получения OAuth2 access token используем нативный HTTPS
// без лишних зависимостей — только jsonwebtoken который уже есть
const jwt  = require('jsonwebtoken');
const https = require('https');
 
const { query } = require('../config/db');
 
let serviceAccount = null;
let cachedToken    = null;
let tokenExpiresAt = 0;
 
function loadServiceAccount() {
    if (serviceAccount) return serviceAccount;
    const saPath = process.env.FCM_SERVICE_ACCOUNT_PATH
        || path.join(__dirname, '../../config/fcm-service-account.json');
    try {
        serviceAccount = JSON.parse(fs.readFileSync(saPath, 'utf8'));
    } catch {
        console.warn('[FCM] Service account not found — push notifications disabled');
    }
    return serviceAccount;
}
 
// Получаем OAuth2 access token через JWT-подписанный запрос к Google.
// Токен живёт 1 час, кэшируем его.
async function getFcmAccessToken() {
    if (cachedToken && Date.now() < tokenExpiresAt - 60_000) {
        return cachedToken;
    }
 
    const sa = loadServiceAccount();
    if (!sa) return null;
 
    const now    = Math.floor(Date.now() / 1000);
    const jwtPayload = {
        iss:   sa.client_email,
        scope: 'https://www.googleapis.com/auth/firebase.messaging',
        aud:   'https://oauth2.googleapis.com/token',
        exp:   now + 3600,
        iat:   now,
    };
 
    const signedJwt = jwt.sign(jwtPayload, sa.private_key, { algorithm: 'RS256' });
 
    // Обмениваем подписанный JWT на access token
    return new Promise((resolve, reject) => {
        const body = `grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=${signedJwt}`;
        const options = {
            hostname: 'oauth2.googleapis.com',
            path:     '/token',
            method:   'POST',
            headers: {
                'Content-Type':   'application/x-www-form-urlencoded',
                'Content-Length': Buffer.byteLength(body),
            },
        };
 
        const req = https.request(options, (res) => {
            let data = '';
            res.on('data', chunk => data += chunk);
            res.on('end', () => {
                try {
                    const parsed = JSON.parse(data);
                    cachedToken    = parsed.access_token;
                    tokenExpiresAt = Date.now() + (parsed.expires_in * 1000);
                    resolve(cachedToken);
                } catch (e) {
                    reject(e);
                }
            });
        });
        req.on('error', reject);
        req.write(body);
        req.end();
    });
}
 
// Основная функция отправки push-уведомления.
// fcmToken — это токен устройства, который Android-клиент сохраняет в БД.
// payload = { title, body, data: { ... } }
async function sendPushNotification(fcmToken, payload) {
    const accessToken = await getFcmAccessToken();
    if (!accessToken) return; // push отключены
 
    const sa        = loadServiceAccount();
    const projectId = sa.project_id;
 
    const message = {
        message: {
            token: fcmToken,
            notification: {
                title: payload.title,
                body:  payload.body,
            },
            // data — дополнительные поля для клиента (не показываются в уведомлении,
            // но доступны в коде приложения для навигации)
            data: Object.fromEntries(
                Object.entries(payload.data || {}).map(([k, v]) => [k, String(v)])
            ),
            android: {
                priority: 'HIGH',
                notification: {
                    sound:        'default',
                    click_action: 'FLUTTER_NOTIFICATION_CLICK', // или ваш action
                },
            },
        },
    };
 
    const body    = JSON.stringify(message);
    const options = {
        hostname: 'fcm.googleapis.com',
        path:     `/v1/projects/${projectId}/messages:send`,
        method:   'POST',
        headers: {
            'Authorization': `Bearer ${accessToken}`,
            'Content-Type':  'application/json',
            'Content-Length': Buffer.byteLength(body),
        },
    };
 
    return new Promise((resolve) => {
        const req = https.request(options, (res) => {
            let data = '';
            res.on('data', chunk => data += chunk);
            res.on('end', () => {
                if (res.statusCode !== 200) {
                    console.warn('[FCM] Send failed:', data);
                }
                resolve();
            });
        });
        req.on('error', (e) => console.error('[FCM] Request error:', e));
        req.write(body);
        req.end();
    });
}
 
// Уведомить конкретного пользователя (используется в eventsController)
async function notifyEventParticipants(eventId, { title, body, targetUserId }) {
    const userId = targetUserId;
    const res    = await query('SELECT fcm_token FROM users WHERE id = $1', [userId]);
    const token  = res.rows[0]?.fcm_token;
    if (token) {
        await sendPushNotification(token, { title, body, data: { eventId } });
    }
}
 
// Уведомления о новом мероприятии: друзьям создателя и пользователям по интересам.
// Учитывает пользовательские настройки уведомлений. Не блокирует ответ API (fire-and-forget).
async function notifyNewEvent(event, creatorId) {
    try {
        // 1) Друзья создателя (у кого включено notify_friend_events)
        const friends = await query(`
            SELECT u.id, u.fcm_token
            FROM friendships f
            JOIN users u ON u.id = CASE WHEN f.requester_id = $1 THEN f.addressee_id ELSE f.requester_id END
            WHERE (f.requester_id = $1 OR f.addressee_id = $1) AND f.status = 'accepted'
              AND u.notifications_enabled = true AND u.notify_friend_events = true
              AND u.fcm_token IS NOT NULL
        `, [creatorId]);

        for (const u of friends.rows) {
            await sendPushNotification(u.fcm_token, {
                title: 'Новое мероприятие от друга',
                body:  event.title,
                data:  { eventId: event.id, type: 'friend_event' },
            }).catch(() => {});
        }

        // 2) По интересам (только для публичных мероприятий, исключая друзей — им уже ушло)
        if (!event.is_private) {
            const catLike  = `%${String(event.category || '').toLowerCase()}%`;
            const typeLike = `%${String(event.event_type || '').toLowerCase()}%`;
            const interested = await query(`
                SELECT id, fcm_token FROM users
                WHERE id <> $1
                  AND notifications_enabled = true AND notify_interest_events = true
                  AND fcm_token IS NOT NULL AND sports IS NOT NULL AND sports <> ''
                  AND (LOWER(sports) LIKE $2 OR ($3 <> '%%' AND LOWER(sports) LIKE $3))
                  AND NOT EXISTS (
                      SELECT 1 FROM friendships f WHERE f.status = 'accepted' AND (
                        (f.requester_id = $1 AND f.addressee_id = users.id) OR
                        (f.addressee_id = $1 AND f.requester_id = users.id)
                      )
                  )
                LIMIT 500
            `, [creatorId, catLike, typeLike]);

            for (const u of interested.rows) {
                await sendPushNotification(u.fcm_token, {
                    title: 'Мероприятие по твоим интересам',
                    body:  event.title,
                    data:  { eventId: event.id, type: 'interest_event' },
                }).catch(() => {});
            }
        }
    } catch (err) {
        console.error('[notifyNewEvent]', err.message);
    }
}

module.exports = { sendPushNotification, notifyEventParticipants, notifyNewEvent };