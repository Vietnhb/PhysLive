import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { MOCK_ASSIGNMENTS } from '../../mocks/mockAssignments';
import { MOCK_SIMULATIONS } from '../../mocks/mockSimulations';
import { MOCK_CLASS_LESSONS } from '../../mocks/mockTaxonomy';
import {
  Lock,
  Unlock,
  GraduationCap,
  Target,
  Send,
  FlaskConical,
  CheckCircle2,
  AlertTriangle,
  Clock,
  HelpCircle,
  FileText,
  Sparkles,
  ArrowRight,
} from 'lucide-react';

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
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px', maxWidth: '1200px', margin: '0 auto' }}>
      {/* Page Header */}
      <div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
          <Target size={22} color="#10b981" strokeWidth={2.5} />
          <h1 style={{ margin: 0, fontSize: '24px', fontWeight: 800, color: '#0f172a' }}>
            Không Gian Bài Tập & Thử Thách Dự Đoán
          </h1>
        </div>
        <p style={{ margin: 0, fontSize: '14px', color: '#64748b' }}>
          Học sinh tính toán lý thuyết và đưa ra con số dự đoán trước khi hệ thống mở khóa mô phỏng vật lý đối chiếu thực nghiệm.
        </p>
      </div>

      {/* Grid: Assignment List vs Detail Panel */}
      <div style={{ display: 'grid', gridTemplateColumns: '320px 1fr', gap: '24px' }}>
        {/* Danh sách bài tập được giao */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          <div style={{ fontSize: '13px', fontWeight: 800, color: '#334155', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
            Bài tập lớp học ({MOCK_ASSIGNMENTS.length})
          </div>

          {MOCK_ASSIGNMENTS.map((asg) => {
            const isSelected = asg.id === currentAssignment.id;
            const isDone = !!submittedPredictions[asg.id];

            return (
              <div
                key={asg.id}
                onClick={() => setSelectedAsgId(asg.id)}
                style={{
                  padding: '16px',
                  borderRadius: '12px',
                  border: isSelected ? '2px solid #2563eb' : '1px solid #e2e8f0',
                  backgroundColor: isSelected ? '#eff6ff' : '#ffffff',
                  cursor: 'pointer',
                  transition: 'all 0.15s ease',
                  boxShadow: isSelected ? '0 4px 12px rgba(37, 99, 235, 0.12)' : 'none',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                  <span
                    style={{
                      fontSize: '11px',
                      padding: '2px 8px',
                      borderRadius: '4px',
                      backgroundColor: isDone ? '#dcfce7' : '#fef3c7',
                      color: isDone ? '#166534' : '#92400e',
                      fontWeight: 700,
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: '4px',
                    }}
                  >
                    {isDone ? <CheckCircle2 size={11} /> : <Clock size={11} />}
                    <span>{isDone ? 'Đã gửi dự đoán' : 'Chờ dự đoán'}</span>
                  </span>
                  <span style={{ fontSize: '11px', color: '#94a3b8' }}>
                    {new Date(asg.due_date).toLocaleDateString('vi-VN')}
                  </span>
                </div>

                <div style={{ fontSize: '14px', fontWeight: 700, color: isSelected ? '#1d4ed8' : '#0f172a', lineHeight: 1.4 }}>
                  {asg.title}
                </div>
              </div>
            );
          })}
        </div>

        {/* Khung nội dung làm bài tập */}
        <div style={{ backgroundColor: '#ffffff', borderRadius: '16px', border: '1px solid #e2e8f0', padding: '28px', display: 'flex', flexDirection: 'column', gap: '22px', boxShadow: '0 2px 8px rgba(0,0,0,0.02)' }}>
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

            <span
              style={{
                fontSize: '12px',
                padding: '5px 12px',
                borderRadius: '999px',
                backgroundColor: hasSubmitted ? '#dcfce7' : '#fef9c3',
                color: hasSubmitted ? '#15803d' : '#854d0e',
                fontWeight: 700,
                display: 'inline-flex',
                alignItems: 'center',
                gap: '6px',
              }}
            >
              {hasSubmitted ? <Unlock size={14} /> : <Lock size={14} />}
              <span>{hasSubmitted ? 'MÔ PHỎNG ĐÃ MỞ KHÓA' : 'MÔ PHỎNG ĐANG KHÓA'}</span>
            </span>
          </div>

          {/* Lời dặn dò của giáo viên từ bài trước (Teacher Explanation Notes) */}
          <div style={{ padding: '16px 18px', backgroundColor: '#f0fdf4', borderRadius: '12px', border: '1px solid #bbf7d0' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px' }}>
              <GraduationCap size={18} color="#166534" />
              <span style={{ fontSize: '13px', fontWeight: 800, color: '#166534', textTransform: 'uppercase', letterSpacing: '0.03em' }}>
                Ghi chú hướng dẫn bài giải từ bài học trước của Thầy:
              </span>
            </div>
            <p style={{ margin: 0, fontSize: '13px', color: '#14532d', lineHeight: 1.6 }}>
              {classLesson.explanation_notes}
            </p>
          </div>

          {/* Đề bài và hướng dẫn */}
          <div>
            <div style={{ fontSize: '13px', fontWeight: 700, color: '#334155', marginBottom: '6px', display: 'flex', alignItems: 'center', gap: '6px' }}>
              <FileText size={15} color="#2563eb" />
              <span>Yêu cầu bài tập:</span>
            </div>
            <div style={{ fontSize: '14px', color: '#475569', lineHeight: 1.5 }}>
              {currentAssignment.instructions}
            </div>
          </div>

          {/* KHỐI NHẬP DỰ ĐOÁN CỦA HỌC SINH (CORE WORKFLOW) */}
          <div style={{ padding: '20px', backgroundColor: '#f8fafc', borderRadius: '12px', border: '1px solid #e2e8f0' }}>
            <div style={{ fontSize: '14px', fontWeight: 800, color: '#1e293b', marginBottom: '14px', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <HelpCircle size={17} color="#2563eb" />
              <span>{currentAssignment.target_question}</span>
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
                    padding: '12px 22px',
                    backgroundColor: '#2563eb',
                    color: '#ffffff',
                    border: 'none',
                    borderRadius: '8px',
                    fontWeight: 700,
                    fontSize: '13px',
                    cursor: 'pointer',
                    boxShadow: '0 2px 6px rgba(37, 99, 235, 0.2)',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '6px',
                  }}
                >
                  <Send size={14} />
                  <span>Xác nhận gửi dự đoán</span>
                </button>
              </form>
            ) : (
              // BẢNG ĐỐI CHIẾU KẾT QUẢ THỰC NGHIỆM VÀ DỰ ĐOÁN
              <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '12px' }}>
                  <div style={{ padding: '14px', backgroundColor: '#eff6ff', borderRadius: '10px', border: '1px solid #bfdbfe' }}>
                    <div style={{ fontSize: '11px', color: '#1d4ed8', fontWeight: 700, textTransform: 'uppercase' }}>DỰ ĐOÁN CỦA EM</div>
                    <div style={{ fontSize: '22px', fontWeight: 800, color: '#1e40af', fontFamily: 'Consolas, monospace', marginTop: '4px' }}>
                      {studentPred} m
                    </div>
                  </div>

                  <div style={{ padding: '14px', backgroundColor: '#ecfdf5', borderRadius: '10px', border: '1px solid #a7f3d0' }}>
                    <div style={{ fontSize: '11px', color: '#047857', fontWeight: 700, textTransform: 'uppercase' }}>THỰC NGHIỆM MÔ PHỎNG</div>
                    <div style={{ fontSize: '22px', fontWeight: 800, color: '#065f46', fontFamily: 'Consolas, monospace', marginTop: '4px' }}>
                      {actualValue} m
                    </div>
                  </div>

                  <div style={{ padding: '14px', backgroundColor: isPassed ? '#f0fdf4' : '#fef2f2', borderRadius: '10px', border: isPassed ? '1px solid #86efac' : '1px solid #fecaca' }}>
                    <div style={{ fontSize: '11px', color: isPassed ? '#166534' : '#991b1b', fontWeight: 700, textTransform: 'uppercase' }}>ĐỘ LỆCH SAI SỐ (Δ)</div>
                    <div style={{ fontSize: '22px', fontWeight: 800, color: isPassed ? '#16a34a' : '#dc2626', fontFamily: 'Consolas, monospace', marginTop: '4px' }}>
                      {discrepancy.toFixed(2)}% {isPassed ? '✓' : '✗'}
                    </div>
                  </div>
                </div>

                <div style={{ fontSize: '13px', color: isPassed ? '#15803d' : '#b91c1c', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '6px' }}>
                  {isPassed ? (
                    <>
                      <Sparkles size={16} color="#16a34a" />
                      <span>Xuất sắc! Kết quả dự đoán của em khớp với thực nghiệm vật lý trong ngưỡng cho phép (sai số ≤ 2%).</span>
                    </>
                  ) : (
                    <>
                      <AlertTriangle size={16} color="#dc2626" />
                      <span>Độ lệch giữa tính toán lý thuyết và thực nghiệm máy tính vượt quá 2%. Em hãy khởi chạy mô phỏng bên dưới để xem lại từng bước giải nhé!</span>
                    </>
                  )}
                </div>
              </div>
            )}
          </div>

          {/* ACTION UNLOCK LAUNCH SIMULATION */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingTop: '16px', borderTop: '1px solid #f1f5f9' }}>
            <div style={{ fontSize: '13px', color: '#64748b' }}>
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
                boxShadow: hasSubmitted ? '0 4px 14px rgba(16, 185, 129, 0.3)' : 'none',
                cursor: hasSubmitted ? 'pointer' : 'not-allowed',
              }}
            >
              <FlaskConical size={16} />
              <span>Khởi chạy phòng thí nghiệm để kiểm chứng thực nghiệm</span>
              <ArrowRight size={14} />
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
};

export default StudentAssignmentsPage;
