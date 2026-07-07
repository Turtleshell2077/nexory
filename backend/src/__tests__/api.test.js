const request = require('supertest');
const app = require('../app');

describe('Health', () => {
    it('GET /health → 200 ok', async () => {
        const res = await request(app).get('/health');
        expect(res.status).toBe(200);
        expect(res.body.status).toBe('ok');
    });
});

describe('Validation', () => {
    it('register с пустым телом → 400', async () => {
        const res = await request(app).post('/api/v1/auth/register').send({});
        expect(res.status).toBe(400);
        expect(res.body.error).toBeDefined();
    });

    it('register с коротким паролем → 400', async () => {
        const res = await request(app)
            .post('/api/v1/auth/register')
            .send({ username: 'ok', email: 'a@b.com', password: '123' });
        expect(res.status).toBe(400);
    });

    it('login с невалидным email → 400', async () => {
        const res = await request(app).post('/api/v1/auth/login').send({ email: 'not-email', password: 'x' });
        expect(res.status).toBe(400);
    });
});

describe('Auth guard', () => {
    it('защищённый маршрут без токена → 401', async () => {
        const res = await request(app).get('/api/v1/users/me');
        expect(res.status).toBe(401);
    });
});

describe('404', () => {
    it('неизвестный путь → 404', async () => {
        const res = await request(app).get('/api/v1/definitely/not/here');
        expect(res.status).toBe(404);
    });
});
