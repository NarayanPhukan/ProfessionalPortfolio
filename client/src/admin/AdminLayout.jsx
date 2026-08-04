import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { FiGrid, FiUser, FiFolder, FiStar, FiMail, FiLogOut, FiHome } from 'react-icons/fi';

export default function AdminLayout() {
  const { logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/admin/login');
  };

  return (
    <div className="admin-layout">
      <aside className="admin-sidebar">
        <div className="admin-sidebar-brand">
          <h2>Portfolio Admin</h2>
          <span>Manage your content</span>
        </div>
        <nav className="admin-nav">
          <NavLink to="/admin" end><FiGrid /> Dashboard</NavLink>
          <NavLink to="/admin/profile"><FiUser /> Profile</NavLink>
          <NavLink to="/admin/projects"><FiFolder /> Projects</NavLink>
          <NavLink to="/admin/skills"><FiStar /> Skills</NavLink>
          <NavLink to="/admin/messages"><FiMail /> Messages</NavLink>
          <a href="/" target="_blank" rel="noopener noreferrer"><FiHome /> View Site</a>
          <a href="#" onClick={(e) => { e.preventDefault(); handleLogout(); }}><FiLogOut /> Logout</a>
        </nav>
      </aside>
      <main className="admin-content">
        <Outlet />
      </main>
    </div>
  );
}
