# PHYSLIVE — UI/UX DESIGN SYSTEM & IMPLEMENTATION GUIDE

Tài liệu này tổng hợp toàn bộ quy chuẩn thiết kế UI/UX, bảng màu, typography, hệ thống layout và các React Component mẫu được trích xuất từ dự án **PhysLive (react-client)**. Bạn có thể sử dụng file này làm **"Kim chỉ nam thiết kế"** để tích hợp và chuẩn hóa giao diện cho dự án chính của nhóm.

---

## 1. Triết lý Thiết kế (Design Philosophy)

- **Phong cách**: Hiện đại, tối giản theo phong cách EdTech chuyên nghiệp (Google Workspace / Material Design kết hợp Linear/Stripe aesthetic).
- **Cảm quan**: Gọn gàng, thoáng đãng, phân cấp thị giác rõ ràng (Visual Hierarchy), độ tương phản tốt, bo góc mềm mại (8px - 14px).
- **Trọng tâm tương tác AI**: Minh bạch các bước xử lý (stepper 3 giai đoạn), có cảnh báo thông minh khi đề bài mơ hồ (ambiguity box), và trạng thái rành mạch (status pills).

---

## 2. Bảng màu chuẩn (Color Palette & Design Tokens)

### 2.1 Màu chủ đạo & Thương hiệu (Brand & Accents)
| Tên biến | Mã màu HEX | Ứng dụng |
| :--- | :--- | :--- |
| **Primary Accent** | `#1a73e8` | Nút chính (CTA), link kích hoạt, icon active, đường viền tiêu điểm |
| **Primary Hover** | `#1765cc` | Trạng thái hover của nút chính |
| **Accent Soft** | `#eef3fe` hoặc `#e8f0fe` | Nền nút phụ (secondary), badge xanh nhạt, highlight |
| **Accent Text Dark** | `#174ea6` hoặc `#1967d2` | Chữ trên nền xanh nhạt, tiêu đề mục con |
| **Focus Ring** | `#8ab4f8` | Viền focus cho accessibility (outline 3px) |

### 2.2 Màu nền & Bề mặt (Surfaces & Backgrounds)
| Tên biến | Mã màu HEX | Ứng dụng |
| :--- | :--- | :--- |
| **App Background** | `#f8fafd` | Nền trang tổng thể (xám xanh nhạt, tạo chiều sâu so với nền trắng tinh) |
| **Card Surface** | `#ffffff` | Nền thẻ card, form nhập liệu, panel nội dung |
| **Surface Muted** | `#f1f3f4` / `#edf0f2` | Nền item danh sách, đường kẻ ngăn cách |
| **Card Border** | `#e1e5eb` | Viền thẻ card (border 1px mỏng) |
| **Input Border** | `#bdc1c6` | Viền ô input, textarea, select |

### 2.3 Màu văn bản (Typography Colors)
| Phân loại | Mã màu HEX | Ứng dụng |
| :--- | :--- | :--- |
| **Text Primary** | `#202124` | Tiêu đề chính, nội dung văn bản quan trọng |
| **Text Secondary** | `#5f6368` | Đoạn mô tả phụ, nhãn phụ, placeholder |
| **Text Muted** | `#80868b` | Ghi chú nhỏ, timestamp, đơn vị đo |

### 2.4 Màu trạng thái (Status Colors)
| Trạng thái | Nền (Background) | Màu chữ (Text/Icon) | Ứng dụng |
| :--- | :--- | :--- | :--- |
| **Success (Tốt)** | `#e6f4ea` | `#137333` | Hoàn thành, đã kiểm chứng, solver khớp |
| **Warning (Cảnh báo/Mơ hồ)** | `#fef7e0` | `#b06000` | Cần bổ sung giả thiết, đang chờ review |
| **Danger (Lỗi)** | `#fce8e6` | `#c5221f` | Dữ liệu sai lệch, lỗi hệ thống |

---

## 3. Typography & Quy tắc Font chữ

```css
:root {
  font-family: "Google Sans", Roboto, Inter, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
  color: #202124;
  background: #f8fafd;
  line-height: 1.55;
}
```

