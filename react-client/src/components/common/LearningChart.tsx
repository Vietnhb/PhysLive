import React, { useEffect, useMemo, useRef, useState } from 'react';

export interface ChartSeries {
  label: string;
  unit: string;
  color: string;
  data: number[];
}

interface LearningChartProps {
  series: ChartSeries;
  times: number[];
  currentTime: number;
  onSeek: (time: number) => void;
  height?: number;
}

export const LearningChart: React.FC<LearningChartProps> = ({
  series,
  times,
  currentTime,
  onSeek,
  height = 180,
}) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const [width, setWidth] = useState<number>(720);

  useEffect(() => {
    if (!containerRef.current) return;
    const observer = new ResizeObserver((entries) => {
      if (entries[0]) {
        setWidth(Math.max(280, Math.round(entries[0].contentRect.width)));
      }
    });
    observer.observe(containerRef.current);
    return () => observer.disconnect();
  }, []);

  // Find index closest to currentTime
  const currentIndex = useMemo(() => {
    if (!times.length) return 0;
    let closestIdx = 0;
    let minDiff = Math.abs(times[0] - currentTime);
    for (let i = 1; i < times.length; i++) {
      const diff = Math.abs(times[i] - currentTime);
      if (diff < minDiff) {
        minDiff = diff;
        closestIdx = i;
      }
    }
    return closestIdx;
  }, [times, currentTime]);

  const plot = useMemo(() => {
    const data = series.data.length ? series.data : [0];
    const low = Math.min(...data);
    const high = Math.max(...data);
    const span = high - low;
    const pad = Math.max(span * 0.15, Math.abs(high) * 0.08, 0.05);
    const min = low - pad;
    const max = high + pad;

    const start = times[0] ?? 0;
    const end = times[times.length - 1] ?? 1;
    const duration = Math.max(0.001, end - start);

    const left = 65;
    const right = width - 24;
    const top = 20;
    const bottom = height - 35;

    const scaleX = (t: number) => left + ((t - start) / duration) * (right - left);
    const scaleY = (val: number) => bottom - ((val - min) / (max - min)) * (bottom - top);

    const stride = Math.max(1, Math.ceil(times.length / 600));
    const points = data
      .flatMap((val, i) =>
        i % stride === 0 || i === times.length - 1
          ? [`${scaleX(times[i] ?? 0).toFixed(1)},${scaleY(val).toFixed(1)}`]
          : []
      )
      .join(' ');

    return {
      scaleX,
      scaleY,
      left,
      right,
      top,
      bottom,
      min,
      max,
      points,
      start,
      duration,
    };
  }, [series.data, times, width, height]);

  const currentValue = series.data[currentIndex] ?? 0;
  const currentT = times[currentIndex] ?? currentTime;
  const cursorX = plot.scaleX(currentT);
  const cursorY = plot.scaleY(currentValue);

  const handlePointerDown = (event: React.PointerEvent<SVGSVGElement>) => {
    const rect = event.currentTarget.getBoundingClientRect();
    const clickX = ((event.clientX - rect.left) / rect.width) * width;
    const ratio = Math.max(0, Math.min(1, (clickX - plot.left) / (plot.right - plot.left)));
    const targetTime = plot.start + ratio * plot.duration;
    onSeek(targetTime);
  };

  return (
    <div
      ref={containerRef}
      style={{
        width: '100%',
        backgroundColor: '#ffffff',
        borderRadius: '12px',
        border: '1px solid #e2e8f0',
        padding: '12px 16px',
        boxShadow: '0 2px 8px rgba(0,0,0,0.02)',
        userSelect: 'none',
      }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span
            style={{
              width: '10px',
              height: '10px',
              borderRadius: '50%',
              backgroundColor: series.color,
              display: 'inline-block',
            }}
          />
          <span style={{ fontSize: '13px', fontWeight: 700, color: '#1e293b' }}>
            {series.label} ({series.unit}) theo thời gian
          </span>
        </div>
        <div style={{ fontSize: '12px', fontFamily: 'monospace', fontWeight: 700, color: series.color }}>
          t = {currentT.toFixed(2)}s ➔ {currentValue.toFixed(2)} {series.unit}
        </div>
      </div>

      <svg
        viewBox={`0 0 ${width} ${height}`}
        style={{ width: '100%', height: 'auto', display: 'block', cursor: 'crosshair' }}
        onPointerDown={handlePointerDown}
      >
        <title>{`${series.label} (${series.unit})`}</title>

        {/* Horizontal grid lines & Y ticks */}
        {[0, 0.5, 1].map((ratio) => {
          const val = plot.min + (plot.max - plot.min) * ratio;
          const y = plot.scaleY(val);
          return (
            <g key={ratio}>
              <line
                x1={plot.left}
                x2={plot.right}
                y1={y}
                y2={y}
                stroke="#e2e8f0"
                strokeWidth={1}
                strokeDasharray="4 4"
              />
              <text
                x={plot.left - 10}
                y={y + 4}
                textAnchor="end"
                fontSize="10"
                fill="#64748b"
                fontFamily="monospace"
                fontWeight="600"
              >
                {val.toFixed(Math.abs(val) < 0.1 ? 2 : 1)}
              </text>
            </g>
          );
        })}

        {/* Time X Ticks */}
        {[0, 0.25, 0.5, 0.75, 1].map((ratio) => {
          const tVal = plot.start + ratio * plot.duration;
          const x = plot.scaleX(tVal);
          return (
            <text
              key={ratio}
              x={x}
              y={plot.bottom + 18}
              textAnchor="middle"
              fontSize="10"
              fill="#94a3b8"
              fontFamily="monospace"
              fontWeight="600"
            >
              {tVal.toFixed(1)}s
            </text>
          );
        })}

        {/* Polyline series curve */}
        <polyline
          points={plot.points}
          fill="none"
          stroke={series.color}
          strokeWidth="2.8"
          strokeLinecap="round"
          strokeLinejoin="round"
        />

        {/* Time cursor line */}
        <line
          x1={cursorX}
          x2={cursorX}
          y1={plot.top}
          y2={plot.bottom}
          stroke="#ef4444"
          strokeWidth="1.5"
          strokeDasharray="3 3"
        />

        {/* Active point marker */}
        <circle
          cx={cursorX}
          cy={cursorY}
          r={6}
          fill={series.color}
          stroke="#ffffff"
          strokeWidth={2.5}
          style={{ filter: 'drop-shadow(0 2px 4px rgba(0,0,0,0.2))' }}
        />
      </svg>
    </div>
  );
};

export default LearningChart;
