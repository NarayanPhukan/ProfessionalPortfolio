const express = require('express');
const { db } = require('../lib/firebase');
const authMiddleware = require('../middleware/auth');
const router = express.Router();

// GET all projects (public)
router.get('/', async (req, res) => {
  try {
    const snapshot = await db.collection('projects').orderBy('display_order', 'asc').get();
    let projects = snapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data()
    }));
    if (req.query.include_hidden !== 'true') {
      projects = projects.filter(p => !p.hidden && p.visible !== false && !p.is_hidden);
    }
    res.json(projects);
  } catch (err) {
    // Fallback without ordering if index is building
    try {
      const snapshot = await db.collection('projects').get();
      let projects = snapshot.docs
        .map(doc => ({ id: doc.id, ...doc.data() }))
        .sort((a, b) => (a.display_order || 0) - (b.display_order || 0));
      if (req.query.include_hidden !== 'true') {
        projects = projects.filter(p => !p.hidden && p.visible !== false && !p.is_hidden);
      }
      res.json(projects);
    } catch (fallbackErr) {
      res.status(500).json({ error: fallbackErr.message });
    }
  }
});

// GET single project (public)
router.get('/:id', async (req, res) => {
  try {
    const doc = await db.collection('projects').doc(req.params.id).get();
    if (!doc.exists) {
      return res.status(404).json({ error: 'Project not found' });
    }
    const data = doc.data();
    if (req.query.include_hidden !== 'true' && (data.hidden || data.visible === false || data.is_hidden)) {
      return res.status(404).json({ error: 'Project not found' });
    }
    res.json({ id: doc.id, ...data });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// POST create project (admin)
router.post('/', authMiddleware, async (req, res) => {
  try {
    const { title, description, long_description, image_url, tech_stack, live_url, github_url, featured, hidden, display_order } = req.body;
    const newProject = {
      title: title || '',
      description: description || '',
      long_description: long_description || '',
      image_url: image_url || '',
      tech_stack: Array.isArray(tech_stack) ? tech_stack : [],
      live_url: live_url || '',
      github_url: github_url || '',
      featured: Boolean(featured),
      hidden: Boolean(hidden),
      display_order: Number(display_order) || 0,
      created_at: new Date().toISOString()
    };

    const docRef = await db.collection('projects').add(newProject);
    res.status(201).json({ id: docRef.id, ...newProject });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// PUT update project (admin)
router.put('/:id', authMiddleware, async (req, res) => {
  try {
    const updates = { ...req.body, updated_at: new Date().toISOString() };
    delete updates.id;
    if (updates.display_order !== undefined) {
      updates.display_order = Number(updates.display_order) || 0;
    }

    const docRef = db.collection('projects').doc(req.params.id);
    await docRef.set(updates, { merge: true });
    const updatedDoc = await docRef.get();

    res.json({ id: updatedDoc.id, ...updatedDoc.data() });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// DELETE project (admin)
router.delete('/:id', authMiddleware, async (req, res) => {
  try {
    await db.collection('projects').doc(req.params.id).delete();
    res.json({ message: 'Project deleted' });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
