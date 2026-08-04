const express = require('express');
const supabase = require('../lib/supabase');
const authMiddleware = require('../middleware/auth');
const router = express.Router();

// GET profile (public)
router.get('/', async (req, res) => {
  try {
    const { data, error } = await supabase
      .from('portfolio_profile')
      .select('*')
      .limit(1)
      .single();

    if (error) throw error;
    res.json(data);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// PUT update profile (admin)
router.put('/', authMiddleware, async (req, res) => {
  try {
    const updates = { ...req.body, updated_at: new Date().toISOString() };

    const { data: existing } = await supabase
      .from('portfolio_profile')
      .select('id')
      .limit(1)
      .single();

    if (!existing) {
      return res.status(404).json({ error: 'Profile not found' });
    }

    const { data, error } = await supabase
      .from('portfolio_profile')
      .update(updates)
      .eq('id', existing.id)
      .select()
      .single();

    if (error) throw error;
    res.json(data);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
