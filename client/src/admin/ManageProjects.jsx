import { useState, useEffect } from 'react';
import { projectsAPI, uploadAPI } from '../api';
import { FiPlus, FiEdit2, FiTrash2, FiSave, FiX, FiUpload } from 'react-icons/fi';

export default function ManageProjects() {
  const [projects, setProjects] = useState([]);
  const [showModal, setShowModal] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState({
    title: '', description: '', long_description: '', image_url: '',
    tech_stack: '', live_url: '', github_url: '', featured: false, display_order: 0
  });
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState(null);

  useEffect(() => { loadProjects(); }, []);

  const loadProjects = async () => {
    try {
      const { data } = await projectsAPI.getAll();
      setProjects(data);
    } catch (err) {
      console.error('Error loading projects:', err);
    }
  };

  const openNew = () => {
    setEditing(null);
    setForm({ title: '', description: '', long_description: '', image_url: '', tech_stack: '', live_url: '', github_url: '', featured: false, display_order: 0 });
    setShowModal(true);
  };

  const openEdit = (project) => {
    setEditing(project.id);
    setForm({
      ...project,
      tech_stack: (project.tech_stack || []).join(', ')
    });
    setShowModal(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    setSaving(true);
    setMessage(null);
    try {
      const payload = {
        ...form,
        tech_stack: form.tech_stack ? form.tech_stack.split(',').map(s => s.trim()).filter(Boolean) : [],
        display_order: parseInt(form.display_order) || 0
      };
      delete payload.id;
      delete payload.created_at;

      if (editing) {
        await projectsAPI.update(editing, payload);
      } else {
        await projectsAPI.create(payload);
      }
      setShowModal(false);
      loadProjects();
      setMessage({ type: 'success', text: `Project ${editing ? 'updated' : 'created'} successfully!` });
    } catch (err) {
      setMessage({ type: 'error', text: 'Failed to save project.' });
    }
    setSaving(false);
  };

  const handleDelete = async (id) => {
    if (!confirm('Are you sure you want to delete this project?')) return;
    try {
      await projectsAPI.delete(id);
      loadProjects();
      setMessage({ type: 'success', text: 'Project deleted.' });
    } catch (err) {
      setMessage({ type: 'error', text: 'Failed to delete project.' });
    }
  };

  const handleImageUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;
    try {
      const { data } = await uploadAPI.upload(file, 'projects');
      setForm({ ...form, image_url: data.url });
    } catch (err) {
      console.error('Upload failed:', err);
    }
  };

  return (
    <>
      <div className="admin-header">
        <h1>Manage Projects</h1>
        <button className="btn btn-primary" onClick={openNew}><FiPlus /> Add Project</button>
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
              <th>Title</th>
              <th>Tech Stack</th>
              <th>Featured</th>
              <th>Order</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {projects.length === 0 ? (
              <tr><td colSpan="5" style={{ textAlign: 'center', padding: 40, color: 'var(--neutral-500)' }}>No projects yet. Click "Add Project" to create one.</td></tr>
            ) : (
              projects.map((project) => (
                <tr key={project.id}>
                  <td><strong>{project.title}</strong></td>
                  <td>{(project.tech_stack || []).join(', ')}</td>
                  <td>{project.featured ? '⭐' : '—'}</td>
                  <td>{project.display_order}</td>
                  <td>
                    <div className="actions">
                      <button className="btn-icon edit" onClick={() => openEdit(project)}><FiEdit2 /></button>
                      <button className="btn-icon delete" onClick={() => handleDelete(project.id)}><FiTrash2 /></button>
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
            <h2>{editing ? 'Edit Project' : 'New Project'}</h2>
            <form onSubmit={handleSave}>
              <div className="form-group">
                <label>Title *</label>
                <input type="text" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} required />
              </div>
              <div className="form-group">
                <label>Short Description *</label>
                <textarea value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} required style={{ minHeight: 80 }} />
              </div>
              <div className="form-group">
                <label>Detailed Description</label>
                <textarea value={form.long_description} onChange={(e) => setForm({ ...form, long_description: e.target.value })} style={{ minHeight: 100 }} />
              </div>
              <div className="form-group">
                <label>Project Image</label>
                {form.image_url && <img src={form.image_url} alt="Preview" style={{ maxHeight: 150, borderRadius: 8, marginBottom: 12 }} />}
                <div className="image-upload">
                  <input type="file" accept="image/*" onChange={handleImageUpload} />
                  <p><FiUpload /> Click to upload image</p>
                </div>
              </div>
              <div className="form-group">
                <label>Tech Stack (comma-separated)</label>
                <input type="text" value={form.tech_stack} onChange={(e) => setForm({ ...form, tech_stack: e.target.value })} placeholder="React, Node.js, MongoDB" />
              </div>
              <div className="form-row">
                <div className="form-group">
                  <label>Live URL</label>
                  <input type="url" value={form.live_url} onChange={(e) => setForm({ ...form, live_url: e.target.value })} />
                </div>
                <div className="form-group">
                  <label>GitHub URL</label>
                  <input type="url" value={form.github_url} onChange={(e) => setForm({ ...form, github_url: e.target.value })} />
                </div>
              </div>
              <div className="form-row">
                <div className="form-group">
                  <label>Display Order</label>
                  <input type="number" value={form.display_order} onChange={(e) => setForm({ ...form, display_order: e.target.value })} />
                </div>
                <div className="form-group">
                  <label style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 28 }}>
                    <input type="checkbox" checked={form.featured} onChange={(e) => setForm({ ...form, featured: e.target.checked })} style={{ width: 18, height: 18 }} />
                    Featured Project
                  </label>
                </div>
              </div>
              <div className="modal-actions">
                <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}><FiX /> Cancel</button>
                <button type="submit" className="btn btn-primary" disabled={saving}>
                  {saving ? <><span className="spinner"></span> Saving...</> : <><FiSave /> {editing ? 'Update' : 'Create'}</>}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </>
  );
}
