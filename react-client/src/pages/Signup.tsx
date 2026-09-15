import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import axios from "axios";
import { signup } from "../api/authApi";

function Signup() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [fullName, setFullName] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [role, setRole] = useState("TEACHER");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSignup = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email || !fullName || !password) {
      setError("Vui lòng điền đầy đủ tất cả các thông tin!");
      return;
    }

    if (password !== confirmPassword) {
      setError("Mật khẩu xác nhận không khớp!");
      return;
    }

    setLoading(true);
    setError("");

    try {
      const res = await signup(email, fullName, password);
      alert(res || "Đăng ký tài khoản thành công! Vui lòng đăng nhập.");
      navigate("/login");
    } catch (err: unknown) {
      if (axios.isAxiosError<{ message?: string }>(err) && err.response) {
        setError(err.response.data?.message ?? "Đăng ký thất bại");
        return;
      }
      setError("Không thể kết nối tới server Spring Boot. Vui lòng kiểm tra lại dịch vụ Backend!");
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="auth-page">
      <form className="auth-card" onSubmit={handleSignup}>
        <p className="auth-kicker">PhysLive workspace</p>
        <h1>Tạo tài khoản mới</h1>
        <p className="muted">Đăng ký để khám phá và tương tác với các phòng thí nghiệm vật lý.</p>

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
          <label htmlFor="signup-name">Họ và tên</label>
          <input
            id="signup-name"
            type="text"
            placeholder="ví dụ: Nguyễn Thế Hùng"
            value={fullName}
            onChange={(e) => setFullName(e.target.value)}
            required
          />
        </div>

        <div className="field">
          <label htmlFor="signup-email">Địa chỉ Email</label>
          <input
            id="signup-email"
            type="email"
            placeholder="you@example.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
        </div>

        <div className="field">
          <label htmlFor="signup-role">Vai trò của bạn</label>
          <select
            id="signup-role"
            value={role}
            onChange={(e) => setRole(e.target.value)}
          >
            <option value="TEACHER">Giáo viên (Teacher) — Biên soạn đề & Quản lý lớp</option>
            <option value="STUDENT">Học sinh (Student) — Thực nghiệm & Dự đoán</option>
            <option value="REVIEWER">Kiểm duyệt (Reviewer) — Thẩm định Schema Solver</option>
          </select>
        </div>

        <div className="field">
          <label htmlFor="signup-password">Mật khẩu</label>
          <input
            id="signup-password"
            type="password"
            placeholder="Tối thiểu 6 ký tự"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </div>

        <div className="field">
          <label htmlFor="signup-confirm-pwd">Xác nhận mật khẩu</label>
          <input
            id="signup-confirm-pwd"
            type="password"
            placeholder="Nhập lại mật khẩu"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            required
          />
        </div>

        <div className="actions">
          <button type="submit" className="btn-primary" disabled={loading}>
            {loading ? "Đang xử lý..." : "Tạo tài khoản"}
          </button>
          <button type="button" className="secondary" onClick={() => navigate("/login")}>
            Đã có tài khoản? Đăng nhập
          </button>
        </div>

        <p className="auth-footer">
          Bằng việc đăng ký, bạn đồng ý với <a href="#terms">Điều khoản sử dụng</a> PhysLive.
        </p>
      </form>
    </main>
  );
}

export default Signup;
export { Signup };
