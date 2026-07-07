// Простой фильтр нецензурной лексики (RU/EN). Для продакшена можно заменить
// на более полный словарь или внешний сервис модерации.
const BANNED = [
    'бляд', 'сука', 'хуй', 'хуя', 'пизд', 'ебан', 'ебат', 'еблан', 'мудак',
    'гандон', 'пидор', 'долбоеб', 'манда', 'ублюдок',
    'fuck', 'shit', 'bitch', 'asshole', 'cunt', 'nigger', 'faggot',
];

function normalize(text) {
    return String(text || '')
        .toLowerCase()
        .replace(/[0@]/g, 'о')
        .replace(/3/g, 'е')
        .replace(/1/g, 'и')
        .replace(/\s+/g, ' ');
}

// Есть ли запрещённые слова
function containsProfanity(text) {
    const n = normalize(text);
    return BANNED.some(w => n.includes(w));
}

// Замаскировать запрещённые слова звёздочками (для сообщений)
function censor(text) {
    let out = String(text || '');
    const n = normalize(out);
    BANNED.forEach(w => {
        let idx = n.indexOf(w);
        while (idx !== -1) {
            out = out.slice(0, idx) + '*'.repeat(w.length) + out.slice(idx + w.length);
            idx = n.indexOf(w, idx + w.length);
        }
    });
    return out;
}

module.exports = { containsProfanity, censor };
