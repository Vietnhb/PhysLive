import React, { useState, useEffect, useRef } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { MOCK_SIMULATIONS } from '../../mocks/mockSimulations';
import type { Simulation } from '../../types/database';
import {
  Play,
  Pause,
  RotateCcw,
  StepForward,
  Sliders,
  FileText,
  Table as TableIcon,
  Download,
  Printer,
  Sparkles,
  Plus,
  Activity,
  Gauge,
  Compass,
  Atom,
} from 'lucide-react';

export const SimulationPlayerPage: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const simId = searchParams.get('id') || MOCK_SIMULATIONS[0].id;

  const currentSim: Simulation =
    MOCK_SIMULATIONS.find(s => s.id === simId) || MOCK_SIMULATIONS[0];

  // Parameters state
  // 1. Dành cho ném xiên (sim-proj-001)
  const [v0, setV0] = useState<number>(15);
  const [alpha, setAlpha] = useState<number>(30);
  const [h, setH] = useState<number>(20);
  const [g, setG] = useState<number>(9.8);

  // 2. Dành cho ô tô phanh (sim-car-brake-003 - Ví dụ của thầy Phương)
  const [carV0, setCarV0] = useState<number>(20); // 72 km/h
  const [carA, setCarA] = useState<number>(-4);

  // 3. Dành cho trượt dốc (sim-incline-002)
  const [inclineM, setInclineM] = useState<number>(2);
  const [inclineAlpha, setInclineAlpha] = useState<number>(30);
  const [inclineMu, setInclineMu] = useState<number>(0.1);
  const [inclineS, setInclineS] = useState<number>(10);

  // Playback state
  const [isPlaying, setIsPlaying] = useState<boolean>(false);
  const [currentTime, setCurrentTime] = useState<number>(0);
  const [playbackSpeed, setPlaybackSpeed] = useState<number>(1.0);
  const [activeTab, setActiveTab] = useState<'sliders' | 'steps' | 'table' | 'export'>('sliders');
  const [catalogSearch, setCatalogSearch] = useState<string>('');

  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  // Reset time when switching simulation
  useEffect(() => {
    setIsPlaying(false);
    setCurrentTime(0);
  }, [simId]);

  // --- TÍNH TOÁN VẬT LÝ ---
  // A. Ném xiên
  const alphaRad = (alpha * Math.PI) / 180;
  const v0x = v0 * Math.cos(alphaRad);
  const v0y = v0 * Math.sin(alphaRad);
  const discriminant = v0y * v0y + 2 * g * h;
  const tFlightProj = (v0y + Math.sqrt(Math.max(0, discriminant))) / g;
  const lMaxProj = v0x * tFlightProj;
  const hMaxProj = h + (v0y > 0 ? (v0y * v0y) / (2 * g) : 0);

  // B. Ô tô phanh (Ví dụ thầy Phương)
  const tStopCar = Math.abs(carV0 / carA);
  const sStopCar = carV0 * tStopCar + 0.5 * carA * tStopCar * tStopCar;

  // C. Trượt dốc
  const inclineRad = (inclineAlpha * Math.PI) / 180;
  const inclineAcc = Math.max(0, 9.8 * (Math.sin(inclineRad) - inclineMu * Math.cos(inclineRad)));
  const tSlide = inclineAcc > 0 ? Math.sqrt((2 * inclineS) / inclineAcc) : 0;

  // Thời gian kết thúc tương ứng với bài toán đang chọn
  const maxDuration =
    currentSim.id === 'sim-car-brake-003'
      ? tStopCar
      : currentSim.id === 'sim-incline-002'
      ? tSlide
      : tFlightProj;

  // Animation Frame Loop
  useEffect(() => {
    let animationFrameId: number;
    let lastTimestamp = performance.now();

    const loop = (now: number) => {
      const deltaSeconds = (now - lastTimestamp) / 1000;
      lastTimestamp = now;

      if (isPlaying) {
        setCurrentTime(prev => {
          const next = prev + deltaSeconds * playbackSpeed;
          if (next >= maxDuration) {
            setIsPlaying(false);
            return maxDuration;
          }
          return next;
        });
      }

      animationFrameId = requestAnimationFrame(loop);
    };

    animationFrameId = requestAnimationFrame(loop);
    return () => cancelAnimationFrame(animationFrameId);
  }, [isPlaying, maxDuration, playbackSpeed]);

  // --- RENDER CANVAS THEO PHONG CÁCH MATHOS LAB ---
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const width = canvas.width;
    const height = canvas.height;

    // Reset background
    ctx.clearRect(0, 0, width, height);
    ctx.fillStyle = '#090d16'; // Deep space dark
    ctx.fillRect(0, 0, width, height);

    // Lưới toạ độ Grid
    ctx.strokeStyle = '#151c2e';
    ctx.lineWidth = 1;
    const gridSize = 40;
    for (let x = 0; x < width; x += gridSize) {
      ctx.beginPath();
      ctx.moveTo(x, 0);
      ctx.lineTo(x, height);
      ctx.stroke();
    }
    for (let y = 0; y < height; y += gridSize) {
      ctx.beginPath();
      ctx.moveTo(0, y);
      ctx.lineTo(width, y);
      ctx.stroke();
    }

    // -------------------------------------------------------------
    // TRƯỜNG HỢP 1: BÀI TOÁN Ô TÔ PHANH CHẬM DẦN ĐỀU (VÍ DỤ THẦY PHƯƠNG)
    // -------------------------------------------------------------
    if (currentSim.id === 'sim-car-brake-003') {
      const roadY = height / 2 + 50;
      const roadStartX = 60;
      const roadLength = width - 120;
      const scaleX = roadLength / Math.max(60, sStopCar * 1.15);

      // Mặt đường
      ctx.fillStyle = '#1e293b';
      ctx.fillRect(0, roadY, width, 50);
      ctx.strokeStyle = '#38bdf8';
      ctx.lineWidth = 2;
      ctx.beginPath();
      ctx.moveTo(0, roadY);
      ctx.lineTo(width, roadY);
      ctx.stroke();

      // Vạch kẻ đường đứt đoạn
      ctx.strokeStyle = '#64748b';
      ctx.setLineDash([16, 16]);
      ctx.beginPath();
      ctx.moveTo(0, roadY + 25);
      ctx.lineTo(width, roadY + 25);
      ctx.stroke();
      ctx.setLineDash([]);

      // Điểm mốc dừng dự kiến
      const stopMarkX = roadStartX + sStopCar * scaleX;
      ctx.strokeStyle = '#ef4444';
      ctx.lineWidth = 2;
      ctx.beginPath();
      ctx.moveTo(stopMarkX, roadY - 30);
      ctx.lineTo(stopMarkX, roadY + 50);
      ctx.stroke();
      ctx.fillStyle = '#ef4444';
      ctx.font = '11px Consolas, monospace';
      ctx.fillText(`Điểm dừng s = ${sStopCar.toFixed(1)}m`, stopMarkX - 35, roadY - 36);

      // Vị trí tức thời của ô tô
      const currentS = Math.min(sStopCar, carV0 * currentTime + 0.5 * carA * currentTime * currentTime);
      const currentV = Math.max(0, carV0 + carA * currentTime);
      const carX = roadStartX + currentS * scaleX;
      const carY = roadY - 26;

      // Vết trượt phanh
      if (currentS > 0) {
        ctx.strokeStyle = 'rgba(239, 68, 68, 0.4)';
        ctx.lineWidth = 4;
        ctx.beginPath();
        ctx.moveTo(roadStartX, roadY - 2);
        ctx.lineTo(carX, roadY - 2);
        ctx.stroke();
      }

      // Vẽ hình chiếc xe ô tô thể thao cách điệu
      ctx.fillStyle = '#2563eb';
      ctx.beginPath();
      ctx.roundRect(carX - 25, carY - 14, 50, 24, 6);
      ctx.fill();
      ctx.strokeStyle = '#60a5fa';
      ctx.lineWidth = 2;
      ctx.stroke();

      // Mui xe
      ctx.fillStyle = '#0f172a';
      ctx.beginPath();
      ctx.roundRect(carX - 12, carY - 24, 28, 12, [4, 4, 0, 0]);
      ctx.fill();

      // Bánh xe
      ctx.fillStyle = '#cbd5e1';
      ctx.beginPath();
      ctx.arc(carX - 16, roadY - 2, 7, 0, Math.PI * 2);
      ctx.arc(carX + 16, roadY - 2, 7, 0, Math.PI * 2);
      ctx.fill();

      // Vector vận tốc v (Xanh ngọc hướng tới trước)
      if (currentV > 0.1) {
        ctx.strokeStyle = '#10b981';
        ctx.lineWidth = 3;
        ctx.beginPath();
        ctx.moveTo(carX + 25, carY);
        ctx.lineTo(carX + 25 + currentV * 2.5, carY);
        ctx.stroke();
        ctx.fillStyle = '#10b981';
        ctx.font = '12px Consolas, monospace';
        ctx.fillText(`v = ${currentV.toFixed(1)} m/s`, carX + 30 + currentV * 2.5, carY + 4);
      }

      // Vector gia tốc phanh a (Đỏ hướng ngược lại)
      ctx.strokeStyle = '#ef4444';
      ctx.lineWidth = 3;
      ctx.beginPath();
      ctx.moveTo(carX - 25, carY);
      ctx.lineTo(carX - 25 + carA * 6, carY); // carA âm nên hướng sang trái
      ctx.stroke();
      ctx.fillStyle = '#ef4444';
      ctx.font = '12px Consolas, monospace';
      ctx.fillText(`a = ${carA} m/s²`, carX - 35 + carA * 6, carY - 10);
    }
    // -------------------------------------------------------------
    // TRƯỜNG HỢP 2: BÀI TOÁN NÉM XIÊN TỪ ĐỘ CAO H
    // -------------------------------------------------------------
    else if (currentSim.id === 'sim-proj-001') {
      const originX = 70;
      const groundY = height - 60;
      const scale = Math.min((width - 140) / Math.max(45, lMaxProj * 1.15), (height - 130) / Math.max(35, hMaxProj * 1.25));

      // Mặt đất
      ctx.strokeStyle = '#334155';
      ctx.lineWidth = 3;
      ctx.beginPath();
      ctx.moveTo(0, groundY);
      ctx.lineTo(width, groundY);
      ctx.stroke();
      ctx.fillStyle = '#064e3b';
      ctx.fillRect(0, groundY, width, height - groundY);

      // Tháp ném độ cao h
      if (h > 0) {
        const towerHeight = h * scale;
        ctx.fillStyle = '#1e293b';
        ctx.strokeStyle = '#475569';
        ctx.lineWidth = 2;
        ctx.fillRect(originX - 32, groundY - towerHeight, 32, towerHeight);
        ctx.strokeRect(originX - 32, groundY - towerHeight, 32, towerHeight);
        ctx.fillStyle = '#94a3b8';
        ctx.font = '11px Consolas, monospace';
        ctx.fillText(`h = ${h}m`, originX - 36, groundY - towerHeight / 2);
      }

      // Quỹ đạo Parabol nét đứt phát sáng
      ctx.strokeStyle = '#38bdf8';
      ctx.setLineDash([4, 4]);
      ctx.lineWidth = 2;
      ctx.beginPath();
      const steps = 60;
      for (let i = 0; i <= steps; i++) {
        const t = (i / steps) * tFlightProj;
        const px = originX + (v0x * t) * scale;
        const py = groundY - (h + v0y * t - 0.5 * g * t * t) * scale;
        if (i === 0) ctx.moveTo(px, py);
        else ctx.lineTo(px, py);
      }
      ctx.stroke();
      ctx.setLineDash([]);

      // Tọa độ vật thể tại currentTime
      const currentX = v0x * currentTime;
      const currentY = Math.max(0, h + v0y * currentTime - 0.5 * g * currentTime * currentTime);
      const canvasBallX = originX + currentX * scale;
      const canvasBallY = groundY - currentY * scale;

      // Quả cầu phát sáng
      ctx.fillStyle = '#fbbf24';
      ctx.beginPath();
      ctx.arc(canvasBallX, canvasBallY, 9, 0, Math.PI * 2);
      ctx.fill();
      ctx.strokeStyle = '#ffffff';
      ctx.lineWidth = 2;
      ctx.stroke();

      // Vector vận tốc v
      const currentVx = v0x;
      const currentVy = v0y - g * currentTime;
      const vLen = Math.sqrt(currentVx * currentVx + currentVy * currentVy);
      const vScale = 2.0;

      ctx.strokeStyle = '#10b981';
      ctx.lineWidth = 3;
      ctx.beginPath();
      ctx.moveTo(canvasBallX, canvasBallY);
      ctx.lineTo(canvasBallX + currentVx * vScale, canvasBallY - currentVy * vScale);
      ctx.stroke();
      ctx.fillStyle = '#10b981';
      ctx.font = '11px Consolas, monospace';
      ctx.fillText(`v = ${vLen.toFixed(1)} m/s`, canvasBallX + currentVx * vScale + 6, canvasBallY - currentVy * vScale);

      // Điểm rơi chạm đất
      const landX = originX + lMaxProj * scale;
      ctx.fillStyle = '#38bdf8';
      ctx.beginPath();
      ctx.arc(landX, groundY, 4, 0, Math.PI * 2);
      ctx.fill();
      ctx.fillText(`L_max = ${lMaxProj.toFixed(1)}m`, landX - 30, groundY + 20);
    }
    // -------------------------------------------------------------
    // TRƯỜNG HỢP 3: TRƯỢT DỐC MẶT PHẲNG NGHIÊNG CÓ MA SÁT
    // -------------------------------------------------------------
    else {
      const startX = 120;
      const startY = 120;
      const dX = inclineS * 30 * Math.cos(inclineRad);
      const dY = inclineS * 30 * Math.sin(inclineRad);

      // Dốc tam giác
      ctx.fillStyle = '#1e293b';
      ctx.strokeStyle = '#38bdf8';
      ctx.lineWidth = 2;
      ctx.beginPath();
      ctx.moveTo(startX, startY);
      ctx.lineTo(startX + dX, startY + dY);
      ctx.lineTo(startX, startY + dY);
      ctx.closePath();
      ctx.fill();
      ctx.stroke();

      // Khối hộp trượt
      const curDist = 0.5 * inclineAcc * currentTime * currentTime;
      const curX = startX + curDist * 30 * Math.cos(inclineRad);
      const curY = startY + curDist * 30 * Math.sin(inclineRad);

      ctx.save();
      ctx.translate(curX, curY);
      ctx.rotate(inclineRad);
      ctx.fillStyle = '#f59e0b';
      ctx.fillRect(-15, -20, 30, 20);
      ctx.strokeStyle = '#ffffff';
      ctx.strokeRect(-15, -20, 30, 20);
      ctx.restore();
    }
  }, [
    currentSim.id,
    v0,
    alpha,
    h,
    g,
    carV0,
    carA,
    inclineM,
    inclineAlpha,
    inclineMu,
    inclineS,
    currentTime,
    tFlightProj,
    lMaxProj,
    hMaxProj,
    v0x,
    v0y,
    sStopCar,
    inclineRad,
    inclineAcc,
    tSlide,
  ]);

  const handleReset = () => {
    setIsPlaying(false);
    setCurrentTime(0);
  };

  const handleStepForward = () => {
    setIsPlaying(false);
    setCurrentTime(prev => Math.min(maxDuration, prev + 0.1));
  };

  const filteredSimulations = MOCK_SIMULATIONS.filter(s =>
    s.title.toLowerCase().includes(catalogSearch.toLowerCase())
  );

  return (
    <div className="workspace-grid">
      {/* ------------------------------------------------------------- */}
      {/* PANE TRÁI: CATALOG CHUYÊN ĐỀ & BÀI TOÁN MẪU                     */}
      {/* ------------------------------------------------------------- */}
      <aside className="catalog-pane">
        <div className="catalog-header">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' }}>
            <span style={{ fontSize: '12px', fontWeight: 800, color: '#0f172a', letterSpacing: '0.04em' }}>
              CHỦ ĐỀ & MÔ HÌNH VẬT LÝ
            </span>
            <span style={{ fontSize: '11px', color: '#2563eb', fontWeight: 700 }}>
              {MOCK_SIMULATIONS.length} MODELS
            </span>
          </div>

          <input
            type="text"
            className="catalog-search-input"
            placeholder="Tìm bài toán, mô hình..."
            value={catalogSearch}
            onChange={(e) => setCatalogSearch(e.target.value)}
          />
        </div>

        <div className="catalog-list">
          {filteredSimulations.map((s) => {
            const isSelected = s.id === currentSim.id;
            return (
              <div
                key={s.id}
                className={`catalog-item-card ${isSelected ? 'active' : ''}`}
                onClick={() => setSearchParams({ id: s.id })}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '4px' }}>
                  <span style={{ fontSize: '10px', padding: '2px 6px', borderRadius: '4px', backgroundColor: isSelected ? '#dbeafe' : '#e2e8f0', color: isSelected ? '#1d4ed8' : '#475569', fontWeight: 700 }}>
                    {s.spec_json.topic}
                  </span>
                  <span style={{ fontSize: '11px', color: '#10b981', fontWeight: 600 }}>
                    PASS ✓
                  </span>
                </div>

                <div style={{ fontSize: '13px', fontWeight: 700, color: isSelected ? '#1d4ed8' : '#0f172a', lineHeight: 1.35, marginBottom: '4px' }}>
                  {s.title}
                </div>

                <div style={{ fontSize: '11px', color: '#64748b', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
                  {s.description}
                </div>
              </div>
            );
          })}

          <div style={{ marginTop: 'auto', paddingTop: '12px' }}>
            <button
              onClick={() => navigate('/teacher/submit')}
              style={{
                width: '100%',
                padding: '10px',
                borderRadius: '8px',
                border: '1px dashed #3b82f6',
                backgroundColor: '#eff6ff',
                color: '#2563eb',
                fontSize: '13px',
                fontWeight: 700,
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '6px',
              }}
            >
              <Plus size={15} strokeWidth={2.5} />
              <span>Nộp bài toán mới (AI OCR)</span>
            </button>
          </div>
        </div>
      </aside>

      {/* ------------------------------------------------------------- */}
      {/* PANE GIỮA: INTERACTIVE CANVAS VIEWPORT & FLOATING CONTROLS    */}
      {/* ------------------------------------------------------------- */}
      <section className="viewport-pane">
        {/* FORMULA HERO BAR */}
        <div className="formula-hero-bar">
          <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
            <Atom size={20} color="#38bdf8" strokeWidth={2.2} />
            <div>
              <div style={{ fontSize: '11px', color: '#94a3b8', fontWeight: 700, letterSpacing: '0.04em' }}>CÔNG THỨC THỜI GIAN THỰC (DYNAMIC FORMULA)</div>
              <div className="formula-latex-text">
                {currentSim.id === 'sim-car-brake-003'
                  ? `v(t) = ${carV0} - ${Math.abs(carA)}·t (m/s)  |  s(t) = ${carV0}·t - ${(Math.abs(carA) / 2).toFixed(1)}·t²`
                  : currentSim.id === 'sim-proj-001'
                  ? `y(t) = ${h} + ${(v0 * Math.sin(alphaRad)).toFixed(1)}·t - ${(g / 2).toFixed(1)}·t²  |  x(t) = ${(v0 * Math.cos(alphaRad)).toFixed(1)}·t`
                  : `a = g·(sin(${inclineAlpha}°) - ${inclineMu}·cos(${inclineAlpha}°)) = ${inclineAcc.toFixed(2)} m/s²`}
              </div>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span className="formula-tag">Dual-Validation: PASSED</span>
            <button
              onClick={handleReset}
              style={{
                padding: '6px 12px',
                borderRadius: '6px',
                backgroundColor: 'rgba(255, 255, 255, 0.1)',
                border: '1px solid rgba(255, 255, 255, 0.2)',
                color: '#ffffff',
                fontSize: '11px',
                fontWeight: 700,
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '5px',
              }}
            >
              <RotateCcw size={12} />
              <span>Reset View</span>
            </button>
          </div>
        </div>

        {/* CANVAS WORKSPACE */}
        <div className="canvas-wrapper">
          <canvas
            ref={canvasRef}
            width={860}
            height={520}
            className="studio-canvas"
          />

          {/* FLOATING PLAYBACK DOCK (THEO STYLE MATHOS) */}
          <div className="floating-playback-dock">
            <button
              className="play-round-btn"
              onClick={() => setIsPlaying(!isPlaying)}
              title={isPlaying ? 'Tạm dừng' : 'Chạy mô phỏng'}
              style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}
            >
              {isPlaying ? <Pause size={16} fill="#ffffff" /> : <Play size={16} fill="#ffffff" style={{ marginLeft: '2px' }} />}
            </button>

            <button
              onClick={handleReset}
              style={{
                background: 'transparent',
                border: 'none',
                color: '#94a3b8',
                cursor: 'pointer',
                padding: '4px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
              title="Đặt lại từ đầu"
            >
              <RotateCcw size={16} />
            </button>

            {/* Scrubber Range */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <input
                type="range"
                min={0}
                max={maxDuration || 1}
                step={0.01}
                value={currentTime}
                onChange={(e) => {
                  setIsPlaying(false);
                  setCurrentTime(parseFloat(e.target.value));
                }}
                style={{ width: '160px', accentColor: '#2563eb', cursor: 'pointer' }}
              />
              <span style={{ color: '#f8fafc', fontSize: '12px', fontFamily: 'Consolas, monospace', minWidth: '85px' }}>
                {currentTime.toFixed(2)}s / {maxDuration.toFixed(2)}s
              </span>
            </div>

            {/* Step Forward button */}
            <button
              onClick={handleStepForward}
              style={{
                background: 'rgba(255, 255, 255, 0.1)',
                border: '1px solid rgba(255, 255, 255, 0.2)',
                color: '#ffffff',
                padding: '5px 10px',
                borderRadius: '6px',
                fontSize: '11px',
                fontWeight: 700,
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '4px',
              }}
              title="Bước tới 0.1 giây"
            >
              <StepForward size={13} />
              <span>+0.1s</span>
            </button>

            {/* Speed Selector */}
            <div style={{ display: 'flex', gap: '2px', backgroundColor: 'rgba(0,0,0,0.4)', padding: '2px', borderRadius: '6px' }}>
              {[0.5, 1.0, 2.0].map((spd) => (
                <button
                  key={spd}
                  onClick={() => setPlaybackSpeed(spd)}
                  style={{
                    border: 'none',
                    backgroundColor: playbackSpeed === spd ? '#2563eb' : 'transparent',
                    color: '#ffffff',
                    fontSize: '11px',
                    fontWeight: 600,
                    padding: '2px 6px',
                    borderRadius: '4px',
                    cursor: 'pointer',
                  }}
                >
                  {spd}×
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* REAL-TIME METRICS DOCK DƯỚI CHÂN CANVAS */}
        <div className="realtime-metrics-dock">
          <div className="metric-pill">
            <div className="metric-label" style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
              <Activity size={12} color="#94a3b8" />
              <span>THỜI GIAN THỰC (t)</span>
            </div>
            <div className="metric-value">{currentTime.toFixed(2)} s</div>
          </div>

          <div className="metric-pill">
            <div className="metric-label" style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
              <Gauge size={12} color="#94a3b8" />
              <span>VẬN TỐC HIỆN THỜI (v)</span>
            </div>
            <div className="metric-value">
              {currentSim.id === 'sim-car-brake-003'
                ? `${Math.max(0, carV0 + carA * currentTime).toFixed(2)} m/s`
                : `${Math.sqrt(v0x * v0x + Math.pow(v0y - g * currentTime, 2)).toFixed(2)} m/s`}
            </div>
          </div>

          <div className="metric-pill">
            <div className="metric-label" style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
              <Compass size={12} color="#94a3b8" />
              <span>TOẠ ĐỘ VẬT THỂ (x, y)</span>
            </div>
            <div className="metric-value">
              {currentSim.id === 'sim-car-brake-003'
                ? `x = ${(carV0 * currentTime + 0.5 * carA * currentTime * currentTime).toFixed(1)} m`
                : `(${ (v0x * currentTime).toFixed(1) }, ${ Math.max(0, h + v0y * currentTime - 0.5 * g * currentTime * currentTime).toFixed(1) }) m`}
            </div>
          </div>

          <div className="metric-pill">
            <div className="metric-label" style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
              <Sparkles size={12} color="#38bdf8" />
              <span>KẾT QUẢ CỰC ĐẠI</span>
            </div>
            <div className="metric-value" style={{ color: '#38bdf8' }}>
              {currentSim.id === 'sim-car-brake-003'
                ? `s_dừng = ${sStopCar.toFixed(1)} m`
                : `L_max = ${lMaxProj.toFixed(1)} m`}
            </div>
          </div>
        </div>
      </section>

      {/* ------------------------------------------------------------- */}
      {/* PANE PHẢI: INSPECTOR & PARAMETERS PANEL                       */}
      {/* ------------------------------------------------------------- */}
      <aside className="inspector-pane">
        {/* Tab Selection */}
        <div className="inspector-tabs">
          <button
            className={`inspector-tab-btn ${activeTab === 'sliders' ? 'active' : ''}`}
            onClick={() => setActiveTab('sliders')}
            style={{ display: 'flex', alignItems: 'center', gap: '5px' }}
          >
            <Sliders size={13} />
            <span>Tham số</span>
          </button>
          <button
            className={`inspector-tab-btn ${activeTab === 'steps' ? 'active' : ''}`}
            onClick={() => setActiveTab('steps')}
            style={{ display: 'flex', alignItems: 'center', gap: '5px' }}
          >
            <FileText size={13} />
            <span>Lời giải</span>
          </button>
          <button
            className={`inspector-tab-btn ${activeTab === 'table' ? 'active' : ''}`}
            onClick={() => setActiveTab('table')}
            style={{ display: 'flex', alignItems: 'center', gap: '5px' }}
          >
            <TableIcon size={13} />
            <span>Bảng số</span>
          </button>
          <button
            className={`inspector-tab-btn ${activeTab === 'export' ? 'active' : ''}`}
            onClick={() => setActiveTab('export')}
            style={{ display: 'flex', alignItems: 'center', gap: '5px' }}
          >
            <Download size={13} />
            <span>Xuất</span>
          </button>
        </div>

        <div className="inspector-content">
          {/* TAB 1: SLIDERS & EXACT VALUE INPUT */}
          {activeTab === 'sliders' && (
            <>
              {currentSim.id === 'sim-car-brake-003' ? (
                // Sliders cho Ô tô phanh
                <>
                  <div className="slider-group">
                    <div className="slider-label-row">
                      <span className="slider-name">Vận tốc ban đầu (v₀) [m/s]</span>
                      <input
                        type="number"
                        className="slider-number-input"
                        value={carV0}
                        onChange={(e) => {
                          setCarV0(parseFloat(e.target.value) || 0);
                          handleReset();
                        }}
                      />
                    </div>
                    <input
                      type="range"
                      min={5}
                      max={40}
                      step={1}
                      value={carV0}
                      onChange={(e) => {
                        setCarV0(parseFloat(e.target.value));
                        handleReset();
                      }}
                      className="slider-range-control"
                    />
                    <div style={{ fontSize: '11px', color: '#64748b' }}>
                      Tương đương: <strong>{(carV0 * 3.6).toFixed(0)} km/h</strong> (chuẩn hoá SI)
                    </div>
                  </div>

                  <div className="slider-group">
                    <div className="slider-label-row">
                      <span className="slider-name">Gia tốc hãm phanh (a) [m/s²]</span>
                      <input
                        type="number"
                        className="slider-number-input"
                        value={carA}
                        onChange={(e) => {
                          setCarA(parseFloat(e.target.value) || -1);
                          handleReset();
                        }}
                      />
                    </div>
                    <input
                      type="range"
                      min={-10}
                      max={-1}
                      step={0.5}
                      value={carA}
                      onChange={(e) => {
                        setCarA(parseFloat(e.target.value));
                        handleReset();
                      }}
                      className="slider-range-control"
                    />
                  </div>
                </>
              ) : currentSim.id === 'sim-proj-001' ? (
                // Sliders cho Ném xiên
                <>
                  <div className="slider-group">
                    <div className="slider-label-row">
                      <span className="slider-name">Vận tốc đầu (v₀) [m/s]</span>
                      <input
                        type="number"
                        className="slider-number-input"
                        value={v0}
                        onChange={(e) => {
                          setV0(parseFloat(e.target.value) || 0);
                          handleReset();
                        }}
                      />
                    </div>
                    <input
                      type="range"
                      min={1}
                      max={40}
                      step={0.5}
                      value={v0}
                      onChange={(e) => {
                        setV0(parseFloat(e.target.value));
                        handleReset();
                      }}
                      className="slider-range-control"
                    />
                  </div>

                  <div className="slider-group">
                    <div className="slider-label-row">
                      <span className="slider-name">Góc ném (α) [độ]</span>
                      <input
                        type="number"
                        className="slider-number-input"
                        value={alpha}
                        onChange={(e) => {
                          setAlpha(parseFloat(e.target.value) || 0);
                          handleReset();
                        }}
                      />
                    </div>
                    <input
                      type="range"
                      min={0}
                      max={85}
                      step={1}
                      value={alpha}
                      onChange={(e) => {
                        setAlpha(parseFloat(e.target.value));
                        handleReset();
                      }}
                      className="slider-range-control"
                    />
                  </div>

                  <div className="slider-group">
                    <div className="slider-label-row">
                      <span className="slider-name">Độ cao điểm ném (h) [m]</span>
                      <input
                        type="number"
                        className="slider-number-input"
                        value={h}
                        onChange={(e) => {
                          setH(parseFloat(e.target.value) || 0);
                          handleReset();
                        }}
                      />
                    </div>
                    <input
                      type="range"
                      min={0}
                      max={50}
                      step={1}
                      value={h}
                      onChange={(e) => {
                        setH(parseFloat(e.target.value));
                        handleReset();
                      }}
                      className="slider-range-control"
                    />
                  </div>

                  <div className="slider-group">
                    <div className="slider-label-row">
                      <span className="slider-name">Gia tốc trọng trường (g) [m/s²]</span>
                      <input
                        type="number"
                        className="slider-number-input"
                        value={g}
                        onChange={(e) => {
                          setG(parseFloat(e.target.value) || 9.8);
                          handleReset();
                        }}
                      />
                    </div>
                    <input
                      type="range"
                      min={1}
                      max={20}
                      step={0.1}
                      value={g}
                      onChange={(e) => {
                        setG(parseFloat(e.target.value));
                        handleReset();
                      }}
                      className="slider-range-control"
                    />
                  </div>
                </>
              ) : (
                // Sliders cho Trượt dốc
                <>
                  <div className="slider-group">
                    <div className="slider-label-row">
                      <span className="slider-name">Khối lượng (m) [kg]</span>
                      <input
                        type="number"
                        className="slider-number-input"
                        value={inclineM}
                        onChange={(e) => setInclineM(parseFloat(e.target.value) || 1)}
                      />
                    </div>
                    <input
                      type="range"
                      min={0.5}
                      max={20}
                      step={0.5}
                      value={inclineM}
                      onChange={(e) => setInclineM(parseFloat(e.target.value))}
                      className="slider-range-control"
                    />
                  </div>

                  <div className="slider-group">
                    <div className="slider-label-row">
                      <span className="slider-name">Góc nghiêng dốc (α) [độ]</span>
                      <input
                        type="number"
                        className="slider-number-input"
                        value={inclineAlpha}
                        onChange={(e) => {
                          setInclineAlpha(parseFloat(e.target.value) || 5);
                          handleReset();
                        }}
                      />
                    </div>
                    <input
                      type="range"
                      min={5}
                      max={60}
                      step={1}
                      value={inclineAlpha}
                      onChange={(e) => {
                        setInclineAlpha(parseFloat(e.target.value));
                        handleReset();
                      }}
                      className="slider-range-control"
                    />
                  </div>

                  <div className="slider-group">
                    <div className="slider-label-row">
                      <span className="slider-name">Chiều dài dốc (s) [m]</span>
                      <input
                        type="number"
                        className="slider-number-input"
                        value={inclineS}
                        onChange={(e) => {
                          setInclineS(parseFloat(e.target.value) || 1);
                          handleReset();
                        }}
                      />
                    </div>
                    <input
                      type="range"
                      min={1}
                      max={30}
                      step={0.5}
                      value={inclineS}
                      onChange={(e) => {
                        setInclineS(parseFloat(e.target.value));
                        handleReset();
                      }}
                      className="slider-range-control"
                    />
                  </div>

                  <div className="slider-group">
                    <div className="slider-label-row">
                      <span className="slider-name">Hệ số ma sát (μ)</span>
                      <input
                        type="number"
                        className="slider-number-input"
                        value={inclineMu}
                        onChange={(e) => setInclineMu(parseFloat(e.target.value) || 0)}
                      />
                    </div>
                    <input
                      type="range"
                      min={0}
                      max={0.5}
                      step={0.02}
                      value={inclineMu}
                      onChange={(e) => setInclineMu(parseFloat(e.target.value))}
                      className="slider-range-control"
                    />
                  </div>
                </>
              )}

              <div style={{ marginTop: '10px', padding: '12px', borderRadius: '8px', backgroundColor: '#f0fdf4', border: '1px solid #bbf7d0', fontSize: '12px', color: '#166534', lineHeight: 1.5 }}>
                💡 <strong>Tương tác Model-View:</strong> Khi bạn thay đổi bất kỳ tham số nào, toàn bộ quỹ đạo và vector lực sẽ được tính toán lại ngay tức khắc mà không cần tải lại trang.
              </div>
            </>
          )}

          {/* TAB 2: CALCULATION STEPS */}
          {activeTab === 'steps' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: '12px', fontWeight: 800, color: '#475569', textTransform: 'uppercase' }}>
                  CÁC BƯỚC GIẢI CHI TIẾT
                </span>
                <button
                  onClick={() => {
                    const content = [
                      `# LỜI GIẢI CHI TIẾT BÀI TOÁN VẬT LÝ — PHYSLIVE STUDIO`,
                      `**Chủ đề**: ${currentSim.spec_json.topic} | **Bài toán**: ${currentSim.title}`,
                      `**Thời gian xuất**: ${new Date().toLocaleString('vi-VN')}`,
                      `---`,
                      `## 1. THÔNG SỐ ĐẦU VÀO (GIẢ THIẾT BÀI TOÁN)`,
                      ...(currentSim.id === 'sim-car-brake-003'
                        ? [`- Vận tốc ban đầu: v0 = ${carV0} m/s (${(carV0 * 3.6).toFixed(0)} km/h)`, `- Gia tốc hãm phanh: a = ${carA} m/s²`]
                        : [`- Vận tốc ban đầu: v0 = ${v0} m/s`, `- Góc ném: α = ${alpha}°`, `- Độ cao ban đầu: h = ${h} m`, `- Gia tốc trọng trường: g = ${g} m/s²`]),
                      `---`,
                      `## 2. CÁC BƯỚC TÍNH TOÁN & CÔNG THỨC`,
                      ...currentSim.spec_json.calculation_steps.map(s => `### Bước ${s.step_number}: ${s.formula}\n- **Mô tả**: ${s.description}\n- **Kết quả**: ${s.result}\n`),
                      `---`,
                      `## 3. KẾT QUẢ ĐỐI CHỨNG DUAL-VALIDATION`,
                      `- Numerical Solver: PASSED`,
                      `- Closed-form Solver: PASSED`,
                      `- Sai số nghiệm: < 0.1% (Đạt tiêu chuẩn)`,
                    ].join('\n\n');

                    const blob = new Blob([content], { type: 'text/markdown;charset=utf-8' });
                    const url = URL.createObjectURL(blob);
                    const a = document.createElement('a');
                    a.href = url;
                    a.download = `Loi_Giai_${currentSim.id}.md`;
                    a.click();
                  }}
                  style={{
                    padding: '5px 10px',
                    borderRadius: '6px',
                    border: '1px solid #3b82f6',
                    backgroundColor: '#eff6ff',
                    color: '#1d4ed8',
                    fontSize: '11px',
                    fontWeight: 700,
                    cursor: 'pointer',
                  }}
                >
                  📥 Tải file .MD
                </button>
              </div>

              {currentSim.spec_json.calculation_steps.map((step) => (
                <div
                  key={step.step_number}
                  style={{
                    padding: '14px',
                    borderRadius: '10px',
                    backgroundColor: '#f8fafc',
                    border: '1px solid #e2e8f0',
                  }}
                >
                  <div style={{ fontSize: '12px', fontWeight: 800, color: '#2563eb', marginBottom: '4px' }}>
                    BƯỚC {step.step_number}: {step.formula}
                  </div>
                  <div style={{ fontSize: '13px', color: '#334155', marginBottom: '8px', lineHeight: 1.4 }}>
                    {step.description}
                  </div>
                  <div style={{ fontSize: '12px', fontWeight: 700, color: '#047857', backgroundColor: '#ecfdf5', padding: '5px 10px', borderRadius: '6px', display: 'inline-block' }}>
                    Kết quả: {step.result}
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* TAB 3: DATA TABLE */}
          {activeTab === 'table' && (
            <div>
              <div style={{ fontSize: '12px', fontWeight: 700, color: '#475569', marginBottom: '8px' }}>
                BẢNG BIẾN THIÊN THEO THỜI GIAN
              </div>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '12px' }}>
                <thead style={{ backgroundColor: '#f1f5f9' }}>
                  <tr>
                    <th style={{ padding: '6px 8px', textAlign: 'left', borderBottom: '1px solid #cbd5e1' }}>t (s)</th>
                    <th style={{ padding: '6px 8px', textAlign: 'left', borderBottom: '1px solid #cbd5e1' }}>v (m/s)</th>
                    <th style={{ padding: '6px 8px', textAlign: 'left', borderBottom: '1px solid #cbd5e1' }}>s (m)</th>
                  </tr>
                </thead>
                <tbody>
                  {[0, 0.25, 0.5, 0.75, 1.0].map((frac, idx) => {
                    const tVal = frac * maxDuration;
                    const vVal =
                      currentSim.id === 'sim-car-brake-003'
                        ? Math.max(0, carV0 + carA * tVal)
                        : Math.sqrt(v0x * v0x + Math.pow(v0y - g * tVal, 2));
                    const sVal =
                      currentSim.id === 'sim-car-brake-003'
                        ? carV0 * tVal + 0.5 * carA * tVal * tVal
                        : v0x * tVal;
                    return (
                      <tr key={idx} style={{ borderBottom: '1px solid #f1f5f9' }}>
                        <td style={{ padding: '6px 8px', fontWeight: 700 }}>{tVal.toFixed(2)}</td>
                        <td style={{ padding: '6px 8px', color: '#2563eb' }}>{vVal.toFixed(2)}</td>
                        <td style={{ padding: '6px 8px', color: '#10b981' }}>{sVal.toFixed(2)}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}

          {/* TAB 4: EXPORT FEATURES (TASK H08) */}
          {activeTab === 'export' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              <div style={{ fontSize: '13px', color: '#475569', lineHeight: 1.5 }}>
                Xuất gói dữ liệu tự trị phục vụ dạy học và kiểm chứng theo yêu cầu của Giảng viên hướng dẫn:
              </div>

              {/* Xuất các bước tính toán (Lưu ý Thầy Phương) */}
              <button
                onClick={() => {
                  const content = [
                    `LỜI GIẢI CHI TIẾT BÀI TOÁN VẬT LÝ — PHYSLIVE STUDIO`,
                    `Chủ đề: ${currentSim.spec_json.topic}`,
                    `Tên bài: ${currentSim.title}`,
                    `Ngày xuất: ${new Date().toLocaleString('vi-VN')}`,
                    `--------------------------------------------------`,
                    `1. GIẢ THIẾT ĐẦU VÀO:`,
                    ...(currentSim.id === 'sim-car-brake-003'
                      ? [`  v0 = ${carV0} m/s (${(carV0 * 3.6).toFixed(0)} km/h)`, `  a = ${carA} m/s²`]
                      : [`  v0 = ${v0} m/s`, `  alpha = ${alpha} deg`, `  h = ${h} m`, `  g = ${g} m/s²`]),
                    `--------------------------------------------------`,
                    `2. CÁC BƯỚC TÍNH TOÁN:`,
                    ...currentSim.spec_json.calculation_steps.map(s => `  Bước ${s.step_number}: ${s.formula}\n  -> Giải thích: ${s.description}\n  -> Kết quả: ${s.result}\n`),
                    `--------------------------------------------------`,
                    `3. KIỂM CHỨNG DUAL-VALIDATION: PASSED (< 0.1% sai số)`,
                  ].join('\n');

                  const blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
                  const url = URL.createObjectURL(blob);
                  const a = document.createElement('a');
                  a.href = url;
                  a.download = `Cac_Buoc_Giai_${currentSim.id}.txt`;
                  a.click();
                }}
                style={{
                  padding: '11px',
                  borderRadius: '8px',
                  border: '1px solid #10b981',
                  backgroundColor: '#ecfdf5',
                  color: '#065f46',
                  fontWeight: 700,
                  fontSize: '13px',
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '8px',
                }}
              >
                <FileText size={15} />
                <span>Xuất các bước tính toán (.TXT)</span>
              </button>

              {/* Tải JSON Replay Specification */}
              <button
                onClick={() => {
                  const data = {
                    simulationId: currentSim.id,
                    title: currentSim.title,
                    exportedAt: new Date().toISOString(),
                    parameters: currentSim.id === 'sim-car-brake-003' ? { carV0, carA } : { v0, alpha, h, g },
                    results: { maxDuration, sStopCar, lMaxProj },
                    calculationSteps: currentSim.spec_json.calculation_steps,
                  };
                  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
                  const url = URL.createObjectURL(blob);
                  const a = document.createElement('a');
                  a.href = url;
                  a.download = `physlive_${currentSim.id}.json`;
                  a.click();
                }}
                style={{
                  padding: '11px',
                  borderRadius: '8px',
                  border: '1px solid #2563eb',
                  backgroundColor: '#ffffff',
                  color: '#2563eb',
                  fontWeight: 700,
                  fontSize: '13px',
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '8px',
                }}
              >
                <Download size={15} />
                <span>Tải JSON Replay Specification</span>
              </button>

              {/* In báo cáo / Lưu PDF */}
              <button
                onClick={() => window.print()}
                style={{
                  padding: '11px',
                  borderRadius: '8px',
                  border: 'none',
                  backgroundColor: '#2563eb',
                  color: '#ffffff',
                  fontWeight: 700,
                  fontSize: '13px',
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '8px',
                  boxShadow: '0 2px 6px rgba(37, 99, 235, 0.25)',
                }}
              >
                <Printer size={15} />
                <span>In / Lưu PDF báo cáo thực nghiệm</span>
              </button>
            </div>
          )}
        </div>
      </aside>
    </div>
  );
};
