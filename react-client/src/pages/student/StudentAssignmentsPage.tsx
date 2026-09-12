import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { MOCK_ASSIGNMENTS } from '../../mocks/mockAssignments';
import { MOCK_SIMULATIONS } from '../../mocks/mockSimulations';
import { MOCK_CLASS_LESSONS } from '../../mocks/mockTaxonomy';

export const StudentAssignmentsPage: React.FC = () => {
  const [selectedAsgId, setSelectedAsgId] = useState<string>(MOCK_ASSIGNMENTS[0].id);
  const [predictionInput, setPredictionInput] = useState<string>('');
  
  // Trạng thái đã nộp dự đoán cho từng bài tập
  const [submittedPredictions, setSubmittedPredictions] = useState<Record<string, {
    prediction: number;
    submittedAt: string;
  }>>({
    'asg-001': { prediction: 38.5, submittedAt: '2026-09-12T11:20:00Z' }
  });

  const currentAssignment = MOCK_ASSIGNMENTS.find(a => a.id === selectedAsgId) || MOCK_ASSIGNMENTS[0];
  const currentSim = MOCK_SIMULATIONS.find(s => s.id === currentAssignment.simulation_id) || MOCK_SIMULATIONS[0];
  const classLesson = MOCK_CLASS_LESSONS.find(c => c.id === currentAssignment.class_lesson_id) || MOCK_CLASS_LESSONS[0];

  const hasSubmitted = !!submittedPredictions[currentAssignment.id];
  const studentPred = submittedPredictions[currentAssignment.id]?.prediction;

  // Giá trị thực nghiệm lý thuyết
  const actualValue =
    currentAssignment.id === 'asg-001'
      ? 38.45
      : currentAssignment.id === 'asg-002'
      ? 50.0
      : 4.13;

  // Tính sai số
  const discrepancy = studentPred !== undefined
    ? (Math.abs(studentPred - actualValue) / actualValue) * 100
    : 0;
  const isPassed = discrepancy <= 2.0; // Dung sai 2%

  const handleSubmitPrediction = (e: React.FormEvent) => {
    e.preventDefault();
    const val = parseFloat(predictionInput);
    if (isNaN(val)) {
      alert('Vui lòng nhập một số thực hợp lệ!');
      return;
    }

    setSubmittedPredictions({
      ...submittedPredictions,
      [currentAssignment.id]: {
        prediction: val,
        submittedAt: new Date().toISOString(),
      }
    });
    setPredictionInput('');
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Header */}
      <div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
          <span style={{ fontSize: '22px' }}>🎯</span>
          <h1 style={{ margin: 0, fontSize: '24px', fontWeight: 800, color: '#0f172a' }}>
            Bàn học & Thử thách dự đoán Vật lý (Student Prediction Flow)
          </h1>
        </div>
        <p style={{ margin: 0, fontSize: '14px', color: '#64748b' }}>
          Theo quy trình chuẩn của PhysLive: Học sinh bắt buộc tính toán lý thuyết và <strong>gửi dự đoán trước</strong> khi hệ thống mở khóa mô phỏng để đối chiếu thực nghiệm.
        </p>
      </div>

      {/* Main Layout: Danh sách bài tập bên trái, Chi tiết thử thách bên phải */}
      <div style={{ display: 'grid', gridTemplateColumns: '320px 1fr', gap: '24px' }}>
        {/* Danh sách bài tập */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
          <div style={{ fontSize: '12px', fontWeight: 800, color: '#475569', letterSpacing: '0.04em', textTransform: 'uppercase', marginBottom: '4px' }}>
            BÀI TẬP ĐƯỢC GIAO ({MOCK_ASSIGNMENTS.length})
          </div>

          {MOCK_ASSIGNMENTS.map((asg) => {
            const isSelected = asg.id === currentAssignment.id;
            const isDone = !!submittedPredictions[asg.id];

            return (
              <div
                key={asg.id}
                onClick={() => setSelectedAsgId(asg.id)}
                style={{
                  padding: '14px 16px',
                  borderRadius: '12px',
                  border: isSelected ? '2px solid #2563eb' : '1px solid #e2e8f0',
                  backgroundColor: isSelected ? '#eff6ff' : '#ffffff',
                  cursor: 'pointer',
                  transition: 'all 0.15s ease',
                  boxShadow: isSelected ? '0 2px 8px rgba(37, 99, 235, 0.1)' : 'none',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                  <span style={{ fontSize: '11px', padding: '2px 6px', borderRadius: '4px', backgroundColor: isDone ? '#dcfce7' : '#fef3c7', color: isDone ? '#166534' : '#92400e', fontWeight: 700 }}>
                    {isDone ? '✓ Đã gửi dự đoán' : '⏳ Chờ dự đoán'}
                  </span>
                  <span style={{ fontSize: '11px', color: '#94a3b8' }}>
                    Hạn: {new Date(asg.due_date).toLocaleDateString('vi-VN')}
                  </span>
                </div>

                <div style={{ fontSize: '13px', fontWeight: 700, color: isSelected ? '#1d4ed8' : '#0f172a', lineHeight: 1.4 }}>
                  {asg.title}
                </div>
              </div>
            );
          })}
        </div>

        {/* Khung nội dung làm bài tập */}
        <div style={{ backgroundColor: '#ffffff', borderRadius: '16px', border: '1px solid #e2e8f0', padding: '28px', display: 'flex', flexDirection: 'column', gap: '20px', boxShadow: '0 2px 8px rgba(0,0,0,0.02)' }}>
          {/* Metadata header */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', borderBottom: '1px solid #f1f5f9', paddingBottom: '16px' }}>
            <div>
              <span style={{ fontSize: '11px', padding: '3px 8px', borderRadius: '4px', backgroundColor: '#f1f5f9', color: '#475569', fontWeight: 700 }}>
                {classLesson.class_name} • {classLesson.name}
              </span>
              <h2 style={{ margin: '8px 0 4px', fontSize: '20px', fontWeight: 800, color: '#0f172a' }}>
                {currentAssignment.title}
              </h2>
              <div style={{ fontSize: '13px', color: '#64748b' }}>
                Giáo viên phụ trách: <strong>Thầy Nguyễn Thế Hùng</strong>
              </div>
            </div>

            <span style={{ fontSize: '12px', padding: '4px 10px', borderRadius: '999px', backgroundColor: hasSubmitted ? '#dcfce7' : '#fef9c3', color: hasSubmitted ? '#15803d' : '#854d0e', fontWeight: 700 }}>
              {hasSubmitted ? 'MÔ PHỎNG ĐÃ MỞ KHÓA 🔓' : 'MÔ PHỎNG ĐANG KHÓA 🔒'}
            </span>
          </div>

          {/* Lời dặn dò của giáo viên từ bài trước (Teacher Explanation Notes) */}
          <div style={{ padding: '16px', backgroundColor: '#f0fdf4', borderRadius: '12px', border: '1px solid #bbf7d0' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px' }}>
              <span style={{ fontSize: '16px' }}>👨‍🏫</span>
              <span style={{ fontSize: '13px', fontWeight: 800, color: '#166534', textTransform: 'uppercase' }}>
                Ghi chú hướng dẫn bài giải từ bài học trước của Thầy:
              </span>
            </div>
            <p style={{ margin: 0, fontSize: '13px', color: '#14532d', lineHeight: 1.6 }}>
              {classLesson.explanation_notes}
            </p>
          </div>

          {/* Đề bài và hướng dẫn */}
          <div>
            <div style={{ fontSize: '13px', fontWeight: 700, color: '#334155', marginBottom: '6px' }}>
              📝 Yêu cầu bài tập:
            </div>
            <div style={{ fontSize: '14px', color: '#475569', lineHeight: 1.5 }}>
              {currentAssignment.instructions}
            </div>
          </div>

          {/* KHỐI NHẬP DỰ ĐOÁN CỦA HỌC SINH (CORE WORKFLOW) */}
          <div style={{ padding: '20px', backgroundColor: '#f8fafc', borderRadius: '12px', border: '1px solid #e2e8f0' }}>
            <div style={{ fontSize: '14px', fontWeight: 800, color: '#1e293b', marginBottom: '12px', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <span>❓</span> {currentAssignment.target_question}
            </div>

            {!hasSubmitted ? (
              <form onSubmit={handleSubmitPrediction} style={{ display: 'flex', gap: '12px', alignItems: 'center', maxWidth: '480px' }}>
                <input
                  type="number"
                  step="0.01"
                  placeholder="Nhập giá trị dự đoán của em..."
                  value={predictionInput}
                  onChange={(e) => setPredictionInput(e.target.value)}
                  style={{
                    flex: 1,
                    padding: '12px 14px',
                    borderRadius: '8px',
                    border: '1px solid #cbd5e1',
                    fontSize: '14px',
                    outline: 'none',
                    fontWeight: 600,
                  }}
                />
                <button
                  type="submit"
                  style={{
                    padding: '12px 24px',
                    backgroundColor: '#2563eb',
                    color: '#ffffff',
                    border: 'none',
                    borderRadius: '8px',
                    fontWeight: 700,
                    fontSize: '13px',
                    cursor: 'pointer',
                    boxShadow: '0 2px 6px rgba(37, 99, 235, 0.2)',
                  }}
                >
                  Xác nhận gửi dự đoán 🚀
                </button>
              </form>
            ) : (
              // BẢNG ĐỐI CHIẾU KẾT QUẢ THỰC NGHIỆM VÀ DỰ ĐOÁN
              <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '12px' }}>
                  <div style={{ padding: '12px', backgroundColor: '#eff6ff', borderRadius: '8px', border: '1px solid #bfdbfe' }}>
                    <div style={{ fontSize: '11px', color: '#1d4ed8', fontWeight: 600 }}>DỰ ĐOÁN CỦA EM</div>
                    <div style={{ fontSize: '20px', fontWeight: 800, color: '#1e40af', fontFamily: 'Consolas, monospace' }}>
                      {studentPred} m
                    </div>
                  </div>

                  <div style={{ padding: '12px', backgroundColor: '#ecfdf5', borderRadius: '8px', border: '1px solid #a7f3d0' }}>
                    <div style={{ fontSize: '11px', color: '#047857', fontWeight: 600 }}>THỰC NGHIỆM MÔ PHỎNG</div>
                    <div style={{ fontSize: '20px', fontWeight: 800, color: '#065f46', fontFamily: 'Consolas, monospace' }}>
                      {actualValue} m
                    </div>
                  </div>

                  <div style={{ padding: '12px', backgroundColor: isPassed ? '#f0fdf4' : '#fef2f2', borderRadius: '8px', border: isPassed ? '1px solid #86efac' : '1px solid #fecaca' }}>
                    <div style={{ fontSize: '11px', color: isPassed ? '#166534' : '#991b1b', fontWeight: 600 }}>ĐỘ LỆCH SAI SỐ (Δ)</div>
                    <div style={{ fontSize: '20px', fontWeight: 800, color: isPassed ? '#16a34a' : '#dc2626', fontFamily: 'Consolas, monospace' }}>
                      {discrepancy.toFixed(2)}% {isPassed ? '✓' : '✗'}
                    </div>
                  </div>
                </div>

                <div style={{ fontSize: '13px', color: isPassed ? '#15803d' : '#b91c1c', fontWeight: 600 }}>
                  {isPassed
                    ? '🎉 Xuất sắc! Kết quả dự đoán của em khớp với thực nghiệm vật lý trong ngưỡng cho phép (sai số ≤ 2%).'
                    : '⚠️ Độ lệch giữa tính toán lý thuyết và thực nghiệm máy tính vượt quá 2%. Em hãy khởi chạy mô phỏng bên dưới để xem lại từng bước giải nhé!'}
                </div>
              </div>
            )}
          </div>

          {/* ACTION UNLOCK LAUNCH SIMULATION */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingTop: '16px', borderTop: '1px solid #f1f5f9' }}>
            <div style={{ fontSize: '12px', color: '#64748b' }}>
              Mô hình thực nghiệm: <strong>{currentSim.title}</strong>
            </div>

            <Link
              to={`/player?id=${currentSim.id}`}
              style={{
                padding: '12px 24px',
                backgroundColor: hasSubmitted ? '#10b981' : '#cbd5e1',
                color: '#ffffff',
                borderRadius: '10px',
                fontWeight: 700,
                fontSize: '14px',
                textDecoration: 'none',
                display: 'inline-flex',
                alignItems: 'center',
                gap: '8px',
                pointerEvents: hasSubmitted ? 'auto' : 'none',
                boxShadow: hasSubmitted ? '0 4px 12px rgba(16, 185, 129, 0.3)' : 'none',
                cursor: hasSubmitted ? 'pointer' : 'not-allowed',
              }}
            >
              <span>🧪</span> Khởi chạy phòng thí nghiệm để kiểm chứng thực nghiệm ➔
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
};
