import { useState, useEffect } from 'react';
import { profileAPI, uploadAPI } from '../api';
import { FiSave, FiUpload } from 'react-icons/fi';

export default function ManageProfile() {
  const [profile, setProfile] = useState({
    name: '', title: '', bio: '', about_text: '', email: '', phone: '',
    avatar_url: '', resume_url: '', github_url: '', linkedin_url: '', instagram_url: '', location: '', available_for_hire: true
  });
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState(null);
  const [uploading, setUploading] = useState(false);

  useEffect(() => { loadProfile(); }, []);

  const loadProfile = async () => {
    try {
      const { data } = await profileAPI.get();
      if (data) {
        const sanitizedData = Object.fromEntries(
          Object.entries(data).map(([key, val]) => [key, val === null ? '' : val])
        );
        setProfile(sanitizedData);
      }
    } catch (err) {
      console.error('Error loading profile:', err);
    }
  };

  const handleSave = async (e) => {
    e.preventDefault();
    setSaving(true);
    setMessage(null);
    try {
      const { id, created_at, updated_at, ...updates } = profile;
      await profileAPI.update(updates);
      setMessage({ type: 'success', text: 'Profile updated successfully!' });
    } catch (err) {
      setMessage({ type: 'error', text: 'Failed to update profile.' });
    }
    setSaving(false);
  };

  const handleImageUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;
    setUploading(true);
    try {
      const { data } = await uploadAPI.upload(file, 'avatars');
      setProfile({ ...profile, avatar_url: data.url });
      setMessage({ type: 'success', text: 'Avatar uploaded! Click Save to apply.' });
    } catch (err) {
      setMessage({ type: 'error', text: 'Failed to upload image.' });
    }
    setUploading(false);
  };

  return (
    <>
      <div className="admin-header">
        <h1>Manage Profile</h1>
      </div>

      {message && (
        <div className={message.type === 'success' ? 'form-success' : 'form-error'} style={{ marginBottom: 24 }}>
          {message.text}
        </div>
      )}

      <form className="admin-form" onSubmit={handleSave}>
        <h2>Profile Picture</h2>
        <div style={{ display: 'flex', alignItems: 'center', gap: 24, marginBottom: 32 }}>
          <img
            src={profile.avatar_url || '/mannequin.png'}
            alt="Avatar"
            style={{ width: 100, height: 100, borderRadius: '50%', objectFit: 'cover', border: '3px solid var(--primary)' }}
          />
          <div className="image-upload" style={{ flex: 1 }}>
            <input type="file" accept="image/*" onChange={handleImageUpload} />
            <p><FiUpload size={24} style={{ marginBottom: 8 }} /><br />{uploading ? 'Uploading...' : 'Click to upload new avatar'}</p>
          </div>
        </div>

        <h2>Personal Information</h2>
        <div className="form-row">
          <div className="form-group">
            <label>Full Name</label>
            <input type="text" value={profile.name} onChange={(e) => setProfile({ ...profile, name: e.target.value })} required />
          </div>
          <div className="form-group">
            <label>Title / Designation</label>
            <input type="text" value={profile.title} onChange={(e) => setProfile({ ...profile, title: e.target.value })} />
          </div>
        </div>

        <div className="form-group">
          <label>Short Bio (Hero Section)</label>
          <textarea value={profile.bio} onChange={(e) => setProfile({ ...profile, bio: e.target.value })} style={{ minHeight: 80 }} />
        </div>

        <div className="form-group">
          <label>About Text (About Section)</label>
          <textarea value={profile.about_text} onChange={(e) => setProfile({ ...profile, about_text: e.target.value })} style={{ minHeight: 120 }} />
        </div>

        <h2>Contact Details</h2>
        <div className="form-row">
          <div className="form-group">
            <label>Email</label>
            <input type="email" value={profile.email} onChange={(e) => setProfile({ ...profile, email: e.target.value })} />
          </div>
          <div className="form-group">
            <label>Phone</label>
            <input type="text" value={profile.phone} onChange={(e) => setProfile({ ...profile, phone: e.target.value })} />
          </div>
        </div>
        <div className="form-group">
          <label>Location</label>
          <input type="text" value={profile.location} onChange={(e) => setProfile({ ...profile, location: e.target.value })} />
        </div>

        <h2>Links</h2>
        <div className="form-row">
          <div className="form-group">
            <label>GitHub URL</label>
            <input type="url" value={profile.github_url} onChange={(e) => setProfile({ ...profile, github_url: e.target.value })} placeholder="https://github.com/..." />
          </div>
          <div className="form-group">
            <label>LinkedIn URL</label>
            <input type="url" value={profile.linkedin_url} onChange={(e) => setProfile({ ...profile, linkedin_url: e.target.value })} placeholder="https://linkedin.com/in/..." />
          </div>
        </div>
        <div className="form-row">
          <div className="form-group">
            <label>Resume URL</label>
            <input type="url" value={profile.resume_url} onChange={(e) => setProfile({ ...profile, resume_url: e.target.value })} placeholder="Link to your resume/CV" />
          </div>
          <div className="form-group">
            <label>Instagram URL</label>
            <input type="url" value={profile.instagram_url} onChange={(e) => setProfile({ ...profile, instagram_url: e.target.value })} placeholder="https://instagram.com/..." />
          </div>
        </div>

        <div className="form-group" style={{ marginTop: 16 }}>
          <label style={{ display: 'flex', alignItems: 'center', gap: 12, cursor: 'pointer' }}>
            <input
              type="checkbox"
              checked={profile.available_for_hire}
              onChange={(e) => setProfile({ ...profile, available_for_hire: e.target.checked })}
              style={{ width: 20, height: 20 }}
            />
            Available for Hire
          </label>
        </div>

        <div style={{ marginTop: 24 }}>
          <button type="submit" className="btn btn-primary" disabled={saving}>
            {saving ? <><span className="spinner"></span> Saving...</> : <><FiSave /> Save Changes</>}
          </button>
        </div>
      </form>
    </>
  );
}
