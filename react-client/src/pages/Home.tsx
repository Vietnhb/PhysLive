import React from 'react';
import { useNavigate } from 'react-router-dom';
import { MOCK_SIMULATIONS } from '../mocks/mockSimulations';
import {
  Sparkles,
  ArrowRight,
  FlaskConical,
  LineChart,
  BrainCircuit,
  ShieldCheck,
  GraduationCap,
  BookOpen,
  CheckCircle2,
} from 'lucide-react';

export const Home: React.FC = () => {
  const navigate = useNavigate();

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '56px', paddingBottom: '48px' }}>
      {/* 1. HERO SECTION */}
      <section
        style={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          textAlign: 'center',
          padding: '48px 16px 20px',
          maxWidth: '960px',
          margin: '0 auto',
        }}
      >
        <div
          className="eyebrow"
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '8px',
            backgroundColor: '#eef3fe',
            color: '#174ea6',
            padding: '6px 14px',
            borderRadius: '9999px',
            marginBottom: '20px',
            border: '1px solid #c2e7ff',
          }}
        >
          <Sparkles size={14} color="#1a73e8" />
          <span>NỀN TẢNG MÔ PHỎNG VẬT LÝ SỐ THẾ HỆ MỚI • CAPSTONE FA26</span>
        </div>

        <h1
          style={{
            margin: '0 0 18px',
            fontSize: 'clamp(32px, 4.5vw, 54px)',
            fontWeight: 800,
            lineHeight: 1.15,
            letterSpacing: '-0.04em',
            color: '#202124',
          }}
        >
          Mô phỏng Vật lý Trực quan, <br />
          <span style={{ color: '#1a73e8' }}>Chuẩn hóa & Sinh động</span>
        </h1>

        <p
          style={{
            fontSize: '17px',
            lineHeight: 1.6,
            color: '#5f6368',
            maxWidth: '720px',
            margin: '0 0 32px',
          }}
        >
          PhysLive giúp giáo viên và học sinh chuyển hóa các bài toán giải tích trừu tượng
          thành thí nghiệm ảo tương tác cao, kết hợp đồ thị SVG thời gian thực và công nghệ
          tinh gọn thông tin theo chuẩn thiết kế hiện đại.
        </p>

        <div style={{ display: 'flex', gap: '14px', flexWrap: 'wrap', justifyContent: 'center' }}>
          <button
            onClick={() => navigate('/player')}
            style={{
              padding: '12px 26px',
              backgroundColor: '#1a73e8',
              color: '#ffffff',
              border: 'none',
              borderRadius: '8px',
              fontSize: '15px',
              fontWeight: 700,
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              boxShadow: '0 4px 14px rgba(26, 115, 232, 0.35)',
              transition: 'transform 0.15s ease, background-color 0.15s ease',
            }}
            onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = '#1765cc')}
            onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = '#1a73e8')}
          >
            <FlaskConical size={18} />
            <span>Vào phòng thí nghiệm</span>
            <ArrowRight size={16} />
          </button>

          <button
            onClick={() => navigate('/library')}
            style={{
              padding: '12px 22px',
              backgroundColor: '#eef3fe',
              color: '#174ea6',
              border: '1px solid #c2e7ff',
              borderRadius: '8px',
              fontSize: '15px',
              fontWeight: 700,
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              transition: 'all 0.15s ease',
            }}
            onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = '#e0ecfe')}
            onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = '#eef3fe')}
          >
            <BookOpen size={18} />
            <span>Thư viện chuyên đề</span>
          </button>

          <button
            onClick={() => navigate('/student/assignments')}
            style={{
              padding: '12px 20px',
              backgroundColor: '#ffffff',
              color: '#202124',
              border: '1px solid #e1e5eb',
              borderRadius: '8px',
              fontSize: '15px',
              fontWeight: 700,
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
            }}
          >
            <span>Thử thách dự đoán</span>
          </button>
        </div>
      </section>

      {/* 2. SHOWCASE MODELS GRID (MÔ HÌNH NỔI BẬT) */}
      <section style={{ maxWidth: '1200px', margin: '0 auto', width: '100%' }}>
        <div style={{ marginBottom: '24px', display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end' }}>
          <div>
            <div className="eyebrow" style={{ color: '#174ea6', marginBottom: '6px' }}>CHỦ ĐỀ TIÊU BIỂU</div>
            <h2 style={{ margin: 0, fontSize: '24px', fontWeight: 800, color: '#202124' }}>
              Mô hình Vật lý Sẵn sàng Tương tác
            </h2>
          </div>
          <button
            onClick={() => navigate('/library')}
            style={{
              background: 'none',
              border: 'none',
              color: '#1a73e8',
              fontWeight: 700,
              fontSize: '14px',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '4px',
            }}
          >
            <span>Xem tất cả {MOCK_SIMULATIONS.length} mô hình</span>
            <ArrowRight size={14} />
          </button>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(340px, 1fr))', gap: '20px' }}>
          {MOCK_SIMULATIONS.slice(0, 3).map((sim) => (
            <div
              key={sim.id}
              className="card"
              style={{
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
                padding: '24px',
                borderRadius: '16px',
                transition: 'transform 0.2s ease, box-shadow 0.2s ease',
                cursor: 'pointer',
              }}
              onClick={() => navigate(`/player?id=${sim.id}`)}
            >
              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
                  <span
                    style={{
                      fontSize: '11px',
                      fontWeight: 700,
                      backgroundColor: '#eef3fe',
                      color: '#174ea6',
                      padding: '3px 8px',
                      borderRadius: '6px',
                    }}
                  >
                    {sim.spec_json.topic}
                  </span>
                  <span style={{ fontSize: '12px', color: '#137333', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '4px' }}>
                    <CheckCircle2 size={13} color="#137333" />
                    <span>Dual-Valid</span>
                  </span>
                </div>

                <h3 style={{ margin: '0 0 8px', fontSize: '17px', fontWeight: 800, color: '#202124' }}>
                  {sim.title}
                </h3>
                <p style={{ margin: '0 0 16px', fontSize: '13px', color: '#5f6368', lineHeight: 1.5 }}>
                  {sim.description}
                </p>
              </div>

              <div style={{ borderTop: '1px solid #f1f3f4', paddingTop: '14px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: '12px', fontFamily: 'monospace', color: '#1a73e8', fontWeight: 700 }}>
                  {sim.id === 'sim-car-brake-003'
                    ? 'v(t) = v₀ - a·t'
                    : sim.id === 'sim-proj-001'
                    ? 'y(t) = h + v₀y·t - ½gt²'
                    : 'a = g(sinα - μ·cosα)'}
                </span>

                <span
                  style={{
                    fontSize: '13px',
                    fontWeight: 700,
                    color: '#1a73e8',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '4px',
                  }}
                >
                  <span>Chạy thử</span>
                  <ArrowRight size={14} />
                </span>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* 3. FOUR CORE ADVANTAGES (TÍNH NĂNG ĐẮT GIÁ) */}
      <section style={{ maxWidth: '1200px', margin: '0 auto', width: '100%' }}>
        <div style={{ textAlign: 'center', marginBottom: '36px' }}>
          <div className="eyebrow" style={{ color: '#174ea6', marginBottom: '6px' }}>CÔNG NGHỆ & ĐỔI MỚI</div>
          <h2 style={{ margin: 0, fontSize: '26px', fontWeight: 800, color: '#202124' }}>
            Trực quan hóa Đột phá cho Giảng dạy Vật lý
          </h2>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '20px' }}>
          <div className="card" style={{ padding: '24px', borderRadius: '16px' }}>
            <div style={{ width: '44px', height: '44px', borderRadius: '10px', backgroundColor: '#eef3fe', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '16px', color: '#1a73e8' }}>
              <FlaskConical size={22} />
            </div>
            <h3 style={{ margin: '0 0 8px', fontSize: '16px', fontWeight: 800, color: '#202124' }}>
              Mô phỏng Canvas Tinh Gọn
            </h3>
            <p style={{ margin: 0, fontSize: '13px', color: '#5f6368', lineHeight: 1.55 }}>
              Khử nhiễu số học đè lên vật thể, chuyển đổi linh hoạt giữa chế độ Tối giản (Clean) và Chi tiết (Detailed) theo đúng mục tiêu bài giảng.
            </p>
          </div>

          <div className="card" style={{ padding: '24px', borderRadius: '16px' }}>
            <div style={{ width: '44px', height: '44px', borderRadius: '10px', backgroundColor: '#e6f4ea', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '16px', color: '#137333' }}>
              <LineChart size={22} />
            </div>
            <h3 style={{ margin: '0 0 8px', fontSize: '16px', fontWeight: 800, color: '#202124' }}>
              Đồ thị SVG Thời gian thực
            </h3>
            <p style={{ margin: 0, fontSize: '13px', color: '#5f6368', lineHeight: 1.55 }}>
              Vẽ đồng thời đồ thị v(t), s(t), a(t) bằng vector SVG sắc nét; học sinh có thể bấm kéo trên đồ thị để tua con trỏ thời gian mô phỏng.
            </p>
          </div>

          <div className="card" style={{ padding: '24px', borderRadius: '16px' }}>
            <div style={{ width: '44px', height: '44px', borderRadius: '10px', backgroundColor: '#fef7e0', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '16px', color: '#b06000' }}>
              <BrainCircuit size={22} />
            </div>
            <h3 style={{ margin: '0 0 8px', fontSize: '16px', fontWeight: 800, color: '#202124' }}>
              Trích xuất Bài toán AI OCR
            </h3>
            <p style={{ margin: 0, fontSize: '13px', color: '#5f6368', lineHeight: 1.55 }}>
              Nhập đề bài bằng văn bản hoặc ảnh chụp SGK; hệ thống tự động bóc tách tham số và làm rõ các giả thiết vật lý cần thiết.
            </p>
          </div>

          <div className="card" style={{ padding: '24px', borderRadius: '16px' }}>
            <div style={{ width: '44px', height: '44px', borderRadius: '10px', backgroundColor: '#fce8e6', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '16px', color: '#c5221f' }}>
              <ShieldCheck size={22} />
            </div>
            <h3 style={{ margin: '0 0 8px', fontSize: '16px', fontWeight: 800, color: '#202124' }}>
              Thẩm định Kép (Dual-Validation)
            </h3>
            <p style={{ margin: 0, fontSize: '13px', color: '#5f6368', lineHeight: 1.55 }}>
              So sánh sai số giữa phương trình giải tích lý thuyết và tích phân số học Euler/Verlet, đảm bảo tính chuẩn xác khoa học tuyệt đối.
            </p>
          </div>
        </div>
      </section>

      {/* 4. ROLE ENTRY POINTS (DÀNH CHO GIÁO VIÊN & HỌC SINH) */}
      <section style={{ maxWidth: '1200px', margin: '0 auto', width: '100%' }}>
        <div style={{ backgroundColor: '#ffffff', borderRadius: '20px', border: '1px solid #e1e5eb', padding: '36px', display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '28px' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
              <GraduationCap size={22} color="#1a73e8" />
              <h3 style={{ margin: 0, fontSize: '18px', fontWeight: 800, color: '#202124' }}>Dành cho Giáo viên</h3>
            </div>
            <p style={{ fontSize: '14px', color: '#5f6368', lineHeight: 1.6, marginBottom: '16px' }}>
              Tạo bài giảng sinh động, quản lý kho chuyên đề bài tập, nhúng trực tiếp thí nghiệm ảo vào hệ thống LMS (Moodle, Canvas) của trường.
            </p>
            <button
              onClick={() => navigate('/teacher/submit')}
              style={{
                padding: '8px 16px',
                backgroundColor: '#eef3fe',
                color: '#174ea6',
                border: '1px solid #c2e7ff',
                borderRadius: '8px',
                fontSize: '13px',
                fontWeight: 700,
                cursor: 'pointer',
              }}
            >
              Nộp đề bài mới →
            </button>
          </div>

          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
              <BookOpen size={22} color="#137333" />
              <h3 style={{ margin: 0, fontSize: '18px', fontWeight: 800, color: '#202124' }}>Dành cho Học sinh</h3>
            </div>
            <p style={{ fontSize: '14px', color: '#5f6368', lineHeight: 1.6, marginBottom: '16px' }}>
              Tham gia thử thách dự đoán vận tốc, khoảng cách dừng và tầm xa trước khi chạy mô phỏng, ghi nhận điểm rèn luyện tư duy thực nghiệm.
            </p>
            <button
              onClick={() => navigate('/student/assignments')}
              style={{
                padding: '8px 16px',
                backgroundColor: '#e6f4ea',
                color: '#137333',
                border: '1px solid #b7e1cd',
                borderRadius: '8px',
                fontSize: '13px',
                fontWeight: 700,
                cursor: 'pointer',
              }}
            >
              Vào làm bài tập →
            </button>
          </div>
        </div>
      </section>
    </div>
  );
};

export default Home;
