const express = require('express');
const { db } = require('../lib/firebase');
const authMiddleware = require('../middleware/auth');
const router = express.Router();

// GET profile (public)
router.get('/', async (req, res) => {
  try {
    const docRef = db.collection('profile').doc('main');
    const doc = await docRef.get();

    if (!doc.exists) {
      // Fallback: check first doc in profile collection
      const snapshot = await db.collection('profile').limit(1).get();
      if (snapshot.empty) {
        return res.status(404).json({ error: 'Profile not found' });
      }
      return res.json({ id: snapshot.docs[0].id, ...snapshot.docs[0].data() });
    }

    res.json({ id: doc.id, ...doc.data() });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// PUT update profile (admin)
router.put('/', authMiddleware, async (req, res) => {
  try {
    const updates = { ...req.body, updated_at: new Date().toISOString() };
    delete updates.id;

    const docRef = db.collection('profile').doc('main');
    await docRef.set(updates, { merge: true });

    const updatedDoc = await docRef.get();
    res.json({ id: updatedDoc.id, ...updatedDoc.data() });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
