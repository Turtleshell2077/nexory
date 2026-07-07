const jwt = require('jsonwebtoken');

const ACCESS_SECRET  = process.env.JWT_ACCESS_SECRET  || 'nexory_access_secret';
const REFRESH_SECRET = process.env.JWT_REFRESH_SECRET || 'nexory_refresh_secret';

const generateAccessToken  = (userId, role) =>
    jwt.sign({ sub: userId, role }, ACCESS_SECRET, { expiresIn: '15m' });

const generateRefreshToken = (userId) =>
    jwt.sign({ sub: userId }, REFRESH_SECRET, { expiresIn: '30d' });

const verifyAccessToken  = (token) => jwt.verify(token, ACCESS_SECRET);
const verifyRefreshToken = (token) => jwt.verify(token, REFRESH_SECRET);

module.exports = { generateAccessToken, generateRefreshToken, verifyAccessToken, verifyRefreshToken };