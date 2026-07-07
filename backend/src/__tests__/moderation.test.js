const { containsProfanity, censor } = require('../utils/moderation');

describe('Moderation', () => {
    it('находит нецензурную лексику', () => {
        expect(containsProfanity('ты сука')).toBe(true);
        expect(containsProfanity('hello fuck')).toBe(true);
    });
    it('пропускает нормальный текст', () => {
        expect(containsProfanity('Привет, как дела?')).toBe(false);
        expect(containsProfanity('Играем в футбол')).toBe(false);
    });
    it('маскирует мат звёздочками', () => {
        const out = censor('ты сука');
        expect(out).not.toContain('сука');
        expect(out).toContain('*');
    });
});
