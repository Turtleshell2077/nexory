const multer = require('multer');
const path   = require('path');
const fs     = require('fs');

// sharp — опционально: если установлен, сжимаем/уменьшаем изображения (экономия трафика).
// Если по какой-то причине недоступен на сервере — просто отдаём оригинал.
let sharp = null;
try { sharp = require('sharp'); } catch (_) { console.warn('[upload] sharp недоступен — без сжатия'); }

const uploadDir = path.join(__dirname, '../../uploads');
if (!fs.existsSync(uploadDir)) fs.mkdirSync(uploadDir, { recursive: true });

const storage = multer.diskStorage({
    destination: (req, file, cb) => cb(null, uploadDir),
    filename: (req, file, cb) => {
        const ext = path.extname(file.originalname) || '.jpg';
        cb(null, `${Date.now()}_${Math.round(Math.random() * 1e9)}${ext}`);
    },
});

// ⚠️ ВАЖНО ПРО NGINX:
// Даже с этим лимитом загрузка оборвётся, если у nginx не поднят client_max_body_size —
// по умолчанию он всего 1 МБ, и запрос не доходит до Node (ответ 413).
// В конфиге сайта должно быть:  client_max_body_size 15m;
// Клиент сжимает фото до ~200–600 КБ, но запас нужен для нестандартных форматов.
const upload = multer({
    storage,
    limits: { fileSize: 12 * 1024 * 1024 }, // 12 МБ
    fileFilter: (req, file, cb) => {
        if (file.mimetype.startsWith('image/')) cb(null, true);
        else cb(new Error('Можно загружать только изображения'));
    },
}).single('file');

// POST /upload — multipart/form-data, поле "file". Возвращает абсолютный URL.
const uploadImage = (req, res) => {
    upload(req, res, async (err) => {
        if (err) {
            console.error('[upload]', err.code || '', err.message);
            // Разделяем причины: клиенту важно понимать, менять фото или настройки
            if (err.code === 'LIMIT_FILE_SIZE') {
                return res.status(413).json({ error: 'Файл слишком большой (максимум 12 МБ)' });
            }
            return res.status(400).json({ error: err.message });
        }
        if (!req.file) return res.status(400).json({ error: 'Файл не получен' });

        const base = `${req.protocol}://${req.get('host')}`;

        // Пробуем сжать через sharp
        if (sharp) {
            try {
                const outName = `${path.parse(req.file.filename).name}_p.jpg`;
                const outPath = path.join(uploadDir, outName);
                await sharp(req.file.path)
                    .rotate() // авто-ориентация по EXIF
                    .resize(1080, 1080, { fit: 'inside', withoutEnlargement: true })
                    .jpeg({ quality: 82 })
                    .toFile(outPath);
                fs.unlink(req.file.path, () => {}); // удаляем оригинал
                return res.status(201).json({ url: `${base}/uploads/${outName}` });
            } catch (e) {
                console.error('[upload sharp]', e.message);
                // падаем на оригинал
            }
        }
        res.status(201).json({ url: `${base}/uploads/${req.file.filename}` });
    });
};

/**
 * Удаляет ранее загруженный файл по его публичному URL.
 *
 * Используется при удалении фото профиля/мероприятия и при удалении аккаунта,
 * чтобы на диске не копились «осиротевшие» картинки.
 *
 * Безопасность: берём только basename и проверяем, что он не содержит переходов
 * по каталогам — URL приходит из БД, но относиться к нему как к доверенному пути нельзя.
 * Ошибки не пробрасываем: неудача удаления файла не должна ломать основную операцию.
 */
function deleteUploadedFile(fileUrl) {
    if (!fileUrl) return false;
    try {
        const pathname = new URL(fileUrl, 'http://local').pathname;
        // Трогаем только собственные загрузки
        if (!pathname.includes('/uploads/')) return false;

        const fileName = path.basename(pathname);
        if (!fileName || fileName.includes('..') || fileName.includes('/')) return false;

        const filePath = path.join(uploadDir, fileName);
        // Финальная проверка: результат обязан лежать внутри uploads
        if (!filePath.startsWith(uploadDir)) return false;

        if (fs.existsSync(filePath)) {
            fs.unlinkSync(filePath);
            return true;
        }
    } catch (e) {
        console.warn('[deleteUploadedFile]', e.message);
    }
    return false;
}

module.exports = { uploadImage, uploadDir, deleteUploadedFile };
