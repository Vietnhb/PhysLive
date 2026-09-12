import React from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import { Header } from './Header';

export const AppLayout: React.FC = () => {
  const location = useLocation();
  const isPlayerPage = location.pathname === '/player';

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', backgroundColor: '#f8fafc' }}>
      <Header />
      <main style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
        {isPlayerPage ? (
          <Outlet />
        ) : (
          <div style={{ maxWidth: '1280px', width: '100%', margin: '0 auto', padding: '24px' }}>
            <Outlet />
          </div>
        )}
      </main>
    </div>
  );
};
