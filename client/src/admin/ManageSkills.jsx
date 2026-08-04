import { useState, useEffect } from 'react';
import { skillsAPI } from '../api';
import { FiPlus, FiEdit2, FiTrash2, FiSave, FiX } from 'react-icons/fi';

const CATEGORIES = ['Frontend', 'Backend', 'Database', 'DevOps', 'Tools', 'Languages', 'Other'];

export default function ManageSkills() {
  const [skills, setSkills] = useState([]);
  const [showModal, setShowModal] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState({ name: '', category: 'Frontend', proficiency: 50, icon_name: '', display_order: 0 });
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState(null);

  useEffect(() => { loadSkills(); }, []);

  const loadSkills = async () => {
    try {
      const { data } = await skillsAPI.getAll();
      setSkills(data);
    } catch (err) {
      console.error('Error loading skills:', err);
    }
  };

  const openNew = () => {
    setEditing(null);
    setForm({ name: '', category: 'Frontend', proficiency: 50, icon_name: '', display_order: 0 });
    setShowModal(true);
  };

  const openEdit = (skill) => {
    setEditing(skill.id);
    setForm({ ...skill });
    setShowModal(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    setSaving(true);
    setMessage(null);
    try {
      const payload = { ...form, proficiency: parseInt(form.proficiency), display_order: parseInt(form.display_order) || 0 };
      delete payload.id;
      delete payload.created_at;

      if (editing) {
        await skillsAPI.update(editing, payload);
      } else {
        await skillsAPI.create(payload);
      }
      setShowModal(false);
      loadSkills();
      setMessage({ type: 'success', text: `Skill ${editing ? 'updated' : 'added'} successfully!` });
    } catch (err) {
      setMessage({ type: 'error', text: 'Failed to save skill.' });
    }
    setSaving(false);
  };

  const handleDelete = async (id) => {
    if (!confirm('Are you sure you want to delete this skill?')) return;
    try {
      await skillsAPI.delete(id);
      loadSkills();
      setMessage({ type: 'success', text: 'Skill deleted.' });
    } catch (err) {
      setMessage({ type: 'error', text: 'Failed to delete skill.' });
    }
  };

  return (
    <>
      <div className="admin-header">
        <h1>Manage Skills</h1>
        <button className="btn btn-primary" onClick={openNew}><FiPlus /> Add Skill</button>
      </div>

      {message && (
        <div className={message.type === 'success' ? 'form-success' : 'form-error'} style={{ marginBottom: 24 }}>
          {message.text}
        </div>
      )}

      <div className="admin-table">
        <table>
          <thead>
            <tr>
              <th>Skill</th>
              <th>Category</th>
              <th>Proficiency</th>
              <th>Order</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {skills.length === 0 ? (
              <tr><td colSpan="5" style={{ textAlign: 'center', padding: 40, color: 'var(--neutral-500)' }}>No skills yet. Click "Add Skill" to create one.</td></tr>
            ) : (
              skills.map((skill) => (
                <tr key={skill.id}>
                  <td><strong>{skill.name}</strong></td>
                  <td><span className="badge badge-unread">{skill.category}</span></td>
                  <td>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                      <div className="skill-bar" style={{ flex: 1, maxWidth: 120 }}>
                        <div className="skill-bar-fill" style={{ width: `${skill.proficiency}%` }}></div>
                      </div>
                      <span style={{ fontWeight: 600, fontSize: '0.85rem' }}>{skill.proficiency}%</span>
                    </div>
                  </td>
                  <td>{skill.display_order}</td>
                  <td>
                    <div className="actions">
                      <button className="btn-icon edit" onClick={() => openEdit(skill)}><FiEdit2 /></button>
                      <button className="btn-icon delete" onClick={() => handleDelete(skill.id)}><FiTrash2 /></button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {showModal && (
        <div className="modal-overlay" onClick={(e) => e.target === e.currentTarget && setShowModal(false)}>
          <div className="modal">
            <h2>{editing ? 'Edit Skill' : 'New Skill'}</h2>
            <form onSubmit={handleSave}>
              <div className="form-row">
                <div className="form-group">
                  <label>Skill Name *</label>
                  <input type="text" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required placeholder="e.g. React, Python" />
                </div>
                <div className="form-group">
                  <label>Category *</label>
                  <select
                    value={form.category}
                    onChange={(e) => setForm({ ...form, category: e.target.value })}
                    style={{ width: '100%', padding: '14px 18px', border: '2px solid var(--neutral-200)', borderRadius: 'var(--radius-sm)', fontSize: '1rem', background: 'var(--neutral)', fontFamily: 'var(--font-body)' }}
                  >
                    {CATEGORIES.map(cat => <option key={cat} value={cat}>{cat}</option>)}
                  </select>
                </div>
              </div>
              <div className="form-group">
                <label>Proficiency: {form.proficiency}%</label>
                <input
                  type="range"
                  min="0" max="100"
                  value={form.proficiency}
                  onChange={(e) => setForm({ ...form, proficiency: e.target.value })}
                  style={{ width: '100%', accentColor: 'var(--primary)' }}
                />
              </div>
              <div className="form-group">
                <label>Display Order</label>
                <input type="number" value={form.display_order} onChange={(e) => setForm({ ...form, display_order: e.target.value })} />
              </div>
              <div className="modal-actions">
                <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}><FiX /> Cancel</button>
                <button type="submit" className="btn btn-primary" disabled={saving}>
                  {saving ? <><span className="spinner"></span> Saving...</> : <><FiSave /> {editing ? 'Update' : 'Add'}</>}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </>
  );
}
