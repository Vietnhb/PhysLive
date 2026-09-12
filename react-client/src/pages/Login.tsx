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

  // Xử lý đăng nhập với Backend API thật
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
      // Cập nhật user từ response
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

  // Đăng nhập nhanh Demo dành cho Hùng và Team test
  const handleDemoLogin = (targetEmail: string) => {
    const user = MOCK_USERS.find(u => u.email === targetEmail) || MOCK_USERS[0];
    loginWithUser(user, `mock_jwt_token_${user.role.toLowerCase()}`);
    navigate("/");
  };

  return (
    <div
      style={{
        minHeight: "100vh",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        background: "linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #0b1329 100%)",
        padding: "20px",
        fontFamily: "'Inter', sans-serif",
      }}
    >
      <div
        style={{
          width: "100%",
          maxWidth: "460px",
          backgroundColor: "#ffffff",
          borderRadius: "20px",
          padding: "36px",
          boxShadow: "0 25px 50px -12px rgba(0, 0, 0, 0.4)",
        }}
      >
        {/* Header Logo */}
        <div style={{ textAlign: "center", marginBottom: "28px" }}>
          <div
            style={{
              width: "56px",
              height: "56px",
              borderRadius: "14px",
              background: "linear-gradient(135deg, #2563eb, #06b6d4)",
              color: "#ffffff",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              fontSize: "28px",
              margin: "0 auto 14px",
              boxShadow: "0 8px 20px rgba(37, 99, 235, 0.3)",
            }}
          >
            ⚛️
          </div>
          <h1 style={{ margin: "0 0 6px", fontSize: "22px", fontWeight: 800, color: "#0f172a" }}>
            Đăng nhập PhysLive Studio
          </h1>
          <p style={{ margin: 0, fontSize: "13px", color: "#64748b" }}>
            Nền tảng mô phỏng vật lý tương tác dành cho Giáo viên & Học sinh
          </p>
        </div>

        {/* Error notification */}
        {error && (
          <div
            style={{
              padding: "12px 14px",
              backgroundColor: "#fef2f2",
              border: "1px solid #fecaca",
              borderRadius: "10px",
              color: "#991b1b",
              fontSize: "13px",
              marginBottom: "20px",
              lineHeight: 1.4,
            }}
          >
            ⚠️ {error}
          </div>
        )}

        {/* Login Form */}
        <form onSubmit={handleLogin} style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
          <div>
            <label style={{ display: "block", fontSize: "13px", fontWeight: 700, color: "#334155", marginBottom: "6px" }}>
              Địa chỉ Email
            </label>
            <input
              type="email"
              placeholder="ví dụ: teacher@physlive.edu.vn"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              style={{
                width: "100%",
                padding: "11px 14px",
                borderRadius: "10px",
                border: "1px solid #cbd5e1",
                fontSize: "14px",
                outline: "none",
                boxSizing: "border-box",
                transition: "border-color 0.2s",
              }}
            />
          </div>

          <div>
            <div style={{ display: "flex", justifyContent: "space-between", marginBottom: "6px" }}>
              <label style={{ fontSize: "13px", fontWeight: 700, color: "#334155" }}>
                Mật khẩu
              </label>
              <a href="#forgot" onClick={(e) => { e.preventDefault(); alert("Vui lòng liên hệ Admin để cấp lại mật khẩu!"); }} style={{ fontSize: "12px", color: "#2563eb", textDecoration: "none", fontWeight: 600 }}>
                Quên mật khẩu?
              </a>
            </div>
            <input
              type="password"
              placeholder="Nhập mật khẩu..."
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              style={{
                width: "100%",
                padding: "11px 14px",
                borderRadius: "10px",
                border: "1px solid #cbd5e1",
                fontSize: "14px",
                outline: "none",
                boxSizing: "border-box",
              }}
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            style={{
              marginTop: "6px",
              padding: "13px",
              backgroundColor: loading ? "#94a3b8" : "#2563eb",
              color: "#ffffff",
              border: "none",
              borderRadius: "10px",
              fontWeight: 700,
              fontSize: "14px",
              cursor: loading ? "not-allowed" : "pointer",
              boxShadow: "0 4px 14px rgba(37, 99, 235, 0.3)",
              transition: "background-color 0.2s",
            }}
          >
            {loading ? "Đang xác thực..." : "Đăng nhập hệ thống"}
          </button>
        </form>

        {/* Quick Demo Login Buttons (Rất hữu ích khi test) */}
        <div style={{ marginTop: "24px", paddingTop: "20px", borderTop: "1px solid #f1f5f9" }}>
          <div style={{ fontSize: "11px", fontWeight: 800, color: "#64748b", textTransform: "uppercase", letterSpacing: "0.05em", textAlign: "center", marginBottom: "12px" }}>
            ⚡ ĐĂNG NHẬP NHANH THỬ NGHIỆM (DEMO PRESETS)
          </div>

          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "8px" }}>
            <button
              onClick={() => handleDemoLogin("hung.teacher@physlive.edu.vn")}
              style={{
                padding: "8px 10px",
                borderRadius: "8px",
                border: "1px solid #bfdbfe",
                backgroundColor: "#eff6ff",
                color: "#1d4ed8",
                fontSize: "12px",
                fontWeight: 700,
                cursor: "pointer",
                textAlign: "center",
              }}
            >
              👨‍🏫 Thầy Hùng (Teacher)
            </button>

            <button
              onClick={() => handleDemoLogin("nam.student@physlive.edu.vn")}
              style={{
                padding: "8px 10px",
                borderRadius: "8px",
                border: "1px solid #bbf7d0",
                backgroundColor: "#f0fdf4",
                color: "#15803d",
                fontSize: "12px",
                fontWeight: 700,
                cursor: "pointer",
                textAlign: "center",
              }}
            >
              🎒 Em Nam (Student)
            </button>

            <button
              onClick={() => handleDemoLogin("kiet.reviewer@physlive.edu.vn")}
              style={{
                padding: "8px 10px",
                borderRadius: "8px",
                border: "1px solid #e9d5ff",
                backgroundColor: "#faf5ff",
                color: "#7e22ce",
                fontSize: "12px",
                fontWeight: 700,
                cursor: "pointer",
                textAlign: "center",
              }}
            >
              🔍 Kiệt (Reviewer)
            </button>

            <button
              onClick={() => handleDemoLogin("admin@physlive.edu.vn")}
              style={{
                padding: "8px 10px",
                borderRadius: "8px",
                border: "1px solid #fed7aa",
                backgroundColor: "#fff7ed",
                color: "#c2410c",
                fontSize: "12px",
                fontWeight: 700,
                cursor: "pointer",
                textAlign: "center",
              }}
            >
              ⚡ Quản trị Admin
            </button>
          </div>
        </div>

        {/* Footer link to Signup */}
        <div style={{ marginTop: "24px", textAlign: "center", fontSize: "13px", color: "#64748b" }}>
          Chưa có tài khoản?{" "}
          <Link to="/signup" style={{ color: "#2563eb", fontWeight: 700, textDecoration: "none" }}>
            Đăng ký tài khoản mới
          </Link>
          <div style={{ marginTop: "8px" }}>
            <Link to="/" style={{ color: "#94a3b8", fontSize: "12px", textDecoration: "none" }}>
              ← Quay về Trang chủ
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}

export default Login;
