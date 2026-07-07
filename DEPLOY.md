# Nexory — развёртывание в продакшн

## 1. Бэкенд (Node.js + PostgreSQL)

### Требования
- Node.js 18+ (проверено на 24)
- PostgreSQL 13+
- (опц.) домен + SSL-сертификат, nginx как reverse proxy

### Шаги
```bash
cd backend
cp .env.example .env          # заполните реальными значениями
npm install --omit=dev        # прод-зависимости
npm run migrate               # применить все миграции 001–009
npm start                     # или через pm2 (см. ниже)
```

### Обязательно перед продом
- Смените `JWT_ACCESS_SECRET` и `JWT_REFRESH_SECRET` на длинные случайные строки
  (`node -e "console.log(require('crypto').randomBytes(48).toString('hex'))"`).
- Задайте сильный `DB_PASSWORD`.
- Ограничьте `CORS_ORIGINS` (не `*`) если у вас есть веб-клиент.
- Поставьте `NODE_ENV=production`.

### Запуск как сервис (pm2)
```bash
npm install -g pm2
pm2 start src/index.js --name nexory-api
pm2 save && pm2 startup
```

### HTTPS
Вариант А — терминация SSL на nginx (рекомендуется):
```nginx
server {
    listen 443 ssl;
    server_name your-domain;
    ssl_certificate     /etc/letsencrypt/live/your-domain/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your-domain/privkey.pem;

    location / {
        proxy_pass http://127.0.0.1:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;      # для WebSocket
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```
При этом в `.env` поставьте `TRUST_PROXY=1`.
Сертификат: `sudo certbot --nginx -d your-domain` (Let's Encrypt, бесплатно, автопродление).

Вариант Б — HTTPS прямо в Node: задайте `SSL_KEY_PATH` и `SSL_CERT_PATH` в `.env`.

### Тесты
```bash
npm test        # jest: валидация, авторизация, модерация, health (без БД)
```

### Что уже сделано для продакшна
- Валидация входных данных (express-validator) на всех ключевых эндпоинтах.
- Rate limiting: 120 req/мин общий + 30/15мин на `/auth` (защита от перебора и спама).
- helmet (безопасные заголовки), compression (сжатие ответов).
- Модерация: фильтр мата (маскирование в чатах, запрет в событиях) + жалобы `/reports`.
- Загрузка и сжатие фото (multer + sharp, до 1080px, jpeg 82%).
- Индексы БД под частые запросы; graceful shutdown; глобальная обработка ошибок.
- Health-check `/health` для балансировщика/мониторинга.

## 2. Android-приложение

1. В `app/src/main/java/com/nexory/app/di/AppModule.kt` замените `BASE_URL`
   на адрес сервера: `https://your-domain/api/v1/`.
2. В `data/websocket/ChatWebSocketManager.kt` замените `WS_BASE_URL`
   на `wss://your-domain/ws`.
3. Если сервер по HTTPS — уберите `usesCleartextTraffic="true"` из `AndroidManifest.xml`.
4. `Build → Generate Signed Bundle / APK` → загрузите `.aab` в Google Play Console.

## 3. Чек-лист перед публикацией
- [ ] JWT-секреты и пароль БД сменены
- [ ] Сервер по HTTPS, домен настроен
- [ ] BASE_URL / WS_BASE_URL в приложении указывают на прод
- [ ] `npm run migrate` выполнен на проде
- [ ] `npm test` зелёный
- [ ] Настроены бэкапы PostgreSQL (например, `pg_dump` по cron)
- [ ] Мониторинг `/health`
