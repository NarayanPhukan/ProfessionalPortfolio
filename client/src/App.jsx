import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import Portfolio from './pages/Portfolio';
import AdminLogin from './admin/AdminLogin';
import AdminLayout from './admin/AdminLayout';
import Dashboard from './admin/Dashboard';
import ManageProfile from './admin/ManageProfile';
import ManageProjects from './admin/ManageProjects';
import ManageSkills from './admin/ManageSkills';
import Messages from './admin/Messages';
import { Analytics } from '@vercel/analytics/react';

function ProtectedRoute({ children }) {
  const { isAuthenticated, loading } = useAuth();

  if (loading) {
    return (
      <div className="page-loader">
        <div className="spinner"></div>
      </div>
    );
  }

  return isAuthenticated ? children : <Navigate to="/admin/login" />;
}

function AdminLoginRoute() {
  const { isAuthenticated, loading } = useAuth();

  if (loading) {
    return (
      <div className="page-loader">
        <div className="spinner"></div>
      </div>
    );
  }

  return isAuthenticated ? <Navigate to="/admin" /> : <AdminLogin />;
}

function App() {
  return (
    <>
      <BrowserRouter>
        <AuthProvider>
          <Routes>
            {/* Public Portfolio */}
            <Route path="/" element={<Portfolio />} />

            {/* Admin Routes */}
            <Route path="/admin/login" element={<AdminLoginRoute />} />
            <Route path="/admin" element={<ProtectedRoute><AdminLayout /></ProtectedRoute>}>
              <Route index element={<Dashboard />} />
              <Route path="profile" element={<ManageProfile />} />
              <Route path="projects" element={<ManageProjects />} />
              <Route path="skills" element={<ManageSkills />} />
              <Route path="messages" element={<Messages />} />
            </Route>

            {/* Fallback */}
            <Route path="*" element={<Navigate to="/" />} />
          </Routes>
        </AuthProvider>
      </BrowserRouter>
      <Analytics />
    </>
  );
}

export default App;
