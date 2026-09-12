import React from 'react';

interface Props {
  lines?: number;
  height?: number;
  className?: string;
}

export const LoadingSkeleton: React.FC<Props> = ({ lines = 3, height = 24 }) => {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', width: '100%' }}>
      {Array.from({ length: lines }).map((_, i) => (
        <div
          key={i}
          style={{
            height: `${height}px`,
            width: i === lines - 1 ? '70%' : '100%',
            backgroundColor: '#e2e8f0',
            borderRadius: '6px',
            animation: 'pulse 1.5s ease-in-out infinite',
          }}
        />
      ))}
      <style>{`
        @keyframes pulse {
          0%, 100% { opacity: 0.95; }
          50% { opacity: 0.45; }
        }
      `}</style>
    </div>
  );
};
