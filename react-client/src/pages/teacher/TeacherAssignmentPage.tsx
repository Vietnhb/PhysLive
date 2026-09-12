import React, { useState } from 'react';
import { MOCK_ASSIGNMENTS } from '../../mocks/mockAssignments';
import { MOCK_SIMULATIONS } from '../../mocks/mockSimulations';
import { MOCK_CLASS_LESSONS } from '../../mocks/mockTaxonomy';
import type { Assignment } from '../../types/database';

export const TeacherAssignmentPage: React.FC = () => {
  const [assignments, setAssignments] = useState<Assignment[]>(MOCK_ASSIGNMENTS);
  const [isCreateModalOpen, setIsCreateModalOpen] = useState<boolean>(false);

  // Form state
  const [selectedSimId, setSelectedSimId] = useState<string>(MOCK_SIMULATIONS[0].id);
  const [selectedClassLessonId, setSelectedClassLessonId] = useState<string>(MOCK_CLASS_LESSONS[0].id);
  const [title, setTitle] = useState<string>('');
  const [instructions, setInstructions] = useState<string>('');
  const [targetQuestion, setTargetQuestion] = useState<string>('');
  const [targetQuantitySymbol, setTargetQuantitySymbol] = useState<string>('L_max (m)');
  const [dueDate, setDueDate] = useState<string>('2026-09-30');

  // Lấy ghi chú bài giảng trước của ClassLesson được chọn
  const currentClassLesson = MOCK_CLASS_LESSONS.find(c => c.id === selectedClassLessonId) || MOCK_CLASS_LESSONS[0];

  const handleCreateAssignment = (e: React.FormEvent) => {
    e.preventDefault();
    if (!title || !targetQuestion) {
      alert('Vui lòng điền tiêu đề và câu hỏi dự đoán!');
      return;
    }

    const newAssignment: Assignment = {
      id: `asg-${Date.now()}`,
      simulation_id: selectedSimId,
      teacher_id: 'u-teacher-hung',
      class_lesson_id: selectedClassLessonId,
      title,
      instructions: instructions || currentClassLesson.explanation_notes,
      target_question: targetQuestion,
      target_quantity_symbol: targetQuantitySymbol,
      due_date: new Date(dueDate).toISOString(),
      created_at: new Date().toISOString(),
    };

    setAssignments([newAssignment, ...assignments]);
    setIsCreateModalOpen(false);
    setTitle('');
    setInstructions('');
    setTargetQuestion('');
    alert('Đã giao bài tập thành công cho lớp học!');
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h1 style={{ margin: '0 0 6px', fontSize: '24px', fontWeight: 800, color: '#0f172a' }}>
            Quản trị & Giao bài tập cho Học sinh (Teacher Assignment Workspace)
          </h1>
          <p style={{ margin: 0, fontSize: '14px', color: '#64748b' }}>
            Gắn mô phỏng tương tác vào phân cấp bài học (Class/Lesson), đưa ra thử thách dự đoán và theo dõi đối chiếu thực nghiệm của học sinh.
          </p>
        </div>

        <button
          onClick={() => setIsCreateModalOpen(true)}
          style={{
            padding: '10px 20px',
            backgroundColor: '#2563eb',
            color: '#ffffff',
            border: 'none',
            borderRadius: '10px',
            fontWeight: 700,
            fontSize: '13px',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            boxShadow: '0 2px 8px rgba(37, 99, 235, 0.25)',
          }}
        >
          <span>➕</span> Tạo bài tập mới
        </button>
      </div>

      {/* Danh sách bài tập đã giao */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(360px, 1fr))', gap: '20px' }}>
        {assignments.map((asg) => {
          const sim = MOCK_SIMULATIONS.find(s => s.id === asg.simulation_id);
          const classLesson = MOCK_CLASS_LESSONS.find(c => c.id === asg.class_lesson_id);

          return (
            <div
              key={asg.id}
              style={{
                backgroundColor: '#ffffff',
                borderRadius: '16px',
                border: '1px solid #e2e8f0',
                padding: '22px',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
                boxShadow: '0 2px 6px rgba(0,0,0,0.03)',
              }}
            >
              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
                  <span style={{ fontSize: '11px', padding: '3px 8px', borderRadius: '4px', backgroundColor: '#eff6ff', color: '#1d4ed8', fontWeight: 700 }}>
                    {classLesson ? classLesson.class_name : 'Lớp học chung'}
                  </span>
                  <span style={{ fontSize: '12px', color: '#94a3b8' }}>
                    Hạn: {new Date(asg.due_date).toLocaleDateString('vi-VN')}
                  </span>
                </div>

                <h3 style={{ margin: '0 0 8px', fontSize: '16px', fontWeight: 700, color: '#0f172a' }}>
                  {asg.title}
                </h3>

                <div style={{ fontSize: '13px', color: '#64748b', marginBottom: '14px', lineHeight: 1.4 }}>
                  {asg.instructions}
                </div>

                {/* Target question highlight */}
                <div style={{ padding: '12px', backgroundColor: '#f8fafc', borderRadius: '8px', border: '1px solid #e2e8f0', marginBottom: '16px' }}>
                  <div style={{ fontSize: '11px', fontWeight: 700, color: '#2563eb', textTransform: 'uppercase', marginBottom: '4px' }}>
                    🎯 Câu hỏi thử thách học sinh:
                  </div>
                  <div style={{ fontSize: '13px', fontWeight: 600, color: '#1e293b' }}>
                    {asg.target_question}
                  </div>
                </div>
              </div>

              {/* Footer card */}
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingTop: '14px', borderTop: '1px solid #f1f5f9' }}>
                <span style={{ fontSize: '12px', color: '#10b981', fontWeight: 600 }}>
                  Mô phỏng: {sim ? sim.title : 'Chưa gán'}
                </span>
                <span style={{ fontSize: '12px', padding: '2px 8px', borderRadius: '4px', backgroundColor: '#ecfdf5', color: '#047857', fontWeight: 600 }}>
                  ✓ Đang mở
                </span>
              </div>
            </div>
          );
        })}
      </div>

      {/* MODAL TẠO BÀI TẬP MỚI */}
      {isCreateModalOpen && (
        <div
          style={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: 'rgba(15, 23, 42, 0.7)',
            backdropFilter: 'blur(4px)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 100,
            padding: '20px',
          }}
        >
          <div
            style={{
              backgroundColor: '#ffffff',
              borderRadius: '16px',
              maxWidth: '620px',
              width: '100%',
              padding: '28px',
              boxShadow: '0 20px 40px rgba(0,0,0,0.2)',
              maxHeight: '90vh',
              overflowY: 'auto',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
              <h3 style={{ margin: 0, fontSize: '18px', fontWeight: 800, color: '#0f172a' }}>
                ➕ Giao bài tập & Thử thách dự đoán mới
              </h3>
              <button
                onClick={() => setIsCreateModalOpen(false)}
                style={{ border: 'none', background: 'transparent', fontSize: '20px', cursor: 'pointer', color: '#64748b' }}
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleCreateAssignment} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              {/* Chọn lớp học & bài học (4-tier taxonomy) */}
              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: '#334155', marginBottom: '6px' }}>
                  🏫 Chọn Lớp học & Tiết dạy (Class/Lesson)
                </label>
                <select
                  value={selectedClassLessonId}
                  onChange={(e) => setSelectedClassLessonId(e.target.value)}
                  style={{ width: '100%', padding: '10px', borderRadius: '8px', border: '1px solid #cbd5e1', fontSize: '13px', outline: 'none' }}
                >
                  {MOCK_CLASS_LESSONS.map((cls) => (
                    <option key={cls.id} value={cls.id}>
                      {cls.class_name} — {cls.name}
                    </option>
                  ))}
                </select>
              </div>

              {/* Lời dặn dò & Lời giải bài trước (Giảng viên note) */}
              <div style={{ padding: '12px', backgroundColor: '#eff6ff', borderRadius: '8px', border: '1px solid #bfdbfe' }}>
                <div style={{ fontSize: '12px', fontWeight: 700, color: '#1d4ed8', marginBottom: '4px' }}>
                  📖 Ghi chú bài giải tiết trước của Giáo viên:
                </div>
                <div style={{ fontSize: '12px', color: '#1e40af', lineHeight: 1.4 }}>
                  {currentClassLesson.explanation_notes}
                </div>
              </div>

              {/* Chọn Mô phỏng liên kết */}
              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: '#334155', marginBottom: '6px' }}>
                  🧪 Mô phỏng vật lý liên kết (Simulation)
                </label>
                <select
                  value={selectedSimId}
                  onChange={(e) => setSelectedSimId(e.target.value)}
                  style={{ width: '100%', padding: '10px', borderRadius: '8px', border: '1px solid #cbd5e1', fontSize: '13px', outline: 'none' }}
                >
                  {MOCK_SIMULATIONS.map((sim) => (
                    <option key={sim.id} value={sim.id}>
                      [{sim.spec_json.topic}] {sim.title}
                    </option>
                  ))}
                </select>
              </div>

              {/* Tiêu đề bài tập */}
              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: '#334155', marginBottom: '6px' }}>
                  Tiêu đề thử thách
                </label>
                <input
                  type="text"
                  placeholder="Ví dụ: Thử thách dự đoán tầm xa cực đại của vật..."
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  style={{ width: '100%', padding: '10px', borderRadius: '8px', border: '1px solid #cbd5e1', fontSize: '13px', outline: 'none' }}
                />
              </div>

              {/* Câu hỏi mục tiêu cho học sinh dự đoán */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 140px', gap: '12px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: '#334155', marginBottom: '6px' }}>
                    🎯 Câu hỏi yêu cầu học sinh tính và dự đoán (Target Question)
                  </label>
                  <input
                    type="text"
                    placeholder="Ví dụ: Tầm xa cực đại L_max (m) khi vật chạm đất là bao nhiêu?"
                    value={targetQuestion}
                    onChange={(e) => setTargetQuestion(e.target.value)}
                    style={{ width: '100%', padding: '10px', borderRadius: '8px', border: '1px solid #cbd5e1', fontSize: '13px', outline: 'none' }}
                  />
                </div>
                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: '#334155', marginBottom: '6px' }}>
                    Đơn vị đo
                  </label>
                  <input
                    type="text"
                    placeholder="e.g. m, s, m/s"
                    value={targetQuantitySymbol}
                    onChange={(e) => setTargetQuantitySymbol(e.target.value)}
                    style={{ width: '100%', padding: '10px', borderRadius: '8px', border: '1px solid #cbd5e1', fontSize: '13px', outline: 'none' }}
                  />
                </div>
              </div>

              {/* Hạn nộp */}
              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: '#334155', marginBottom: '6px' }}>
                  Hạn hoàn thành bài tập
                </label>
                <input
                  type="date"
                  value={dueDate}
                  onChange={(e) => setDueDate(e.target.value)}
                  style={{ width: '100%', padding: '10px', borderRadius: '8px', border: '1px solid #cbd5e1', fontSize: '13px', outline: 'none' }}
                />
              </div>

              {/* Buttons */}
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '12px' }}>
                <button
                  type="button"
                  onClick={() => setIsCreateModalOpen(false)}
                  style={{ padding: '10px 18px', borderRadius: '8px', border: '1px solid #cbd5e1', background: '#ffffff', color: '#475569', fontWeight: 600, cursor: 'pointer' }}
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  style={{ padding: '10px 22px', borderRadius: '8px', border: 'none', background: '#2563eb', color: '#ffffff', fontWeight: 700, cursor: 'pointer' }}
                >
                  Giao bài tập ngay
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