- **H1 (Hero Title)**: `font-size: clamp(32px, 4vw, 52px); letter-spacing: -0.045em; line-height: 1.08; font-weight: 800;`
- **H2 (Section Title)**: `font-size: 20px; letter-spacing: -0.02em; font-weight: 700;`
- **H3 (Card Title)**: `font-size: 16px; font-weight: 700;`
- **Eyebrow (Nhãn dẫn trên tiêu đề)**: `font-size: 11px; text-transform: uppercase; letter-spacing: 0.12em; font-weight: 800; color: #1967d2;`
- **Body**: `font-size: 14px - 15px; color: #202124;`

---

## 4. Kiến trúc Layout (Layout Architecture)

Dự án sử dụng 2 mô hình Layout chính:

### Mô hình 1: Standard Dashboard Layout (`.shell`)
Thích hợp cho: *Trang Thư viện, Quản trị, Bài tập, Chương trình học.*
- **Sidebar bên trái cố định**: Chiều rộng `248px`, viền phải `1px solid #e1e5eb`, gồm Logo, Menu phân nhóm, Badge phiên bản Engine, Profile.
- **Vùng làm việc chính (`.main`)**: Margin trái `248px`, chiều rộng tối đa `1240px`, padding `40px 28px 72px`.

### Mô hình 2: Fullscreen Studio Workspace (`.full-shell`)
Thích hợp cho: *Trang giải bài toán AI, Mô phỏng Vật lý, Thí nghiệm.*
- **Không gian tràn viền**: `height: 100vh; width: 100vw; overflow: hidden;`
- **Chia 2 cột (Split Screen)**:
  - **Cột trái (Input / Prompt Panel - ~40%)**: Ô nhập đề bài, nút chọn ví dụ mẫu nhanh (Chips), quy trình stepper 3 bước.
  - **Cột phải (Visualizer / Solution Panel - ~60%)**: Khung hiển thị mô phỏng 2D/3D (Canvas/SVG), công thức Latex/Math, bảng thông số và đồ thị.

---

## 5. Thư viện CSS Utility Classes cốt lõi (Có thể dùng ngay)

```css
/* Card trắng bo góc cao cấp */
.card {
  background: #fff;
  border: 1px solid #e1e5eb;
  border-radius: 14px;
  padding: 24px;
  box-shadow: 0 1px 2px rgba(32, 33, 36, 0.05);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}
.card:hover {
  border-color: #d2d8e2;
}

/* Nút bấm (Buttons) */
button {
  min-height: 40px;
  border: 0;
  border-radius: 8px;
  padding: 10px 16px;
  cursor: pointer;
  background: #1a73e8;
  color: #fff;
  font-weight: 700;
  font-size: 14px;
  transition: background-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}
button:hover:not(:disabled) {
  background: #1765cc;
  box-shadow: 0 2px 5px rgba(26, 115, 232, 0.2);
}
button:active:not(:disabled) {
  transform: translateY(1px);
}
button.secondary, .secondary {
  background: #eef3fe;
  color: #174ea6;
}
button.secondary:hover:not(:disabled), .secondary:hover {
  background: #e2ebfd;
}

/* Form Fields */
.field {
  display: grid;
  gap: 8px;
  margin: 16px 0;
}
.field label {
  font-size: 13px;
  font-weight: 700;
  color: #3c4043;
}
textarea, input, select {
  width: 100%;
  border: 1px solid #bdc1c6;
  border-radius: 8px;
  padding: 12px 13px;
  background: #fff;
  color: #202124;
  font-size: 14px;
  box-sizing: border-box;
}
textarea:focus, input:focus, select:focus {
  outline: 3px solid #8ab4f8;
  border-color: #1a73e8;
}

/* Status Pill (Viên thuốc trạng thái) */
.status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border-radius: 999px;
  padding: 6px 10px;
  font-size: 12px;
  font-weight: 700;
  background: #e6f4ea;
  color: #137333;
}
.status::before {
  content: "";
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}
.status.warn { background: #fef7e0; color: #b06000; }
.status.bad { background: #fce8e6; color: #c5221f; }

/* Hộp cảnh báo đề bài mơ hồ / Ambiguity Warning Box */
.ambiguity {
  background: #fffaf0;
  border: 1px solid #f6d98b;
  border-radius: 10px;
  padding: 14px;
  margin: 12px 0;
  color: #5f4100;
  font-size: 13px;
}

/* Tiến trình 3 bước (Stepper list) */
.step-list {
  display: grid;
  gap: 12px;
  margin: 0;
  padding: 0;
  list-style: none;
}
.step-list li {
  display: grid;
  grid-template-columns: 32px 1fr;
  gap: 12px;
  align-items: center;
  color: #5f6368;
  font-size: 14px;
}
.step-list li strong {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #e8f0fe;
  color: #1967d2;
  font-size: 12px;
}
.step-list li span {
  display: block;
  color: #202124;
  font-weight: 650;
}
```

