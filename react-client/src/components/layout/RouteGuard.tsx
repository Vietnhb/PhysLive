import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import type { RoleType } from '../../types/database';

interface Props {
  allowedRoles?: RoleType[];
  children: React.ReactNode;
}

export const RouteGuard: React.FC<Props> = ({ allowedRoles, children }) => {
  const { currentUser } = useAuth();
  const location = useLocation();

  if (!currentUser) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (allowedRoles && allowedRoles.length > 0 && !allowedRoles.includes(currentUser.role)) {
    return (
      <div style={{ padding: '48px 24px', textAlign: 'center' }}>
        <div style={{ fontSize: '56px', marginBottom: '16px' }}>🔒</div>
        <h2 style={{ fontSize: '24px', color: '#1e293b', marginBottom: '8px' }}>
          403 - Giới hạn quyền truy cập
        </h2>
        <p style={{ color: '#64748b', marginBottom: '24px', maxWidth: '500px', margin: '0 auto 24px' }}>
          Tính năng này chỉ dành cho vai trò <strong>{allowedRoles.join(', ')}</strong>. Vai trò hiện tại của bạn là <strong>{currentUser.role}</strong>. Bạn có thể sử dụng bộ chuyển vai trò nhanh ở thanh điều hướng để thử nghiệm!
        </p>
      </div>
    );
  }

  return <>{children}</>;
};
