const validator = require('validator');
const { normalizeUrl } = require('../utils/url');

// Правила должны совпадать с клиентскими (CreateEventScreen.isValidTicketUrl):
// расхождение означает, что кнопка сохранения активна, а сервер отвечает 400.
const isAccepted = (raw) => {
    const normalized = normalizeUrl(raw);
    if (normalized === '') return true;   // пустое поле допустимо (ссылки нет)
    return validator.isURL(normalized, { protocols: ['http', 'https'], require_protocol: true });
};

describe('Ссылка на покупку билета', () => {
    it('принимает полный адрес с протоколом', () => {
        expect(isAccepted('https://timepad.ru/event/12345/')).toBe(true);
        expect(isAccepted('http://example.com/tickets')).toBe(true);
        expect(isAccepted('https://vk.com/club123')).toBe(true);
    });

    it('требует протокол — адрес указывается полностью', () => {
        // Схема намеренно НЕ дописывается за пользователя: приложение открывает
        // ссылку во внешнем браузере, и догадка о протоколе увела бы его не туда
        expect(normalizeUrl('vk.com/club123')).toBe('vk.com/club123');
        expect(isAccepted('vk.com/club123')).toBe(false);
        expect(isAccepted('www.timepad.ru/event/1')).toBe(false);
    });

    it('переживает пробелы по краям (вставка из буфера)', () => {
        expect(normalizeUrl('  https://example.com/x  ')).toBe('https://example.com/x');
        expect(isAccepted('  https://example.com/tickets  ')).toBe(true);
    });

    it('пустое значение остаётся пустым — это «ссылки нет»', () => {
        expect(normalizeUrl('')).toBe('');
        expect(normalizeUrl('   ')).toBe('');
        expect(isAccepted('')).toBe(true);
    });

    it('оставляет undefined/null нетронутыми («поле не менять»)', () => {
        expect(normalizeUrl(undefined)).toBeUndefined();
        expect(normalizeUrl(null)).toBeNull();
    });

    it('отклоняет схемы, кроме http и https', () => {
        expect(isAccepted('intent://evil/#Intent;end')).toBe(false);
        expect(isAccepted('file:///etc/passwd')).toBe(false);
        expect(isAccepted('javascript:alert(1)')).toBe(false);
    });

    it('отклоняет то, что не похоже на адрес', () => {
        expect(isAccepted('просто текст')).toBe(false);
        expect(isAccepted('@@@')).toBe(false);
    });
});
