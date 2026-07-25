const request = require('supertest');
const app = require('../app');

// Ссылки на юридические документы обязательны для модерации RuStore и Google Play.
// Если эти тесты падают — билд отправлять нельзя: приложение ссылается на эти адреса
// с экрана согласия ДО регистрации, и модератор их обязательно проверит.
describe('Юридические документы', () => {
    it('GET /legal/privacy → 200, HTML политики конфиденциальности', async () => {
        const res = await request(app).get('/legal/privacy');
        expect(res.status).toBe(200);
        expect(res.headers['content-type']).toMatch(/html/);
        expect(res.text).toMatch(/Политика конфиденциальности/);
    });

    it('GET /legal/terms → 200, HTML пользовательского соглашения', async () => {
        const res = await request(app).get('/legal/terms');
        expect(res.status).toBe(200);
        expect(res.headers['content-type']).toMatch(/html/);
        expect(res.text).toMatch(/Пользовательское соглашение/);
    });

    it('политика описывает удаление аккаунта (требование Google Play)', async () => {
        const res = await request(app).get('/legal/privacy');
        expect(res.text).toMatch(/Удалить аккаунт/);
    });

    // Google Play требует способ запросить удаление данных ВНЕ приложения.
    // Этот URL указывается в Play Console — если страница отдаёт 404, будет отказ.
    it('GET /legal/delete-account → 200, страница удаления аккаунта', async () => {
        const res = await request(app).get('/legal/delete-account');
        expect(res.status).toBe(200);
        expect(res.text).toMatch(/Удаление аккаунта/);
        expect(res.text).toMatch(/Настройки/);
    });

    // Реквизиты оператора заполнены. Тест держит это состояние: если placeholder
    // вернётся (например, при правке шаблона), сборка упадёт до отправки в стор,
    // а не будет отклонена модерацией.
    it('в документах не осталось незаполненных placeholder\'ов', async () => {
        for (const path of ['/legal/privacy', '/legal/terms', '/legal/delete-account']) {
            const res = await request(app).get(path);
            expect(res.text).not.toMatch(/\[УКАЖИТЕ/);
        }
    });

    it('указан оператор персональных данных и контакт для обращений', async () => {
        const privacy = await request(app).get('/legal/privacy');
        // 152-ФЗ требует, чтобы оператор был идентифицируем и имел контакт для обращений
        expect(privacy.text).toMatch(/Малышев Алексей Александрович/);
        expect(privacy.text).toMatch(/turtleshell2077@gmail\.com/);
    });

    it('на странице удаления аккаунта есть контакт для потерявших доступ', async () => {
        const res = await request(app).get('/legal/delete-account');
        expect(res.text).toMatch(/turtleshell2077@gmail\.com/);
    });
});
