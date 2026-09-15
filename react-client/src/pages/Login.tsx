import React, { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import axios from "axios";
import { login } from "../api/authApi";
import { setToken } from "../utils/token";
import { useAuth } from "../context/AuthContext";
import { MOCK_USERS } from "../mocks/mockUsers";
import { IS_MOCK_ENABLED } from "../config/api";

function Login() {
  const navigate = useNavigate();
  const { loginWithUser } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [showDemoLogins, setShowDemoLogins] = useState(false);

  // Xử lý đăng nhập với Backend API
  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email || !password) {
      setError("Vui lòng nhập đầy đủ Email và Mật khẩu!");
      return;
    }

    setLoading(true);
    setError("");

    try {
      const res = await login(email, password);
      if (res.user) {
        loginWithUser(
          {
            id: String(res.user.id),
            role_id: `r-${res.user.role.toLowerCase()}`,
            role: res.user.role as any,
            email: res.user.email,
            full_name: res.user.fullName || res.user.email,
            status: 'active',
            created_at: new Date().toISOString(),
            updated_at: new Date().toISOString(),
          },
          res.token
        );
      } else {
        setToken(res.token);
      }
      navigate("/");
    } catch (err: unknown) {
      if (!axios.isAxiosError<{ message?: string }>(err) || !err.response) {
        if (IS_MOCK_ENABLED) {
          setError("Chưa kết nối được Server Spring Boot. Bạn có thể sử dụng các nút Đăng nhập nhanh Demo bên dưới để trải nghiệm ngay!");
        } else {
          setError("Không thể kết nối tới server. Vui lòng kiểm tra lại Backend!");
        }
        return;
      }
      if (err.response.status === 401) {
        setError(err.response.data?.message ?? "Email hoặc mật khẩu không đúng");
        return;
      }
      setError("Có lỗi xảy ra từ máy chủ backend");
    } finally {
      setLoading(false);
    }
  };

  // Đăng nhập nhanh Demo dành cho Giáo viên / Học sinh / Kiểm duyệt / Admin
  const handleDemoLogin = (targetEmail: string) => {
    const user = MOCK_USERS.find(u => u.email === targetEmail) || MOCK_USERS[0];
    loginWithUser(user, `mock_jwt_token_${user.role.toLowerCase()}`);
    navigate("/");
  };

  return (
    <main className="auth-page">
      <form className="auth-card" onSubmit={handleLogin}>
        <p className="auth-kicker">PhysLive workspace</p>
        <h1>Chào mừng trở lại</h1>
        <p className="muted">Đăng nhập để tiếp tục xây dựng và quản trị nền tảng.</p>

        {error && (
          <div
            className="ambiguity"
            style={{
              borderColor: '#fca5a5',
              background: '#fef2f2',
              color: '#991b1b',
              marginBottom: 18,
            }}
          >
            ⚠️ {error}
          </div>
        )}

        <div className="field">
          <label htmlFor="login-email">Email</label>
          <input
            id="login-email"
            type="email"
            placeholder="you@example.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
        </div>

        <div className="field">
          <label htmlFor="login-password">Mật khẩu</label>
          <input
            id="login-password"
            type="password"
            placeholder="Nhập mật khẩu"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </div>

        <div className="actions">
          <button type="submit" className="btn-primary" disabled={loading}>
            {loading ? "Đang đăng nhập..." : "Đăng nhập"}
          </button>
          <button type="button" className="secondary" onClick={() => navigate("/")}>
            Trang chủ
          </button>
        </div>

        {/* Demo Fast Login Switcher (Gọn gàng & chuyên nghiệp) */}
        <div style={{ marginTop: 24, paddingTop: 18, borderTop: '1px solid var(--border-subtle)' }}>
          <button
            type="button"
            className="secondary"
            onClick={() => setShowDemoLogins(!showDemoLogins)}
            style={{ width: '100%', minHeight: 34, fontSize: 12, padding: '6px 12px' }}
          >
            ⚡ {showDemoLogins ? "Ẩn danh sách tài khoản Demo" : "Đăng nhập nhanh với tài khoản Demo"}
          </button>

          {showDemoLogins && (
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8, marginTop: 12 }}>
              {MOCK_USERS.map((u) => (
                <button
                  key={u.id}
                  type="button"
                  onClick={() => handleDemoLogin(u.email)}
                  style={{
                    padding: '8px 10px',
                    fontSize: 11,
                    textAlign: 'left',
                    background: 'var(--surface-muted)',
                    border: '1px solid var(--border-subtle)',
                    borderRadius: 6,
                    color: 'var(--text-primary)',
                    cursor: 'pointer',
                    display: 'flex',
                    flexDirection: 'column',
                    gap: 2,
                  }}
                >
                  <strong style={{ color: 'var(--primary)' }}>{u.role}</strong>
                  <span style={{ fontSize: 10, color: 'var(--text-secondary)', textOverflow: 'ellipsis', overflow: 'hidden', whiteSpace: 'nowrap' }}>
                    {u.email}
                  </span>
                </button>
              ))}
            </div>
          )}
        </div>

        <p className="auth-footer">
          Chưa có tài khoản? <Link to="/signup">Đăng ký ngay</Link>
        </p>
      </form>
    </main>
  );
}

export default Login;
export { Login };
