const bcrypt   = require('bcryptjs');
const nodemailer = require('nodemailer');
const { v4: uuidv4 } = require('uuid');
const { query, transaction } = require('../config/db');
const {
    generateAccessToken,
    generateRefreshToken,
    verifyRefreshToken,
} = require('../utils/jwt');

const { sendMail } = require('../utils/mailer');

// Коды сброса пароля и подтверждения email: key -> { code, expires }. In-memory.
const resetCodes  = new Map();
const verifyCodes = new Map();   // userId -> { code, expires }

// Отправить код подтверждения email
function sendVerifyCode(userId, email) {
    const code = String(Math.floor(100000 + Math.random() * 900000));
    verifyCodes.set(userId, { code, expires: Date.now() + 30 * 60 * 1000 });
    sendMail({
        to: email,
        subject: 'Подтверждение почты в Nexory',
        text: `Ваш код подтверждения: ${code}\nКод действует 30 минут.`,
    }).then(sent => { if (!sent) console.log(`[verify] Код для ${email}: ${code}`); });
}
 
// POST /auth/register
// Создаёт нового пользователя. Email и username должны быть уникальными.
const register = async (req, res) => {
    const { username, email, password, phone } = req.body;
 
    try {
        // Проверяем, не занят ли email или username
        const existing = await query(
            'SELECT id FROM users WHERE email = $1 OR username = $2',
            [email, username]
        );
        if (existing.rows.length > 0) {
            return res.status(409).json({ error: 'Почта или никнейм уже заняты' });
        }
 
        // bcrypt с cost factor 12 — хороший баланс безопасности и скорости.
        // Никогда не храним пароль в открытом виде — только хэш.
        const passwordHash = await bcrypt.hash(password, 12);
 
        // Создаём пользователя и сразу генерируем токены в одной транзакции.
        // Если что-то упадёт — пользователь не создастся наполовину.
        const result = await transaction(async (client) => {
            const userRes = await client.query(
                `INSERT INTO users (username, email, phone, password_hash)
                 VALUES ($1, $2, $3, $4)
                 RETURNING id, username, email, role`,
                [username, email, phone || null, passwordHash]
            );
            const user = userRes.rows[0];
 
            const accessToken  = generateAccessToken(user.id, user.role);
            const refreshToken = generateRefreshToken(user.id);
 
            // Сохраняем refresh token в БД. expires_at = сейчас + 30 дней
            await client.query(
                `INSERT INTO refresh_tokens (user_id, token, expires_at)
                 VALUES ($1, $2, NOW() + INTERVAL '60 days')`,
                [user.id, refreshToken]
            );
 
            return { user, accessToken, refreshToken };
        });
 
        res.status(201).json(result);

        // Отправляем код подтверждения на email (не блокирует ответ)
        sendVerifyCode(result.user.id, email);
    } catch (err) {
        console.error('[register]', err);
        res.status(500).json({ error: 'Internal server error' });
    }
};
 
// POST /auth/login
const login = async (req, res) => {
    const { email, password } = req.body;
 
    try {
        const userRes = await query(
            'SELECT id, username, email, password_hash, role, avatar_url FROM users WHERE email = $1',
            [email]
        );
        if (userRes.rows.length === 0) {
            // Намеренно общее сообщение — не раскрываем, что именно неверно
            return res.status(401).json({ error: 'Неверная почта или пароль' });
        }
 
        const user = userRes.rows[0];
        const valid = await bcrypt.compare(password, user.password_hash);
        if (!valid) {
            return res.status(401).json({ error: 'Неверная почта или пароль' });
        }
 
        const accessToken  = generateAccessToken(user.id, user.role);
        const refreshToken = generateRefreshToken(user.id);
 
        await query(
            `INSERT INTO refresh_tokens (user_id, token, expires_at)
             VALUES ($1, $2, NOW() + INTERVAL '60 days')`,
            [user.id, refreshToken]
        );
 
        // Не отдаём password_hash клиенту
        const { password_hash, ...safeUser } = user;
        res.json({ user: safeUser, accessToken, refreshToken });
    } catch (err) {
        console.error('[login]', err);
        res.status(500).json({ error: 'Internal server error' });
    }
};
 
