import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import type { RoleType } from '../../types/database';

interface Props {
  allowedRoles?: RoleType[];
  children: React.ReactNode;
}

export const RouteGuard: React.FC<Props> = ({ allowedRoles, children }) => {
  const { currentUser, isAuthenticated } = useAuth();
  const location = useLocation();

  // Nếu chưa đăng nhập -> Chuyển hướng về trang Login
  if (!isAuthenticated || !currentUser) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  // Nếu đã đăng nhập nhưng không có quyền phù hợp -> Hiển thị màn hình 403 Forbidden
  if (allowedRoles && allowedRoles.length > 0 && !allowedRoles.includes(currentUser.role)) {
    return (
      <div style={{ padding: '48px 24px', textAlign: 'center', maxWidth: '600px', margin: '40px auto', backgroundColor: '#ffffff', borderRadius: '16px', border: '1px solid #e2e8f0', boxShadow: '0 4px 12px rgba(0,0,0,0.05)' }}>
        <div style={{ fontSize: '56px', marginBottom: '16px' }}>🔒</div>
        <h2 style={{ fontSize: '22px', color: '#0f172a', marginBottom: '8px', fontWeight: 800 }}>
          403 — Không có quyền truy cập
        </h2>
        <p style={{ color: '#64748b', marginBottom: '20px', fontSize: '14px', lineHeight: 1.5 }}>
          Chức năng này chỉ dành cho vai trò: <strong style={{ color: '#2563eb' }}>{allowedRoles.join(', ')}</strong>.<br/>
          Vai trò hiện tại của bạn là <strong style={{ color: '#dc2626' }}>{currentUser.role}</strong>.
        </p>
      </div>
    );
  }

  return <>{children}</>;
};
