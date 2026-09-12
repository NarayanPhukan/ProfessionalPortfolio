const express = require('express');
const multer = require('multer');
const { storage } = require('../lib/firebase');
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

    const bucket = storage.bucket();
    const blob = bucket.file(fileName);

    await blob.save(req.file.buffer, {
      metadata: {
        contentType: req.file.mimetype
      },
      resumable: false
    });

    try {
      await blob.makePublic();
    } catch (e) {
      // Ignore if uniform bucket-level access is on
    }

    const publicUrl = `https://firebasestorage.googleapis.com/v0/b/${bucket.name}/o/${encodeURIComponent(fileName)}?alt=media`;

    res.json({ url: publicUrl, path: fileName });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
