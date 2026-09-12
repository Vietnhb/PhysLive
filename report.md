 # BÁO CÁO KẾT QUẢ TRIỂN KHAI FRONTEND WEB (REACT-CLIENT)
## DỰ ÁN PHYSLIVE — NỀN TẢNG MÔ PHỎNG VẬT LÝ TƯƠNG TÁC
> **Người thực hiện**: Nguyễn Thế Hùng (Mã SV: SE183807) — Vai trò: **Frontend Web Lead**  
> **Người Review Code**: Võ Trần Tuấn Kiệt (Platform / Flutter Lead)  
> **Đánh giá / Leader**: Nguyễn Hoàng Bảo Việt (Project Leader)  
> **Giảng viên hướng dẫn**: Thầy Lâm Hữu Khánh Phương  
> **Ngày báo cáo**: 12/09/2026 — Phiên bản: **Release Candidate v1.0 (Sprint 1)**

---

## 1. TỔNG QUAN KẾT QUẢ ĐÃ ĐẠT ĐƯỢC
Frontend Web (`react-client`) đã hoàn thành **100% khung giao diện và luồng nghiệp vụ tương tác** từ Task **H01 đến H10**, bám sát chặt chẽ theo:
1. Đề cương đăng ký Capstone gốc của Thầy Phương (*Capstone Project Register FA26SE309*).
2. Sơ đồ thực thể CSDL Supabase (*14 bảng ERD chuẩn hoá*).
3. Lưu ý từ Mentor trong buổi họp tuần 1 (*Phân cấp học liệu 4 tầng, lưu ghi chú bài giải trước của giáo viên, xuất các bước tính toán*).
4. Chuẩn mực thiết kế công nghệ cao lấy cảm hứng từ studio toán học hiện đại (*Alex Phan Creative — MathOS Workspace*).

---

## 2. CHI TIẾT KẾT QUẢ TRIỂN KHAI THEO 10 TASK (H01 - H10)

### 📌 Task H01: App Shell & Role Routing (Hoàn thành 100%)
* **Layout Studio**: Thanh Header kính mờ, chuyển đổi linh hoạt các không gian làm việc (`Phòng thí nghiệm Player`, `Nộp đề & OCR`, `Thư viện`, `Bài tập dự đoán`).
* **Quick Role Switcher (Bộ chuyển vai trò 1-Click)**: Tích hợp ngay trên Topbar, cho phép chuyển tức thì giữa 4 vai trò: `👨‍🏫 Giáo viên` $\leftrightarrow$ `🎒 Học sinh` $\leftrightarrow$ `🔍 Thẩm định` $\leftrightarrow$ `⚡ Quản trị` để test phân quyền mà không cần đăng nhập/đăng xuất thủ công.
* **Route Guard & UI States**: Bảo vệ định tuyến an toàn theo RoleType (hiển thị trang 403 thân thiện nếu không đủ quyền), có sẵn các component trạng thái: `LoadingSkeleton`, `EmptyState`, `ErrorBanner`.

### 📌 Task H02: Authentication & Profile (Hoàn thành 100%)
* **Cơ chế Xác thực Chuẩn (Strict Authentication)**:
  * Trạng thái `isAuthenticated` được tính toán động dựa trên JWT Token còn hạn và đối tượng người dùng hợp lệ (`currentUser != null`).
  * Tuyệt đối không hardcode `isAuthenticated: true` hay tự động đăng nhập nếu không có token hoặc token đã hết hạn.
  * Các route cần quyền (`/profile`, `/teacher/*`, `/assignments`, `/student/*`, v.v.) được bọc bởi `RouteGuard`, tự động chuyển hướng khách (Guest) về `/login`.
  * Topbar Header tự động đổi giao diện linh hoạt: hiển thị nút *"Đăng nhập / Đăng ký"* khi chưa có phiên đăng nhập, và hiển thị cụm *"Tên, Avatar, Bộ chuyển vai trò, Đăng xuất"* khi đã đăng nhập.
* **Trang Đăng nhập (`/login`)**:
  * Form đăng nhập kết nối API Backend thật (`/api/auth/login`).
  * Tích hợp sẵn cụm nút **"Đăng nhập nhanh Demo 1-Click"** (Thầy Hùng, Em Nam, Kiệt Reviewer, Admin) giúp test giao diện độc lập ngay cả khi Backend chưa bật.
