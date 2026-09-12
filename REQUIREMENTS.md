# PHYSLIVE — YÊU CẦU & KẾ HOẠCH TRIỂN KHAI FRONTEND (H01 - H10)

> **Dự án**: PhysLive — Nền tảng Mô phỏng Vật lý Tương tác Đa nền tảng  
> **Người đảm nhận**: Nguyễn Thế Hùng (Vai trò: **Frontend Web Lead — ReactJS**)  
> **Người Review Code**: Tuấn Kiệt (Platform / Flutter / Release)  
> **Phiên bản tài liệu**: 1.0 — Sprint MVP (30 ngày)

---

## 1. TỔNG QUAN VAI TRÒ & PHẠM VI CỦA HÙNG
Với vai trò **Frontend Web Lead**, Hùng chịu trách nhiệm chính về toàn bộ trải nghiệm người dùng (UI/UX) trên nền tảng Web:
* Xây dựng giao diện tương tác cho **Giáo viên (Teacher)**: Nộp đề bài, sửa lỗi OCR, giải quyết dữ liệu mơ hồ, quản trị thư viện bài giảng.
* Xây dựng giao diện cho **Học sinh (Student)**: Xem bài tập, nhập số liệu dự đoán (Prediction Sandbox), thực nghiệm mô phỏng.
* Xây dựng trung tâm điều khiển **Interactive Classroom Player (Task H05)**: Trái tim của hệ thống, mô phỏng chuyển động vật lý, điều khiển thanh trượt (sliders), đồ thị động và lời giải từng bước.
* Xây dựng cơ chế **Mock Data tự chủ**: Cho phép phát triển và kiểm thử giao diện độc lập mà không bị gián đoạn hay phụ thuộc vào tiến độ Backend của Kiệt, Việt và Minh.

---

## 2. BẢNG TIẾN ĐỘ VÀ CHI TIẾT CÁC TASK (H01 - H10)

| Mã Task | Tên Task | Mô tả chi tiết nghiệp vụ | Trạng thái hiện tại |
| :--- | :--- | :--- | :---: |
| **H01** | **App Shell & Role Routing** | Khung Studio chuẩn MathOS, Header điều hướng Workspace, Quick Role Switcher, Route Guard bảo vệ theo vai trò, các trạng thái `LoadingSkeleton`, `EmptyState`, `ErrorBanner`. | <span style="color:green">**HOÀN THÀNH 100%**</span> |
| **H02** | **Authentication & Profile** | Màn hình Đăng nhập (Login), Đăng ký (Signup) kèm 4 nút Đăng nhập nhanh Demo 1-Click; Trang **Hồ sơ cá nhân (Profile)** cập nhật thông tin & Đổi mật khẩu. | <span style="color:green">**HOÀN THÀNH 100%**</span> |
| **H03** | **Teacher Submit UI** | Workspace cho Giáo viên: Nhập text, dán văn bản, kéo thả ảnh chụp bài tập (Photo OCR Dropzone), sửa văn bản sau OCR, thanh tiến trình Understanding Engine. | <span style="color:green">**HOÀN THÀNH 100%**</span> |
| **H04** | **Ambiguity Confirmation UI** | Modal giải quyết dữ liệu mơ hồ, bắt buộc giáo viên xác nhận các giả thiết thiếu trước khi tạo mô phỏng (Human-in-the-loop). | <span style="color:green">**HOÀN THÀNH 100%**</span> |
| **H05** | **Interactive Classroom Player** | **Bố cục 3-Pane Studio**: Formula Live Bar động, Canvas toạ độ Grid vẽ vector $\vec{v}, \vec{a}$, vệt phanh xe ô tô, tháp ném xiên, Floating Controls, Metrics Dock và Sliders thời gian thực. | <span style="color:green">**HOÀN THÀNH 100%**</span> |
| **H06** | **Library & Draft Management** | Thư viện quản lý mô phỏng cá nhân và chia sẻ chung, lưu bản nháp (Draft), tìm kiếm đề bài theo chủ đề SGK 10 & 11. | <span style="color:green">**HOÀN THÀNH 100%**</span> |
| **H07** | **Assignment & Student Flow** | Luồng giao bài của Giáo viên theo lớp học (`ClassLesson`) kèm lời dặn giải bài trước; Luồng Học sinh bắt buộc **dự đoán trước** rồi mới mở khóa mô phỏng và đối chiếu sai số. | <span style="color:green">**HOÀN THÀNH 100%**</span> |
| **H08** | **Export Features** | Xuất gói dữ liệu tự trị **JSON Replay Specification** để chạy offline trên web hoặc Flutter, xuất báo cáo thực nghiệm PDF. | <span style="color:green">**HOÀN THÀNH 100%**</span> |
| **H09** | **Testing & Bugfix** | Biên dịch TypeScript & Vite build sạch sẽ 100% (**0 errors, 0 warnings**), kiểm tra responsive mượt mà trên Desktop/Laptop. | <span style="color:green">**HOÀN THÀNH 100%**</span> |
| **H10** | **Taxonomy Browser & Filter** | Bộ lọc phân cấp học liệu chuẩn 4 tầng của Mentor: `Topic → Module → Level → ClassLesson` theo chương trình SGK Vật lí 10 & 11. | <span style="color:green">**HOÀN THÀNH 100%**</span> |

