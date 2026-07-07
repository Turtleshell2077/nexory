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

const upload = multer({
    storage,
    limits: { fileSize: 8 * 1024 * 1024 }, // 8 МБ
    fileFilter: (req, file, cb) => {
        if (file.mimetype.startsWith('image/')) cb(null, true);
        else cb(new Error('Только изображения'));
    },
}).single('file');

// POST /upload — multipart/form-data, поле "file". Возвращает абсолютный URL.
const uploadImage = (req, res) => {
    upload(req, res, async (err) => {
        if (err) {
            console.error('[upload]', err.message);
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

module.exports = { uploadImage, uploadDir };