* **Trang Đăng ký (`/signup`)**:
  * Form đăng ký chọn vai trò trực quan (Giáo viên / Học sinh), xác thực mật khẩu.
* **Trang Hồ sơ cá nhân (`/profile`)**:
  * Click trực tiếp vào Tên hoặc Avatar trên Header để mở (có RouteGuard bảo vệ).
  * Xem và cập nhật thông tin cá nhân, trường học, vai trò RBAC.
  * Form đổi mật khẩu (Mật khẩu cũ, mật khẩu mới, xác nhận).
  * Nút Đăng xuất an toàn (xoá Token và reset trạng thái người dùng).
* **Trang Quản trị Người Dùng & Phân quyền RBAC (`/admin`)**:
  * **List users**: Bảng danh sách toàn bộ tài khoản với đầy đủ thông tin (Họ tên, Email, Trường học, Vai trò, Trạng thái, Ngày tham gia). Kèm thanh tìm kiếm tức thì, bộ lọc theo vai trò (TEACHER, STUDENT, REVIEWER, ADMIN) và bộ lọc theo trạng thái (`active`, `disabled`), kèm 5 thẻ KPI thống kê tổng số lượng từng nhóm.
  * **View user**: Modal xem chi tiết hồ sơ tài khoản (User ID UUID, ngày khởi tạo, lần cập nhật cuối, quyền hạn phân vai trò).
  * **Disable user**: Nút Khóa tài khoản (chuyển sang trạng thái `disabled`, gắn badge đỏ, chặn không cho tự khóa tài khoản Admin của chính mình).
  * **Restore user**: Nút Khôi phục tài khoản (chuyển trạng thái trở lại `active`, hiển thị badge xanh lá).
  * **Change role**: Dropdown và Modal đổi vai trò RBAC tức thì cho từng tài khoản, lưu và đồng bộ state/localStorage/Backend API.
  * **Create user**: Modal thêm người dùng mới nhanh chóng.
* **Kiến trúc CSS Tinh gọn (Single Style Entrypoint)**:
  * Toàn bộ Style của hệ thống được quy hoạch duy nhất tại `src/styles/layout.css` và chỉ nạp 1 lần ở `src/main.tsx`.
  * Xoá bỏ hoàn toàn các file style rác cũ (`NavBar.css`, `Home.tsx`), chấm dứt triệt để tình trạng import style phân tán, lẫn lộn.

### 📌 Task H03: Teacher Submit UI (Hoàn thành 100%)
* **Đa dạng phương thức nhập**:
  * Nhập văn bản trực tiếp (Text).
  * Dán đề bài (Paste).
  * Kéo thả tải ảnh chụp bài tập từ SGK (Photo / OCR Dropzone).
* **Khung chỉnh sửa sau OCR**: Cho phép giáo viên rà soát và chỉnh sửa lại văn bản nhận diện trước khi phân tích.
* **Thanh tiến trình Understanding Engine**: Mô phỏng quá trình bóc tách thực thể qua 3 giai đoạn: *Đọc ngữ nghĩa bài toán $\to$ Chuẩn hoá đơn vị SI $\to$ Kiểm tra dữ kiện mơ hồ*.
* **Bảng xem trước đặc tả (Specification JSON)**: Liệt kê rõ các đại lượng đã cho ($v_0, \alpha, h, g$) và đại lượng cần tìm ($L_{max}, t_{flight}$).

### 📌 Task H04: Ambiguity Confirmation UI (Hoàn thành 100%)
* **Modal xử lý dữ liệu mơ hồ (Human-in-the-loop)**:
  * Khi đề bài thiếu giả thiết (ví dụ: góc ném $30^\circ$ chếch lên hay chúc xuống; có tính ma sát $\mu$ hay bỏ qua), hệ thống tự động bật Modal bắt buộc Giáo viên chọn phương án chuẩn.
  * **Quy tắc chặn nghiêm ngặt**: Khóa hoàn toàn nút *"Khởi tạo mô phỏng"* nếu còn cờ mơ hồ (`ambiguity_flags`) chưa được giải quyết.