---

## 6. Mẫu Cấu trúc Component React (JSX Reference)

### 6.1 Cấu trúc Sidebar Navigation mẫu (`NavBar.tsx`)
```tsx
import { NavLink } from "react-router-dom";

export default function Sidebar() {
  return (
    <aside style={{ width: 248, position: "fixed", top: 0, bottom: 0, left: 0, background: "#fff", borderRight: "1px solid #e1e5eb", padding: "24px 16px" }}>
      <div style={{ padding: "0 12px 24px", display: "flex", alignItems: "center", gap: 10 }}>
        <div style={{ width: 32, height: 32, borderRadius: 8, background: "#1a73e8", color: "#fff", display: "grid", placeItems: "center", fontWeight: "bold" }}>P</div>
        <span style={{ fontWeight: 800, fontSize: 18, color: "#202124" }}>PhysLive Studio</span>
      </div>

      <nav style={{ display: "flex", flexDirection: "column", gap: 4 }}>
        <NavLink to="/workspace" className={({ isActive }) => `nav-link ${isActive ? "active" : ""}`}>
          🎯 Không gian làm việc
        </NavLink>
        <NavLink to="/library" className={({ isActive }) => `nav-link ${isActive ? "active" : ""}`}>
          📚 Thư viện mô hình
        </NavLink>
        <NavLink to="/curriculum" className={({ isActive }) => `nav-link ${isActive ? "active" : ""}`}>
          📋 Chương trình học
        </NavLink>
      </nav>

      {/* Backend / Engine Status badge */}
      <div style={{ position: "absolute", bottom: 20, left: 16, right: 16, padding: 12, borderRadius: 8, background: "#f8fafd", border: "1px solid #edf0f2" }}>
        <div style={{ fontSize: 11, fontWeight: 700, color: "#5f6368" }}>AI & SOLVER ENGINE</div>
        <div style={{ display: "flex", alignItems: "center", gap: 6, marginTop: 4 }}>
          <span className="status">Sẵn sàng</span>
        </div>
      </div>
    </aside>
  );
}
```

### 6.2 Cấu trúc Workspace 2 cột (`Workspace.tsx`)
```tsx
export default function WorkspacePage() {
  return (
    <div style={{ display: "grid", gridTemplateColumns: "420px 1fr", height: "100vh", overflow: "hidden" }}>
      {/* Cột trái: Nhập bài toán & Các bước AI */}
      <div style={{ background: "#fff", borderRight: "1px solid #e1e5eb", padding: 24, overflowY: "auto" }}>
        <span className="eyebrow">Studio Giải Toán & Mô Phỏng</span>
        <h2 style={{ marginTop: 6 }}>Nhập đề bài</h2>
        
        <div className="field">
          <textarea placeholder="Ví dụ: Một ô tô chuyển động nhanh dần đều với vận tốc ban đầu 5 m/s, gia tốc 2 m/s²..." rows={6} />
        </div>

        <div style={{ display: "flex", gap: 8, marginBottom: 20 }}>
          <button type="button">🚀 Tạo mô phỏng</button>
          <button type="button" className="secondary">Ví dụ mẫu</button>
        </div>

        {/* 3-Step Process List */}
        <h3 style={{ fontSize: 14, color: "#5f6368", textTransform: "uppercase", letterSpacing: "0.05em" }}>Quy trình xử lý AI</h3>
        <ul className="step-list">
          <li><strong>1</strong><span>AI hiểu đề & trích xuất đại lượng</span></li>
          <li><strong>2</strong><span>Xác nhận giả thiết & mô hình toán</span></li>
          <li><strong>3</strong><span>Dựng mô phỏng chuyển động trực quan</span></li>
        </ul>
      </div>

      {/* Cột phải: Không gian hiển thị mô phỏng / kết quả */}
      <div style={{ background: "#f8fafd", padding: 28, overflowY: "auto", display: "flex", flexDirection: "column", gap: 20 }}>
        <div className="card">
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <h3>Khung nhìn mô phỏng</h3>
            <span className="status">2D Simulation</span>
          </div>
          <div style={{ height: 380, background: "#f1f3f4", borderRadius: 8, marginTop: 16, display: "grid", placeItems: "center", color: "#5f6368" }}>
            [ Canvas Render Mô Phỏng Vật Lý ]
          </div>
        </div>
      </div>
    </div>
  );
}
```

