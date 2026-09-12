import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { MOCK_TOPICS } from '../mocks/mockTaxonomy';
import { MOCK_PROBLEMS } from '../mocks/mockProblems';
import { MOCK_SIMULATIONS } from '../mocks/mockSimulations';

export const Dashboard: React.FC = () => {
  const { currentUser } = useAuth();

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Hero Welcome Banner */}
      <div
        style={{
          background: 'linear-gradient(135deg, #1e3a8a 0%, #2563eb 60%, #06b6d4 100%)',
          borderRadius: '20px',
          padding: '32px',
          color: '#ffffff',
          position: 'relative',
          overflow: 'hidden',
          boxShadow: '0 10px 30px rgba(37, 99, 235, 0.2)',
        }}
      >
        <div style={{ position: 'relative', zIndex: 2, maxWidth: '700px' }}>
          <span
            style={{
              display: 'inline-block',
              padding: '4px 12px',
              borderRadius: '9999px',
              backgroundColor: 'rgba(255, 255, 255, 0.15)',
              backdropFilter: 'blur(8px)',
              fontSize: '12px',
              fontWeight: 600,
              letterSpacing: '0.04em',
              marginBottom: '12px',
            }}
          >
            PHYSLIVE STUDIO — VẬT LÝ TƯƠNG TÁC ĐA NỀN TẢNG
          </span>
          <h1 style={{ margin: '0 0 10px', fontSize: '28px', fontWeight: 800, letterSpacing: '-0.02em' }}>
            Xin chào, {currentUser.full_name}! 👋
          </h1>
          <p style={{ margin: '0 0 20px', fontSize: '15px', color: '#e0f2fe', lineHeight: 1.6 }}>
            Hệ thống biến bài toán vật lý tĩnh thành mô phỏng tương tác 2D/3D sinh động, đối chiếu kiểm chứng hai lõi và hỗ trợ giảng dạy trực quan.
          </p>

          <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
            {currentUser.role === 'TEACHER' || currentUser.role === 'ADMIN' ? (
              <Link
                to="/teacher/submit"
                style={{
                  padding: '11px 22px',
                  backgroundColor: '#ffffff',
                  color: '#1d4ed8',
                  borderRadius: '10px',
                  fontWeight: 700,
                  fontSize: '14px',
                  textDecoration: 'none',
                  boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '8px',
                }}
              >
                <span>➕</span> Nộp đề bài mới & Trích xuất AI
              </Link>
            ) : null}

            <Link
              to="/player"
              style={{
                padding: '11px 22px',
                backgroundColor: 'rgba(255, 255, 255, 0.15)',
                color: '#ffffff',
                borderRadius: '10px',
                fontWeight: 600,
                fontSize: '14px',
                textDecoration: 'none',
                backdropFilter: 'blur(8px)',
                border: '1px solid rgba(255, 255, 255, 0.25)',
                display: 'inline-flex',
                alignItems: 'center',
                gap: '8px',
              }}
            >
              <span>🧪</span> Vào phòng thí nghiệm Player
            </Link>
          </div>
        </div>

        {/* Decorative graphic element */}
        <div
          style={{
            position: 'absolute',
            right: '-40px',
            bottom: '-40px',
            fontSize: '160px',
            opacity: 0.15,
            pointerEvents: 'none',
            userSelect: 'none',
          }}
        >
          ⚛️
        </div>
      </div>

      {/* 3 Core Topics Cards */}
      <div>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
          <h2 style={{ margin: 0, fontSize: '18px', fontWeight: 700, color: '#1e293b' }}>
            3 Chủ đề Vật lý cốt lõi (MVP Baseline)
          </h2>
          <span style={{ fontSize: '13px', color: '#64748b' }}>Chuẩn phân loại học liệu SGK mới</span>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '16px' }}>
          {MOCK_TOPICS.map((topic) => {
            const icons: Record<string, string> = {
              KINEMATICS: '🚀',
              DYNAMICS: '⚖️',
              CIRCUITS: '⚡',
            };
            const colors: Record<string, string> = {
              KINEMATICS: '#3b82f6',
              DYNAMICS: '#f59e0b',
              CIRCUITS: '#10b981',
            };

            return (
              <div
                key={topic.id}
                style={{
                  backgroundColor: '#ffffff',
                  borderRadius: '14px',
                  padding: '20px',
                  border: '1px solid #e2e8f0',
                  boxShadow: '0 2px 8px rgba(0,0,0,0.03)',
                  display: 'flex',
                  flexDirection: 'column',
                  justifyContent: 'space-between',
                  borderTop: `4px solid ${colors[topic.code] || '#3b82f6'}`,
                }}
              >
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '10px' }}>
                    <span style={{ fontSize: '24px' }}>{icons[topic.code]}</span>
                    <div>
                      <h3 style={{ margin: 0, fontSize: '16px', fontWeight: 700, color: '#0f172a' }}>
                        {topic.name}
                      </h3>
                      <span style={{ fontSize: '11px', color: '#64748b', fontWeight: 600 }}>
                        CODE: {topic.code}
                      </span>
                    </div>
                  </div>
                  <p style={{ margin: 0, fontSize: '13px', color: '#475569', lineHeight: 1.5 }}>
                    {topic.description}
                  </p>
                </div>

                <div style={{ marginTop: '16px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingTop: '12px', borderTop: '1px solid #f1f5f9' }}>
                  <span style={{ fontSize: '12px', color: '#10b981', fontWeight: 600 }}>
                    ✓ 2 Archetypes sẵn sàng
                  </span>
                  <Link
                    to="/player"
                    style={{ fontSize: '13px', color: '#2563eb', fontWeight: 600, textDecoration: 'none' }}
                  >
                    Xem mô phỏng →
                  </Link>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Quick Access Sections: Submissions & Simulations */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(400px, 1fr))', gap: '20px' }}>
        {/* Recent Problem Submissions */}
        <div style={{ backgroundColor: '#ffffff', borderRadius: '16px', padding: '22px', border: '1px solid #e2e8f0' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
            <h3 style={{ margin: 0, fontSize: '16px', fontWeight: 700, color: '#0f172a' }}>
              📝 Bài tập vừa nộp & Trích xuất
            </h3>
            <Link to="/teacher/submit" style={{ fontSize: '12px', color: '#2563eb', fontWeight: 600, textDecoration: 'none' }}>
              + Thêm mới
            </Link>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {MOCK_PROBLEMS.map((item) => (
              <div
                key={item.submission.id}
                style={{
                  padding: '14px',
                  borderRadius: '10px',
                  backgroundColor: '#f8fafc',
                  border: '1px solid #e2e8f0',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '6px' }}>
                  <span style={{ fontSize: '12px', fontWeight: 700, color: '#3b82f6' }}>
                    {item.extraction.extracted_spec.archetype}
                  </span>
                  <span
                    style={{
                      fontSize: '11px',
                      padding: '2px 8px',
                      borderRadius: '9999px',
                      fontWeight: 600,
                      backgroundColor: item.submission.status === 'Completed' ? '#dcfce7' : '#fef9c3',
                      color: item.submission.status === 'Completed' ? '#166534' : '#854d0e',
                    }}
                  >
                    {item.submission.status}
                  </span>
                </div>
                <p style={{ margin: '0 0 10px', fontSize: '13px', color: '#334155', lineHeight: 1.4, display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
                  {item.submission.raw_text}
                </p>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '12px' }}>
                  <span style={{ color: item.ambiguities.some(a => a.status === 'pending') ? '#dc2626' : '#16a34a', fontWeight: 600 }}>
                    {item.ambiguities.some(a => a.status === 'pending') ? '⚠️ Cần xác nhận mơ hồ' : '✓ Dữ kiện đầy đủ'}
                  </span>
                  <Link
                    to="/teacher/submit"
                    style={{ color: '#2563eb', fontWeight: 600, textDecoration: 'none' }}
                  >
                    Kiểm tra chi tiết
                  </Link>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Live Simulations Ready */}
        <div style={{ backgroundColor: '#ffffff', borderRadius: '16px', padding: '22px', border: '1px solid #e2e8f0' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
            <h3 style={{ margin: 0, fontSize: '16px', fontWeight: 700, color: '#0f172a' }}>
              🧪 Mô phỏng tương tác có sẵn
            </h3>
            <Link to="/player" style={{ fontSize: '12px', color: '#2563eb', fontWeight: 600, textDecoration: 'none' }}>
              Xem tất cả
            </Link>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {MOCK_SIMULATIONS.map((sim) => (
              <div
                key={sim.id}
                style={{
                  padding: '14px',
                  borderRadius: '10px',
                  backgroundColor: '#f8fafc',
                  border: '1px solid #e2e8f0',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '6px' }}>
                  <span style={{ fontSize: '13px', fontWeight: 700, color: '#0f172a' }}>
                    {sim.title}
                  </span>
                  <span
                    style={{
                      fontSize: '11px',
                      padding: '2px 8px',
                      borderRadius: '9999px',
                      fontWeight: 600,
                      backgroundColor: '#dcfce7',
                      color: '#15803d',
                    }}
                  >
                    Đã kiểm chứng ✓
                  </span>
                </div>
                <p style={{ margin: '0 0 10px', fontSize: '12px', color: '#64748b' }}>
                  {sim.description}
                </p>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <span style={{ fontSize: '11px', color: '#475569' }}>
                    {sim.spec_json.variables.length} thông số điều chỉnh
                  </span>
                  <Link
                    to={`/player?id=${sim.id}`}
                    style={{
                      padding: '5px 12px',
                      backgroundColor: '#2563eb',
                      color: '#ffffff',
                      borderRadius: '6px',
                      fontSize: '12px',
                      fontWeight: 600,
                      textDecoration: 'none',
                    }}
                  >
                    Khởi chạy Player 🚀
                  </Link>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};