### 📌 Task H05: Interactive Classroom Player (Hoàn thành 100% — Trọng tâm dự án)
* **Bố cục 3-Pane Studio chuyên nghiệp**:
  1. **Pane Trái (Catalog)**: Danh mục các mô hình bài toán có sẵn (Ném xiên từ độ cao $h$, Xe ô tô phanh chậm dần đều theo ví dụ của Thầy Phương, Khối hộp trượt dốc có ma sát).
  2. **Pane Giữa (Viewport)**:
     * *Dynamic Formula Live Bar*: Hiển thị công thức toán lý trực tiếp theo thời gian thực, tự động cập nhật số liệu khi kéo slider ($y(t) = 20 + 7.50t - 4.90t^2$, $v(t) = 20 - 4t$).
     * *Canvas Toạ độ Grid*: Vẽ quỹ đạo Parabol phát sáng, tháp ném, hình xe ô tô chuyển động, vệt bánh xe phanh màu đỏ, vector vận tốc $\vec{v}$ (xanh ngọc) và gia tốc hãm $\vec{a}$ (đỏ).
     * *Floating Playback Dock*: Nút Play/Pause tròn, thanh Scrubber tua thời gian, nút bước lặp `+0.1s` để giảng bài từng nhịp, nút chọn tốc độ ($0.5\times, 1\times, 2\times$).
     * *Real-time Metrics Dock*: 4 ô hiển thị chỉ số đo đạc tức thì ($t, v, x, y, L_{max}/s_{dừng}$).
  3. **Pane Phải (Inspector)**:
     * Bộ thanh trượt Sliders mượt mà kết hợp ô nhập số chính xác (`number input`).
     * Tab Lời giải chi tiết từng bước.
     * Tab Bảng số liệu biến thiên theo thời gian.
     * Tab Xuất dữ liệu.

### 📌 Task H06: Library & Draft Management (Hoàn thành 100%)
* Thư viện bài tập và mô phỏng cá nhân / dùng chung (`/library`).
* Hỗ trợ lưu bản nháp (Save Draft) cho đề bài đang soạn dở.
* Bộ lọc tìm kiếm theo chủ đề và từ khoá.

### 📌 Task H07: Assignment & Student Prediction Flow (Hoàn thành 100%)
* **Giao diện Giáo viên giao bài (`/assignments`)**:
  * Tạo bài tập gắn với từng Lớp học (`ClassLesson` - ví dụ: Lớp 10A1).
  * Tự động hiển thị **"Mô tả bài giải của giảng viên trong bài học trước"** (`explanation_notes`) để dặn dò học sinh.
  * Đặt câu hỏi thử thách dự đoán và hạn nộp bài.
* **Giao diện Học sinh làm bài (`/student/assignments`)**:
  * Xem lời dặn & tóm tắt kiến thức của Thầy giáo từ bài học trước.
  * **Bắt buộc nhập số liệu Dự đoán (Prediction)** trước khi hệ thống mở khóa mô phỏng.
  * Sau khi gửi dự đoán: Mở khóa phòng thí nghiệm Player, tự động tính toán đối chiếu 3 chỉ số: *Dự đoán của em* vs *Thực nghiệm máy tính* vs *Độ lệch sai số $\Delta\%$ (đánh giá Đạt chuẩn nếu sai số $\le 2\%$).

### 📌 Task H08: Export Features (Hoàn thành 100% — Bổ sung lưu ý Thầy Phương)
* **Xuất các bước tính toán (Calculation Steps)**:
  * Nút tải file Markdown (`.md`) chứa trọn vẹn giả thiết, công thức và từng bước thay số giải tích.
  * Nút xuất file văn bản thuần (`.txt`) để dán vào giáo án.
  * Chức năng **In / Lưu PDF báo cáo thực nghiệm** trực tiếp từ trình duyệt (`window.print()`).
* **Xuất gói JSON Replay Specification**: Gói dữ liệu tự trị đầy đủ tham số và kết quả để chạy lại offline hoặc đồng bộ sang Mobile Flutter.

### 📌 Task H09: Testing, Refactoring & Code Quality (Hoàn thành 100%)
* Mã nguồn được viết theo chuẩn **TypeScript nghiêm ngặt** (`verbatimModuleSyntax`, `strict: true`).
* Cấu trúc thư mục phân tách rõ ràng (Separation of Concerns): `api/`, `components/`, `context/`, `mocks/`, `pages/`, `styles/`, `types/`.
* Kết quả kiểm tra biên dịch Vite Production Build:
  ```bash
  ✓ 117 modules transformed.
  ✓ built in 718ms (0 errors, 0 warnings)
  ```

### 📌 Task H10: Curriculum Taxonomy Browser (Hoàn thành 100%)
* Cấu trúc phân cấp 4 tầng chuẩn học thuật theo chương trình SGK mới (Vật lí 10 & 11):
  $$\text{Topic} \xrightarrow{1-N} \text{Module} \xrightarrow{1-N} \text{Level} \xrightarrow{1-1} \text{ClassLesson}$$
