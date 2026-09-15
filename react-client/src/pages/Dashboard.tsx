import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { MOCK_TOPICS } from '../mocks/mockTaxonomy';
import { MOCK_PROBLEMS } from '../mocks/mockProblems';
import { MOCK_SIMULATIONS } from '../mocks/mockSimulations';
import {
  FlaskConical,
  FileEdit,
  Target,
  Rocket,
  Scale,
  Zap,
  ArrowRight,
  CheckCircle2,
  Atom,
  Sparkles,
  Play,
  Layers,
} from 'lucide-react';

export const Dashboard: React.FC = () => {
  const { currentUser } = useAuth();

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Hero Welcome Banner with Glassmorphism */}
      <div
        style={{
          background: 'linear-gradient(135deg, #0f172a 0%, #1e3a8a 50%, #0369a1 100%)',
          borderRadius: '20px',
          padding: '36px',
          color: '#ffffff',
          position: 'relative',
          overflow: 'hidden',
          boxShadow: '0 12px 36px rgba(15, 23, 42, 0.25)',
          border: '1px solid rgba(255, 255, 255, 0.1)',
        }}
      >
        <div style={{ position: 'relative', zIndex: 2, maxWidth: '720px' }}>
          <div
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
              padding: '4px 12px',
              borderRadius: '9999px',
              backgroundColor: 'rgba(255, 255, 255, 0.12)',
              backdropFilter: 'blur(10px)',
              border: '1px solid rgba(255, 255, 255, 0.2)',
              fontSize: '11px',
              fontWeight: 700,
              letterSpacing: '0.06em',
              marginBottom: '14px',
              textTransform: 'uppercase',
            }}
          >
            <Sparkles size={13} color="#38bdf8" />
            <span>PhysLive Studio — Nền tảng mô phỏng tương tác cao cấp</span>
          </div>

          <h1 style={{ margin: '0 0 10px', fontSize: '30px', fontWeight: 800, letterSpacing: '-0.02em', lineHeight: 1.2 }}>
            Xin chào, {currentUser ? currentUser.full_name : 'bạn'}! 👋
          </h1>
          <p style={{ margin: '0 0 24px', fontSize: '15px', color: '#cbd5e1', lineHeight: 1.6 }}>
            Hệ thống biến các bài toán vật lý SGK tĩnh thành không gian mô phỏng tương tác 2D/3D sinh động, đối chiếu kiểm chứng hai lõi (Dual-Validation) và hỗ trợ giáo viên lẫn học sinh giảng dạy trực quan.
          </p>

          <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
            {currentUser && (currentUser.role === 'TEACHER' || currentUser.role === 'ADMIN') ? (
              <Link
                to="/teacher/submit"
                style={{
                  padding: '11px 20px',
                  backgroundColor: '#2563eb',
                  color: '#ffffff',
                  borderRadius: '10px',
                  fontSize: '13px',
                  fontWeight: 700,
                  textDecoration: 'none',
                  boxShadow: '0 4px 14px rgba(37, 99, 235, 0.4)',
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '8px',
                  transition: 'transform 0.15s ease',
                }}
              >
                <FileEdit size={16} strokeWidth={2.2} />
                <span>Nộp bài toán mới (AI OCR)</span>
              </Link>
            ) : null}

            {currentUser && currentUser.role === 'STUDENT' ? (
              <Link
                to="/student/assignments"
                style={{
                  padding: '11px 20px',
                  backgroundColor: '#10b981',
                  color: '#ffffff',
                  borderRadius: '10px',
                  fontSize: '13px',
                  fontWeight: 700,
                  textDecoration: 'none',
                  boxShadow: '0 4px 14px rgba(16, 185, 129, 0.4)',
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '8px',
                }}
              >
                <Target size={16} strokeWidth={2.2} />
                <span>Vào thử thách dự đoán</span>
              </Link>
            ) : null}

            <Link
              to="/player"
              style={{
                padding: '11px 20px',
                backgroundColor: 'rgba(255, 255, 255, 0.12)',
                color: '#ffffff',
                borderRadius: '10px',
                fontSize: '13px',
                fontWeight: 700,
                textDecoration: 'none',
                backdropFilter: 'blur(8px)',
                border: '1px solid rgba(255, 255, 255, 0.25)',
                display: 'inline-flex',
                alignItems: 'center',
                gap: '8px',
              }}
            >
              <FlaskConical size={16} strokeWidth={2.2} />
              <span>Vào phòng thí nghiệm Player</span>
            </Link>
          </div>
        </div>

        {/* Decorative graphic element */}
        <div
          style={{
            position: 'absolute',
            right: '-30px',
            bottom: '-40px',
            opacity: 0.12,
            pointerEvents: 'none',
            userSelect: 'none',
            color: '#38bdf8',
          }}
        >
          <Atom size={240} strokeWidth={0.8} />
        </div>
      </div>

      {/* 3 Core Topics Cards */}
      <div>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Layers size={18} color="var(--primary)" />
            <h2 style={{ margin: 0, fontSize: '18px', fontWeight: 800, color: 'var(--text-primary)' }}>
              3 Chủ đề Vật lý cốt lõi (MVP Baseline)
            </h2>
          </div>
          <span style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>Chuẩn phân loại học liệu SGK mới 4 tầng</span>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '16px' }}>
          {MOCK_TOPICS.map((topic) => {
            const icons: Record<string, React.ReactNode> = {
              KINEMATICS: <Rocket size={22} color="#2563eb" />,
              DYNAMICS: <Scale size={22} color="#f59e0b" />,
              CIRCUITS: <Zap size={22} color="#10b981" />,
            };
            const colors: Record<string, string> = {
              KINEMATICS: '#3b82f6',
              DYNAMICS: '#f59e0b',
              CIRCUITS: '#10b981',
            };

            return (
              <div
                key={topic.id}
                className="studio-card-interactive card"
                style={{
                  backgroundColor: 'var(--bg-card)',
                  borderRadius: '16px',
                  padding: '22px',
                  border: '1px solid var(--border-card)',
                  boxShadow: '0 2px 8px rgba(0,0,0,0.02)',
                  display: 'flex',
                  flexDirection: 'column',
                  justifyContent: 'space-between',
                  borderTop: `4px solid ${colors[topic.code] || '#3b82f6'}`,
                }}
              >
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '12px' }}>
                    <div
                      style={{
                        width: '42px',
                        height: '42px',
                        borderRadius: '10px',
                        backgroundColor: 'var(--surface-muted)',
                        border: '1px solid var(--border-subtle)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                      }}
                    >
                      {icons[topic.code]}
                    </div>
                    <div>
                      <h3 style={{ margin: 0, fontSize: '16px', fontWeight: 800, color: 'var(--text-primary)' }}>
                        {topic.name}
                      </h3>
                      <span style={{ fontSize: '11px', color: 'var(--text-secondary)', fontWeight: 700 }}>
                        CODE: {topic.code}
                      </span>
                    </div>
                  </div>
                  <p style={{ margin: 0, fontSize: '13px', color: 'var(--text-secondary)', lineHeight: 1.5 }}>
                    {topic.description}
                  </p>
                </div>

                <div style={{ marginTop: '18px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingTop: '14px', borderTop: '1px solid var(--border-subtle)' }}>
                  <span style={{ fontSize: '12px', color: 'var(--status-success-text)', fontWeight: 700, display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                    <CheckCircle2 size={13} />
                    <span>2 Archetypes sẵn sàng</span>
                  </span>
                  <Link
                    to="/player"
                    style={{ fontSize: '13px', color: 'var(--primary)', fontWeight: 700, textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: '4px' }}
                  >
                    <span>Xem mô phỏng</span>
                    <ArrowRight size={13} />
                  </Link>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Quick Access Sections: Submissions & Simulations */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(400px, 1fr))', gap: '20px' }}>
        {/* Recent Problems */}
        <div className="card" style={{ backgroundColor: 'var(--bg-card)', borderRadius: '16px', padding: '22px', border: '1px solid var(--border-card)', boxShadow: '0 2px 8px rgba(0,0,0,0.02)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
            <h3 style={{ margin: 0, fontSize: '16px', fontWeight: 800, color: 'var(--text-primary)' }}>
              Bài toán giáo viên vừa nộp
            </h3>
            <Link
              to="/teacher/submit"
              style={{ fontSize: '12px', color: 'var(--primary)', fontWeight: 700, textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: '3px' }}
            >
              <span>Tạo đề mới</span>
              <ArrowRight size={12} />
            </Link>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {MOCK_PROBLEMS.slice(0, 3).map((prob) => (
              <div
                key={prob.submission.id}
                style={{
                  padding: '14px',
                  borderRadius: '10px',
                  backgroundColor: 'var(--surface-muted)',
                  border: '1px solid var(--border-subtle)',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '6px',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ fontSize: '13px', fontWeight: 700, color: 'var(--text-primary)' }}>
                    {prob.extraction.extracted_spec.archetype}
                  </span>
                  <span
                    style={{
                      fontSize: '11px',
                      padding: '2px 8px',
                      borderRadius: '9999px',
                      fontWeight: 700,
                      backgroundColor: prob.submission.status === 'Completed' ? 'var(--status-success-bg)' : 'var(--status-warn-bg)',
                      color: prob.submission.status === 'Completed' ? 'var(--status-success-text)' : 'var(--status-warn-text)',
                    }}
                  >
                    {prob.submission.status === 'Completed' ? 'Sẵn sàng mô phỏng' : 'Chờ bóc tách'}
                  </span>
                </div>
                <p style={{ margin: 0, fontSize: '12px', color: 'var(--text-secondary)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  {prob.submission.raw_text}
                </p>
              </div>
            ))}
          </div>
        </div>

        {/* Available Simulations */}
        <div className="card" style={{ backgroundColor: 'var(--bg-card)', borderRadius: '16px', padding: '22px', border: '1px solid var(--border-card)', boxShadow: '0 2px 8px rgba(0,0,0,0.02)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
            <h3 style={{ margin: 0, fontSize: '16px', fontWeight: 800, color: 'var(--text-primary)' }}>
              Mô phỏng vật lý sẵn có
            </h3>
            <Link
              to="/library"
              style={{ fontSize: '12px', color: 'var(--primary)', fontWeight: 700, textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: '3px' }}
            >
              <span>Vào thư viện</span>
              <ArrowRight size={12} />
            </Link>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {MOCK_SIMULATIONS.map((sim) => (
              <div
                key={sim.id}
                style={{
                  padding: '14px',
                  borderRadius: '10px',
                  backgroundColor: 'var(--surface-muted)',
                  border: '1px solid var(--border-subtle)',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '6px' }}>
                  <span style={{ fontSize: '13px', fontWeight: 700, color: 'var(--text-primary)' }}>
                    {sim.title}
                  </span>
                  <span
                    style={{
                      fontSize: '11px',
                      padding: '2px 8px',
                      borderRadius: '9999px',
                      fontWeight: 700,
                      backgroundColor: 'var(--status-success-bg)',
                      color: 'var(--status-success-text)',
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: '4px',
                    }}
                  >
                    <CheckCircle2 size={11} />
                    <span>Đã kiểm chứng</span>
                  </span>
                </div>
                <p style={{ margin: '0 0 10px', fontSize: '12px', color: 'var(--text-secondary)' }}>
                  {sim.description}
                </p>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <span style={{ fontSize: '11px', color: 'var(--text-secondary)', fontWeight: 600 }}>
                    {sim.spec_json.variables.length} thông số điều chỉnh
                  </span>
                  <Link
                    to={`/player?id=${sim.id}`}
                    style={{
                      padding: '6px 14px',
                      backgroundColor: 'var(--primary)',
                      color: '#ffffff',
                      borderRadius: '7px',
                      fontSize: '12px',
                      fontWeight: 700,
                      textDecoration: 'none',
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: '5px',
                      boxShadow: '0 2px 6px rgba(37, 99, 235, 0.25)',
                    }}
                  >
                    <Play size={12} fill="#ffffff" />
                    <span>Khởi chạy Player</span>
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

export default Dashboard;
