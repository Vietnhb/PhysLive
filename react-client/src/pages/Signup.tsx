import React, { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
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
          maxWidth: "480px",
          backgroundColor: "#ffffff",
          borderRadius: "20px",
          padding: "36px",
          boxShadow: "0 25px 50px -12px rgba(0, 0, 0, 0.4)",
        }}
      >
        {/* Header Logo */}
        <div style={{ textAlign: "center", marginBottom: "24px" }}>
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
            Tạo tài khoản PhysLive
          </h1>
          <p style={{ margin: 0, fontSize: "13px", color: "#64748b" }}>
            Đăng ký để khám phá và tương tác với các phòng thí nghiệm vật lý
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
            }}
          >
            ⚠️ {error}
          </div>
        )}

        {/* Form */}
        <form onSubmit={handleSignup} style={{ display: "flex", flexDirection: "column", gap: "14px" }}>
          <div>
            <label style={{ display: "block", fontSize: "13px", fontWeight: 700, color: "#334155", marginBottom: "6px" }}>
              Họ và tên của bạn
            </label>
            <input
              type="text"
              placeholder="ví dụ: Nguyễn Thế Hùng"
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              style={{
                width: "100%",
                padding: "10px 14px",
                borderRadius: "10px",
                border: "1px solid #cbd5e1",
                fontSize: "14px",
                outline: "none",
                boxSizing: "border-box",
              }}
            />
          </div>

          <div>
            <label style={{ display: "block", fontSize: "13px", fontWeight: 700, color: "#334155", marginBottom: "6px" }}>
              Địa chỉ Email
            </label>
            <input
              type="email"
              placeholder="email@physlive.edu.vn"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              style={{
                width: "100%",
                padding: "10px 14px",
                borderRadius: "10px",
                border: "1px solid #cbd5e1",
                fontSize: "14px",
                outline: "none",
                boxSizing: "border-box",
              }}
            />
          </div>

          {/* Vai trò */}
          <div>
            <label style={{ display: "block", fontSize: "13px", fontWeight: 700, color: "#334155", marginBottom: "6px" }}>
              Vai trò của bạn
            </label>
            <div style={{ display: "flex", gap: "10px" }}>
              <label
                style={{
                  flex: 1,
                  padding: "10px",
                  borderRadius: "8px",
                  border: role === "TEACHER" ? "2px solid #2563eb" : "1px solid #cbd5e1",
                  backgroundColor: role === "TEACHER" ? "#eff6ff" : "#f8fafc",
                  cursor: "pointer",
                  textAlign: "center",
                  fontSize: "13px",
                  fontWeight: 700,
                  color: role === "TEACHER" ? "#1d4ed8" : "#475569",
                }}
              >
                <input
                  type="radio"
                  name="role"
                  value="TEACHER"
                  checked={role === "TEACHER"}
                  onChange={() => setRole("TEACHER")}
                  style={{ display: "none" }}
                />
                👨‍🏫 Giáo viên
              </label>

              <label
                style={{
                  flex: 1,
                  padding: "10px",
                  borderRadius: "8px",
                  border: role === "STUDENT" ? "2px solid #2563eb" : "1px solid #cbd5e1",
                  backgroundColor: role === "STUDENT" ? "#eff6ff" : "#f8fafc",
                  cursor: "pointer",
                  textAlign: "center",
                  fontSize: "13px",
                  fontWeight: 700,
                  color: role === "STUDENT" ? "#1d4ed8" : "#475569",
                }}
              >
                <input
                  type="radio"
                  name="role"
                  value="STUDENT"
                  checked={role === "STUDENT"}
                  onChange={() => setRole("STUDENT")}
                  style={{ display: "none" }}
                />
                🎒 Học sinh
              </label>
            </div>
          </div>

          <div>
            <label style={{ display: "block", fontSize: "13px", fontWeight: 700, color: "#334155", marginBottom: "6px" }}>
              Mật khẩu
            </label>
            <input
              type="password"
              placeholder="Tối thiểu 6 ký tự..."
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              style={{
                width: "100%",
                padding: "10px 14px",
                borderRadius: "10px",
                border: "1px solid #cbd5e1",
                fontSize: "14px",
                outline: "none",
                boxSizing: "border-box",
              }}
            />
          </div>

          <div>
            <label style={{ display: "block", fontSize: "13px", fontWeight: 700, color: "#334155", marginBottom: "6px" }}>
              Xác nhận mật khẩu
            </label>
            <input
              type="password"
              placeholder="Nhập lại mật khẩu..."
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              style={{
                width: "100%",
                padding: "10px 14px",
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
              marginTop: "8px",
              padding: "12px",
              backgroundColor: loading ? "#94a3b8" : "#2563eb",
              color: "#ffffff",
              border: "none",
              borderRadius: "10px",
              fontWeight: 700,
              fontSize: "14px",
              cursor: loading ? "not-allowed" : "pointer",
              boxShadow: "0 4px 14px rgba(37, 99, 235, 0.3)",
            }}
          >
            {loading ? "Đang tạo tài khoản..." : "Đăng ký ngay"}
          </button>
        </form>

        {/* Footer */}
        <div style={{ marginTop: "24px", textAlign: "center", fontSize: "13px", color: "#64748b" }}>
          Đã có tài khoản?{" "}
          <Link to="/login" style={{ color: "#2563eb", fontWeight: 700, textDecoration: "none" }}>
            Đăng nhập tại đây
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

export default Signup;
