const express = require('express');
const multer = require('multer');
const supabase = require('../lib/supabase');
const authMiddleware = require('../middleware/auth');
const router = express.Router();

const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 5 * 1024 * 1024 }, // 5MB limit
  fileFilter: (req, file, cb) => {
    const allowed = ['image/jpeg', 'image/png', 'image/webp', 'image/gif', 'application/pdf'];
    if (allowed.includes(file.mimetype)) {
      cb(null, true);
    } else {
      cb(new Error('Invalid file type'), false);
    }
  }
});

// POST upload file (admin)
router.post('/', authMiddleware, upload.single('file'), async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ error: 'No file provided' });
    }

    const folder = req.body.folder || 'general';
    const timestamp = Date.now();
    const ext = req.file.originalname.split('.').pop();
    const fileName = `${folder}/${timestamp}.${ext}`;

    const { data, error } = await supabase.storage
      .from('portfolio-assets')
      .upload(fileName, req.file.buffer, {
        contentType: req.file.mimetype,
        upsert: true
      });

    if (error) throw error;

    const { data: urlData } = supabase.storage
      .from('portfolio-assets')
      .getPublicUrl(fileName);

    res.json({ url: urlData.publicUrl, path: fileName });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
