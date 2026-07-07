const { validationResult } = require('express-validator');

// Прогоняет результаты express-validator; при ошибках — 400 с деталями.
module.exports = function validate(req, res, next) {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
        return res.status(400).json({
            error: 'Проверьте правильность заполнения полей',
            details: errors.array().map(e => ({ field: e.path, msg: e.msg })),
        });
    }
    next();
};
