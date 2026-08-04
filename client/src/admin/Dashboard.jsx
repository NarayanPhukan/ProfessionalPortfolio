import { useState, useEffect } from 'react';
import { FiFolder, FiStar, FiMail, FiUser } from 'react-icons/fi';
import { projectsAPI, skillsAPI, contactsAPI, profileAPI } from '../api';

export default function Dashboard() {
  const [stats, setStats] = useState({ projects: 0, skills: 0, messages: 0, unread: 0 });
  const [recentMessages, setRecentMessages] = useState([]);

  useEffect(() => {
    loadStats();
  }, []);

  const loadStats = async () => {
    try {
      const [projects, skills, contacts] = await Promise.all([
        projectsAPI.getAll(),
        skillsAPI.getAll(),
        contactsAPI.getAll()
      ]);
      const unread = contacts.data.filter(c => !c.is_read).length;
      setStats({
        projects: projects.data.length,
        skills: skills.data.length,
        messages: contacts.data.length,
        unread
      });
      setRecentMessages(contacts.data.slice(0, 5));
    } catch (err) {
      console.error('Error loading stats:', err);
    }
  };

  return (
    <>
      <div className="admin-header">
        <h1>Dashboard</h1>
      </div>

      <div className="admin-stats">
        <div className="admin-stat-card">
          <div className="icon primary"><FiFolder /></div>
          <div className="info">
            <h3>{stats.projects}</h3>
            <p>Total Projects</p>
          </div>
        </div>
        <div className="admin-stat-card">
          <div className="icon secondary"><FiStar /></div>
          <div className="info">
            <h3>{stats.skills}</h3>
            <p>Skills Listed</p>
          </div>
        </div>
        <div className="admin-stat-card">
          <div className="icon success"><FiMail /></div>
          <div className="info">
            <h3>{stats.messages}</h3>
            <p>Total Messages</p>
          </div>
        </div>
        <div className="admin-stat-card">
          <div className="icon warning"><FiMail /></div>
          <div className="info">
            <h3>{stats.unread}</h3>
            <p>Unread Messages</p>
          </div>
        </div>
      </div>

      {recentMessages.length > 0 && (
        <div className="admin-table">
          <div style={{ padding: '20px 20px 12px' }}>
            <h2 style={{ fontSize: '1.2rem' }}>Recent Messages</h2>
          </div>
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Email</th>
                <th>Subject</th>
                <th>Status</th>
                <th>Date</th>
              </tr>
            </thead>
            <tbody>
              {recentMessages.map((msg) => (
                <tr key={msg.id}>
                  <td><strong>{msg.name}</strong></td>
                  <td>{msg.email}</td>
                  <td>{msg.subject || '—'}</td>
                  <td>
                    <span className={`badge ${msg.is_read ? 'badge-read' : 'badge-unread'}`}>
                      {msg.is_read ? 'Read' : 'New'}
                    </span>
                  </td>
                  <td>{new Date(msg.created_at).toLocaleDateString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}
