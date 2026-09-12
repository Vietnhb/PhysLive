import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { MOCK_LESSONS } from '../../mocks/mockTaxonomy';
import { MOCK_PROBLEMS } from '../../mocks/mockProblems';
import type { AmbiguityFlag, ExtractedQuantity } from '../../types/database';
import {
  FileEdit,
  Camera,
  UploadCloud,
  Sparkles,
  AlertTriangle,
  Play,
  BookOpen,
  CheckCircle2,
} from 'lucide-react';

export const ProblemSubmitPage: React.FC = () => {
  const navigate = useNavigate();
  const [inputType, setInputType] = useState<'text' | 'image'>('text');
  const [selectedLesson, setSelectedLesson] = useState<string>(MOCK_LESSONS[0].id);
  const [rawText, setRawText] = useState<string>(
    'Một vật được ném từ độ cao h = 20m so với mặt đất với vận tốc đầu v0 = 15 m/s dưới góc ném alpha = 30 độ. Lấy gia tốc g = 9.8 m/s^2. Hãy xác định quỹ đạo chuyển động, tầm xa vật đạt được và thời gian chạm đất.'
  );
  const [uploadedImage, setUploadedImage] = useState<string | null>(null);
  const [isAnalyzing, setIsAnalyzing] = useState<boolean>(false);
  const [analysisProgress, setAnalysisProgress] = useState<number>(0);
  const [progressStage, setProgressStage] = useState<string>('');
  
  // Kết quả sau trích xuất
  const [hasExtracted, setHasExtracted] = useState<boolean>(true);
  const [extractedQuantities] = useState<ExtractedQuantity[]>(
    MOCK_PROBLEMS[0].extraction.extracted_spec.quantities
  );
  
  // Ambiguity Resolution Modal State (Task H04)
  const [ambiguities, setAmbiguities] = useState<AmbiguityFlag[]>([
    {
      id: 'amb-client-01',
      extraction_id: 'ext-temp',
      field_path: 'parameters.alpha.direction',
      prompt_question: 'Đề bài chưa nói rõ: Góc ném 30° là chếch lên trên (ném lên) hay chúc xuống dưới (ném xiên xuống) so với phương ngang?',
      candidate_options: [
        { label: 'Chếch lên trên (+30°)', value: 'UPWARD', description: 'Vật bay lên đỉnh parabol rồi rơi xuống (phổ biến nhất).' },
        { label: 'Chúc xuống dưới (-30°)', value: 'DOWNWARD', description: 'Vật lao dốc ngay từ thời điểm ban đầu.' }
      ],
      resolved_value: '',
      status: 'pending',
      created_at: new Date().toISOString(),
    }
  ]);
  const [isAmbiguityModalOpen, setIsAmbiguityModalOpen] = useState<boolean>(false);
  const [notification, setNotification] = useState<string | null>(null);

  const handleStartAnalysis = () => {
    setIsAnalyzing(true);
    setAnalysisProgress(10);
    setProgressStage('Đang đọc ngữ nghĩa bài toán (NLP Parsing)...');

    setTimeout(() => {
      setAnalysisProgress(45);
      setProgressStage('Đang chuẩn hoá đơn vị & trích xuất đại lượng...');
    }, 600);

    setTimeout(() => {
      setAnalysisProgress(80);
      setProgressStage('Đang kiểm tra dữ kiện mơ hồ & đối chiếu sơ bộ...');
    }, 1200);

    setTimeout(() => {
      setAnalysisProgress(100);
      setIsAnalyzing(false);
      setHasExtracted(true);
      // Nếu có mơ hồ, mở modal
      if (ambiguities.some(a => a.status === 'pending')) {
        setIsAmbiguityModalOpen(true);
      }
    }, 1800);
  };

  const handleResolveAmbiguity = (ambId: string, val: string | number) => {
    setAmbiguities(prev =>
      prev.map(a => (a.id === ambId ? { ...a, resolved_value: val, status: 'resolved' } : a))
    );
  };

  const handleConfirmAmbiguities = () => {
    const stillPending = ambiguities.some(a => !a.resolved_value);
    if (stillPending) {
      alert('Vui lòng chọn hoặc cung cấp đầy đủ thông số cho các dữ kiện mơ hồ trước khi tiếp tục!');
      return;
    }
    setIsAmbiguityModalOpen(false);
    setNotification('Đã giải quyết tất cả dữ kiện mơ hồ thành công! Sẵn sàng tạo mô phỏng.');
    setTimeout(() => setNotification(null), 4000);
  };

  const isAllAmbiguityResolved = ambiguities.every(a => a.status === 'resolved');

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Header Bar */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h1 style={{ margin: '0 0 6px', fontSize: '24px', fontWeight: 800, color: '#0f172a' }}>
            Nộp đề bài & Trích xuất AI (Task H03 & H04)
          </h1>
          <p style={{ margin: 0, fontSize: '14px', color: '#64748b' }}>
            Nhập đề bài bằng văn bản hoặc ảnh chụp. Hệ thống sẽ tự động bóc tách thực thể vật lý, nhận diện dữ kiện mơ hồ và khởi tạo mô phỏng tương tác.
          </p>
        </div>

        <div style={{ display: 'flex', gap: '10px' }}>
          <button
            onClick={() => alert('Đã lưu bản nháp thành công vào Thư viện!')}
            style={{
              padding: '9px 16px',
              backgroundColor: '#ffffff',
              border: '1px solid #cbd5e1',
              borderRadius: '8px',
              fontSize: '13px',
              fontWeight: 600,
              color: '#334155',
              cursor: 'pointer',
            }}
          >
            💾 Lưu bản nháp (Draft)
          </button>
        </div>
      </div>

      {notification && (
        <div
          style={{
            padding: '12px 18px',
            backgroundColor: '#ecfdf5',
            color: '#065f46',
            border: '1px solid #a7f3d0',
            borderRadius: '10px',
            fontSize: '14px',
            fontWeight: 500,
          }}
        >
          ✓ {notification}
        </div>
      )}

      {/* Main Grid: Input Form vs Extracted Preview */}
      <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr', gap: '24px' }}>
        {/* Left Column: Input Form */}
        <div style={{ backgroundColor: '#ffffff', borderRadius: '16px', padding: '24px', border: '1px solid #e2e8f0', boxShadow: '0 2px 8px rgba(0,0,0,0.02)' }}>
          {/* Lesson Selection */}
          <div style={{ marginBottom: '20px' }}>
            <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: '#334155', marginBottom: '8px' }}>
              <span style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
                <BookOpen size={15} />
                <span>Liên kết phân cấp học liệu (Curriculum Taxonomy)</span>
              </span>
            </label>
            <select
              value={selectedLesson}
              onChange={(e) => setSelectedLesson(e.target.value)}
              style={{
                width: '100%',
                padding: '10px 12px',
                borderRadius: '8px',
                border: '1px solid #cbd5e1',
                fontSize: '14px',
                outline: 'none',
                backgroundColor: '#f8fafc',
              }}
            >
              {MOCK_LESSONS.map((les) => (
                <option key={les.id} value={les.id}>
                  Lớp {les.grade_level}: {les.name}
                </option>
              ))}
            </select>
          </div>

          {/* Mode Tabs */}
          <div style={{ display: 'flex', gap: '8px', marginBottom: '16px', borderBottom: '1px solid #e2e8f0', paddingBottom: '10px' }}>
            <button
              onClick={() => setInputType('text')}
              style={{
                padding: '8px 16px',
                borderRadius: '8px',
                border: 'none',
                backgroundColor: inputType === 'text' ? '#2563eb' : '#f1f5f9',
                color: inputType === 'text' ? '#ffffff' : '#64748b',
                fontWeight: 600,
                fontSize: '13px',
                cursor: 'pointer',
                display: 'inline-flex',
                alignItems: 'center',
                gap: '6px',
              }}
            >
              <FileEdit size={14} />
              <span>Nhập văn bản / Dán đề</span>
            </button>
            <button
              onClick={() => setInputType('image')}
              style={{
                padding: '8px 16px',
                borderRadius: '8px',
                border: 'none',
                backgroundColor: inputType === 'image' ? '#2563eb' : '#f1f5f9',
                color: inputType === 'image' ? '#ffffff' : '#64748b',
                fontWeight: 600,
                fontSize: '13px',
                cursor: 'pointer',
                display: 'inline-flex',
                alignItems: 'center',
                gap: '6px',
              }}
            >
              <Camera size={14} />
              <span>Tải ảnh bài tập (OCR)</span>
            </button>
          </div>

          {inputType === 'image' && (
            <div style={{ marginBottom: '16px' }}>
              <div
                style={{
                  border: '2px dashed #93c5fd',
                  borderRadius: '12px',
                  padding: '24px',
                  textAlign: 'center',
                  backgroundColor: '#f0f7ff',
                  cursor: 'pointer',
                }}
                onClick={() => {
                  setUploadedImage('https://images.unsplash.com/photo-1636466497217-26a8cbeaf0aa?auto=format&fit=crop&w=600&q=80');
                  setRawText('[OCR]: Một xe lăn khối lượng m = 2kg chuyển động trên dốc nghiêng 30 độ dài 10m.');
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '8px' }}>
                  <UploadCloud size={36} color="#2563eb" />
                </div>
                <div style={{ fontSize: '13px', fontWeight: 600, color: '#1e40af' }}>
                  Kéo thả ảnh đề bài hoặc click để chọn ảnh (Hỗ trợ JPG, PNG)
                </div>
                <div style={{ fontSize: '11px', color: '#64748b', marginTop: '4px' }}>
                  Hệ thống sử dụng OpenRouter OCR model để bóc tách văn bản tự động
                </div>
              </div>

              {uploadedImage && (
                <div style={{ marginTop: '12px', display: 'flex', alignItems: 'center', gap: '10px' }}>
                  <img
                    src={uploadedImage}
                    alt="Upload preview"
                    style={{ width: '60px', height: '60px', objectFit: 'cover', borderRadius: '6px' }}
                  />
                  <div style={{ fontSize: '12px', color: '#16a34a', fontWeight: 600 }}>
                    ✓ Đã nhận diện ảnh thành công! Bạn có thể xem và sửa văn bản OCR bên dưới.
                  </div>
                </div>
              )}
            </div>
          )}

          {/* Text Area */}
          <div style={{ marginBottom: '20px' }}>
            <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, color: '#334155', marginBottom: '8px' }}>
              {inputType === 'image' ? 'Nội dung OCR nhận diện (Có thể chỉnh sửa):' : 'Nội dung đề bài Vật lý:'}
            </label>
            <textarea
              rows={6}
              value={rawText}
              onChange={(e) => setRawText(e.target.value)}
              placeholder="Nhập hoặc dán nội dung đề bài tại đây..."
              style={{
                width: '100%',
                padding: '12px',
                borderRadius: '10px',
                border: '1px solid #cbd5e1',
                fontSize: '14px',
                lineHeight: 1.5,
                outline: 'none',
                fontFamily: 'inherit',
                backgroundColor: '#fafafa',
              }}
            />
          </div>

          {/* Progress Indicator when Analyzing */}
          {isAnalyzing && (
            <div style={{ marginBottom: '20px', padding: '14px', backgroundColor: '#f0fdf4', borderRadius: '10px', border: '1px solid #bbf7d0' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px', fontWeight: 600, color: '#166534', marginBottom: '6px' }}>
                <span>{progressStage}</span>
                <span>{analysisProgress}%</span>
              </div>
              <div style={{ height: '8px', backgroundColor: '#dcfce7', borderRadius: '9999px', overflow: 'hidden' }}>
                <div
                  style={{
                    height: '100%',
                    width: `${analysisProgress}%`,
                    backgroundColor: '#16a34a',
                    transition: 'width 0.4s ease',
                  }}
                />
              </div>
            </div>
          )}

          {/* Action Buttons */}
          <div style={{ display: 'flex', gap: '12px' }}>
            <button
              onClick={handleStartAnalysis}
              disabled={isAnalyzing || !rawText.trim()}
              style={{
                flex: 1,
                padding: '12px',
                backgroundColor: isAnalyzing ? '#94a3b8' : '#2563eb',
                color: '#ffffff',
                border: 'none',
                borderRadius: '10px',
                fontWeight: 700,
                fontSize: '14px',
                cursor: isAnalyzing ? 'not-allowed' : 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '8px',
                boxShadow: isAnalyzing ? 'none' : '0 4px 12px rgba(37, 99, 235, 0.25)',
              }}
            >
              <Sparkles size={16} />
              <span>{isAnalyzing ? 'Đang trích xuất dữ liệu...' : 'Phân tích bài toán (Understanding Engine)'}</span>
            </button>
          </div>
        </div>

        {/* Right Column: Extracted Specifications & Ambiguity Status */}
        <div style={{ backgroundColor: '#ffffff', borderRadius: '16px', padding: '24px', border: '1px solid #e2e8f0', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px', borderBottom: '1px solid #e2e8f0', paddingBottom: '12px' }}>
              <h3 style={{ margin: 0, fontSize: '16px', fontWeight: 700, color: '#0f172a' }}>
                Đặc tả trích xuất (Specification JSON)
              </h3>
              <span style={{ fontSize: '12px', padding: '3px 8px', borderRadius: '4px', backgroundColor: '#f1f5f9', color: '#475569', fontWeight: 600 }}>
                Archetype: KINEMATICS
              </span>
            </div>

            {hasExtracted ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                {/* Extracted Quantities Table */}
                <div>
                  <div style={{ fontSize: '12px', fontWeight: 700, textTransform: 'uppercase', color: '#64748b', marginBottom: '8px' }}>
                    1. Các đại lượng vật lý đã nhận diện:
                  </div>
                  <div style={{ border: '1px solid #e2e8f0', borderRadius: '8px', overflow: 'hidden' }}>
                    <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '13px' }}>
                      <thead style={{ backgroundColor: '#f8fafc' }}>
                        <tr>
                          <th style={{ padding: '8px 12px', textAlign: 'left', borderBottom: '1px solid #e2e8f0' }}>Đại lượng</th>
                          <th style={{ padding: '8px 12px', textAlign: 'left', borderBottom: '1px solid #e2e8f0' }}>Ký hiệu</th>
                          <th style={{ padding: '8px 12px', textAlign: 'left', borderBottom: '1px solid #e2e8f0' }}>Giá trị</th>
                          <th style={{ padding: '8px 12px', textAlign: 'left', borderBottom: '1px solid #e2e8f0' }}>Loại</th>
                        </tr>
                      </thead>
                      <tbody>
                        {extractedQuantities.map((q, idx) => (
                          <tr key={idx} style={{ borderBottom: idx === extractedQuantities.length - 1 ? 'none' : '1px solid #f1f5f9' }}>
                            <td style={{ padding: '8px 12px', color: '#1e293b', fontWeight: 500 }}>{q.name}</td>
                            <td style={{ padding: '8px 12px', color: '#2563eb', fontWeight: 600 }}>{q.symbol}</td>
                            <td style={{ padding: '8px 12px', color: '#0f172a', fontWeight: 600 }}>
                              {q.value !== undefined ? `${q.value} ${q.unit}` : 'Chưa có (?)'}
                            </td>
                            <td style={{ padding: '8px 12px' }}>
                              <span
                                style={{
                                  fontSize: '11px',
                                  padding: '2px 6px',
                                  borderRadius: '4px',
                                  backgroundColor: q.is_given ? '#ecfdf5' : '#eff6ff',
                                  color: q.is_given ? '#047857' : '#1d4ed8',
                                  fontWeight: 600,
                                }}
                              >
                                {q.is_given ? 'Đã cho' : 'Cần tìm'}
                              </span>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>

                {/* Ambiguity Status Banner */}
                <div
                  style={{
                    padding: '14px',
                    borderRadius: '10px',
                    border: isAllAmbiguityResolved ? '1px solid #bbf7d0' : '1px solid #fecaca',
                    backgroundColor: isAllAmbiguityResolved ? '#f0fdf4' : '#fef2f2',
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                      {isAllAmbiguityResolved ? (
                        <CheckCircle2 size={22} color="#16a34a" />
                      ) : (
                        <AlertTriangle size={22} color="#dc2626" />
                      )}
                      <div>
                        <div style={{ fontSize: '13px', fontWeight: 700, color: isAllAmbiguityResolved ? '#166534' : '#991b1b' }}>
                          {isAllAmbiguityResolved ? 'Tất cả dữ kiện đã rõ ràng!' : 'Có 1 dữ kiện mơ hồ cần xác nhận!'}
                        </div>
                        <div style={{ fontSize: '12px', color: isAllAmbiguityResolved ? '#15803d' : '#b91c1c' }}>
                          {isAllAmbiguityResolved
                            ? 'Đã chọn: Góc ném chếch lên trên (+30°)'
                            : 'Đề bài chưa chỉ rõ phương hướng ném của góc 30°.'}
                        </div>
                      </div>
                    </div>

                    <button
                      onClick={() => setIsAmbiguityModalOpen(true)}
                      style={{
                        padding: '6px 14px',
                        backgroundColor: isAllAmbiguityResolved ? '#ffffff' : '#dc2626',
                        color: isAllAmbiguityResolved ? '#166534' : '#ffffff',
                        border: isAllAmbiguityResolved ? '1px solid #86efac' : 'none',
                        borderRadius: '6px',
                        fontSize: '12px',
                        fontWeight: 700,
                        cursor: 'pointer',
                      }}
                    >
                      {isAllAmbiguityResolved ? 'Xem lại lựa chọn' : 'Xác nhận ngay'}
                    </button>
                  </div>
                </div>
              </div>
            ) : (
              <div style={{ padding: '32px 0', textAlign: 'center', color: '#94a3b8' }}>
                Vui lòng nhập đề bài và nhấn <strong>Phân tích bài toán</strong> để xem đặc tả trích xuất.
              </div>
            )}
          </div>

          {/* Bottom Action: Proceed to Player (Blocked if not resolved) */}
          <div style={{ marginTop: '24px', paddingTop: '16px', borderTop: '1px solid #e2e8f0' }}>
            <button
              disabled={!isAllAmbiguityResolved}
              onClick={() => navigate('/player?id=sim-proj-001')}
              style={{
                width: '100%',
                padding: '14px',
                backgroundColor: isAllAmbiguityResolved ? '#10b981' : '#cbd5e1',
                color: '#ffffff',
                border: 'none',
                borderRadius: '10px',
                fontWeight: 700,
                fontSize: '15px',
                cursor: isAllAmbiguityResolved ? 'pointer' : 'not-allowed',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '8px',
                boxShadow: isAllAmbiguityResolved ? '0 4px 14px rgba(16, 185, 129, 0.3)' : 'none',
              }}
            >
              <Play size={16} fill="#ffffff" />
              <span>Khởi tạo phòng thí nghiệm mô phỏng (Interactive Player)</span>
            </button>
            {!isAllAmbiguityResolved && (
              <div style={{ fontSize: '12px', color: '#ef4444', textAlign: 'center', marginTop: '8px', fontWeight: 600 }}>
                * Bắt buộc giải quyết xong dữ kiện mơ hồ trước khi chuyển sang mô phỏng
              </div>
            )}
          </div>
        </div>
      </div>

      {/* AMBIGUITY CONFIRMATION MODAL (TASK H04) */}
      {isAmbiguityModalOpen && (
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
              maxWidth: '560px',
              width: '100%',
              padding: '28px',
              boxShadow: '0 20px 40px rgba(0,0,0,0.2)',
              animation: 'fadeIn 0.2s ease',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '14px' }}>
              <div
                style={{
                  width: '40px',
                  height: '40px',
                  borderRadius: '10px',
                  backgroundColor: '#fee2e2',
                  color: '#dc2626',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              >
                <AlertTriangle size={20} color="#dc2626" />
              </div>
              <div>
                <h3 style={{ margin: 0, fontSize: '18px', fontWeight: 800, color: '#0f172a' }}>
                  Xác nhận dữ kiện mơ hồ (Ambiguity Resolution)
                </h3>
                <span style={{ fontSize: '12px', color: '#64748b' }}>
                  Đặc tả bài toán cần sự khẳng định của Giáo viên để đảm bảo tính đúng đắn vật lý
                </span>
              </div>
            </div>

            {ambiguities.map((amb) => (
              <div key={amb.id} style={{ margin: '20px 0' }}>
                <p style={{ fontSize: '14px', fontWeight: 600, color: '#1e293b', marginBottom: '12px', lineHeight: 1.5 }}>
                  {amb.prompt_question}
                </p>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                  {amb.candidate_options.map((opt) => (
                    <label
                      key={String(opt.value)}
                      style={{
                        display: 'flex',
                        alignItems: 'flex-start',
                        gap: '12px',
                        padding: '12px 14px',
                        borderRadius: '10px',
                        border: amb.resolved_value === opt.value ? '2px solid #2563eb' : '1px solid #cbd5e1',
                        backgroundColor: amb.resolved_value === opt.value ? '#eff6ff' : '#f8fafc',
                        cursor: 'pointer',
                        transition: 'all 0.2s',
                      }}
                    >
                      <input
                        type="radio"
                        name={amb.id}
                        value={String(opt.value)}
                        checked={amb.resolved_value === opt.value}
                        onChange={() => handleResolveAmbiguity(amb.id, opt.value)}
                        style={{ marginTop: '3px' }}
                      />
                      <div>
                        <div style={{ fontSize: '13px', fontWeight: 700, color: '#0f172a' }}>
                          {opt.label}
                        </div>
                        {opt.description && (
                          <div style={{ fontSize: '12px', color: '#64748b', marginTop: '2px' }}>
                            {opt.description}
                          </div>
                        )}
                      </div>
                    </label>
                  ))}
                </div>
              </div>
            ))}

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '24px' }}>
              <button
                onClick={() => setIsAmbiguityModalOpen(false)}
                style={{
                  padding: '10px 18px',
                  backgroundColor: '#f1f5f9',
                  border: 'none',
                  borderRadius: '8px',
                  fontSize: '13px',
                  fontWeight: 600,
                  color: '#475569',
                  cursor: 'pointer',
                }}
              >
                Đóng lại
              </button>
              <button
                onClick={handleConfirmAmbiguities}
                style={{
                  padding: '10px 22px',
                  backgroundColor: '#2563eb',
                  border: 'none',
                  borderRadius: '8px',
                  fontSize: '13px',
                  fontWeight: 700,
                  color: '#ffffff',
                  cursor: 'pointer',
                }}
              >
                Xác nhận & Cập nhật bài toán
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
