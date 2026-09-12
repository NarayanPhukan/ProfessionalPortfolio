const express = require('express');
const { db } = require('../lib/firebase');
const authMiddleware = require('../middleware/auth');
const router = express.Router();

// GET all skills (public)
router.get('/', async (req, res) => {
  try {
    const snapshot = await db.collection('skills').orderBy('display_order', 'asc').get();
    const skills = snapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data()
    }));
    res.json(skills);
  } catch (err) {
    try {
      const snapshot = await db.collection('skills').get();
      const skills = snapshot.docs
        .map(doc => ({ id: doc.id, ...doc.data() }))
        .sort((a, b) => (a.display_order || 0) - (b.display_order || 0));
      res.json(skills);
    } catch (fallbackErr) {
      res.status(500).json({ error: fallbackErr.message });
    }
  }
});

// POST create skill (admin)
router.post('/', authMiddleware, async (req, res) => {
  try {
    const { name, category, proficiency, icon_name, display_order } = req.body;
    const newSkill = {
      name: name || '',
      category: category || 'Other',
      proficiency: Number(proficiency) || 50,
      icon_name: icon_name || '',
      display_order: Number(display_order) || 0,
      created_at: new Date().toISOString()
    };

    const docRef = await db.collection('skills').add(newSkill);
    res.status(201).json({ id: docRef.id, ...newSkill });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// PUT update skill (admin)
router.put('/:id', authMiddleware, async (req, res) => {
  try {
    const updates = { ...req.body, updated_at: new Date().toISOString() };
    delete updates.id;
    if (updates.proficiency !== undefined) {
      updates.proficiency = Number(updates.proficiency) || 0;
    }
    if (updates.display_order !== undefined) {
      updates.display_order = Number(updates.display_order) || 0;
    }

    const docRef = db.collection('skills').doc(req.params.id);
    await docRef.set(updates, { merge: true });
    const updatedDoc = await docRef.get();

    res.json({ id: updatedDoc.id, ...updatedDoc.data() });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// DELETE skill (admin)
router.delete('/:id', authMiddleware, async (req, res) => {
  try {
    await db.collection('skills').doc(req.params.id).delete();
    res.json({ message: 'Skill deleted' });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