// POST /auth/refresh
// Клиент зовёт этот эндпоинт, когда access token истёк.
// Проверяем refresh token в БД и выдаём новую пару токенов (rotation).
const refresh = async (req, res) => {
    const { refreshToken } = req.body;
    if (!refreshToken) return res.status(400).json({ error: 'Refresh token required' });
 
    try {
        // 1. Проверяем JWT-подпись
        const payload = verifyRefreshToken(refreshToken);
 
        // 2. Проверяем, что токен есть в БД и не истёк
        const tokenRes = await query(
            `SELECT id, user_id FROM refresh_tokens
             WHERE token = $1 AND expires_at > NOW()`,
            [refreshToken]
        );
        if (tokenRes.rows.length === 0) {
            return res.status(401).json({ error: 'Refresh token invalid or expired' });
        }
 
        const { id: tokenId, user_id } = tokenRes.rows[0];
        const userRes = await query('SELECT id, role FROM users WHERE id = $1', [user_id]);
        const user = userRes.rows[0];
 
        // Token rotation: удаляем старый refresh, создаём новый.
        // Это предотвращает повторное использование украденного refresh token.
        const newAccessToken  = generateAccessToken(user.id, user.role);
        const newRefreshToken = generateRefreshToken(user.id);
 
        await transaction(async (client) => {
            await client.query('DELETE FROM refresh_tokens WHERE id = $1', [tokenId]);
            await client.query(
                `INSERT INTO refresh_tokens (user_id, token, expires_at)
                 VALUES ($1, $2, NOW() + INTERVAL '60 days')`,
                [user.id, newRefreshToken]
            );
        });
 
        res.json({ accessToken: newAccessToken, refreshToken: newRefreshToken });
    } catch (err) {
        res.status(401).json({ error: 'Invalid refresh token' });
    }
};
 
// POST /auth/logout
// Удаляем refresh token из БД — этим "убиваем" сессию.
const logout = async (req, res) => {
    const { refreshToken } = req.body;
    if (refreshToken) {
        await query('DELETE FROM refresh_tokens WHERE token = $1', [refreshToken]);
    }
    res.json({ message: 'Logged out' });
};
 
// POST /auth/request-password-reset — отправляет 6-значный код на email
const requestPasswordReset = async (req, res) => {
    const { email } = req.body;
    if (!email) return res.status(400).json({ error: 'Email required' });
    try {
        const userRes = await query('SELECT id FROM users WHERE email = $1', [email]);
        // Не раскрываем, существует ли email — всегда 200
        if (userRes.rows.length > 0) {
            const code = Math.floor(100000 + Math.random() * 900000).toString();
            resetCodes.set(email, { code, expires: Date.now() + 15 * 60 * 1000 });

            const sent = await sendMail({
                to: email,
                subject: 'Код для сброса пароля Nexory',
                text: `Ваш код для смены пароля: ${code}\nКод действует 15 минут.`,
            });
            if (!sent) console.log(`[password-reset] Код для ${email}: ${code}`);
        }
        res.json({ message: 'Если email существует, код отправлен' });
    } catch (err) {
        console.error('[requestPasswordReset]', err);
        res.status(500).json({ error: 'Internal server error' });
    }
};

// POST /auth/reset-password — меняет пароль по коду
const resetPassword = async (req, res) => {
    const { email, code, newPassword } = req.body;
    if (!newPassword || newPassword.length < 8) {
        return res.status(400).json({ error: 'Пароль должен быть не менее 8 символов' });
    }
    const entry = resetCodes.get(email);
    if (!entry || entry.code !== code || Date.now() > entry.expires) {
        return res.status(400).json({ error: 'Неверный или просроченный код' });
    }
    try {
        const hash = await bcrypt.hash(newPassword, 12);
        const r = await query('UPDATE users SET password_hash = $1 WHERE email = $2 RETURNING id', [hash, email]);
        if (r.rows.length === 0) return res.status(404).json({ error: 'User not found' });
        resetCodes.delete(email);
        // Выкидываем все сессии
        await query('DELETE FROM refresh_tokens WHERE user_id = $1', [r.rows[0].id]);
        res.json({ message: 'Пароль изменён' });
    } catch (err) {
        console.error('[resetPassword]', err);
        res.status(500).json({ error: 'Internal server error' });
    }
};

// POST /auth/verify-email (авторизован) — подтвердить почту кодом
const verifyEmail = async (req, res) => {
    const userId = req.user.id;
    const { code } = req.body;
    const entry = verifyCodes.get(userId);
    if (!entry || entry.code !== String(code || '') || Date.now() > entry.expires) {
        return res.status(400).json({ error: 'Неверный или просроченный код' });
    }
    try {
        await query('UPDATE users SET is_verified = true WHERE id = $1', [userId]);
        verifyCodes.delete(userId);
        res.json({ message: 'Почта подтверждена' });
    } catch (err) {
        console.error('[verifyEmail]', err);
        res.status(500).json({ error: 'Internal server error' });
    }
};

// POST /auth/resend-verification (авторизован) — отправить код заново
const resendVerification = async (req, res) => {
    const userId = req.user.id;
    try {
        const r = await query('SELECT email, is_verified FROM users WHERE id = $1', [userId]);
        if (r.rows.length === 0) return res.status(404).json({ error: 'User not found' });
        if (r.rows[0].is_verified) return res.json({ message: 'Уже подтверждено' });
        sendVerifyCode(userId, r.rows[0].email);
        res.json({ message: 'Код отправлен' });
    } catch (err) {
        console.error('[resendVerification]', err);
        res.status(500).json({ error: 'Internal server error' });
    }
};

module.exports = {
    register, login, refresh, logout,
    requestPasswordReset, resetPassword,
    verifyEmail, resendVerification,
};