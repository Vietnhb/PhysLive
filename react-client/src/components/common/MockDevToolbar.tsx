import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import type { RoleType } from '../../types/database';
import { IS_MOCK_ENABLED } from '../../config/api';
import {
  Zap,
  GraduationCap,
  BookOpen,
  ShieldCheck,
  KeyRound,
  ChevronDown,
  ChevronUp,
} from 'lucide-react';

export const MockDevToolbar: React.FC = () => {
  const { currentUser, switchRole } = useAuth();
  const [isExpanded, setIsExpanded] = useState<boolean>(true);

  if (!IS_MOCK_ENABLED) return null;

  const demoRoles: { role: RoleType; label: string; icon: React.ReactNode }[] = [
    { role: 'TEACHER', label: 'Giáo viên', icon: <GraduationCap size={13} strokeWidth={2.5} /> },
    { role: 'STUDENT', label: 'Học sinh', icon: <BookOpen size={13} strokeWidth={2.5} /> },
    { role: 'REVIEWER', label: 'Thẩm định', icon: <ShieldCheck size={13} strokeWidth={2.5} /> },
    { role: 'ADMIN', label: 'Quản trị', icon: <KeyRound size={13} strokeWidth={2.5} /> },
  ];

  return (
    <aside
      aria-label="Mock Data & Role Switcher Console"
      style={{
        position: 'fixed',
        bottom: '24px',
        right: '24px',
        zIndex: 9999,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'flex-end',
        gap: '8px',
        fontFamily: "'Google Sans', 'Inter', system-ui, sans-serif",
      }}
    >
      {isExpanded ? (
        <div
          style={{
            backgroundColor: 'var(--bg-card, #ffffff)',
            borderRadius: '16px',
            border: '1px solid var(--border-card, #cbd5e1)',
            boxShadow: '0 12px 32px rgba(15, 23, 42, 0.2), 0 2px 8px rgba(0,0,0,0.06)',
            padding: '12px 16px',
            display: 'flex',
            flexDirection: 'column',
            gap: '10px',
            minWidth: '290px',
            backdropFilter: 'blur(16px)',
            transition: 'all 0.2s ease',
          }}
        >
          {/* Header row with Mock Indicator & Collapse button */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <span
                style={{
                  fontSize: '11px',
                  padding: '3px 8px',
                  backgroundColor: 'rgba(217, 119, 6, 0.15)',
                  color: '#d97706',
                  borderRadius: '6px',
                  fontWeight: 800,
                  border: '1px solid rgba(217, 119, 6, 0.3)',
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '4px',
                }}
              >
                <Zap size={12} strokeWidth={2.5} />
                <span>MOCK ACTIVE</span>
              </span>
              <span style={{ fontSize: '11px', color: 'var(--text-muted, #64748b)', fontWeight: 600 }}>
                • Role Switcher
              </span>
            </div>

            <button
              onClick={() => setIsExpanded(false)}
              style={{
                background: 'none',
                border: 'none',
                color: 'var(--text-muted, #64748b)',
                cursor: 'pointer',
                padding: '3px',
                borderRadius: '6px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
              title="Thu nhỏ thanh điều khiển"
            >
              <ChevronDown size={16} />
            </button>
          </div>

          {/* Role selector buttons */}
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(2, 1fr)',
              gap: '6px',
              backgroundColor: 'var(--surface-muted, #f1f5f9)',
              padding: '6px',
              borderRadius: '10px',
            }}
          >
            {demoRoles.map((r) => {
              const isSelected = currentUser?.role === r.role;
              return (
                <button
                  key={r.role}
                  onClick={() => switchRole(r.role)}
                  style={{
                    border: 'none',
                    backgroundColor: isSelected ? 'var(--primary, #1a73e8)' : 'transparent',
                    color: isSelected ? '#ffffff' : 'var(--text-secondary, #475569)',
                    padding: '6px 10px',
                    borderRadius: '8px',
                    fontSize: '12px',
                    fontWeight: isSelected ? 700 : 500,
                    cursor: 'pointer',
                    boxShadow: isSelected ? '0 2px 8px rgba(26, 115, 232, 0.35)' : 'none',
                    transition: 'all 0.15s ease',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '6px',
                    justifyContent: 'center',
                  }}
                >
                  <span>{r.icon}</span>
                  <span>{r.label}</span>
                </button>
              );
            })}
          </div>

          <div style={{ fontSize: '11px', color: 'var(--text-muted, #64748b)', textAlign: 'center' }}>
            Đang đóng vai: <strong style={{ color: 'var(--primary, #1a73e8)' }}>{currentUser?.full_name}</strong>
          </div>
        </div>
      ) : (
        /* Collapsed Floating Pill Button */
        <button
          onClick={() => setIsExpanded(true)}
          style={{
            backgroundColor: 'var(--bg-card, #ffffff)',
            color: 'var(--text-primary, #0f172a)',
            border: '1px solid var(--border-card, #cbd5e1)',
            borderRadius: '9999px',
            padding: '8px 14px',
            boxShadow: '0 8px 24px rgba(15, 23, 42, 0.18)',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            fontSize: '12px',
            fontWeight: 700,
            backdropFilter: 'blur(12px)',
            transition: 'transform 0.15s ease',
          }}
          title="Mở bảng điều khiển Mock Role"
          onMouseEnter={(e) => (e.currentTarget.style.transform = 'scale(1.05)')}
          onMouseLeave={(e) => (e.currentTarget.style.transform = 'scale(1)')}
        >
          <span
            style={{
              width: '20px',
              height: '20px',
              borderRadius: '50%',
              backgroundColor: '#fef3c7',
              color: '#d97706',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <Zap size={12} strokeWidth={2.5} />
          </span>
          <span>Role: {currentUser?.role}</span>
          <ChevronUp size={14} color="var(--text-muted, #64748b)" />
        </button>
      )}
    </aside>
  );
};
