import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Portfolio from './pages/Portfolio';
import { Analytics } from '@vercel/analytics/react';
import { SpeedInsights } from '@vercel/speed-insights/react';

function App() {
  return (
    <>
      <BrowserRouter>
        <Routes>
          {/* Public Portfolio */}
          <Route path="/" element={<Portfolio />} />

          {/* Fallback - redirect any unknown route (including /admin) to home */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
      <Analytics />
      <SpeedInsights />
    </>
  );
}

export default App;
