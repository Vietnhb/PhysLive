import React from 'react';

interface Props {
  title: string;
  description?: string;
  actionText?: string;
  onAction?: () => void;
  icon?: React.ReactNode;
}

export const EmptyState: React.FC<Props> = ({
  title,
  description,
  actionText,
  onAction,
  icon,
}) => {
  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '48px 24px',
        textAlign: 'center',
        backgroundColor: '#ffffff',
        border: '2px dashed #cbd5e1',
        borderRadius: '16px',
        margin: '20px 0',
      }}
    >
      <div style={{ fontSize: '48px', marginBottom: '16px', opacity: 0.8 }}>
        {icon || '🔭'}
      </div>
      <h3 style={{ margin: '0 0 8px', fontSize: '18px', color: '#1e293b', fontWeight: 600 }}>
        {title}
      </h3>
      {description && (
        <p style={{ margin: '0 0 20px', color: '#64748b', fontSize: '14px', maxWidth: '420px', lineHeight: 1.5 }}>
          {description}
        </p>
      )}
      {actionText && onAction && (
        <button
          onClick={onAction}
          style={{
            padding: '10px 20px',
            backgroundColor: '#2563eb',
            color: '#ffffff',
            border: 'none',
            borderRadius: '8px',
            fontWeight: 600,
            cursor: 'pointer',
            transition: 'background-color 0.2s',
          }}
          onMouseOver={(e) => (e.currentTarget.style.backgroundColor = '#1d4ed8')}
          onMouseOut={(e) => (e.currentTarget.style.backgroundColor = '#2563eb')}
        >
          {actionText}
        </button>
      )}
    </div>
  );
};
