const nodemailer = require('nodemailer');

// Единый почтовый транспорт. Ключевой фикс: secure зависит от порта
// (465 = SSL, 587/иные = STARTTLS). Раньше secure было жёстко true —
// из-за чего письма молча не отправлялись на портах вроде 587.
let transporter = null;

if (process.env.SMTP_HOST) {
    const port = parseInt(process.env.SMTP_PORT) || 465;
    transporter = nodemailer.createTransport({
        host:   process.env.SMTP_HOST,
        port,
        secure: port === 465,
        auth:   { user: process.env.SMTP_USER, pass: process.env.SMTP_PASS },
    });
    // Проверяем соединение при старте — сразу видно в логах, работает ли почта
    transporter.verify()
        .then(() => console.log('✓ SMTP готов — письма будут отправляться'))
        .catch((e) => console.error('⚠️  SMTP НЕ работает:', e.message,
            '\n   Проверьте SMTP_HOST/PORT/USER/PASS в .env'));
} else {
    console.warn('⚠️  SMTP не настроен (нет SMTP_HOST) — письма и коды не отправляются, коды пишутся в консоль.');
}

// Отправка письма. Возвращает true/false. Не бросает исключений.
async function sendMail({ to, subject, text, html }) {
    if (!transporter) {
        console.log(`[mail:no-smtp] «${subject}» → ${to}`);
        return false;
    }
    try {
        await transporter.sendMail({
            from: process.env.SMTP_FROM || `"Nexory" <${process.env.SMTP_USER}>`,
            to, subject, text, html,
        });
        return true;
    } catch (e) {
        console.error('[mail] ошибка отправки:', e.message);
        return false;
    }
}

const isConfigured = () => !!transporter;

module.exports = { sendMail, isConfigured };
