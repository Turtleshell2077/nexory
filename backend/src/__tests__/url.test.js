const validator = require('validator');
const { normalizeUrl } = require('../utils/url');

// Правила должны совпадать с клиентскими (CreateEventScreen.normalizeTicketUrl):
// расхождение означает, что кнопка сохранения активна, а сервер отвечает 400.
const isAccepted = (raw) => {
    const normalized = normalizeUrl(raw);
    if (normalized === '') return true;   // пустое поле допустимо (ссылки нет)
    return validator.isURL(normalized, { protocols: ['http', 'https'], require_protocol: true });
};

describe('Ссылка на покупку билета', () => {
    it('принимает адрес, введённый без протокола', () => {
        // Именно так его вводит обычный пользователь — раньше такой адрес
        // отклонялся, и создать платное мероприятие было нельзя
        expect(normalizeUrl('vk.com/club123')).toBe('https://vk.com/club123');
        expect(isAccepted('vk.com/club123')).toBe(true);
        expect(isAccepted('www.timepad.ru/event/1')).toBe(true);
    });

    it('не трогает адрес с уже указанным протоколом', () => {
        expect(normalizeUrl('http://example.com/x')).toBe('http://example.com/x');
        expect(isAccepted('https://timepad.ru/event/12345/')).toBe(true);
    });

    it('переживает пробелы по краям', () => {
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
        // По ссылке открывается внешнее приложение — произвольная схема
        // дала бы организатору способ увести пользователя куда угодно
        expect(isAccepted('intent://evil/#Intent;end')).toBe(false);
        expect(isAccepted('file:///etc/passwd')).toBe(false);
        expect(isAccepted('javascript:alert(1)')).toBe(false);
    });

    it('отклоняет то, что не похоже на адрес', () => {
        expect(isAccepted('просто текст')).toBe(false);
        expect(isAccepted('@@@')).toBe(false);
    });
});
