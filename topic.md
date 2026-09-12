> **Tên chính thức (English)**: PhysLive – Interactive Physics Simulation Platform for High School Teachers  
> **Tên tiếng Việt**: PhysLive – Nền tảng mô phỏng vật lý tương tác dành cho giáo viên trung học phổ thông  
> **Mã đồ án**: FA26SE309 — Capstone Project (09/2026 – 03/2027)  
> **Giảng viên hướng dẫn**: Thầy Lâm Hữu Khánh Phương (phuonglhk@fpt.edu.vn)  
> **Nhóm sinh viên thực hiện**:  
> 1. Nguyễn Hoàng Bảo Việt (SE183457) — Leader  
> 2. Trần Bảo Minh (SE184103) — Member  
> 3. Nguyễn Thế Hùng (SE183807) — Member (Frontend Web Lead)  
> 4. Võ Trần Tuấn Kiệt (SE182808) — Member (Platform / Flutter Lead)  

---

## 1. TẦM NHÌN & MỤC TIÊU CỐT LÕI
PhysLive ra đời nhằm giải quyết bài toán: **Học sinh và giáo viên phổ thông thường gặp khó khăn khi hình dung các hiện tượng vật lý trừu tượng từ những đề bài chữ tĩnh trên sách vở.**

PhysLive biến bất kỳ bài toán Vật lý nào (được **nhập tay**, **dán văn bản**, hoặc **chụp ảnh từ sách giáo khoa/đề thi**) thành một **mô phỏng tương tác 2D/3D sinh động**, có thể thay đổi thông số trực tiếp và đã được **kiểm chứng toán lý nghiêm ngặt**.

---

## 2. 4 TRỤ CỘT HỆ THỐNG (4 CORE PILLARS)

```
[ Đề bài (Text/Ảnh) ]
         │
         ▼
┌─────────────────────────────────┐
│ 1. Problem Understanding Engine │ ──▶ Nhận diện thực thể, đại lượng, đơn vị, liên hệ
└─────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────┐
│ 2. Ambiguity Confirmation       │ ──▶ Phát hiện dữ kiện mơ hồ -> Giáo viên xác nhận
└─────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────┐
│ 3. Simulation & Validation Core │ ──▶ Đối chiếu 2 bộ giải (Numerical & Closed-form)
└─────────────────────────────────┘      Chỉ PASS (sai số ≤ tolerance) mới tạo mô phỏng
         │
         ▼
┌─────────────────────────────────┐
│ 4. Interactive Classroom Player │ ──▶ Canvas tương tác, Sliders thời gian thực,
└─────────────────────────────────┘      đồ thị, vector lực/vận tốc, học sinh dự đoán
```

1. **Problem Understanding Engine (Việt)**:
   * Tiếp nhận văn bản hoặc ảnh chụp bài toán.
   * Sử dụng OCR và mô hình ngôn ngữ lớn (LLM Adapter kết hợp tập luật Fallback).
   * Chuẩn hoá đơn vị SI, bóc tách đại lượng cho trước và đại lượng cần tìm.
2. **Ambiguity Confirmation (Việt & Hùng)**:
   * Cơ chế "Human-in-the-loop": Khi đề bài thiếu giả thiết (ví dụ: góc ném chưa rõ hướng, bỏ qua hay tính ma sát), hệ thống không tự ý đoán mò mà bật cảnh báo để Giáo viên xác nhận.
   * **Quy tắc bất di bất dịch**: Nếu còn dữ kiện mơ hồ chưa giải quyết, hệ thống **chặn tuyệt đối** không cho sinh mô phỏng.
3. **Simulation & Validation Core (Minh)**:
   * Cơ chế đối chiếu hai lõi song song (**Dual-Validation**):
     * *Numerical Solver*: Giải phương trình vi phân/số học bằng giải thuật bước lặp (Euler/Runge-Kutta).
     * *Closed-form Solver*: Công thức giải tích nghiệm đóng để kiểm tra chéo.
   * Tính toán độ lệch (`discrepancy`). Nếu sai số vượt quá ngưỡng cho phép (`tolerance`), mô phỏng bị từ chối và ghi log cảnh báo lỗi vật lý.
