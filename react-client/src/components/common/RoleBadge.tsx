import React from 'react';
import type { RoleType } from '../../types/database';
import { GraduationCap, BookOpen, ShieldCheck, KeyRound } from 'lucide-react';

interface Props {
  role: RoleType;
}

export const RoleBadge: React.FC<Props> = ({ role }) => {
  const configs: Record<RoleType, { label: string; bg: string; text: string; border: string; icon: React.ReactNode }> = {
    TEACHER: {
      label: 'Giáo viên',
      bg: 'rgba(37, 99, 235, 0.08)',
      text: '#1d4ed8',
      border: 'rgba(59, 130, 246, 0.3)',
      icon: <GraduationCap size={13} strokeWidth={2.5} />,
    },
    STUDENT: {
      label: 'Học sinh',
      bg: 'rgba(16, 185, 129, 0.08)',
      text: '#047857',
      border: 'rgba(16, 185, 129, 0.3)',
      icon: <BookOpen size={13} strokeWidth={2.5} />,
    },
    REVIEWER: {
      label: 'Thẩm định viên',
      bg: 'rgba(147, 51, 234, 0.08)',
      text: '#6b21a8',
      border: 'rgba(168, 85, 247, 0.3)',
      icon: <ShieldCheck size={13} strokeWidth={2.5} />,
    },
    ADMIN: {
      label: 'Quản trị viên',
      bg: 'rgba(239, 68, 68, 0.08)',
      text: '#b91c1c',
      border: 'rgba(239, 68, 68, 0.3)',
      icon: <KeyRound size={13} strokeWidth={2.5} />,
    },
  };

  const c = configs[role] || configs.TEACHER;

  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: '5px',
        padding: '3px 9px',
        borderRadius: '9999px',
        fontSize: '11px',
        fontWeight: 700,
        backgroundColor: c.bg,
        color: c.text,
        border: `1px solid ${c.border}`,
        letterSpacing: '0.02em',
        boxShadow: '0 1px 2px rgba(0,0,0,0.02)',
      }}
    >
      {c.icon}
      <span>{c.label}</span>
    </span>
  );
};

