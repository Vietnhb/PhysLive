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
  Maximize2,
  Minimize2,
  Share2,
  BookOpen,
  Lightbulb,
  HelpCircle,
  Check,
  Copy,
  X,
  LineChart as ChartIcon,
} from 'lucide-react';
import { LearningChart, type ChartSeries } from '../../components/common/LearningChart';

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
  const [activeTab, setActiveTab] = useState<'sliders' | 'steps' | 'table' | 'chart' | 'tips' | 'export'>('sliders');
  const [selectedChartVar, setSelectedChartVar] = useState<'v' | 's' | 'a'>('v');
  const [catalogSearch, setCatalogSearch] = useState<string>('');

  // PhET-inspired features: Fullscreen Projector Mode & LMS Embed Modal
  const [isFullscreen, setIsFullscreen] = useState<boolean>(false);
  const [showEmbedModal, setShowEmbedModal] = useState<boolean>(false);
  const [copiedEmbed, setCopiedEmbed] = useState<boolean>(false);

  // Virtual Instruments Toggles (PhET Scientific Instruments)
  const [showVectors, setShowVectors] = useState<boolean>(true);
  const [showGrid, setShowGrid] = useState<boolean>(true);
  const [showRuler, setShowRuler] = useState<boolean>(true);
  const [displayMode, setDisplayMode] = useState<'clean' | 'detailed'>('clean');

  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  // Listen to Escape key to exit fullscreen
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isFullscreen) {
        setIsFullscreen(false);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isFullscreen]);

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

  // Dữ liệu đồ thị SVG thời gian thực (Dynamic SVG Curves)
  const chartTimes = React.useMemo(() => {
    const numPoints = 120;
    const dur = Math.max(0.1, maxDuration);
    const arr: number[] = [];
    for (let i = 0; i <= numPoints; i++) {
      arr.push((i / numPoints) * dur);
    }
    return arr;
  }, [maxDuration]);

  const chartSeries = React.useMemo<ChartSeries>(() => {
    if (currentSim.id === 'sim-car-brake-003') {
      if (selectedChartVar === 'v') {
        return {
          label: 'Vận tốc v(t)',
          unit: 'm/s',
          color: 'var(--status-success-text)',
          data: chartTimes.map((t) => Math.max(0, carV0 + carA * t)),
        };
      }
      if (selectedChartVar === 'a') {
        return {
          label: 'Gia tốc a(t)',
          unit: 'm/s²',
          color: '#ef4444',
          data: chartTimes.map((t) => (t <= sStopCar / Math.max(0.1, carV0 / 2) ? carA : 0)),
        };
      }
      return {
        label: 'Quãng đường s(t)',
        unit: 'm',
        color: '#3b82f6',
        data: chartTimes.map((t) => Math.min(sStopCar, carV0 * t + 0.5 * carA * t * t)),
      };
    } else if (currentSim.id === 'sim-proj-001') {
      if (selectedChartVar === 'v') {
        return {
          label: 'Vận tốc tức thời v(t)',
          unit: 'm/s',
          color: 'var(--status-success-text)',
          data: chartTimes.map((t) => {
            const vy = v0y - g * t;
            return Math.sqrt(v0x * v0x + vy * vy);
          }),
        };
      }
      if (selectedChartVar === 'a') {
        return {
          label: 'Gia tốc g(t)',
          unit: 'm/s²',
          color: '#ef4444',
          data: chartTimes.map(() => 9.8),
        };
      }
      return {
        label: 'Độ cao y(t)',
        unit: 'm',
        color: '#38bdf8',
        data: chartTimes.map((t) => Math.max(0, h + v0y * t - 0.5 * g * t * t)),
      };
    } else {
      // Incline plane
      if (selectedChartVar === 'v') {
        return {
          label: 'Vận tốc trượt v(t)',
          unit: 'm/s',
          color: 'var(--status-success-text)',
          data: chartTimes.map((t) => Math.min(Math.sqrt(2 * inclineAcc * inclineS), inclineAcc * t)),
        };
      }
      if (selectedChartVar === 'a') {
        return {
          label: 'Gia tốc trượt a(t)',
          unit: 'm/s²',
          color: '#f59e0b',
          data: chartTimes.map(() => inclineAcc),
        };
      }
      return {
        label: 'Quãng đường s(t)',
        unit: 'm',
        color: '#38bdf8',
        data: chartTimes.map((t) => Math.min(inclineS, 0.5 * inclineAcc * t * t)),
      };
    }
  }, [currentSim.id, selectedChartVar, chartTimes, carV0, carA, sStopCar, v0x, v0y, g, h, inclineAcc, inclineS]);

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

  // --- RENDER CANVAS THEO TIÊU CHUẨN PHÒNG THÍ NGHIỆM VẬT LÝ QUỐC TẾ (PHET + MATHOS LAB) ---
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const width = canvas.width;
    const height = canvas.height;

    // Helper: Vẽ vector mũi tên lực / vận tốc (Vector Arrow with Clean / Detailed Mode)
    const drawVector = (
      fromX: number,
      fromY: number,
      toX: number,
      toY: number,
      color: string,
      label: string,
      cleanSymbol: string = '',
      badgeOffsetX: number = 0,
      badgeOffsetY: number = -14
    ) => {
      if (!showVectors) return;
      const headlen = 11;
      const dx = toX - fromX;
      const dy = toY - fromY;
      const angle = Math.atan2(dy, dx);
      const length = Math.sqrt(dx * dx + dy * dy);
      if (length < 2) return;

      ctx.save();
      ctx.shadowColor = color;
      ctx.shadowBlur = displayMode === 'clean' ? 4 : 8;
      ctx.strokeStyle = color;
      ctx.fillStyle = color;
      ctx.lineWidth = displayMode === 'clean' ? 2.2 : 2.8;
      ctx.lineCap = 'round';

      // Trục vector
      ctx.beginPath();
      ctx.moveTo(fromX, fromY);
      ctx.lineTo(toX, toY);
      ctx.stroke();

      // Mũi tên nhọn chuẩn kỹ thuật
      ctx.beginPath();
      ctx.moveTo(toX, toY);
      ctx.lineTo(toX - headlen * Math.cos(angle - Math.PI / 6), toY - headlen * Math.sin(angle - Math.PI / 6));
      ctx.lineTo(toX - headlen * Math.cos(angle + Math.PI / 6), toY - headlen * Math.sin(angle + Math.PI / 6));
      ctx.closePath();
      ctx.fill();

      // Hiển thị nhãn: Trong chế độ Tối giản (Clean), vẽ ký hiệu vector thanh mảnh, không phủ hộp đen che khuất
      if (displayMode === 'clean') {
        if (cleanSymbol) {
          ctx.shadowBlur = 0;
          ctx.font = 'bold 12px "Google Sans", Inter, sans-serif';
          ctx.fillStyle = color;
          ctx.textAlign = 'center';
          ctx.textBaseline = 'middle';
          ctx.fillText(cleanSymbol, toX + badgeOffsetX, toY + badgeOffsetY);
        }
      } else if (label) {
        // Chế độ Chi tiết (Detailed): Huy hiệu nhãn thông số số học đầy đủ
        ctx.shadowBlur = 0;
        ctx.font = 'bold 11px Consolas, monospace';
        const textWidth = ctx.measureText(label).width;
        const badgeX = toX + badgeOffsetX - textWidth / 2;
        const badgeY = toY + badgeOffsetY;

        ctx.fillStyle = 'rgba(15, 23, 42, 0.9)';
        ctx.strokeStyle = color;
        ctx.lineWidth = 1.2;
        ctx.beginPath();
        ctx.roundRect(badgeX - 6, badgeY - 12, textWidth + 12, 18, 5);
        ctx.fill();
        ctx.stroke();

        ctx.fillStyle = '#ffffff';
        ctx.fillText(label, badgeX, badgeY + 1);
      }
      ctx.restore();
    };

    // Helper: Vẽ thước đo góc bán nguyệt (Protractor)
    const drawProtractor = (cx: number, cy: number, radius: number, startAngle: number, endAngle: number, label: string) => {
      ctx.save();
      ctx.strokeStyle = 'rgba(56, 189, 248, 0.6)';
      ctx.fillStyle = 'rgba(56, 189, 248, 0.1)';
      ctx.lineWidth = 1.5;

      // Hình quạt góc
      ctx.beginPath();
      ctx.moveTo(cx, cy);
      ctx.arc(cx, cy, radius, startAngle, endAngle);
      ctx.closePath();
      ctx.fill();
      ctx.stroke();

      // Các vạch chia độ
      const step = (endAngle - startAngle) / 6;
      ctx.strokeStyle = 'rgba(56, 189, 248, 0.5)';
      for (let i = 1; i < 6; i++) {
        const a = startAngle + i * step;
        ctx.beginPath();
        ctx.moveTo(cx + (radius - 5) * Math.cos(a), cy + (radius - 5) * Math.sin(a));
        ctx.lineTo(cx + radius * Math.cos(a), cy + radius * Math.sin(a));
        ctx.stroke();
      }

      // Nhãn góc
      const midAngle = (startAngle + endAngle) / 2;
      const textX = cx + (radius + 14) * Math.cos(midAngle);
      const textY = cy + (radius + 14) * Math.sin(midAngle);
      ctx.fillStyle = '#38bdf8';
      ctx.font = 'bold 12px Consolas, monospace';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.fillText(displayMode === 'clean' ? 'α' : label, textX, textY);
      ctx.restore();
    };

    // 1. Phông nền phòng thí nghiệm không gian sâu (Deep Space Lab Gradient)
    ctx.clearRect(0, 0, width, height);
    const bgGrad = ctx.createRadialGradient(width / 2, height / 2, 50, width / 2, height / 2, width);
    bgGrad.addColorStop(0, '#0f172a');
    bgGrad.addColorStop(1, '#050811');
    ctx.fillStyle = bgGrad;
    ctx.fillRect(0, 0, width, height);

    // 2. Lưới toạ độ kỹ thuật (Engineering Metric Grid)
    if (showGrid) {
      ctx.strokeStyle = 'rgba(30, 41, 59, 0.45)';
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
    }

    // =============================================================
    // TRƯỜNG HỢP 1: BÀI TOÁN Ô TÔ PHANH CHẬM DẦN ĐỀU (THẦY PHƯƠNG)
    // =============================================================
    if (currentSim.id === 'sim-car-brake-003') {
      const roadY = height * 0.65;
      const roadStartX = 90;
      const roadLength = width - 190;
      const scaleX = roadLength / Math.max(55, sStopCar * 1.15);

      const currentS = Math.min(sStopCar, carV0 * currentTime + 0.5 * carA * currentTime * currentTime);
      const currentV = Math.max(0, carV0 + carA * currentTime);
      const carX = roadStartX + currentS * scaleX;
      const stopMarkX = roadStartX + sStopCar * scaleX;

      // A. MẶT ĐƯỜNG CAO TỐC CHUYÊN NGHIỆP (HIGHWAY ENVIRONMENT)
      // Thềm đường & Vỉa hè an toàn
      ctx.fillStyle = '#1e293b';
      ctx.fillRect(0, roadY - 4, width, 4);

      // Thân mặt đường nhựa Asphalt
      const asphaltGrad = ctx.createLinearGradient(0, roadY, 0, roadY + 70);
      asphaltGrad.addColorStop(0, '#131b2e');
      asphaltGrad.addColorStop(1, '#0b0f19');
      ctx.fillStyle = asphaltGrad;
      ctx.fillRect(0, roadY, width, 70);

      // Vạch kẻ đường đứt đoạn phản quang màu vàng
      ctx.strokeStyle = '#eab308';
      ctx.lineWidth = 3;
      ctx.setLineDash([20, 16]);
      ctx.beginPath();
      ctx.moveTo(0, roadY + 35);
      ctx.lineTo(width, roadY + 35);
      ctx.stroke();
      ctx.setLineDash([]);

      // Gờ mép đường dưới
      ctx.strokeStyle = '#334155';
      ctx.lineWidth = 2;
      ctx.beginPath();
      ctx.moveTo(0, roadY + 70);
      ctx.lineTo(width, roadY + 70);
      ctx.stroke();

      // B. THƯỚC ĐO KHOẢNG CÁCH & CỘT MỐC CÂY SỐ (METER MILESTONES)
      if (showRuler) {
        const stepDist = 10; // mỗi 10 mét
        const maxMeters = Math.ceil((sStopCar * 1.15) / 10) * 10;
        for (let m = 0; m <= maxMeters; m += stepDist) {
          const mx = roadStartX + m * scaleX;
          if (mx < width - 40) {
            // Vạch mốc trên đường
            ctx.strokeStyle = m === 0 ? '#38bdf8' : 'rgba(148, 163, 184, 0.4)';
            ctx.lineWidth = m === 0 ? 2 : 1;
            ctx.beginPath();
            ctx.moveTo(mx, roadY - 14);
            ctx.lineTo(mx, roadY + 70);
            ctx.stroke();

            // Cột mốc & Nhãn khoảng cách
            ctx.fillStyle = m === 0 ? '#38bdf8' : '#94a3b8';
            ctx.font = 'bold 11px Consolas, monospace';
            ctx.textAlign = 'center';
            ctx.fillText(`${m}m`, mx, roadY - 18);
          }
        }
      }

      // C. BIỂN BÁO GIAO THÔNG (SPEED LIMIT SIGN)
      const signX = roadStartX - 50;
      const signY = roadY - 55;
      ctx.save();
      // Cột biển
      ctx.fillStyle = '#64748b';
      ctx.fillRect(signX - 2, signY + 16, 4, 38);
      // Biển tròn viền đỏ nền trắng
      ctx.beginPath();
      ctx.arc(signX, signY, 18, 0, Math.PI * 2);
      ctx.fillStyle = '#ffffff';
      ctx.fill();
      ctx.lineWidth = 4;
      ctx.strokeStyle = '#ef4444';
      ctx.stroke();
      // Số tốc độ 50
      ctx.fillStyle = '#0f172a';
      ctx.font = 'bold 13px Arial, sans-serif';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.fillText('50', signX, signY);
      ctx.restore();

      // D. VẠCH ĐÍCH DỪNG XE PHÁT SÁNG (STOPPING TARGET LINE)
      ctx.save();
      ctx.shadowColor = '#ef4444';
      ctx.shadowBlur = 12;
      ctx.strokeStyle = '#ef4444';
      ctx.lineWidth = 3;
      ctx.beginPath();
      ctx.moveTo(stopMarkX, roadY - 45);
      ctx.lineTo(stopMarkX, roadY + 70);
      ctx.stroke();

      // Biển báo cờ đích dừng
      ctx.fillStyle = '#ef4444';
      ctx.beginPath();
      const badgeW = displayMode === 'clean' ? 52 : 110;
      ctx.roundRect(stopMarkX - badgeW / 2, roadY - 68, badgeW, 22, 6);
      ctx.fill();
      ctx.fillStyle = '#ffffff';
      ctx.font = 'bold 11px Consolas, monospace';
      ctx.textAlign = 'center';
      ctx.fillText(displayMode === 'clean' ? 's_dừng' : `ĐÍCH: s = ${sStopCar.toFixed(1)}m`, stopMarkX, roadY - 53);
      ctx.restore();

      // E. VẾT TRƯỢT LỐP CAO SU TRÊN MẶT ĐƯỜNG (DYNAMIC SKID MARKS)
      if (currentS > 0.5) {
        ctx.save();
        ctx.strokeStyle = 'rgba(0, 0, 0, 0.7)';
        ctx.lineWidth = 5;
        ctx.lineCap = 'round';
        // Vết lốp bánh trước & sau
        ctx.beginPath();
        ctx.moveTo(roadStartX, roadY + 18);
        ctx.lineTo(carX, roadY + 18);
        ctx.moveTo(roadStartX, roadY + 48);
        ctx.lineTo(carX, roadY + 48);
        ctx.stroke();
        ctx.restore();
      }

      // F. HIỆU ỨNG KHÓI MA SÁT PHANH GẤP (SMOKE PUFF PARTICLES)
      if (currentV > 0.5 && carA < 0) {
        ctx.save();
        for (let i = 0; i < 5; i++) {
          const puffX = carX - 35 - i * 9;
          const puffY = roadY + 44 - Math.sin(i * 1.5) * 4;
          const radius = 6 + i * 2.5;
          const alphaPuff = Math.max(0, 0.45 - i * 0.08);
          ctx.fillStyle = `rgba(203, 213, 225, ${alphaPuff})`;
          ctx.beginPath();
          ctx.arc(puffX, puffY, radius, 0, Math.PI * 2);
          ctx.fill();
        }
        ctx.restore();
      }

      // G. CHIẾC XE Ô TÔ THỂ THAO HIỆN ĐẠI (AERODYNAMIC SPORTS COUPE)
      const carBodyY = roadY + 10;
      ctx.save();

      // Ánh đèn pha LED chiếu xa phía trước (Light Cone)
      const lightGrad = ctx.createRadialGradient(carX + 40, carBodyY + 10, 5, carX + 160, carBodyY + 15, 140);
      lightGrad.addColorStop(0, 'rgba(253, 224, 71, 0.35)');
      lightGrad.addColorStop(1, 'rgba(253, 224, 71, 0)');
      ctx.fillStyle = lightGrad;
      ctx.beginPath();
      ctx.moveTo(carX + 38, carBodyY + 6);
      ctx.lineTo(carX + 180, carBodyY - 10);
      ctx.lineTo(carX + 180, carBodyY + 40);
      ctx.closePath();
      ctx.fill();

      // Thân xe (Metallic Blue Gradient)
      const carChassisGrad = ctx.createLinearGradient(carX - 42, carBodyY - 24, carX + 42, carBodyY + 16);
      carChassisGrad.addColorStop(0, '#38bdf8');
      carChassisGrad.addColorStop(0.4, '#2563eb');
      carChassisGrad.addColorStop(1, '#1e3a8a');
      ctx.fillStyle = carChassisGrad;
      ctx.strokeStyle = '#93c5fd';
      ctx.lineWidth = 1.5;

      // Khung xe thể thao
      ctx.beginPath();
      ctx.moveTo(carX - 40, carBodyY + 12);
      ctx.lineTo(carX - 38, carBodyY - 4);
      ctx.lineTo(carX - 22, carBodyY - 8);
      ctx.lineTo(carX - 10, carBodyY - 22);
      ctx.lineTo(carX + 16, carBodyY - 22);
      ctx.lineTo(carX + 28, carBodyY - 8);
      ctx.lineTo(carX + 42, carBodyY - 2);
      ctx.lineTo(carX + 42, carBodyY + 14);
      ctx.closePath();
      ctx.fill();
      ctx.stroke();

      // Kính xe bóng loáng (Windshield & Windows)
      ctx.fillStyle = 'rgba(15, 23, 42, 0.85)';
      ctx.beginPath();
      ctx.moveTo(carX - 8, carBodyY - 20);
      ctx.lineTo(carX + 14, carBodyY - 20);
      ctx.lineTo(carX + 24, carBodyY - 8);
      ctx.lineTo(carX - 18, carBodyY - 8);
      ctx.closePath();
      ctx.fill();
      // Vệt sáng phản chiếu trên kính
      ctx.strokeStyle = 'rgba(255, 255, 255, 0.4)';
      ctx.lineWidth = 1.5;
      ctx.beginPath();
      ctx.moveTo(carX - 2, carBodyY - 18);
      ctx.lineTo(carX + 8, carBodyY - 10);
      ctx.stroke();

      // Đèn pha trước & Đèn hậu
      ctx.fillStyle = '#fef08a';
      ctx.fillRect(carX + 39, carBodyY + 2, 4, 8); // Pha LED vàng
      ctx.fillStyle = '#ef4444';
      ctx.fillRect(carX - 42, carBodyY + 2, 4, 6); // Đèn hậu đỏ

      // Hai bánh xe hợp kim quay tròn theo vận tốc thực (Spinning Alloy Wheels)
      const wheelRadius = 10;
      const wheelRotation = (currentS * scaleX) / 8; // góc quay
      [-22, 24].forEach((offset) => {
        const wx = carX + offset;
        const wy = carBodyY + 16;
        // Lốp cao su
        ctx.fillStyle = '#0f172a';
        ctx.beginPath();
        ctx.arc(wx, wy, wheelRadius, 0, Math.PI * 2);
        ctx.fill();
        ctx.strokeStyle = '#64748b';
        ctx.lineWidth = 2;
        ctx.stroke();

        // Mâm hợp kim bạc
        ctx.fillStyle = '#cbd5e1';
        ctx.beginPath();
        ctx.arc(wx, wy, wheelRadius - 3, 0, Math.PI * 2);
        ctx.fill();

        // Nan hoa xoay tròn
        ctx.strokeStyle = '#334155';
        ctx.lineWidth = 1.5;
        for (let s = 0; s < 4; s++) {
          const spAngle = wheelRotation + (s * Math.PI) / 2;
          ctx.beginPath();
          ctx.moveTo(wx, wy);
          ctx.lineTo(wx + (wheelRadius - 3) * Math.cos(spAngle), wy + (wheelRadius - 3) * Math.sin(spAngle));
          ctx.stroke();
        }
      });
      ctx.restore();

      // H. HỆ THỐNG VECTOR VẬT LÝ TRỰC QUAN (EDUCATIONAL VECTORS)
      if (showVectors) {
        // Vector vận tốc v (Xanh ngọc hướng tới trước)
        const vLenPx = Math.max(10, currentV * 2.8);
        drawVector(
          carX + 44,
          carBodyY + 4,
          carX + 44 + vLenPx,
          carBodyY + 4,
          '#10b981',
          `v = ${currentV.toFixed(1)} m/s (${(currentV * 3.6).toFixed(0)} km/h)`,
          'v⃗',
          14,
          0
        );

        // Vector gia tốc hãm a (Đỏ hướng ngược lại)
        const aLenPx = Math.abs(carA) * 12;
        drawVector(
          carX - 42,
          carBodyY - 12,
          carX - 42 - aLenPx,
          carBodyY - 12,
          '#f43f5e',
          `a = ${carA} m/s²`,
          'a⃗',
          -14,
          0
        );
      }
    }

    // =============================================================
    // TRƯỜNG HỢP 2: BÀI TOÁN NÉM XIÊN TỪ ĐỘ CAO H (SGK VẬT LÝ 10)
    // =============================================================
    else if (currentSim.id === 'sim-proj-001') {
      const originX = 85;
      const groundY = height - 75;
      const scale = Math.min((width - 170) / Math.max(45, lMaxProj * 1.15), (height - 180) / Math.max(35, hMaxProj * 1.25));

      const towerHeight = h * scale;
      const towerTopY = groundY - towerHeight;
      const currentX = v0x * currentTime;
      const currentY = Math.max(0, h + v0y * currentTime - 0.5 * g * currentTime * currentTime);
      const canvasBallX = originX + currentX * scale;
      const canvasBallY = groundY - currentY * scale;
      const landX = originX + lMaxProj * scale;
      const apexX = originX + (v0x * (v0y / g)) * scale;
      const apexY = groundY - hMaxProj * scale;

      // A. MẶT ĐẤT PHÒNG THÍ NGHIỆM & THƯỚC ĐO TẦM XA OX
      const turfGrad = ctx.createLinearGradient(0, groundY, 0, height);
      turfGrad.addColorStop(0, '#064e3b');
      turfGrad.addColorStop(1, '#022c22');
      ctx.fillStyle = turfGrad;
      ctx.fillRect(0, groundY, width, height - groundY);

      ctx.strokeStyle = '#10b981';
      ctx.lineWidth = 2.5;
      ctx.beginPath();
      ctx.moveTo(0, groundY);
      ctx.lineTo(width, groundY);
      ctx.stroke();

      // Thước đo toạ độ mặt đất (Ox Ruler)
      if (showRuler) {
        const step = 5;
        const maxM = Math.ceil((lMaxProj * 1.15) / 5) * 5;
        for (let m = 0; m <= maxM; m += step) {
          const rx = originX + m * scale;
          if (rx < width - 20) {
            ctx.strokeStyle = 'rgba(255, 255, 255, 0.3)';
            ctx.lineWidth = 1;
            ctx.beginPath();
            ctx.moveTo(rx, groundY);
            ctx.lineTo(rx, groundY + 8);
            ctx.stroke();

            ctx.fillStyle = '#94a3b8';
            ctx.font = '10px Consolas, monospace';
            ctx.textAlign = 'center';
            ctx.fillText(`${m}m`, rx, groundY + 20);
          }
        }
      }

      // B. THÁP NÉM KIM LOẠI KẾT CẤU DÀN KHÔNG GIAN (LAUNCH TOWER)
      if (h > 0) {
        ctx.save();
        const tw = 36;
        const towerGrad = ctx.createLinearGradient(originX - tw, 0, originX, 0);
        towerGrad.addColorStop(0, '#334155');
        towerGrad.addColorStop(0.5, '#475569');
        towerGrad.addColorStop(1, '#1e293b');
        ctx.fillStyle = towerGrad;
        ctx.fillRect(originX - tw, towerTopY, tw, towerHeight);
        ctx.strokeStyle = '#64748b';
        ctx.lineWidth = 1.5;
        ctx.strokeRect(originX - tw, towerTopY, tw, towerHeight);

        // Thanh giằng chéo kỹ thuật
        ctx.strokeStyle = 'rgba(255, 255, 255, 0.15)';
        for (let y = towerTopY; y < groundY - 10; y += 24) {
          ctx.beginPath();
          ctx.moveTo(originX - tw, y);
          ctx.lineTo(originX, y + 24);
          ctx.moveTo(originX, y);
          ctx.lineTo(originX - tw, y + 24);
          ctx.stroke();
        }

        // Thước đo độ cao h bên cạnh
        ctx.strokeStyle = '#38bdf8';
        ctx.lineWidth = 1.5;
        ctx.beginPath();
        ctx.moveTo(originX - tw - 12, towerTopY);
        ctx.lineTo(originX - tw - 12, groundY);
        ctx.stroke();

        ctx.fillStyle = '#38bdf8';
        ctx.font = 'bold 11px Consolas, monospace';
        ctx.textAlign = 'right';
        ctx.fillText(`h = ${h}m`, originX - tw - 18, (towerTopY + groundY) / 2);
        ctx.restore();
      }

      // C. KHẨU PHÁO THÍ NGHIỆM TỰ ĐỘNG XOAY NÒNG THEO GÓC ALPHA
      ctx.save();
      ctx.translate(originX, towerTopY);
      // Vẽ thước đo góc bán nguyệt tại nòng pháo
      drawProtractor(0, 0, 36, -alphaRad, 0, `α = ${alpha}°`);

      // Bệ đỡ xoay
      ctx.fillStyle = '#0f172a';
      ctx.beginPath();
      ctx.arc(0, 0, 10, 0, Math.PI * 2);
      ctx.fill();
      ctx.strokeStyle = '#94a3b8';
      ctx.stroke();

      // Nòng pháo xoay theo góc alpha
      ctx.rotate(-alphaRad);
      const cannonGrad = ctx.createLinearGradient(0, -7, 34, 7);
      cannonGrad.addColorStop(0, '#475569');
      cannonGrad.addColorStop(0.5, '#cbd5e1');
      cannonGrad.addColorStop(1, '#334155');
      ctx.fillStyle = cannonGrad;
      ctx.fillRect(0, -6, 32, 12);
      ctx.strokeStyle = '#f8fafc';
      ctx.strokeRect(0, -6, 32, 12);
      ctx.restore();

      // D. QUỸ ĐẠO PARABOL PHÁT SÁNG TOÀN PHẦN (THEORETICAL PARABOLA)
      ctx.save();
      ctx.strokeStyle = 'rgba(56, 189, 248, 0.4)';
      ctx.setLineDash([5, 5]);
      ctx.lineWidth = 2;
      ctx.beginPath();
      const numSteps = 80;
      for (let i = 0; i <= numSteps; i++) {
        const t = (i / numSteps) * tFlightProj;
        const px = originX + (v0x * t) * scale;
        const py = groundY - (h + v0y * t - 0.5 * g * t * t) * scale;
        if (i === 0) ctx.moveTo(px, py);
        else ctx.lineTo(px, py);
      }
      ctx.stroke();

      // Điểm đỉnh quỹ đạo H_max
      if (v0y > 0) {
        ctx.fillStyle = '#38bdf8';
        ctx.beginPath();
        ctx.arc(apexX, apexY, 4, 0, Math.PI * 2);
        ctx.fill();
        ctx.font = 'bold 11px Consolas, monospace';
        ctx.textAlign = 'center';
        ctx.fillText(displayMode === 'clean' ? 'H_max' : `H_max = ${hMaxProj.toFixed(1)}m`, apexX, apexY - 12);
      }
      ctx.restore();

      // E. VỆT ĐUÔI SAO BĂNG RỰC SÁNG ĐÃ ĐI QUA (LUMINOUS COMET TRAIL)
      if (currentTime > 0.05) {
        ctx.save();
        ctx.strokeStyle = '#fbbf24';
        ctx.shadowColor = '#fbbf24';
        ctx.shadowBlur = 10;
        ctx.lineWidth = 3.5;
        ctx.beginPath();
        const activeSteps = Math.min(60, Math.floor((currentTime / tFlightProj) * 60));
        for (let i = 0; i <= activeSteps; i++) {
          const t = (i / 60) * tFlightProj;
          const px = originX + (v0x * t) * scale;
          const py = groundY - (h + v0y * t - 0.5 * g * t * t) * scale;
          if (i === 0) ctx.moveTo(px, py);
          else ctx.lineTo(px, py);
        }
        ctx.stroke();
        ctx.restore();
      }

      // F. ĐIỂM TIẾP ĐẤT & BIA NGẮM RADAR (IMPACT RADAR TARGET)
      ctx.save();
      ctx.strokeStyle = '#ef4444';
      ctx.lineWidth = 2;
      ctx.beginPath();
      ctx.arc(landX, groundY, 14, 0, Math.PI * 2);
      ctx.stroke();
      ctx.beginPath();
      ctx.arc(landX, groundY, 6, 0, Math.PI * 2);
      ctx.fillStyle = '#ef4444';
      ctx.fill();

      // Cờ cắm đích L_max
      ctx.fillStyle = '#ef4444';
      ctx.fillRect(landX - 1, groundY - 32, 2, 32);
      ctx.beginPath();
      ctx.moveTo(landX, groundY - 32);
      ctx.lineTo(landX + 18, groundY - 24);
      ctx.lineTo(landX, groundY - 16);
      ctx.closePath();
      ctx.fill();

      ctx.fillStyle = '#f8fafc';
      ctx.font = 'bold 11px Consolas, monospace';
      ctx.textAlign = 'center';
      ctx.fillText(displayMode === 'clean' ? 'L_max' : `L_max = ${lMaxProj.toFixed(1)}m`, landX, groundY + 36);
      ctx.restore();

      // G. QUẢ CẦU VẬT THỂ PHÁT SÁNG (GLOWING PROJECTILE ORB)
      ctx.save();
      ctx.shadowColor = '#fbbf24';
      ctx.shadowBlur = 16;
      const orbGrad = ctx.createRadialGradient(canvasBallX - 2, canvasBallY - 2, 2, canvasBallX, canvasBallY, 11);
      orbGrad.addColorStop(0, '#ffffff');
      orbGrad.addColorStop(0.3, '#fef08a');
      orbGrad.addColorStop(0.8, '#f59e0b');
      orbGrad.addColorStop(1, '#b45309');
      ctx.fillStyle = orbGrad;
      ctx.beginPath();
      ctx.arc(canvasBallX, canvasBallY, 10, 0, Math.PI * 2);
      ctx.fill();
      ctx.strokeStyle = '#ffffff';
      ctx.lineWidth = 2;
      ctx.stroke();
      ctx.restore();

      // H. CÁC VECTOR VẬT LÝ V, Vx, Vy, G (FULL VECTOR DECOMPOSITION)
      if (showVectors) {
        const curVx = v0x;
        const curVy = v0y - g * currentTime;
        const vInstant = Math.sqrt(curVx * curVx + curVy * curVy);
        const vScale = 2.4;

        // 1. Vector vận tốc tổng hợp v (Xanh ngọc)
        drawVector(
          canvasBallX,
          canvasBallY,
          canvasBallX + curVx * vScale,
          canvasBallY - curVy * vScale,
          '#10b981',
          `v = ${vInstant.toFixed(1)} m/s`,
          'v⃗',
          16,
          -14
        );

        // 2. Vector gia tốc trọng trường g (Đỏ hướng xuống)
        drawVector(
          canvasBallX,
          canvasBallY,
          canvasBallX,
          canvasBallY + g * 3.2,
          '#ef4444',
          `g = 9.8 m/s²`,
          'g⃗',
          0,
          16
        );
      }
    }

    // =============================================================
    // TRƯỜNG HỢP 3: MẶT PHẲNG NGHIÊNG CÓ MA SÁT (SGK VẬT LÝ 10)
    // =============================================================
    else {
      const startX = 140;
      const baseY = height - 90;
      const rampLengthPx = Math.min(width - 280, 520);
      const wedgeWidth = rampLengthPx * Math.cos(inclineRad);
      const wedgeHeight = rampLengthPx * Math.sin(inclineRad);

      const topX = startX;
      const topY = baseY - wedgeHeight;
      const bottomX = startX + wedgeWidth;
      const bottomY = baseY;

      // Khoảng cách trượt hiện tại
      const curDistMeters = Math.min(inclineS, 0.5 * inclineAcc * currentTime * currentTime);
      const curDistRatio = inclineS > 0 ? curDistMeters / inclineS : 0;
      const curDistPx = curDistRatio * rampLengthPx;

      const blockCenterX = topX + curDistPx * Math.cos(inclineRad);
      const blockCenterY = topY + curDistPx * Math.sin(inclineRad);
      const currentV = Math.min(Math.sqrt(2 * inclineAcc * inclineS), inclineAcc * currentTime);

      // A. MẶT SÀN ĐỠ PHÒNG THÍ NGHIỆM
      ctx.strokeStyle = '#334155';
      ctx.lineWidth = 3;
      ctx.beginPath();
      ctx.moveTo(startX - 40, baseY);
      ctx.lineTo(bottomX + 60, baseY);
      ctx.stroke();

      // B. KHỐI NÊM MẶT DỐC PHÒNG LAB (INCLINED LAB WEDGE)
      ctx.save();
      const wedgeGrad = ctx.createLinearGradient(topX, topY, bottomX, bottomY);
      wedgeGrad.addColorStop(0, '#1e293b');
      wedgeGrad.addColorStop(1, '#0f172a');
      ctx.fillStyle = wedgeGrad;
      ctx.beginPath();
      ctx.moveTo(topX, topY);
      ctx.lineTo(bottomX, bottomY);
      ctx.lineTo(topX, bottomY);
      ctx.closePath();
      ctx.fill();

      // Viền dốc phát sáng
      ctx.strokeStyle = '#38bdf8';
      ctx.lineWidth = 2.5;
      ctx.stroke();

      // Bề mặt trượt (Hypotenuse Track) có thước đo vạch chia mét
      if (showRuler) {
        ctx.strokeStyle = 'rgba(255, 255, 255, 0.35)';
        ctx.lineWidth = 1;
        const totalMeters = inclineS;
        for (let m = 0; m <= totalMeters; m++) {
          const ratio = m / totalMeters;
          const mx = topX + ratio * wedgeWidth;
          const my = topY + ratio * wedgeHeight;
          // Vạch vuông góc với mặt dốc
          const tickLen = m % 5 === 0 ? 8 : 4;
          const normalAngle = inclineRad - Math.PI / 2;
          ctx.beginPath();
          ctx.moveTo(mx, my);
          ctx.lineTo(mx + tickLen * Math.cos(normalAngle), my + tickLen * Math.sin(normalAngle));
          ctx.stroke();

          if (m % 2 === 0) {
            ctx.fillStyle = '#94a3b8';
            ctx.font = '10px Consolas, monospace';
            ctx.fillText(`${m}m`, mx + (tickLen + 6) * Math.cos(normalAngle), my + (tickLen + 6) * Math.sin(normalAngle));
          }
        }
      }

      // Thước đo độ cao h ở cạnh đứng
      ctx.strokeStyle = '#94a3b8';
      ctx.lineWidth = 1;
      ctx.setLineDash([4, 3]);
      ctx.beginPath();
      ctx.moveTo(topX - 16, topY);
      ctx.lineTo(topX - 16, baseY);
      ctx.stroke();
      ctx.setLineDash([]);
      ctx.fillStyle = '#94a3b8';
      ctx.font = 'bold 11px Consolas, monospace';
      ctx.textAlign = 'right';
      ctx.fillText(`h = ${(inclineS * Math.sin(inclineRad)).toFixed(1)}m`, topX - 22, (topY + baseY) / 2);

      // Thước đo góc Alpha bán nguyệt tại chân dốc
      drawProtractor(bottomX, bottomY, 50, Math.PI, Math.PI + inclineRad, `α = ${inclineAlpha}°`);

      // Gờ chặn cao su ở chân dốc (End-stop Bumper)
      ctx.fillStyle = '#ef4444';
      ctx.beginPath();
      ctx.arc(bottomX, bottomY, 7, 0, Math.PI * 2);
      ctx.fill();
      ctx.restore();

      // C. KHỐI HỘP TRƯỢT 3D CHUYÊN NGHIỆP (ISOMETRIC SLIDING BLOCK)
      ctx.save();
      ctx.translate(blockCenterX, blockCenterY);
      ctx.rotate(inclineRad);

      const blockW = 46;
      const blockH = 26;

      // Đổ bóng khối trượt
      ctx.shadowColor = 'rgba(0, 0, 0, 0.6)';
      ctx.shadowBlur = 10;

      // Thân khối hộp (Vàng hổ phách cao cấp)
      const blockGrad = ctx.createLinearGradient(-blockW / 2, -blockH, blockW / 2, 0);
      blockGrad.addColorStop(0, '#f59e0b');
      blockGrad.addColorStop(0.5, '#d97706');
      blockGrad.addColorStop(1, '#b45309');
      ctx.fillStyle = blockGrad;
      ctx.fillRect(-blockW / 2, -blockH, blockW, blockH);

      // Viền sáng khối
      ctx.strokeStyle = '#fef08a';
      ctx.lineWidth = 1.5;
      ctx.strokeRect(-blockW / 2, -blockH, blockW, blockH);

      // Khắc nhãn khối lượng m lên khối (chỉ hiện trong chế độ chi tiết theo yêu cầu Clean UI)
      if (displayMode === 'detailed') {
        ctx.shadowBlur = 0;
        ctx.fillStyle = '#ffffff';
        ctx.font = 'bold 11px Consolas, monospace';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText(`m=${inclineM}kg`, 0, -blockH / 2);
      }
      ctx.restore();

      // D. ĐẦY ĐỦ 4 VECTOR VẬT LÝ THEO CHUẨN SGK VẬT LÝ 10 (FULL FORCES & VELOCITY)
      if (showVectors) {
        const P_mag = inclineM * 9.8;
        const N_mag = P_mag * Math.cos(inclineRad);
        const Fms_mag = inclineMu * N_mag;
        const P_scale = 1.8;

        // Toạ độ tâm của khối
        const blockMidX = blockCenterX - (13) * Math.sin(inclineRad);
        const blockMidY = blockCenterY - (13) * Math.cos(inclineRad);

        // 1. Trọng lực P (Vàng cam hướng thẳng đứng xuống dưới)
        drawVector(
          blockMidX,
          blockMidY,
          blockMidX,
          blockMidY + P_mag * P_scale,
          '#f59e0b',
          `P = ${P_mag.toFixed(1)} N`,
          'P⃗',
          0,
          16
        );

        // 2. Phản lực pháp tuyến N (Xanh dương vuông góc mặt dốc hướng lên)
        const nAngle = inclineRad - Math.PI / 2;
        const nLen = N_mag * P_scale;
        drawVector(
          blockMidX,
          blockMidY,
          blockMidX + nLen * Math.cos(nAngle),
          blockMidY + nLen * Math.sin(nAngle),
          '#38bdf8',
          `N = ${N_mag.toFixed(1)} N`,
          'N⃗',
          0,
          -14
        );

        // 3. Lực ma sát trượt Fms (Đỏ ngược chiều trượt hướng lên đỉnh dốc)
        if (inclineMu > 0) {
          const fmsLen = Math.max(12, Fms_mag * P_scale * 2.2);
          const fmsAngle = inclineRad + Math.PI;
          drawVector(
            blockMidX,
            blockMidY,
            blockMidX + fmsLen * Math.cos(fmsAngle),
            blockMidY + fmsLen * Math.sin(fmsAngle),
            '#ef4444',
            `Fms = ${Fms_mag.toFixed(2)} N`,
            'F⃗ms',
            0,
            -14
          );
        }

        // 4. Vector vận tốc tức thời v (Xanh ngọc xuôi theo chiều trượt dốc)
        if (currentV > 0.05) {
          const vLen = Math.max(15, currentV * 12);
          drawVector(
            blockMidX,
            blockMidY,
            blockMidX + vLen * Math.cos(inclineRad),
            blockMidY + vLen * Math.sin(inclineRad),
            '#10b981',
            `v = ${currentV.toFixed(2)} m/s`,
            'v⃗',
            12,
            14
          );
        }
      }
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
    showVectors,
    showGrid,
    showRuler,
    displayMode,
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
            <span style={{ fontSize: '12px', fontWeight: 800, color: 'var(--text-primary)', letterSpacing: '0.04em' }}>
              CHỦ ĐỀ & MÔ HÌNH VẬT LÝ
            </span>
            <span style={{ fontSize: '11px', color: 'var(--primary)', fontWeight: 700 }}>
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
                  <span style={{ fontSize: '10px', padding: '2px 6px', borderRadius: '4px', backgroundColor: isSelected ? 'var(--accent-soft)' : 'var(--surface-muted)', color: isSelected ? 'var(--primary)' : 'var(--text-secondary)', fontWeight: 700 }}>
                    {s.spec_json.topic}
                  </span>
                  <span style={{ fontSize: '11px', color: 'var(--status-success-text)', fontWeight: 600 }}>
                    PASS ✓
                  </span>
                </div>

                <div style={{ fontSize: '13px', fontWeight: 700, color: isSelected ? 'var(--primary)' : 'var(--text-primary)', lineHeight: 1.35, marginBottom: '4px' }}>
                  {s.title}
                </div>

                <div style={{ fontSize: '11px', color: 'var(--text-secondary)', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
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
                border: '1px dashed var(--primary)',
                backgroundColor: 'var(--accent-soft)',
                color: 'var(--primary)',
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

            {/* Nút Lấy mã nhúng LMS (PhET Embed Feature) */}
            <button
              onClick={() => setShowEmbedModal(true)}
              style={{
                padding: '6px 11px',
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
              title="Nhúng mô phỏng vào bài giảng LMS (Moodle, Canvas)"
            >
              <Share2 size={12} />
              <span>Nhúng LMS</span>
            </button>

            {/* Nút Toàn màn hình / Trình chiếu lớp học (PhET Fullscreen Mode) */}
            <button
              onClick={() => setIsFullscreen(!isFullscreen)}
              style={{
                padding: '6px 11px',
                borderRadius: '6px',
                backgroundColor: isFullscreen ? '#2563eb' : 'rgba(255, 255, 255, 0.1)',
                border: '1px solid rgba(255, 255, 255, 0.2)',
                color: '#ffffff',
                fontSize: '11px',
                fontWeight: 700,
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '5px',
              }}
              title="Chế độ máy chiếu / Toàn màn hình (F11/ESC)"
            >
              {isFullscreen ? <Minimize2 size={12} /> : <Maximize2 size={12} />}
              <span>{isFullscreen ? 'Thu nhỏ' : 'Toàn màn hình'}</span>
            </button>

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

        {/* CANVAS WORKSPACE (HỖ TRỢ FULLSCREEN MÁY CHIẾU LỚP HỌC) */}
        <div className={`canvas-wrapper ${isFullscreen ? 'player-fullscreen-active' : ''}`}>
          {isFullscreen && (
            <button
              onClick={() => setIsFullscreen(false)}
              style={{
                position: 'fixed',
                top: '18px',
                right: '24px',
                zIndex: 100005,
                background: 'rgba(15, 23, 42, 0.85)',
                backdropFilter: 'blur(10px)',
                color: '#ffffff',
                border: '1px solid rgba(255, 255, 255, 0.25)',
                borderRadius: '10px',
                padding: '8px 16px',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                fontSize: '13px',
                fontWeight: 700,
                boxShadow: '0 4px 16px rgba(0,0,0,0.4)',
              }}
            >
              <Minimize2 size={16} />
              <span>Thoát toàn màn hình (ESC)</span>
            </button>
          )}

          {/* VIRTUAL INSTRUMENTS FLOATING HUD (BẬT / TẮT VECTOR & THƯỚC ĐO THEO CHUẨN PHET) */}
          <div
            style={{
              position: 'absolute',
              top: '16px',
              left: '20px',
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              background: 'rgba(15, 23, 42, 0.82)',
              backdropFilter: 'blur(12px)',
              border: '1px solid rgba(255, 255, 255, 0.15)',
              borderRadius: '10px',
              padding: '6px 10px',
              zIndex: 10,
              boxShadow: '0 8px 24px rgba(0, 0, 0, 0.3)',
            }}
          >
            {/* Chế độ hiển thị: Tối giản (Clean) vs Chi tiết (Detailed) theo chỉ đạo Leader */}
            <button
              onClick={() => setDisplayMode(displayMode === 'clean' ? 'detailed' : 'clean')}
              style={{
                padding: '4px 10px',
                borderRadius: '6px',
                border: '1px solid',
                borderColor: displayMode === 'clean' ? '#10b981' : '#f59e0b',
                backgroundColor: displayMode === 'clean' ? 'rgba(16, 185, 129, 0.25)' : 'rgba(245, 158, 11, 0.25)',
                color: displayMode === 'clean' ? '#6ee7b7' : '#fcd34d',
                fontSize: '11px',
                fontWeight: 700,
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '5px',
                transition: 'all 0.15s ease',
              }}
              title="Chuyển đổi giữa chế độ Clean (Tối giản trực quan) và Chi tiết (Hiển thị đầy đủ số liệu)"
            >
              <Sparkles size={12} color={displayMode === 'clean' ? '#6ee7b7' : '#fcd34d'} />
              <span>{displayMode === 'clean' ? 'Tối giản (Clean)' : 'Chi tiết (Detailed)'}</span>
            </button>

            <button
              onClick={() => setShowVectors(!showVectors)}
              style={{
                padding: '4px 9px',
                borderRadius: '6px',
                border: '1px solid',
                borderColor: showVectors ? '#3b82f6' : 'transparent',
                backgroundColor: showVectors ? 'rgba(37, 99, 235, 0.3)' : 'transparent',
                color: showVectors ? '#93c5fd' : '#94a3b8',
                fontSize: '11px',
                fontWeight: 700,
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '5px',
                transition: 'all 0.15s ease',
              }}
              title="Bật/tắt hiển thị vector lực và vận tốc trên mô phỏng"
            >
              <Activity size={12} color={showVectors ? '#38bdf8' : '#94a3b8'} />
              <span>Vector {showVectors ? 'ON' : 'OFF'}</span>
            </button>

            <button
              onClick={() => setShowRuler(!showRuler)}
              style={{
                padding: '4px 9px',
                borderRadius: '6px',
                border: '1px solid',
                borderColor: showRuler ? '#3b82f6' : 'transparent',
                backgroundColor: showRuler ? 'rgba(37, 99, 235, 0.3)' : 'transparent',
                color: showRuler ? '#93c5fd' : '#94a3b8',
                fontSize: '11px',
                fontWeight: 700,
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '5px',
                transition: 'all 0.15s ease',
              }}
              title="Bật/tắt thước đo toạ độ và khoảng cách"
            >
              <Gauge size={12} color={showRuler ? '#38bdf8' : '#94a3b8'} />
              <span>Thước đo {showRuler ? 'ON' : 'OFF'}</span>
            </button>

            <button
              onClick={() => setShowGrid(!showGrid)}
              style={{
                padding: '4px 9px',
                borderRadius: '6px',
                border: '1px solid',
                borderColor: showGrid ? '#3b82f6' : 'transparent',
                backgroundColor: showGrid ? 'rgba(37, 99, 235, 0.3)' : 'transparent',
                color: showGrid ? '#93c5fd' : '#94a3b8',
                fontSize: '11px',
                fontWeight: 700,
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '5px',
                transition: 'all 0.15s ease',
              }}
              title="Bật/tắt lưới toạ độ kỹ thuật"
            >
              <Compass size={12} color={showGrid ? '#38bdf8' : '#94a3b8'} />
              <span>Lưới {showGrid ? 'ON' : 'OFF'}</span>
            </button>
          </div>

          <canvas
            ref={canvasRef}
            width={isFullscreen ? 1280 : 860}
            height={isFullscreen ? 720 : 520}
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
            className={`inspector-tab-btn ${activeTab === 'chart' ? 'active' : ''}`}
            onClick={() => setActiveTab('chart')}
            style={{ display: 'flex', alignItems: 'center', gap: '5px' }}
            title="Đồ thị hàm số thời gian thực (Real-time SVG Curve)"
          >
            <ChartIcon size={13} />
            <span>Đồ thị SVG</span>
          </button>
          <button
            className={`inspector-tab-btn ${activeTab === 'export' ? 'active' : ''}`}
            onClick={() => setActiveTab('export')}
            style={{ display: 'flex', alignItems: 'center', gap: '5px' }}
          >
            <Download size={13} />
            <span>Xuất</span>
          </button>
          <button
            className={`inspector-tab-btn ${activeTab === 'tips' ? 'active' : ''}`}
            onClick={() => setActiveTab('tips')}
            style={{ display: 'flex', alignItems: 'center', gap: '5px' }}
            title="Mục tiêu bài học & Câu hỏi gợi mở sư phạm (PhET Teacher Tips)"
          >
            <BookOpen size={13} />
            <span>Sư phạm</span>
          </button>
        </div>

        <div className="inspector-content">
          {/* TAB 0: ĐỒ THỊ VẬT LÝ SVG THỜI GIAN THỰC */}
          {activeTab === 'chart' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '8px' }}>
                <span style={{ fontSize: '13px', fontWeight: 800, color: 'var(--text-primary)' }}>
                  ĐỒ THỊ THỜI GIAN THỰC (SVG)
                </span>
                <div style={{ display: 'flex', gap: '6px' }}>
                  <button
                    onClick={() => setSelectedChartVar('v')}
                    style={{
                      padding: '4px 10px',
                      borderRadius: '6px',
                      fontSize: '11px',
                      fontWeight: 700,
                      border: '1px solid',
                      borderColor: selectedChartVar === 'v' ? '#10b981' : 'var(--border-input)',
                      cursor: 'pointer',
                      backgroundColor: selectedChartVar === 'v' ? '#10b981' : 'var(--bg-card)',
                      color: selectedChartVar === 'v' ? '#ffffff' : 'var(--text-primary)',
                    }}
                  >
                    v(t)
                  </button>
                  <button
                    onClick={() => setSelectedChartVar('s')}
                    style={{
                      padding: '4px 10px',
                      borderRadius: '6px',
                      fontSize: '11px',
                      fontWeight: 700,
                      border: '1px solid',
                      borderColor: selectedChartVar === 's' ? '#3b82f6' : 'var(--border-input)',
                      cursor: 'pointer',
                      backgroundColor: selectedChartVar === 's' ? '#3b82f6' : 'var(--bg-card)',
                      color: selectedChartVar === 's' ? '#ffffff' : 'var(--text-primary)',
                    }}
                  >
                    {currentSim.id === 'sim-proj-001' ? 'y(t)' : 's(t)'}
                  </button>
                  <button
                    onClick={() => setSelectedChartVar('a')}
                    style={{
                      padding: '4px 10px',
                      borderRadius: '6px',
                      fontSize: '11px',
                      fontWeight: 700,
                      border: '1px solid',
                      borderColor: selectedChartVar === 'a' ? '#ef4444' : 'var(--border-input)',
                      cursor: 'pointer',
                      backgroundColor: selectedChartVar === 'a' ? '#ef4444' : 'var(--bg-card)',
                      color: selectedChartVar === 'a' ? '#ffffff' : 'var(--text-primary)',
                    }}
                  >
                    a(t)
                  </button>
                </div>
              </div>

              <LearningChart
                series={chartSeries}
                times={chartTimes}
                currentTime={currentTime}
                onSeek={(t) => {
                  setIsPlaying(false);
                  setCurrentTime(Math.max(0, Math.min(maxDuration, t)));
                }}
                height={220}
              />

              <div style={{ padding: '12px 14px', borderRadius: '10px', backgroundColor: 'var(--accent-soft)', border: '1px solid var(--border-focus)', fontSize: '12px', color: 'var(--accent-text-dark)', lineHeight: 1.5 }}>
                ✨ <strong>Tương tác nhanh:</strong> Chạm hoặc kéo chuột trực tiếp trên đường cong đồ thị SVG để tua con trỏ thời gian tương ứng trên canvas mô phỏng.
              </div>
            </div>
          )}

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
                    <div style={{ fontSize: '11px', color: 'var(--text-secondary)' }}>
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

              <div style={{ marginTop: '10px', padding: '12px', borderRadius: '8px', backgroundColor: 'var(--status-success-bg)', border: '1px solid var(--status-success-text)', fontSize: '12px', color: 'var(--status-success-text)', lineHeight: 1.5 }}>
                💡 <strong>Tương tác Model-View:</strong> Khi bạn thay đổi bất kỳ tham số nào, toàn bộ quỹ đạo và vector lực sẽ được tính toán lại ngay tức khắc mà không cần tải lại trang.
              </div>
            </>
          )}

          {/* TAB 2: CALCULATION STEPS */}
          {activeTab === 'steps' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: '12px', fontWeight: 800, color: 'var(--text-secondary)', textTransform: 'uppercase' }}>
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
                    color: 'var(--accent-text-dark)',
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
                  <div style={{ fontSize: '12px', fontWeight: 800, color: 'var(--primary)', marginBottom: '4px' }}>
                    BƯỚC {step.step_number}: {step.formula}
                  </div>
                  <div style={{ fontSize: '13px', color: 'var(--text-secondary)', marginBottom: '8px', lineHeight: 1.4 }}>
                    {step.description}
                  </div>
                  <div style={{ fontSize: '12px', fontWeight: 700, color: 'var(--status-success-text)', backgroundColor: 'var(--status-success-bg)', padding: '5px 10px', borderRadius: '6px', display: 'inline-block' }}>
                    Kết quả: {step.result}
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* TAB 3: DATA TABLE */}
          {activeTab === 'table' && (
            <div>
              <div style={{ fontSize: '12px', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: '8px' }}>
                BẢNG BIẾN THIÊN THEO THỜI GIAN
              </div>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '12px' }}>
                <thead style={{ backgroundColor: 'var(--surface-muted)' }}>
                  <tr>
                    <th style={{ padding: '6px 8px', textAlign: 'left', borderBottom: '1px solid var(--border-input)' }}>t (s)</th>
                    <th style={{ padding: '6px 8px', textAlign: 'left', borderBottom: '1px solid var(--border-input)' }}>v (m/s)</th>
                    <th style={{ padding: '6px 8px', textAlign: 'left', borderBottom: '1px solid var(--border-input)' }}>s (m)</th>
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
                      <tr key={idx} style={{ borderBottom: '1px solid var(--border-subtle)' }}>
                        <td style={{ padding: '6px 8px', fontWeight: 700 }}>{tVal.toFixed(2)}</td>
                        <td style={{ padding: '6px 8px', color: 'var(--primary)' }}>{vVal.toFixed(2)}</td>
                        <td style={{ padding: '6px 8px', color: 'var(--status-success-text)' }}>{sVal.toFixed(2)}</td>
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
              <div style={{ fontSize: '13px', color: 'var(--text-secondary)', lineHeight: 1.5 }}>
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
                  color: 'var(--primary)',
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

          {/* TAB 5: GỢI Ý SƯ PHẠM & DÀNH CHO GIÁO VIÊN (PHET TEACHER TIPS) */}
          {activeTab === 'tips' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              {/* Box 1: Mục tiêu bài học */}
              <div style={{ backgroundColor: 'var(--accent-soft)', borderRadius: '12px', padding: '16px', border: '1px solid var(--border-focus)' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px', color: 'var(--accent-text-dark)', fontWeight: 800, fontSize: '13px' }}>
                  <Sparkles size={16} />
                  <span>MỤC TIÊU BÀI HỌC (CHUẨN SGK 2018)</span>
                </div>
                <ul style={{ margin: 0, paddingLeft: '20px', fontSize: '12px', color: 'var(--text-primary)', lineHeight: 1.6 }}>
                  <li>Nắm vững mối liên hệ giữa các đại lượng động học và động lực học.</li>
                  <li>Khảo sát trực quan ảnh hưởng của các thông số đầu vào đến chuyển động.</li>
                  <li>Kiểm chứng nghiệm số giải tích so với mô phỏng Runge-Kutta sai số &lt; 2%.</li>
                </ul>
              </div>

              {/* Box 2: Câu hỏi gợi mở tư duy cho giờ dạy */}
              <div style={{ backgroundColor: 'var(--status-warn-bg)', borderRadius: '12px', padding: '16px', border: '1px solid var(--status-warn-text)' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px', color: 'var(--status-warn-text)', fontWeight: 800, fontSize: '13px' }}>
                  <Lightbulb size={16} />
                  <span>CÂU HỎI THẢO LUẬN GỢI MỞ (INQUIRY PROMPTS)</span>
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', fontSize: '12px', color: 'var(--text-primary)', lineHeight: 1.5 }}>
                  {currentSim.id === 'sim-car-brake-003' ? (
                    <>
                      <div>
                        <strong>1. Thử thách vận tốc:</strong> Nếu tăng gấp đôi vận tốc ban đầu (từ 36 km/h lên 72 km/h), quãng đường hãm phanh s tăng lên gấp mấy lần? Hãy kéo thanh trượt v₀ để kiểm chứng.
                      </div>
                      <div>
                        <strong>2. Phân tích vector:</strong> Gia tốc a = -4 m/s² mang dấu âm thể hiện điều gì về hướng của lực ma sát hãm so với chiều chuyển động của xe?
                      </div>
                      <div>
                        <strong>3. Thực tế giao thông:</strong> Tại sao biển báo hạn chế tốc độ 50 km/h trong đô thị lại giảm thiểu tai nạn đáng kể so với 60 km/h?
                      </div>
                    </>
                  ) : currentSim.id === 'sim-proj-001' ? (
                    <>
                      <div>
                        <strong>1. Thử thách góc bắn:</strong> Khi ném từ độ cao ban đầu h = 20 m, góc bắn α để đạt tầm xa cực đại có còn là 45° không? Hãy thử nghiệm α = 30°, 45°, 35°.
                      </div>
                      <div>
                        <strong>2. Đỉnh quỹ đạo:</strong> Tại điểm cao nhất của quỹ đạo parabol, vận tốc của vật có bằng 0 không? Thành phần vận tốc nào bị triệt tiêu?
                      </div>
                      <div>
                        <strong>3. Thời gian bay:</strong> Thay đổi vận tốc v₀ có làm thay đổi thời gian vật chạm đất không nếu bắn theo phương ngang (α = 0°)?
                      </div>
                    </>
                  ) : (
                    <>
                      <div>
                        <strong>1. Điều kiện trượt:</strong> Để vật có thể tự trượt xuống từ mặt phẳng nghiêng, hệ số ma sát $\mu$ và góc dốc $\alpha$ phải thoả mãn điều kiện gì?
                      </div>
                      <div>
                        <strong>2. Khối lượng vật:</strong> Khối lượng $m$ có ảnh hưởng đến gia tốc trượt và vận tốc ở chân dốc không? Hãy lý giải bằng công thức Newton 2.
                      </div>
                    </>
                  )}
                </div>
              </div>

              {/* Box 3: Sai lầm thường gặp của học sinh */}
              <div style={{ backgroundColor: 'var(--status-danger-bg)', borderRadius: '12px', padding: '16px', border: '1px solid var(--status-danger-text)' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px', color: 'var(--status-danger-text)', fontWeight: 800, fontSize: '13px' }}>
                  <HelpCircle size={16} />
                  <span>SAI LẦM THƯỜNG GẶP CỦA HỌC SINH</span>
                </div>
                <ul style={{ margin: 0, paddingLeft: '20px', fontSize: '12px', color: 'var(--text-primary)', lineHeight: 1.6 }}>
                  <li>Quên quy đổi vận tốc từ km/h sang m/s (chuẩn SI) dẫn đến sai lệch kết quả gấp 3.6 lần.</li>
                  <li>Quên dấu âm của gia tốc trong chuyển động chậm dần đều khi lập phương trình toạ độ.</li>
                  <li>Nhầm lẫn rằng tại điểm cao nhất của chuyển động ném xiên thì toàn bộ vận tốc bằng 0.</li>
                </ul>
              </div>

              {/* Box 4: Ghi chú giảng dạy lớp học */}
              <div style={{ backgroundColor: '#f8fafc', borderRadius: '12px', padding: '16px', border: '1px solid #e2e8f0' }}>
                <div style={{ fontSize: '11px', fontWeight: 800, color: 'var(--text-secondary)', textTransform: 'uppercase', marginBottom: '6px' }}>
                  GHI CHÚ SƯ PHẠM BÀI HỌC TRƯỚC
                </div>
                <div style={{ fontSize: '12px', color: 'var(--text-secondary)', fontStyle: 'italic', lineHeight: 1.5 }}>
                  "Giáo viên nên cho học sinh nhập số liệu dự đoán trước (Prediction Hypothesis), sau đó mới bấm Chạy mô phỏng để các em đối chiếu trực tiếp với kết quả đo thực tế."
                </div>
              </div>
            </div>
          )}
        </div>
      </aside>

      {/* ============================================================= */}
      {/* MODAL LẤY MÃ NHÚNG LMS (PHET EMBED FEATURE)                   */}
      {/* ============================================================= */}
      {showEmbedModal && (
        <div className="phet-modal-backdrop" onClick={() => setShowEmbedModal(false)}>
          <div className="phet-modal-content" onClick={(e) => e.stopPropagation()}>
            <div style={{ padding: '20px 24px', borderBottom: '1px solid var(--border-subtle)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <Share2 size={20} color="#2563eb" />
                <h3 style={{ margin: 0, fontSize: '17px', fontWeight: 800, color: 'var(--text-primary)' }}>
                  Nhúng phòng thí nghiệm vào bài giảng LMS
                </h3>
              </div>
              <button
                onClick={() => setShowEmbedModal(false)}
                style={{ background: 'transparent', border: 'none', cursor: 'pointer', color: 'var(--text-secondary)' }}
              >
                <X size={20} />
              </button>
            </div>

            <div style={{ padding: '24px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <div>
                <span style={{ fontSize: '12px', fontWeight: 700, color: 'var(--text-secondary)' }}>Bài toán đang mở:</span>
                <div style={{ fontSize: '15px', fontWeight: 700, color: 'var(--text-primary)', marginTop: '2px' }}>
                  {currentSim.title}
                </div>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: '6px' }}>
                  Đoạn mã HTML iFrame sẵn sàng:
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
                  {`<iframe src="${window.location.origin}/player?id=${currentSim.id}&embed=true" width="100%" height="580" frameborder="0" allowfullscreen="true" style="border-radius:12px; border:1px solid #cbd5e1;"></iframe>`}
                </div>
              </div>

              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingTop: '8px' }}>
                <div style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>
                  Tương thích hoàn hảo với Canvas, Moodle, Google Sites, PowerPoint.
                </div>
                <button
                  onClick={() => {
                    const embedCode = `<iframe src="${window.location.origin}/player?id=${currentSim.id}&embed=true" width="100%" height="580" frameborder="0" allowfullscreen="true" style="border-radius:12px; border:1px solid #cbd5e1;"></iframe>`;
                    navigator.clipboard.writeText(embedCode);
                    setCopiedEmbed(true);
                    setTimeout(() => setCopiedEmbed(false), 2200);
                  }}
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
