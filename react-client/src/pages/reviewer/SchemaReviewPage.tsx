import React from 'react';
import { MOCK_TOPICS } from '../../mocks/mockTaxonomy';

export const SchemaReviewPage: React.FC = () => {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      <div>
        <h1 style={{ margin: '0 0 6px', fontSize: '24px', fontWeight: 800, color: '#0f172a' }}>
          Thẩm định Schema & Khung giải toán Solvers (Reviewer Workspace)
        </h1>
        <p style={{ margin: 0, fontSize: '14px', color: '#64748b' }}>
          Khu vực dành cho Reviewer (Kiệt) và Minh để kiểm tra đối chiếu hai bộ giải: Numerical Solver và Closed-form Solver.
        </p>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '20px' }}>
        {MOCK_TOPICS.map((t) => (
          <div
            key={t.id}
            style={{
              backgroundColor: '#ffffff',
              borderRadius: '16px',
              padding: '20px',
              border: '1px solid #e2e8f0',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '12px' }}>
              <span style={{ fontSize: '14px', fontWeight: 700, color: '#1e293b' }}>{t.name}</span>
              <span style={{ fontSize: '11px', padding: '2px 8px', borderRadius: '4px', backgroundColor: '#dcfce7', color: '#15803d', fontWeight: 600 }}>
                Approved v1.0
              </span>
            </div>
            <div style={{ fontSize: '12px', color: '#64748b', marginBottom: '16px' }}>
              Mã topic: <code>{t.code}</code> — Dung sai kiểm chứng: <strong>tolerance ≤ 1.0%</strong>
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingTop: '12px', borderTop: '1px solid #f1f5f9' }}>
              <span style={{ fontSize: '12px', color: '#059669', fontWeight: 600 }}>
                Dual-Validation: PASSED
              </span>
              <button
                onClick={() => alert(`Xem chi tiết log kiểm chứng của ${t.name}`)}
                style={{
                  padding: '6px 12px',
                  backgroundColor: '#f1f5f9',
                  border: '1px solid #cbd5e1',
                  borderRadius: '6px',
                  fontSize: '12px',
                  fontWeight: 600,
                  cursor: 'pointer',
                }}
              >
                Xem Log kiểm toán
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
