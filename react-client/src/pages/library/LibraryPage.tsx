import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { MOCK_TOPICS } from '../../mocks/mockTaxonomy';
import { MOCK_SIMULATIONS } from '../../mocks/mockSimulations';
import type { Simulation } from '../../types/database';
import {
  Search,
  Filter,
  GraduationCap,
  Play,
  Share2,
  CheckCircle2,
  Sparkles,
  Plus,
  RotateCcw,
  Copy,
  Check,
  X,
  Atom,
  Activity,
  Layers,
  FileText,
  SlidersHorizontal,
} from 'lucide-react';

export const LibraryPage: React.FC = () => {
  const navigate = useNavigate();

  // Filter States (PhET Multi-dimensional Filter Model)
  const [selectedGrade, setSelectedGrade] = useState<string>('ALL');
  const [selectedTopic, setSelectedTopic] = useState<string>('ALL');
  const [selectedFeature, setSelectedFeature] = useState<string>('ALL');
  const [searchTerm, setSearchTerm] = useState<string>('');
  const [sortBy, setSortBy] = useState<'latest' | 'title'>('latest');

  // Embed Modal State
  const [embedSim, setEmbedSim] = useState<Simulation | null>(null);
  const [copiedEmbed, setCopiedEmbed] = useState<boolean>(false);

  // Filter matching
  const filteredSimulations = MOCK_SIMULATIONS.filter((sim) => {
    // 1. Cấp học: Mapping archetype to grade level (Ví dụ: Ném xiên, ô tô phanh, trượt dốc là Lớp 10 THPT)
    const simGrade = sim.spec_json.topic === 'CIRCUITS' ? '11' : '10';
    const matchGrade = selectedGrade === 'ALL' || simGrade === selectedGrade;

    // 2. Phân môn / Chủ đề
    const matchTopic = selectedTopic === 'ALL' || sim.spec_json.topic === selectedTopic;

    // 3. Tính năng đặc biệt (PhET Features)
    let matchFeature = true;
    if (selectedFeature === 'DUAL_VAL') {
      matchFeature = sim.validation_status === 'passed';
    } else if (selectedFeature === 'CALC_STEPS') {
      matchFeature = sim.spec_json.calculation_steps.length > 0;
    } else if (selectedFeature === 'VECTORS') {
      matchFeature = sim.spec_json.render_config.vectors.length > 0;
    }

    // 4. Tìm kiếm từ khóa
    const term = searchTerm.toLowerCase();
    const matchSearch =
      !term ||
      sim.title.toLowerCase().includes(term) ||
      sim.description.toLowerCase().includes(term) ||
      sim.spec_json.archetype.toLowerCase().includes(term);

    return matchGrade && matchTopic && matchFeature && matchSearch;
  });

  // Sort
  const sortedSimulations = [...filteredSimulations].sort((a, b) => {
    if (sortBy === 'title') {
      return a.title.localeCompare(b.title, 'vi');
    }
    return new Date(b.created_at).getTime() - new Date(a.created_at).getTime();
  });

  const handleResetFilters = () => {
    setSelectedGrade('ALL');
    setSelectedTopic('ALL');
    setSelectedFeature('ALL');
    setSearchTerm('');
  };

  const activeFiltersCount =
    (selectedGrade !== 'ALL' ? 1 : 0) +
    (selectedTopic !== 'ALL' ? 1 : 0) +
    (selectedFeature !== 'ALL' ? 1 : 0) +
    (searchTerm.trim() ? 1 : 0);

  const handleCopyEmbedCode = (simId: string) => {
    const embedCode = `<iframe src="${window.location.origin}/player?id=${simId}&embed=true" width="100%" height="580" frameborder="0" allowfullscreen="true" style="border-radius:12px; border:1px solid #cbd5e1;"></iframe>`;
    navigator.clipboard.writeText(embedCode);
    setCopiedEmbed(true);
    setTimeout(() => setCopiedEmbed(false), 2200);
  };

  // Render SVG Schematics for Sim Preview (PhET-style Interactive Lab Preview)
  const renderSimPreviewSchematic = (sim: Simulation) => {
    if (sim.id === 'sim-proj-001') {
      return (
        <svg viewBox="0 0 340 180" width="100%" height="100%" style={{ display: 'block' }}>
          <defs>
            <linearGradient id="skyGrad1" x1="0%" y1="0%" x2="0%" y2="100%">
              <stop offset="0%" stopColor="#0f172a" />
              <stop offset="100%" stopColor="#1e293b" />
            </linearGradient>
            <linearGradient id="trajGrad" x1="0%" y1="0%" x2="100%" y2="0%">
              <stop offset="0%" stopColor="#38bdf8" />
              <stop offset="50%" stopColor="#818cf8" />
              <stop offset="100%" stopColor="#f43f5e" />
            </linearGradient>
          </defs>
          <rect width="340" height="180" fill="url(#skyGrad1)" />

          {/* Grid lines */}
          <line x1="20" y1="150" x2="320" y2="150" stroke="#334155" strokeWidth="1.5" />
          <line x1="40" y1="20" x2="40" y2="150" stroke="#334155" strokeWidth="1.5" />

          {/* Cliff */}
          <rect x="20" y="80" width="40" height="70" fill="#475569" rx="2" />
          <text x="40" y="74" fill="#94a3b8" fontSize="10" textAnchor="middle" fontWeight="bold">h = 20m</text>

          {/* Trajectory Parabola */}
          <path
            d="M 60 80 Q 140 20 280 150"
            fill="none"
            stroke="url(#trajGrad)"
            strokeWidth="3"
            strokeDasharray="4 2"
          />

          {/* Velocity Vector */}
          <line x1="60" y1="80" x2="110" y2="50" stroke="#10b981" strokeWidth="2.5" markerEnd="url(#arrow)" />
          <circle cx="60" cy="80" r="5" fill="#38bdf8" />
          <text x="115" y="48" fill="#10b981" fontSize="11" fontWeight="bold">v₀ = 15 m/s</text>

          {/* Landing point */}
          <circle cx="280" cy="150" r="4" fill="#f43f5e" />
          <text x="280" y="166" fill="#f8fafc" fontSize="10" textAnchor="middle">Lmax = 38.5m</text>

          {/* Badge indicator */}
          <rect x="250" y="14" width="76" height="20" rx="10" fill="rgba(37,99,235,0.3)" stroke="#3b82f6" strokeWidth="1" />
          <text x="288" y="28" fill="#93c5fd" fontSize="9" textAnchor="middle" fontWeight="bold">NÉM XIÊN</text>
        </svg>
      );
    }

    if (sim.id === 'sim-incline-002') {
      return (
        <svg viewBox="0 0 340 180" width="100%" height="100%" style={{ display: 'block' }}>
          <defs>
            <linearGradient id="wedgeGrad" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stopColor="#334155" />
              <stop offset="100%" stopColor="#1e293b" />
            </linearGradient>
          </defs>
          <rect width="340" height="180" fill="#0f172a" />

          {/* Ground */}
          <line x1="20" y1="150" x2="320" y2="150" stroke="#475569" strokeWidth="2" />

          {/* Inclined Wedge */}
          <polygon points="40,150 280,150 40,40" fill="url(#wedgeGrad)" stroke="#64748b" strokeWidth="1.5" />
          <text x="75" y="142" fill="#cbd5e1" fontSize="11" fontWeight="bold">α = 30°</text>

          {/* Sliding Block */}
          <g transform="translate(130, 85) rotate(25)">
            <rect x="-16" y="-12" width="32" height="24" rx="3" fill="#3b82f6" stroke="#93c5fd" strokeWidth="1.5" />
            {/* Normal vector */}
            <line x1="0" y1="-12" x2="0" y2="-36" stroke="#38bdf8" strokeWidth="2" />
            <text x="6" y="-30" fill="#38bdf8" fontSize="10" fontWeight="bold">N</text>
            {/* Friction vector */}
            <line x1="-16" y1="0" x2="-40" y2="0" stroke="#ef4444" strokeWidth="2" />
            <text x="-48" y="-4" fill="#ef4444" fontSize="10" fontWeight="bold">Fms</text>
          </g>

          {/* Gravity Vector */}
          <line x1="130" y1="85" x2="130" y2="125" stroke="#f59e0b" strokeWidth="2" />
          <text x="138" y="118" fill="#f59e0b" fontSize="10" fontWeight="bold">P = mg</text>

          {/* Badge indicator */}
          <rect x="236" y="14" width="90" height="20" rx="10" fill="rgba(245,158,11,0.25)" stroke="#f59e0b" strokeWidth="1" />
          <text x="281" y="28" fill="#fcd34d" fontSize="9" textAnchor="middle" fontWeight="bold">MẶT NGHIÊNG</text>
        </svg>
      );
    }

    if (sim.id === 'sim-car-brake-003') {
      return (
        <svg viewBox="0 0 340 180" width="100%" height="100%" style={{ display: 'block' }}>
          <defs>
            <linearGradient id="roadGrad" x1="0%" y1="0%" x2="0%" y2="100%">
              <stop offset="0%" stopColor="#1e293b" />
              <stop offset="100%" stopColor="#0f172a" />
            </linearGradient>
          </defs>
          <rect width="340" height="180" fill="#090d16" />

          {/* Road */}
          <rect x="0" y="110" width="340" height="50" fill="url(#roadGrad)" />
          {/* Lane marking */}
          <line x1="10" y1="135" x2="330" y2="135" stroke="#facc15" strokeWidth="2" strokeDasharray="16 12" />

          {/* Skid Marks */}
          <line x1="100" y1="126" x2="240" y2="126" stroke="#475569" strokeWidth="3" strokeDasharray="8 4" opacity="0.8" />

          {/* Car Body */}
          <g transform="translate(180, 86)">
            {/* Chassis */}
            <path d="M -30 18 L -20 5 L 10 5 L 25 18 Z" fill="#3b82f6" />
            <rect x="-38" y="14" width="76" height="14" rx="3" fill="#1d4ed8" />
            {/* Wheels */}
            <circle cx="-22" cy="28" r="6" fill="#0f172a" stroke="#cbd5e1" strokeWidth="2" />
            <circle cx="22" cy="28" r="6" fill="#0f172a" stroke="#cbd5e1" strokeWidth="2" />
            {/* Headlights beam */}
            <polygon points="38,18 75,10 75,30 38,24" fill="rgba(250,204,21,0.18)" />
          </g>

          {/* Vectors */}
          <line x1="205" y1="80" x2="255" y2="80" stroke="#10b981" strokeWidth="2" />
          <text x="230" y="72" fill="#10b981" fontSize="10" fontWeight="bold">v₀ = 72 km/h</text>

          <line x1="155" y1="80" x2="115" y2="80" stroke="#ef4444" strokeWidth="2" />
          <text x="82" y="72" fill="#ef4444" fontSize="10" fontWeight="bold">a = -4 m/s²</text>

          {/* Distance bar */}
          <line x1="60" y1="168" x2="280" y2="168" stroke="#94a3b8" strokeWidth="1" />
          <text x="170" y="165" fill="#f8fafc" fontSize="10" textAnchor="middle" fontWeight="bold">
            s_phanh = 50.0 mét (Dừng hẳn)
          </text>

          {/* Badge */}
          <rect x="236" y="14" width="90" height="20" rx="10" fill="rgba(16,185,129,0.25)" stroke="#10b981" strokeWidth="1" />
          <text x="281" y="28" fill="#6ee7b7" fontSize="9" textAnchor="middle" fontWeight="bold">PHANH Ô TÔ</text>
        </svg>
      );
    }

    // Default Fallback
    return (
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%', color: '#94a3b8' }}>
        <Atom size={48} strokeWidth={1.5} color="#38bdf8" />
      </div>
    );
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '26px' }}>
      {/* ============================================================= */}
      {/* 1. HERO HEADER: PHONG CÁCH PHET INTERACTIVE LAB               */}
      {/* ============================================================= */}
      <div
        style={{
          background: 'linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #0369a1 100%)',
          borderRadius: '20px',
          padding: '30px 36px',
          color: '#ffffff',
          boxShadow: '0 12px 30px rgba(15, 23, 42, 0.15)',
          position: 'relative',
          overflow: 'hidden',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '20px',
        }}
      >
        <div style={{ maxWidth: '680px', zIndex: 2 }}>
          <div style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', background: 'rgba(56, 189, 248, 0.15)', border: '1px solid rgba(56, 189, 248, 0.3)', padding: '4px 12px', borderRadius: '9999px', fontSize: '12px', fontWeight: 700, color: '#38bdf8', marginBottom: '12px' }}>
            <Sparkles size={14} />
            <span>PhET-Inspired Interactive Simulations — Chuẩn SGK 2018</span>
          </div>

          <h1 style={{ margin: '0 0 10px', fontSize: '28px', fontWeight: 800, letterSpacing: '-0.02em', lineHeight: 1.25 }}>
            Thư viện Mô phỏng & Phòng thí nghiệm Vật lý
          </h1>
          <p style={{ margin: 0, fontSize: '14px', color: '#cbd5e1', lineHeight: 1.6 }}>
            Khảo sát chuyển động, phân tích vector lực và kiểm chứng nghiệm số với độ chính xác cao (sai số $\le 2\%$). Tự do thay đổi thông số hoặc nộp bài toán mới từ SGK để sinh mô phỏng AI.
          </p>
        </div>

        <div style={{ display: 'flex', gap: '12px', zIndex: 2 }}>
          <button
            onClick={() => navigate('/teacher/submit')}
            style={{
              padding: '12px 20px',
              backgroundColor: '#38bdf8',
              color: '#0f172a',
              borderRadius: '10px',
              fontWeight: 800,
              fontSize: '13px',
              border: 'none',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              boxShadow: '0 4px 16px rgba(56, 189, 248, 0.4)',
              transition: 'all 0.2s',
            }}
          >
            <Plus size={16} strokeWidth={3} />
            <span>Tạo mô phỏng bằng AI (OCR)</span>
          </button>
        </div>

        {/* Decorative background circle */}
        <div
          style={{
            position: 'absolute',
            right: '-60px',
            top: '-60px',
            width: '260px',
            height: '260px',
            borderRadius: '50%',
            background: 'radial-gradient(circle, rgba(56,189,248,0.2) 0%, transparent 70%)',
            zIndex: 1,
            pointerEvents: 'none',
          }}
        />
      </div>

      {/* ============================================================= */}
      {/* 2. TOP FILTER & SEARCH BAR                                    */}
      {/* ============================================================= */}
      <div
        style={{
          backgroundColor: '#ffffff',
          borderRadius: '16px',
          padding: '16px 20px',
          border: '1px solid #e2e8f0',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: '16px',
          flexWrap: 'wrap',
          boxShadow: '0 2px 8px rgba(15,23,42,0.03)',
        }}
      >
        {/* Search Input */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '10px',
            background: '#f8fafc',
            padding: '8px 14px',
            borderRadius: '10px',
            border: '1px solid #cbd5e1',
            flex: '1',
            minWidth: '260px',
          }}
        >
          <Search size={16} color="#64748b" />
          <input
            type="text"
            placeholder="Tìm kiếm theo tên hiện tượng, công thức, vật thể (ví dụ: ném xiên, phanh xe, dốc nghiêng)..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            style={{
              border: 'none',
              background: 'transparent',
              outline: 'none',
              fontSize: '13px',
              width: '100%',
              color: '#0f172a',
            }}
          />
          {searchTerm && (
            <button
              onClick={() => setSearchTerm('')}
              style={{ background: 'transparent', border: 'none', cursor: 'pointer', color: '#94a3b8', display: 'flex' }}
            >
              <X size={15} />
            </button>
          )}
        </div>

        {/* Sort & Count Controls */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
          <span style={{ fontSize: '13px', color: '#64748b', fontWeight: 600 }}>
            Hiển thị <strong style={{ color: '#0f172a' }}>{sortedSimulations.length}</strong> / {MOCK_SIMULATIONS.length} mô phỏng
          </span>

          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <span style={{ fontSize: '12px', color: '#64748b' }}>Sắp xếp:</span>
            <select
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value as any)}
              style={{
                padding: '6px 12px',
                borderRadius: '8px',
                border: '1px solid #cbd5e1',
                fontSize: '12px',
                fontWeight: 600,
                color: '#334155',
                outline: 'none',
                background: '#ffffff',
                cursor: 'pointer',
              }}
            >
              <option value="latest">Mới cập nhật</option>
              <option value="title">Theo tên A-Z</option>
            </select>
          </div>

          {activeFiltersCount > 0 && (
            <button
              onClick={handleResetFilters}
              style={{
                padding: '6px 12px',
                borderRadius: '8px',
                border: '1px solid #fed7aa',
                backgroundColor: '#fff7ed',
                color: '#ea580c',
                fontSize: '12px',
                fontWeight: 700,
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '5px',
              }}
            >
              <RotateCcw size={13} />
              <span>Xóa bộ lọc ({activeFiltersCount})</span>
            </button>
          )}
        </div>
      </div>

      {/* ============================================================= */}
      {/* 3. MAIN PHET LAYOUT: SIDEBAR BỘ LỌC + LƯỚI THẺ MÔ PHỎNG      */}
      {/* ============================================================= */}
      <div className="phet-library-container">
        {/* SIDEBAR BỘ LỌC ĐA CHIỀU (PHET FILTER SIDEBAR) */}
        <aside className="phet-filter-sidebar">
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '18px', paddingBottom: '12px', borderBottom: '1px solid #e2e8f0' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontWeight: 800, fontSize: '15px', color: '#0f172a' }}>
              <Filter size={18} color="#2563eb" />
              <span>Bộ lọc nâng cao</span>
            </div>
            {activeFiltersCount > 0 && (
              <span style={{ fontSize: '11px', padding: '2px 8px', borderRadius: '12px', backgroundColor: '#eff6ff', color: '#2563eb', fontWeight: 700 }}>
                {activeFiltersCount} đang chọn
              </span>
            )}
          </div>

          {/* Filter Group 1: Cấp học (Grade Level) */}
          <div className="phet-filter-group">
            <div className="phet-filter-title">
              <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <GraduationCap size={15} color="#64748b" />
                Cấp học (THPT)
              </span>
            </div>

            <button
              className={`phet-filter-item-btn ${selectedGrade === 'ALL' ? 'active' : ''}`}
              onClick={() => setSelectedGrade('ALL')}
            >
              <span>Tất cả khối lớp</span>
              <span className="phet-filter-badge">3</span>
            </button>
            <button
              className={`phet-filter-item-btn ${selectedGrade === '10' ? 'active' : ''}`}
              onClick={() => setSelectedGrade('10')}
            >
              <span>Vật lý Lớp 10 (Cơ học)</span>
              <span className="phet-filter-badge">3</span>
            </button>
            <button
              className={`phet-filter-item-btn ${selectedGrade === '11' ? 'active' : ''}`}
              onClick={() => setSelectedGrade('11')}
            >
              <span>Vật lý Lớp 11 (Điện & Sóng)</span>
              <span className="phet-filter-badge">1</span>
            </button>
            <button
              className={`phet-filter-item-btn ${selectedGrade === '12' ? 'active' : ''}`}
              onClick={() => setSelectedGrade('12')}
            >
              <span>Vật lý Lớp 12 (Hạt nhân)</span>
              <span className="phet-filter-badge">0</span>
            </button>
          </div>

          {/* Filter Group 2: Chủ đề môn học (Physics Topics) */}
          <div className="phet-filter-group">
            <div className="phet-filter-title">
              <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <Layers size={15} color="#64748b" />
                Chủ đề Vật lý
              </span>
            </div>

            <button
              className={`phet-filter-item-btn ${selectedTopic === 'ALL' ? 'active' : ''}`}
              onClick={() => setSelectedTopic('ALL')}
            >
              <span>Tất cả chủ đề</span>
              <span className="phet-filter-badge">{MOCK_TOPICS.length}</span>
            </button>

            {MOCK_TOPICS.map((topic) => {
              const count = MOCK_SIMULATIONS.filter(s => s.spec_json.topic === topic.code).length;
              return (
                <button
                  key={topic.id}
                  className={`phet-filter-item-btn ${selectedTopic === topic.code ? 'active' : ''}`}
                  onClick={() => setSelectedTopic(topic.code)}
                >
                  <span style={{ textOverflow: 'ellipsis', overflow: 'hidden', whiteSpace: 'nowrap' }}>
                    {topic.name.split('(')[0]}
                  </span>
                  <span className="phet-filter-badge">{count}</span>
                </button>
              );
            })}
          </div>

          {/* Filter Group 3: Tính năng tương tác (PhET Features) */}
          <div className="phet-filter-group">
            <div className="phet-filter-title">
              <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <SlidersHorizontal size={15} color="#64748b" />
                Tính năng phòng Lab
              </span>
            </div>

            <button
              className={`phet-filter-item-btn ${selectedFeature === 'ALL' ? 'active' : ''}`}
              onClick={() => setSelectedFeature('ALL')}
            >
              <span>Tất cả tính năng</span>
            </button>
            <button
              className={`phet-filter-item-btn ${selectedFeature === 'DUAL_VAL' ? 'active' : ''}`}
              onClick={() => setSelectedFeature('DUAL_VAL')}
            >
              <span>Nghiệm số Dual-Validation</span>
              <CheckCircle2 size={13} color="#10b981" />
            </button>
            <button
              className={`phet-filter-item-btn ${selectedFeature === 'CALC_STEPS' ? 'active' : ''}`}
              onClick={() => setSelectedFeature('CALC_STEPS')}
            >
              <span>Lời giải LaTeX từng bước</span>
              <FileText size={13} color="#3b82f6" />
            </button>
            <button
              className={`phet-filter-item-btn ${selectedFeature === 'VECTORS' ? 'active' : ''}`}
              onClick={() => setSelectedFeature('VECTORS')}
            >
              <span>Vector lực & Vận tốc trực quan</span>
              <Activity size={13} color="#f59e0b" />
            </button>
          </div>
        </aside>

        {/* LƯỚI THẺ MÔ PHỎNG VISUAL LAB (PHET SIMULATION CARDS) */}
        <main>
          {sortedSimulations.length === 0 ? (
            <div
              style={{
                backgroundColor: '#ffffff',
                borderRadius: '16px',
                border: '1px dashed #cbd5e1',
                padding: '60px 20px',
                textAlign: 'center',
              }}
            >
              <Atom size={48} color="#94a3b8" style={{ margin: '0 auto 16px' }} />
              <h3 style={{ margin: '0 0 8px', fontSize: '18px', fontWeight: 700, color: '#0f172a' }}>
                Không tìm thấy mô phỏng nào phù hợp
              </h3>
              <p style={{ margin: '0 0 20px', fontSize: '14px', color: '#64748b' }}>
                Vui lòng thử điều chỉnh bộ lọc hoặc từ khoá tìm kiếm của bạn.
              </p>
              <button
                onClick={handleResetFilters}
                style={{
                  padding: '9px 20px',
                  backgroundColor: '#2563eb',
                  color: '#ffffff',
                  borderRadius: '8px',
                  border: 'none',
                  fontWeight: 600,
                  fontSize: '13px',
                  cursor: 'pointer',
                }}
              >
                Xóa bộ lọc tìm kiếm
              </button>
            </div>
          ) : (
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))',
                gap: '24px',
              }}
            >
              {sortedSimulations.map((sim) => (
                <div key={sim.id} className="phet-sim-card">
                  {/* PREVIEW THUMBNAIL AREA VỚI HIỆU ỨNG HOVER PLAY PHET */}
                  <div
                    className="phet-preview-area"
                    onClick={() => navigate(`/player?id=${sim.id}`)}
                    title={`Mở mô phỏng: ${sim.title}`}
                  >
                    {renderSimPreviewSchematic(sim)}

                    {/* OVERLAY PLAY BUTTON */}
                    <div className="phet-preview-overlay">
                      <div className="phet-play-pulse-btn">
                        <Play size={22} fill="#ffffff" style={{ marginLeft: '3px' }} />
                      </div>
                      <span style={{ color: '#ffffff', fontSize: '12px', fontWeight: 700, textShadow: '0 2px 4px rgba(0,0,0,0.8)' }}>
                        Khám phá phòng Lab 🚀
                      </span>
                    </div>
                  </div>

                  {/* CARD BODY METADATA */}
                  <div style={{ padding: '20px', display: 'flex', flexDirection: 'column', flex: 1 }}>
                    {/* Top tags */}
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '10px' }}>
                      <div style={{ display: 'flex', gap: '6px' }}>
                        <span
                          style={{
                            fontSize: '11px',
                            padding: '3px 8px',
                            borderRadius: '6px',
                            backgroundColor: '#eff6ff',
                            color: '#1d4ed8',
                            fontWeight: 700,
                          }}
                        >
                          Lớp 10 THPT
                        </span>
                        <span
                          style={{
                            fontSize: '11px',
                            padding: '3px 8px',
                            borderRadius: '6px',
                            backgroundColor: '#f1f5f9',
                            color: '#475569',
                            fontWeight: 600,
                          }}
                        >
                          {sim.spec_json.topic}
                        </span>
                      </div>

                      <span
                        style={{
                          fontSize: '11px',
                          display: 'flex',
                          alignItems: 'center',
                          gap: '4px',
                          color: '#10b981',
                          fontWeight: 700,
                        }}
                      >
                        <CheckCircle2 size={13} />
                        <span>Sai số &lt; 0.5%</span>
                      </span>
                    </div>

                    {/* Title */}
                    <h3
                      style={{
                        margin: '0 0 8px',
                        fontSize: '16px',
                        fontWeight: 700,
                        color: '#0f172a',
                        lineHeight: 1.4,
                      }}
                    >
                      {sim.title}
                    </h3>

                    {/* Description */}
                    <p
                      style={{
                        margin: '0 0 16px',
                        fontSize: '13px',
                        color: '#64748b',
                        lineHeight: 1.5,
                        display: '-webkit-box',
                        WebkitLineClamp: 2,
                        WebkitBoxOrient: 'vertical',
                        overflow: 'hidden',
                        flex: 1,
                      }}
                    >
                      {sim.description}
                    </p>

                    {/* Simulation Parameters chips */}
                    <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap', marginBottom: '18px' }}>
                      {sim.spec_json.variables.map((v) => (
                        <span
                          key={v.id}
                          style={{
                            fontSize: '11px',
                            backgroundColor: '#f8fafc',
                            border: '1px solid #e2e8f0',
                            padding: '2px 8px',
                            borderRadius: '6px',
                            color: '#334155',
                            fontFamily: 'Consolas, monospace',
                            fontWeight: 600,
                          }}
                        >
                          {v.symbol}: {v.default_value}{v.unit}
                        </span>
                      ))}
                    </div>

                    {/* Actions footer */}
                    <div
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        paddingTop: '14px',
                        borderTop: '1px solid #f1f5f9',
                        gap: '8px',
                      }}
                    >
                      <button
                        onClick={() => setEmbedSim(sim)}
                        style={{
                          padding: '7px 12px',
                          borderRadius: '8px',
                          border: '1px solid #cbd5e1',
                          backgroundColor: '#ffffff',
                          color: '#475569',
                          fontSize: '12px',
                          fontWeight: 600,
                          cursor: 'pointer',
                          display: 'flex',
                          alignItems: 'center',
                          gap: '5px',
                        }}
                        title="Lấy mã nhúng bài giảng LMS"
                      >
                        <Share2 size={13} />
                        <span>Mã nhúng</span>
                      </button>

                      <Link
                        to={`/player?id=${sim.id}`}
                        style={{
                          padding: '8px 16px',
                          backgroundColor: '#2563eb',
                          color: '#ffffff',
                          borderRadius: '8px',
                          fontSize: '13px',
                          fontWeight: 700,
                          textDecoration: 'none',
                          display: 'flex',
                          alignItems: 'center',
                          gap: '6px',
                          boxShadow: '0 2px 6px rgba(37, 99, 235, 0.25)',
                        }}
                      >
                        <Play size={14} fill="#ffffff" />
                        <span>Chạy mô phỏng</span>
                      </Link>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </main>
      </div>

      {/* ============================================================= */}
      {/* 4. MODAL NHÚNG BÀI GIẢNG LMS (EMBED CODE DIALOG)              */}
      {/* ============================================================= */}
      {embedSim && (
        <div className="phet-modal-backdrop" onClick={() => setEmbedSim(null)}>
          <div className="phet-modal-content" onClick={(e) => e.stopPropagation()}>
            <div style={{ padding: '20px 24px', borderBottom: '1px solid #e2e8f0', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <Share2 size={20} color="#2563eb" />
                <h3 style={{ margin: 0, fontSize: '17px', fontWeight: 800, color: '#0f172a' }}>
                  Lấy mã nhúng mô phỏng vào LMS
                </h3>
              </div>
              <button
                onClick={() => setEmbedSim(null)}
                style={{ background: 'transparent', border: 'none', cursor: 'pointer', color: '#64748b' }}
              >
                <X size={20} />
              </button>
            </div>

            <div style={{ padding: '24px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <div>
                <span style={{ fontSize: '12px', fontWeight: 700, color: '#475569' }}>Bài toán:</span>
                <div style={{ fontSize: '14px', fontWeight: 700, color: '#0f172a', marginTop: '2px' }}>
                  {embedSim.title}
                </div>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: '#334155', marginBottom: '6px' }}>
                  Đoạn mã nhúng HTML (iframe):
                </label>
                <div
                  style={{
                    backgroundColor: '#0f172a',
                    padding: '14px',
                    borderRadius: '10px',
                    color: '#38bdf8',
                    fontFamily: 'Consolas, monospace',
                    fontSize: '12px',
                    lineHeight: 1.5,
                    wordBreak: 'break-all',
                    border: '1px solid #334155',
                  }}
                >
                  {`<iframe src="${window.location.origin}/player?id=${embedSim.id}&embed=true" width="100%" height="580" frameborder="0" allowfullscreen="true" style="border-radius:12px; border:1px solid #cbd5e1;"></iframe>`}
                </div>
              </div>

              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingTop: '8px' }}>
                <div style={{ fontSize: '12px', color: '#64748b' }}>
                  Hỗ trợ: Canvas LMS, Moodle, Google Sites, Microsoft Teams, PowerPoint.
                </div>
                <button
                  onClick={() => handleCopyEmbedCode(embedSim.id)}
                  style={{
                    padding: '9px 18px',
                    backgroundColor: copiedEmbed ? '#10b981' : '#2563eb',
                    color: '#ffffff',
                    borderRadius: '8px',
                    border: 'none',
                    fontWeight: 700,
                    fontSize: '13px',
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '6px',
                    transition: 'all 0.2s',
                  }}
                >
                  {copiedEmbed ? (
                    <>
                      <Check size={16} />
                      <span>Đã sao chép!</span>
                    </>
                  ) : (
                    <>
                      <Copy size={16} />
                      <span>Sao chép mã nhúng</span>
                    </>
                  )}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