### 6.3 Cấu trúc Form Sửa thông tin tài khoản & Bảng Quản lý Admin (`AdminConsole.tsx`)
```tsx
/* Form Chỉnh sửa thông tin tài khoản (Dạng 2 cột gọn gàng) */
<form className="ops-form" onSubmit={handleSaveUser}>
  <h3>Chỉnh sửa thông tin · {user.email}</h3>
  
  <div className="ops-form-grid" style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "0 18px" }}>
    <label className="ops-field">
      Họ và tên
      <input name="fullName" defaultValue={user.fullName} required maxLength={200} />
    </label>

    <label className="ops-field">
      Vai trò (Role)
      <select name="role" defaultValue={user.role}>
        <option value="TEACHER">TEACHER (Giáo viên)</option>
        <option value="REVIEWER">REVIEWER (Kiểm duyệt viên)</option>
        <option value="STUDENT">STUDENT (Học sinh)</option>
        <option value="ADMIN">ADMIN (Quản trị viên)</option>
      </select>
    </label>

    <label className="ops-field">
      Trường học / Đơn vị
      <select name="institutionId" defaultValue={user.institutionId}>
        <option value="">Không liên kết</option>
        <option value="sch-1">THPT FPT Hà Nội</option>
        <option value="sch-2">THPT FPT Cần Thơ</option>
      </select>
    </label>

    <label className="ops-field">
      Mật khẩu mới (Nếu cần đổi)
      <input type="password" name="password" minLength={8} placeholder="Để trống nếu giữ nguyên" />
    </label>
  </div>

  <div className="ops-actions" style={{ display: "flex", gap: 8, marginTop: 16 }}>
    <button type="submit">Lưu tài khoản</button>
    <button type="button" className="secondary" onClick={() => setEditing(null)}>Hủy</button>
  </div>
</form>

/* Bảng danh sách tài khoản */
<div className="table-wrap">
  <table>
    <thead>
      <tr>
        <th>Thành viên</th>
        <th>Vai trò</th>
        <th>Trường học</th>
        <th>Trạng thái</th>
        <th>Thao tác</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <td><strong>Nguyễn Văn A</strong><small>nguyenvana@fpt.edu.vn</small></td>
        <td>TEACHER</td>
        <td>THPT FPT Hà Nội</td>
        <td><span className="ops-badge good">Hoạt động</span></td>
        <td>
          <div className="ops-actions">
            <button className="secondary" onClick={() => setEditing(u)}>Sửa</button>
            <button className="danger">Tạm khóa</button>
          </div>
        </td>
      </tr>
    </tbody>
  </table>
</div>
```

### 6.4 Cấu trúc Màn hình Đăng nhập & Đăng ký (`Login.tsx` / `Signup.tsx`)
```tsx
<main className="auth-page">
  <form className="auth-card" onSubmit={handleSubmit}>
    <p className="auth-kicker">PhysLive workspace</p>
    <h1>Chào mừng trở lại</h1>
    <p className="muted">Đăng nhập để tiếp tục xây dựng và quản trị nền tảng.</p>

    <div className="field">
      <label htmlFor="login-email">Email</label>
      <input id="login-email" type="email" placeholder="you@example.com" required />
    </div>

    <div className="field">
      <label htmlFor="login-password">Mật khẩu</label>
      <input id="login-password" type="password" placeholder="Nhập mật khẩu" required />
    </div>

    <div className="actions">
      <button type="submit">Đăng nhập</button>
      <button type="button" className="secondary" onClick={() => navigate("/")}>Trang chủ</button>
    </div>

    <p className="auth-footer">
      Chưa có tài khoản? <a href="/signup">Đăng ký ngay</a>
    </p>
  </form>
</main>
```



