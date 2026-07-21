const jwt = require('jsonwebtoken');

const ACCESS_SECRET  = process.env.JWT_ACCESS_SECRET  || 'nexory_access_secret';
const REFRESH_SECRET = process.env.JWT_REFRESH_SECRET || 'nexory_refresh_secret';

// Access — 1 час (реже дёргаем refresh → меньше гонок и нагрузки).
// Refresh — 60 дней, при каждом обновлении ротируется и продлевается,
// поэтому активный пользователь остаётся в сессии бессрочно.
const generateAccessToken  = (userId, role) =>
    jwt.sign({ sub: userId, role }, ACCESS_SECRET, { expiresIn: '1h' });

const generateRefreshToken = (userId) =>
    jwt.sign({ sub: userId }, REFRESH_SECRET, { expiresIn: '60d' });

const verifyAccessToken  = (token) => jwt.verify(token, ACCESS_SECRET);
const verifyRefreshToken = (token) => jwt.verify(token, REFRESH_SECRET);

module.exports = { generateAccessToken, generateRefreshToken, verifyAccessToken, verifyRefreshToken };