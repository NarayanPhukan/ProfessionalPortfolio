import axios from 'axios';

const FIRESTORE_BASE = 'https://firestore.googleapis.com/v1/projects/narayan-portfolio-app/databases/(default)/documents';
const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:5000/api';

const api = axios.create({
  baseURL: API_URL,
  headers: { 'Content-Type': 'application/json' }
});

// Helper functions to parse Firestore REST API format to standard JSON
function parseFirestoreValue(val) {
  if (!val) return null;
  if ('stringValue' in val) return val.stringValue;
  if ('booleanValue' in val) return val.booleanValue;
  if ('integerValue' in val) return parseInt(val.integerValue, 10);
  if ('doubleValue' in val) return val.doubleValue;
  if ('timestampValue' in val) return val.timestampValue;
  if ('arrayValue' in val) return (val.arrayValue.values || []).map(parseFirestoreValue);
  if ('mapValue' in val) return parseFirestoreFields(val.mapValue.fields || {});
  return null;
}

function parseFirestoreFields(fields) {
  const result = {};
  for (const [key, val] of Object.entries(fields || {})) {
    result[key] = parseFirestoreValue(val);
  }
  return result;
}

function toFirestoreFields(data) {
  const fields = {};
  for (const [key, val] of Object.entries(data)) {
    if (val === undefined || val === null) continue;
    if (typeof val === 'string') fields[key] = { stringValue: val };
    else if (typeof val === 'boolean') fields[key] = { booleanValue: val };
    else if (typeof val === 'number') fields[key] = Number.isInteger(val) ? { integerValue: val.toString() } : { doubleValue: val };
    else if (Array.isArray(val)) fields[key] = { arrayValue: { values: val.map(v => ({ stringValue: String(v) })) } };
  }
  return { fields };
}

// Add auth token to requests
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('admin_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// API helpers with direct real-time Firestore access
export const profileAPI = {
  get: async () => {
    try {
      const res = await fetch(`${FIRESTORE_BASE}/profile/main`);
      if (res.ok) {
        const json = await res.json();
        return { data: { id: json.name.split('/').pop(), ...parseFirestoreFields(json.fields) } };
      }
    } catch (e) {
      console.warn('Direct Firestore profile fetch failed, using fallback:', e);
    }
    return api.get('/profile');
  },
  update: (data) => api.put('/profile', data),
};

export const projectsAPI = {
  getAll: async () => {
    try {
      const res = await fetch(`${FIRESTORE_BASE}/projects`);
      if (res.ok) {
        const json = await res.json();
        const list = (json.documents || []).map(d => ({
          id: d.name.split('/').pop(),
          ...parseFirestoreFields(d.fields)
        })).sort((a, b) => (a.display_order || 0) - (b.display_order || 0));
        return { data: list };
      }
    } catch (e) {
      console.warn('Direct Firestore projects fetch failed, using fallback:', e);
    }
    return api.get('/projects');
  },
  getOne: (id) => api.get(`/projects/${id}`),
  create: (data) => api.post('/projects', data),
  update: (id, data) => api.put(`/projects/${id}`, data),
  delete: (id) => api.delete(`/projects/${id}`),
};

export const skillsAPI = {
  getAll: async () => {
    try {
      const res = await fetch(`${FIRESTORE_BASE}/skills`);
      if (res.ok) {
        const json = await res.json();
        const list = (json.documents || []).map(d => ({
          id: d.name.split('/').pop(),
          ...parseFirestoreFields(d.fields)
        })).sort((a, b) => (a.display_order || 0) - (b.display_order || 0));
        return { data: list };
      }
    } catch (e) {
      console.warn('Direct Firestore skills fetch failed, using fallback:', e);
    }
    return api.get('/skills');
  },
  create: (data) => api.post('/skills', data),
  update: (id, data) => api.put(`/skills/${id}`, data),
  delete: (id) => api.delete(`/skills/${id}`),
};

export const contactsAPI = {
  send: async (data) => {
    try {
      const payload = toFirestoreFields({
        name: data.name,
        email: data.email,
        subject: data.subject,
        message: data.message,
        is_read: false,
        created_at: new Date().toISOString()
      });
      const res = await fetch(`${FIRESTORE_BASE}/contacts`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      if (res.ok) {
        const json = await res.json();
        return { data: { id: json.name.split('/').pop() } };
      }
    } catch (e) {
      console.warn('Direct Firestore contacts post failed, using fallback:', e);
    }
    return api.post('/contacts', data);
  },
  getAll: () => api.get('/contacts'),
  markRead: (id) => api.put(`/contacts/${id}/read`),
  delete: (id) => api.delete(`/contacts/${id}`),
};

export const authAPI = {
  login: (data) => api.post('/auth/login', data),
  verify: () => api.get('/auth/verify'),
};

export const uploadAPI = {
  upload: (file, folder = 'general') => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('folder', folder);
    return api.post('/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
  }
};

export default api;
