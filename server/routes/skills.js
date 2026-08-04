const express = require('express');
const supabase = require('../lib/supabase');
const authMiddleware = require('../middleware/auth');
const router = express.Router();

// GET all skills (public)
router.get('/', async (req, res) => {
  try {
    const { data, error } = await supabase
      .from('portfolio_skills')
      .select('*')
      .order('display_order', { ascending: true });

    if (error) throw error;
    res.json(data);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// POST create skill (admin)
router.post('/', authMiddleware, async (req, res) => {
  try {
    const { name, category, proficiency, icon_name, display_order } = req.body;

    const { data, error } = await supabase
      .from('portfolio_skills')
      .insert([{ name, category: category || 'Other', proficiency: proficiency || 50, icon_name, display_order: display_order || 0 }])
      .select()
      .single();

    if (error) throw error;
    res.status(201).json(data);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// PUT update skill (admin)
router.put('/:id', authMiddleware, async (req, res) => {
  try {
    const { data, error } = await supabase
      .from('portfolio_skills')
      .update(req.body)
      .eq('id', req.params.id)
      .select()
      .single();

    if (error) throw error;
    res.json(data);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// DELETE skill (admin)
router.delete('/:id', authMiddleware, async (req, res) => {
  try {
    const { error } = await supabase
      .from('portfolio_skills')
      .delete()
      .eq('id', req.params.id);

    if (error) throw error;
    res.json({ message: 'Skill deleted' });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