4. **Interactive Classroom Player (Hùng & Kiệt)**:
   * Trình điều khiển tương tác trực quan (Web React & Mobile Flutter):
   * Thanh trượt tham số (Sliders) thay đổi giá trị bài toán theo thời gian thực.
   * Hiển thị vector chuyển động (vận tốc $\vec{v}$, gia tốc $\vec{g}$, lực $\vec{F}$).
   * Cơ chế **Dự đoán trước (Prediction Flow)** dành cho học sinh: kích thích tư duy suy đoán khoa học trước khi xem thực nghiệm máy tính chạy.

---

## 3. PHẠM VI 3 CHỦ ĐỀ CỐT LÕI (MVP BASELINE)
Hệ thống tập trung chuyên sâu vào 3 chủ đề nền tảng của chương trình Vật lý phổ thông mới, mỗi chủ đề có tối thiểu 2 mô hình mẫu (archetypes):

| Chủ đề (Topic) | Mã | Các mô hình điển hình (Archetypes) | Hiện tượng & Đại lượng tương tác |
| :--- | :---: | :--- | :--- |
| **Động học** *(Kinematics)* | `KINEMATICS` | • Chuyển động ném xiên / ném ngang<br/>• Xe ô tô phanh chậm dần đều | Tọa độ $x(t), y(t)$, vận tốc ban đầu $v_0$ (tự động đổi $72\text{ km/h} \to 20\text{ m/s}$), gia tốc $a = -4\text{ m/s}^2$, thời gian dừng $5\text{s}$, quãng đường phanh $50\text{m}$, tầm xa $L_{max}$, độ cao $H_{max}$, vector vận tốc tức thời. |
| **Động lực học** *(Dynamics)* | `DYNAMICS` | • Các định luật Newton & Lực cản/ma sát<br/>• Vật trượt trên mặt phẳng nghiêng | Khối lượng $m$, hệ số ma sát $\mu$, góc nghiêng $\alpha$, phản lực pháp tuyến $\vec{N}$, trọng lực $\vec{P}$, gia tốc trượt. |
| **Mạch điện** *(Circuits)* | `CIRCUITS` | • Mạch điện một chiều định luật Ôm toàn mạch<br/>• Mạch cầu điện trở & Ghép nguồn | Suất điện động $\mathcal{E}$, điện trở trong $r$, biến trở $R$, cường độ dòng điện $I$, hiệu điện thế $U$, công suất $P$. |

---

## 4. KIẾN TRÚC CÔNG NGHỆ (TECH STACK)
* **Frontend Web**: ReactJS (Vite + TypeScript + React Router v7). Giao diện phòng thí nghiệm tương tác (Studio UI), Canvas rendering, MathJax/LaTeX, biểu đồ Chart.
* **Backend RESTful API**: Java Spring Boot (Maven) quản lý nghiệp vụ, thuật toán giải toán vật lý và pipeline xử lý đề bài.
* **Database & Authentication**: Supabase (PostgreSQL), kết nối qua PgBouncer, phân quyền RBAC đa vai trò (Teacher, Student, Reviewer, Admin).
* **AI & OCR Engine**: OpenRouter API tích hợp LLM mô hình thị giác và xử lý ngôn ngữ tự nhiên.
* **Mobile Client**: Flutter (Android) hỗ trợ trải nghiệm di động màn hình ngang (Landscape) và xem lại offline bằng dữ liệu JSON Replay.

---

## 5. PHÂN CÔNG TRÁCH NHIỆM THÀNH VIÊN
* **Việt (Understanding Engine / Data Contract)**: Đảm nhận trích xuất thực thể, OCR, xử lý dữ liệu mơ hồ, cấu trúc dữ liệu phân cấp học liệu và bộ dữ liệu Benchmark.
* **Minh (Physics / Validation Backend)**: Đảm nhận các Solvers số học và giải tích cho 3 chủ đề, cơ chế Dual-Validation, thuật toán kiểm chứng sai số.
* **Hùng (React Web Lead)**: Đảm nhận toàn bộ giao diện Web, App Shell, Workspace nộp đề bài, Interactive Classroom Player, Thư viện, Bài tập dự đoán và Xuất báo cáo.
* **Kiệt (Platform / Flutter / Release Lead)**: Đảm nhận Auth/RBAC, Spring Boot API nền tảng, Ứng dụng di động Flutter Android, CI/CD và quy trình Release.
