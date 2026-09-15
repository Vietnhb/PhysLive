import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useNavigate, Link } from 'react-router-dom';
import { UserPlus, ArrowRight } from 'lucide-react';
import { PhysicsOrbital } from '../components/effects/PhysicsOrbital';
import { Header } from '../components/layout/Header';

export default function Signup() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [role, setRole] = useState<'student' | 'teacher'>('student');
  const { register } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    register(email, password, fullName, role);
    navigate('/login');
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
                <UserPlus color="white" size={24} />
              </div>
              <h2 style={{ margin: 0 }}>Đăng ký</h2>
            </div>
            
            <p className="muted">
              Tạo tài khoản để tham gia vào hệ sinh thái học tập PhysLive.
            </p>

            <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <div className="field" style={{ margin: 0 }}>
                <label>Họ và tên</label>
                <input
                  type="text"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  required
                  placeholder="Nguyễn Văn A"
                />
              </div>
              
              <div className="field" style={{ margin: 0 }}>
                <label>Vai trò</label>
                <select 
                  value={role} 
                  onChange={(e) => setRole(e.target.value as 'student' | 'teacher')}
                  style={{ 
                    backgroundColor: 'rgba(255,255,255,0.05)', 
                    color: '#fff',
                    border: '1px solid rgba(255,255,255,0.1)'
                  }}
                >
                  <option value="student" style={{ color: '#000' }}>Học sinh</option>
                  <option value="teacher" style={{ color: '#000' }}>Giáo viên</option>
                </select>
              </div>

              <div className="field" style={{ margin: 0 }}>
                <label>Email</label>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  placeholder="email@example.com"
                />
              </div>

              <div className="field" style={{ margin: 0 }}>
                <label>Mật khẩu (tối thiểu 6 ký tự)</label>
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  minLength={6}
                  placeholder="••••••••"
                />
              </div>

              <button type="submit" className="btn-primary" style={{ marginTop: '8px' }}>
                Tạo tài khoản <ArrowRight size={18} />
              </button>
            </form>

            <div style={{ marginTop: '24px', textAlign: 'center', fontSize: '14px', color: '#64748b' }}>
              Bằng việc tiếp tục, bạn đồng ý với <a href="#" style={{ color: '#94a3b8' }}>Điều khoản dịch vụ</a> và <a href="#" style={{ color: '#94a3b8' }}>Chính sách bảo mật</a> của chúng tôi.
            </div>

            <div style={{ marginTop: '32px', textAlign: 'center', fontSize: '14px' }}>
              <Link to="/login" style={{ color: '#94a3b8', textDecoration: 'none' }}>
                Đã có tài khoản? <span style={{ color: '#fff', fontWeight: 600 }}>Đăng nhập</span>
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
