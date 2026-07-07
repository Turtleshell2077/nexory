const { body, query } = require('express-validator');

// Наборы правил валидации для маршрутов
module.exports = {
    auth: {
        register: [
            body('username').trim().isLength({ min: 2, max: 50 }).withMessage('Имя: 2–50 символов'),
            body('email').isEmail().withMessage('Некорректный email').normalizeEmail(),
            body('password').isLength({ min: 8 }).withMessage('Пароль минимум 8 символов'),
            body('phone').optional({ checkFalsy: true }).isLength({ max: 20 }),
        ],
        login: [
            body('email').isEmail().withMessage('Некорректный email'),
            body('password').notEmpty().withMessage('Введите пароль'),
        ],
        requestReset: [ body('email').isEmail() ],
        resetPassword: [
            body('email').isEmail(),
            body('code').trim().notEmpty(),
            body('newPassword').isLength({ min: 8 }),
        ],
    },

    events: {
        create: [
            body('title').trim().isLength({ min: 1, max: 200 }).withMessage('Укажите название'),
            body('address').trim().notEmpty().withMessage('Укажите место'),
            body('starts_at').notEmpty().withMessage('Укажите дату и время'),
            body('max_participants').optional({ checkFalsy: true }).isInt({ min: 1, max: 100000 }),
            body('price').optional({ checkFalsy: true }).isFloat({ min: 0 }),
            body('description').optional({ checkFalsy: true }).isLength({ max: 5000 }),
        ],
        update: [
            body('title').optional({ checkFalsy: true }).isLength({ max: 200 }),
            body('max_participants').optional({ checkFalsy: true }).isInt({ min: 1, max: 100000 }),
            body('price').optional({ checkFalsy: true }).isFloat({ min: 0 }),
        ],
    },

    users: {
        update: [
            body('username').optional({ checkFalsy: true }).isLength({ min: 2, max: 50 }),
            body('age').optional({ checkFalsy: true }).isInt({ min: 1, max: 120 }),
            body('bio').optional({ nullable: true }).isLength({ max: 1000 }),
            body('phone').optional({ nullable: true }).isLength({ max: 20 }),
        ],
        changePassword: [
            body('oldPassword').notEmpty(),
            body('newPassword').isLength({ min: 8 }).withMessage('Новый пароль минимум 8 символов'),
        ],
        search: [ query('q').trim().isLength({ min: 2 }).withMessage('Минимум 2 символа') ],
    },

    chats: {
        send: [ body('content').trim().isLength({ min: 1, max: 4000 }).withMessage('Пустое сообщение') ],
    },

    support: {
        create: [
            body('subject').trim().isLength({ min: 1, max: 300 }).withMessage('Укажите тему'),
            body('body').trim().isLength({ min: 1, max: 5000 }).withMessage('Опишите проблему'),
        ],
    },

    reports: {
        create: [
            body('target_type').isIn(['user', 'event', 'message']).withMessage('Некорректный тип'),
            body('target_id').trim().notEmpty(),
            body('reason').trim().isLength({ min: 1, max: 500 }).withMessage('Укажите причину'),
        ],
    },
};
