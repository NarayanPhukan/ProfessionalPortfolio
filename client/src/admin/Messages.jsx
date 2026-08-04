import { useState, useEffect } from 'react';
import { contactsAPI } from '../api';
import { FiMail, FiCheck, FiTrash2, FiEye } from 'react-icons/fi';

export default function Messages() {
  const [messages, setMessages] = useState([]);
  const [selected, setSelected] = useState(null);

  useEffect(() => { loadMessages(); }, []);

  const loadMessages = async () => {
    try {
      const { data } = await contactsAPI.getAll();
      setMessages(data);
    } catch (err) {
      console.error('Error loading messages:', err);
    }
  };

  const handleMarkRead = async (id) => {
    try {
      await contactsAPI.markRead(id);
      loadMessages();
    } catch (err) {
      console.error('Error marking as read:', err);
    }
  };

  const handleDelete = async (id) => {
    if (!confirm('Delete this message?')) return;
    try {
      await contactsAPI.delete(id);
      if (selected?.id === id) setSelected(null);
      loadMessages();
    } catch (err) {
      console.error('Error deleting message:', err);
    }
  };

  const unreadCount = messages.filter(m => !m.is_read).length;

  return (
    <>
      <div className="admin-header">
        <h1>Messages {unreadCount > 0 && <span className="badge badge-unread" style={{ fontSize: '0.8rem', marginLeft: 8 }}>{unreadCount} new</span>}</h1>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: selected ? '1fr 1fr' : '1fr', gap: 24 }}>
        <div className="admin-table">
          <table>
            <thead>
              <tr>
                <th>From</th>
                <th>Subject</th>
                <th>Date</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {messages.length === 0 ? (
                <tr><td colSpan="5" style={{ textAlign: 'center', padding: 40, color: 'var(--neutral-500)' }}>No messages yet.</td></tr>
              ) : (
                messages.map((msg) => (
                  <tr key={msg.id} style={{ cursor: 'pointer', background: selected?.id === msg.id ? 'var(--neutral-100)' : undefined }} onClick={() => { setSelected(msg); if (!msg.is_read) handleMarkRead(msg.id); }}>
                    <td><strong style={{ fontWeight: msg.is_read ? 500 : 700 }}>{msg.name}</strong><br /><small style={{ color: 'var(--neutral-500)' }}>{msg.email}</small></td>
                    <td>{msg.subject || '(No subject)'}</td>
                    <td style={{ whiteSpace: 'nowrap' }}>{new Date(msg.created_at).toLocaleDateString()}</td>
                    <td><span className={`badge ${msg.is_read ? 'badge-read' : 'badge-unread'}`}>{msg.is_read ? 'Read' : 'New'}</span></td>
                    <td>
                      <div className="actions">
                        {!msg.is_read && <button className="btn-icon edit" onClick={(e) => { e.stopPropagation(); handleMarkRead(msg.id); }} title="Mark as read"><FiCheck /></button>}
                        <button className="btn-icon delete" onClick={(e) => { e.stopPropagation(); handleDelete(msg.id); }} title="Delete"><FiTrash2 /></button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {selected && (
          <div className="card" style={{ position: 'sticky', top: 32 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 20 }}>
              <div>
                <h3 style={{ marginBottom: 4 }}>{selected.subject || '(No subject)'}</h3>
                <p style={{ color: 'var(--neutral-500)', fontSize: '0.9rem' }}>
                  From: <strong>{selected.name}</strong> &lt;{selected.email}&gt;
                </p>
                <p style={{ color: 'var(--neutral-400)', fontSize: '0.8rem' }}>
                  {new Date(selected.created_at).toLocaleString()}
                </p>
              </div>
              <button className="btn-icon delete" onClick={() => setSelected(null)} title="Close">✕</button>
            </div>
            <div style={{ borderTop: '1px solid var(--neutral-200)', paddingTop: 20, lineHeight: 1.8, color: 'var(--neutral-700)' }}>
              {selected.message}
            </div>
            <div style={{ marginTop: 20, display: 'flex', gap: 12 }}>
              <a href={`mailto:${selected.email}?subject=Re: ${selected.subject}`} className="btn btn-primary btn-sm"><FiMail /> Reply</a>
              <button className="btn btn-secondary btn-sm" onClick={() => handleDelete(selected.id)}><FiTrash2 /> Delete</button>
            </div>
          </div>
        )}
      </div>
    </>
  );
}
