import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useNavigate, Link } from 'react-router-dom';
import { LogIn, ArrowRight } from 'lucide-react';
import { PhysicsOrbital } from '../components/effects/PhysicsOrbital';
import { Header } from '../components/layout/Header';

export default function Login() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const { login, loginDemo } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    login(email, password);
    navigate('/dashboard');
  };

  const handleDemo = (role: 'teacher' | 'student' | 'reviewer' | 'admin') => {
    loginDemo(role);
    if (role === 'teacher') navigate('/teacher/submit');
    else if (role === 'student') navigate('/student/assignments');
    else if (role === 'reviewer') navigate('/reviewer/schemas');
    else if (role === 'admin') navigate('/admin');
    else navigate('/dashboard');
  };

  return (
    <div className="auth-wrapper-with-header">
      {/* Navbar giữ nguyên như Learning Hub */}
      <Header />

      <div className="auth-split-page">
        {/* Left Column: Physics Animation */}
        <div className="auth-split-visual">
          <PhysicsOrbital />
        </div>

        {/* Right Column: Form */}
        <div className="auth-split-form-container">
          <div className="auth-split-form-inner">
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '24px' }}>
              <div style={{ background: '#8b5cf6', padding: '8px', borderRadius: '10px', display: 'flex' }}>
                <LogIn color="white" size={24} />
              </div>
              <h2 style={{ margin: 0 }}>Đăng nhập</h2>
            </div>
            
            <p className="muted">
              Chào mừng trở lại trạm không gian PhysLive. Vui lòng đăng nhập để tiếp tục.
            </p>

            <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <div className="field" style={{ margin: 0 }}>
                <label>Email</label>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  placeholder="teacher@physlive.edu.vn"
                />
              </div>
              <div className="field" style={{ margin: 0 }}>
                <label>Mật khẩu</label>
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  placeholder="••••••••"
                />
              </div>

              <button type="submit" className="btn-primary" style={{ marginTop: '8px' }}>
                Tiếp tục <ArrowRight size={18} />
              </button>
            </form>

            <div style={{ marginTop: '20px', textAlign: 'center', fontSize: '14px' }}>
              <Link to="/signup" style={{ color: '#94a3b8', textDecoration: 'none' }}>
                Chưa có tài khoản? <span style={{ color: '#fff', fontWeight: 600 }}>Đăng ký</span>
              </Link>
            </div>

            <div className="auth-split-divider">
              <span>Đăng nhập nhanh (Demo)</span>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
              <button 
                type="button" 
                className="auth-demo-btn" 
                onClick={() => handleDemo('teacher')}
                style={{ background: 'rgba(59, 130, 246, 0.12)', color: '#60a5fa', borderColor: 'rgba(59, 130, 246, 0.3)' }}
              >
                👨‍🏫 Bấm vào đây để Test luồng Giáo viên
              </button>
              <button 
                type="button" 
                className="auth-demo-btn" 
                onClick={() => handleDemo('student')}
                style={{ background: 'rgba(34, 197, 94, 0.12)', color: '#4ade80', borderColor: 'rgba(34, 197, 94, 0.3)' }}
              >
                🎒 Bấm vào đây để Test luồng Học sinh
              </button>
              <button 
                type="button" 
                className="auth-demo-btn" 
                onClick={() => handleDemo('reviewer')}
                style={{ background: 'rgba(234, 179, 8, 0.12)', color: '#facc15', borderColor: 'rgba(234, 179, 8, 0.3)' }}
              >
                🔍 Bấm vào đây để Test luồng Thẩm định (Reviewer)
              </button>
              <button 
                type="button" 
                className="auth-demo-btn" 
                onClick={() => handleDemo('admin')}
                style={{ background: 'rgba(239, 68, 68, 0.12)', color: '#f87171', borderColor: 'rgba(239, 68, 68, 0.3)' }}
              >
                ⚡ Bấm vào đây để Test luồng Admin
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