---

## 3. CÁC TÀI NGUYÊN & CẤU TRÚC ĐÃ TRIỂN KHAI
```
react-client/
├── .env.local                     # Biến môi trường Vite (Supabase Public Key, API URL, Mock flag)
├── src/
│   ├── types/
│   │   └── database.ts            # Định nghĩa Typescript chuẩn 14 bảng ERD Supabase
│   ├── mocks/                     # Bộ Mock Data chạy độc lập
│   │   ├── mockUsers.ts           # Tài khoản mẫu Teacher Hùng, Student Nam, Reviewer Kiệt, Admin
│   │   ├── mockTaxonomy.ts        # 3 Chủ đề: Kinematics, Dynamics, Circuits
│   │   ├── mockProblems.ts        # Bài toán ném xiên & trượt dốc kèm cờ mơ hồ
│   │   ├── mockSimulations.ts     # Dữ liệu mô phỏng, sliders, công thức giải từng bước
│   │   ├── mockAssignments.ts     # Dữ liệu bài tập giao và học sinh dự đoán
│   │   └── index.ts
│   ├── context/
│   │   └── AuthContext.tsx        # Quản lý phiên và Quick Role Switcher
│   ├── components/
│   │   ├── common/                # RoleBadge, LoadingSkeleton, EmptyState, ErrorBanner
│   │   └── layout/                # AppLayout, Header, Sidebar, RouteGuard
│   ├── pages/
│   │   ├── Dashboard.tsx          # Bảng điều khiển trung tâm
│   │   ├── teacher/
│   │   │   └── ProblemSubmitPage.tsx  # Workspace nộp bài (H03) + Modal mơ hồ (H04)
│   │   ├── player/
│   │   │   └── SimulationPlayerPage.tsx # Phòng thí nghiệm Classroom Player (H05)
│   │   ├── library/
│   │   │   └── LibraryPage.tsx    # Thư viện & Bộ lọc Taxonomy (H06 + H10)
│   │   ├── student/
│   │   │   └── StudentAssignmentsPage.tsx # Bài tập học sinh dự đoán (H07)
│   │   └── reviewer/
│   │       └── SchemaReviewPage.tsx # Workspace thẩm định của Kiệt & Minh
│   └── styles/
│       └── layout.css             # Thiết kế hiện đại chuẩn PhysLive Studio
```

---

## 4. QUY TẮC PHỐI HỢP & DEFINITION OF DONE (DoD)
1. **Branching**:
   * Không bao giờ commit trực tiếp vào `main` hoặc `develop`.
   * Tạo nhánh theo mã task: `feat/H03-submit-ui`, `feat/H05-player-ui`.
   * Tạo PR trỏ vào `develop`, gán **Kiệt** làm người Reviewer.
2. **Tiêu chuẩn UI**:
   * Luôn có đầy đủ các trạng thái: Đang tải (`loading`), Lỗi kết nối (`error`), Danh sách trống (`empty`), và Xác thực dữ liệu đầu vào (`validation`).
   * Không hard-code URL hoặc để lộ secret keys trong mã nguồn.