* Gắn chặt với `teacher_id` và hỗ trợ trường ghi chú `explanation_notes`.

---

## 3. CẤU TRÚC THƯ MỤC MÃ NGUỒN FRONTEND
```
react-client/
├── .env.local                     # Cấu hình biến môi trường Vite Client (Mock flag, Supabase public)
├── index.html                     # Tối ưu SEO, Meta tags, Google Font Inter
├── src/
│   ├── api/                       # API clients (axios, authApi, userApi)
│   ├── components/
│   │   ├── common/                # RoleBadge, LoadingSkeleton, EmptyState, ErrorBanner
│   │   └── layout/                # Header (Workspace tabs + Role Switcher), AppLayout, RouteGuard
│   ├── config/
│   │   └── api.ts                 # Cấu hình URL endpoint & cờ IS_MOCK_ENABLED
│   ├── context/
│   │   └── AuthContext.tsx        # Quản lý phiên đăng nhập và Quick Role Switcher
│   ├── mocks/                     # Mock Data chuẩn ERD Supabase (Users, Taxonomy, Problems, Simulations, Assignments)
│   ├── pages/
│   │   ├── Dashboard.tsx          # Bảng điều khiển tổng quan
│   │   ├── Login.tsx              # Đăng nhập (kèm Demo Presets 1-Click)
│   │   ├── Signup.tsx             # Đăng ký tài khoản (chọn vai trò)
│   │   ├── profile/
│   │   │   └── ProfilePage.tsx    # Hồ sơ cá nhân & Đổi mật khẩu
│   │   ├── teacher/
│   │   │   ├── ProblemSubmitPage.tsx   # Workspace nộp đề bài (H03) + Modal mơ hồ (H04)
│   │   │   └── TeacherAssignmentPage.tsx # Quản lý giao bài tập theo lớp (H07)
│   │   ├── student/
│   │   │   └── StudentAssignmentsPage.tsx # Học sinh làm bài & dự đoán (H07)
│   │   ├── player/
│   │   │   └── SimulationPlayerPage.tsx # Interactive Classroom Player 3-Pane (H05 + H08)
│   │   ├── library/
│   │   │   └── LibraryPage.tsx    # Thư viện & Bộ lọc Taxonomy (H06 + H10)
│   │   └── reviewer/
│   │       └── SchemaReviewPage.tsx # Workspace thẩm định của Reviewer
│   └── styles/
│       └── layout.css             # Hệ thống Design Tokens & CSS Studio
```

---

## 4. HƯỚNG DẪN REVIEW & DEMO CHO TEAM
1. **Khởi động dự án**:
   ```bash
   cd react-client
   npm run dev
   ```
   Truy cập: `http://localhost:5173`
2. **Kịch bản Demo nhanh**:
   * **Bước 1**: Nhìn lên Header, bấm các nút `Demo Role` để chuyển đổi qua lại giữa `Giáo viên` và `Học sinh`.
   * **Bước 2 (Teacher Flow)**: Vào menu **Nộp đề & OCR** $\to$ Bấm *"Phân tích bài toán"* $\to$ Quan sát tiến trình bóc tách và Modal xử lý cờ mơ hồ $\to$ Bấm *"Khởi tạo mô phỏng"*.
   * **Bước 3 (Player Studio)**: Trải nghiệm Canvas ném xiên và xe ô tô phanh chậm dần đều, kéo các thanh trượt Sliders để xem công thức và đồ thị nhảy thời gian thực $\to$ Vào tab *"Xuất"* để tải file Markdown hoặc in báo cáo PDF.
   * **Bước 4 (Assignment Flow)**: Vào **Quản trị giao bài** (Teacher) để xem bài tập đã giao $\to$ Chuyển role sang Học sinh $\to$ Vào **Thử thách dự đoán** để nhập số liệu dự đoán và mở khóa mô phỏng đối chiếu thực nghiệm.
   * **Bước 5 (Auth & Profile)**: Click vào Tên/Avatar trên Header để xem trang Hồ sơ cá nhân hoặc bấm *"Đăng nhập"* để xem giao diện Login/Signup mới.

---
*Báo cáo được chuẩn bị bởi **Nguyễn Thế Hùng (Frontend Web Lead)** để trình Leader và Reviewer trước kỳ họp đánh giá.*
