import React from 'react';
import type { RoleType } from '../../types/database';

interface Props {
  role: RoleType;
}

export const RoleBadge: React.FC<Props> = ({ role }) => {
  const configs: Record<RoleType, { label: string; bg: string; text: string; border: string }> = {
    TEACHER: { label: 'Giáo viên', bg: '#eff6ff', text: '#1d4ed8', border: '#bfdbfe' },
    STUDENT: { label: 'Học sinh', bg: '#f0fdf4', text: '#15803d', border: '#bbf7d0' },
    REVIEWER: { label: 'Người thẩm định', bg: '#faf5ff', text: '#7e22ce', border: '#e9d5ff' },
    ADMIN: { label: 'Quản trị viên', bg: '#fff7ed', text: '#c2410c', border: '#fed7aa' },
  };

  const c = configs[role] || configs.TEACHER;

  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        padding: '3px 10px',
        borderRadius: '9999px',
        fontSize: '12px',
        fontWeight: 600,
        backgroundColor: c.bg,
        color: c.text,
        border: `1px solid ${c.border}`,
        letterSpacing: '0.02em',
      }}
    >
      {c.label}
    </span>
  );
};
