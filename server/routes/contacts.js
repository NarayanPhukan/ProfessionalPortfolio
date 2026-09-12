const express = require('express');
const { db } = require('../lib/firebase');
const authMiddleware = require('../middleware/auth');
const router = express.Router();

// POST submit contact message (public)
router.post('/', async (req, res) => {
  try {
    const { name, email, subject, message } = req.body;

    if (!name || !email || !message) {
      return res.status(400).json({ error: 'Name, email, and message are required' });
    }

    const newMessage = {
      name,
      email,
      subject: subject || '',
      message,
      is_read: false,
      created_at: new Date().toISOString()
    };

    const docRef = await db.collection('contacts').add(newMessage);
    res.status(201).json({ id: docRef.id, message: 'Message sent successfully' });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// GET all messages (admin)
router.get('/', authMiddleware, async (req, res) => {
  try {
    const snapshot = await db.collection('contacts').orderBy('created_at', 'desc').get();
    const contacts = snapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data()
    }));
    res.json(contacts);
  } catch (err) {
    try {
      const snapshot = await db.collection('contacts').get();
      const contacts = snapshot.docs
        .map(doc => ({ id: doc.id, ...doc.data() }))
        .sort((a, b) => new Date(b.created_at) - new Date(a.created_at));
      res.json(contacts);
    } catch (fallbackErr) {
      res.status(500).json({ error: fallbackErr.message });
    }
  }
});

// PUT mark message as read (admin)
router.put('/:id/read', authMiddleware, async (req, res) => {
  try {
    const docRef = db.collection('contacts').doc(req.params.id);
    await docRef.update({ is_read: true });
    const updated = await docRef.get();
    res.json({ id: updated.id, ...updated.data() });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// DELETE message (admin)
router.delete('/:id', authMiddleware, async (req, res) => {
  try {
    await db.collection('contacts').doc(req.params.id).delete();
    res.json({ message: 'Contact deleted' });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
