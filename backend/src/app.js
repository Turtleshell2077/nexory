require('dotenv').config();

const express     = require('express');
const path        = require('path');
const cors        = require('cors');
const helmet      = require('helmet');
const compression = require('compression');
const rateLimit   = require('express-rate-limit');
const routes      = require('./routes');

const app = express();

// За обратным прокси (nginx) доверяем ОДНОМУ хопу, чтобы rate limit и логи видели
// реальный IP клиента из X-Forwarded-For. Значение 1 (а не true) безопасно: клиент
// не может подделать свой IP лишним заголовком. В dev без прокси заголовка нет —
// Express берёт IP сокета, защита не слабеет. Переопределяется через env TRUST_PROXY.
app.set('trust proxy', Number(process.env.TRUST_PROXY) || 1);

// Безопасные заголовки. CORP отключаем, чтобы картинки из /uploads грузились с другого origin.
app.use(helmet({ crossOriginResourcePolicy: false, contentSecurityPolicy: false }));

// Сжатие ответов — меньше трафика при большом числе пользователей
app.use(compression());

// CORS: список разрешённых origin из env (через запятую) или * по умолчанию
const corsOrigins = (process.env.CORS_ORIGINS || '*').split(',').map(s => s.trim());
app.use(cors({
    origin: corsOrigins.includes('*') ? '*' : corsOrigins,
    methods: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS'],
    allowedHeaders: ['Content-Type', 'Authorization'],
}));

app.use(express.json({ limit: '5mb' }));

// ---- Rate limiting ----
// Общий лимит на API + строгий лимит на авторизацию (защита от перебора паролей и спама)
const globalLimiter = rateLimit({
    windowMs: 60 * 1000,
    max: 120,
    standardHeaders: true,
    legacyHeaders: false,
    message: { error: 'Слишком много запросов, попробуйте чуть позже' },
});
const authLimiter = rateLimit({
    windowMs: 15 * 60 * 1000,
    max: 30,
    standardHeaders: true,
    legacyHeaders: false,
    message: { error: 'Слишком много попыток входа, попробуйте позже' },
});
app.use('/api/', globalLimiter);
app.use('/api/v1/auth', authLimiter);

// Статика загруженных изображений (с кэшированием)
app.use('/uploads', express.static(path.join(__dirname, '../uploads'), {
    maxAge: '7d',
    immutable: true,
}));

// Healthcheck — для мониторинга/балансировщика
app.get('/health', (req, res) => res.json({ status: 'ok', time: new Date().toISOString() }));

// Все API-маршруты под префиксом /api/v1
app.use('/api/v1', routes);

// 404 для неизвестных путей
app.use((req, res) => res.status(404).json({ error: 'Not found' }));

// Глобальный обработчик ошибок
app.use((err, req, res, next) => {
    if (err && err.type === 'entity.too.large') {
        return res.status(413).json({ error: 'Слишком большой запрос' });
    }
    console.error('[express error]', err);
    if (!res.headersSent) res.status(500).json({ error: 'Internal server error' });
});

module.exports = app;
