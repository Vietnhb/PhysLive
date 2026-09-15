import React, { useEffect, useRef } from 'react';

export const PhysicsOrbital: React.FC = () => {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    let animationFrameId: number;

    interface Shockwave {
      x: number;
      y: number;
      radius: number;
      maxRadius: number;
      opacity: number;
    }

    let shockwaves: Shockwave[] = [];

    // Particle definition
    class Particle {
      x: number;
      y: number;
      vx: number;
      vy: number;
      radius: number;
      color: string;
      isHub: boolean;

      constructor(w: number, h: number) {
        this.x = Math.random() * (w || 800);
        this.y = Math.random() * (h || 600);
        this.vx = (Math.random() - 0.5) * 1.5;
        this.vy = (Math.random() - 0.5) * 1.5;
        this.isHub = Math.random() < 0.18;
        this.radius = this.isHub ? Math.random() * 2 + 4.5 : Math.random() * 2 + 2.5;
        const hues = ['#818cf8', '#a855f7', '#38bdf8', '#22d3ee', '#c084fc', '#60a5fa'];
        this.color = hues[Math.floor(Math.random() * hues.length)];
      }

      update(w: number, h: number, mouseX: number, mouseY: number) {
        // Bounce off walls
        if (this.x < 0 || this.x > w) this.vx *= -1;
        if (this.y < 0 || this.y > h) this.vy *= -1;

        // Mouse repulsion
        const dx = this.x - mouseX;
        const dy = this.y - mouseY;
        const distSq = dx * dx + dy * dy;
        if (distSq < 180 * 180 && distSq > 1) {
          const dist = Math.sqrt(distSq);
          const force = ((180 - dist) / 180) * 0.8;
          this.vx += (dx / dist) * force;
          this.vy += (dy / dist) * force;
        }

        // Friction
        this.vx *= 0.99;
        this.vy *= 0.99;

        // Speed clamping
        const speed = Math.sqrt(this.vx * this.vx + this.vy * this.vy);
        if (speed < 0.3) {
          this.vx *= 1.08;
          this.vy *= 1.08;
        } else if (speed > 7) {
          this.vx *= 0.96;
          this.vy *= 0.96;
        }

        this.x += this.vx;
        this.y += this.vy;
      }

      draw(context: CanvasRenderingContext2D) {
        context.save();
        // Halo
        context.beginPath();
        context.arc(this.x, this.y, this.radius * 2.2, 0, Math.PI * 2);
        context.fillStyle = this.color;
        context.globalAlpha = this.isHub ? 0.35 : 0.18;
        context.fill();

        // Core
        context.beginPath();
        context.arc(this.x, this.y, this.radius, 0, Math.PI * 2);
        context.fillStyle = this.color;
        context.globalAlpha = 1.0;
        context.fill();
        context.restore();
      }
    }

    let particles: Particle[] = [];

    const initParticles = (w: number, h: number) => {
      particles = [];
      const count = Math.max(70, Math.min(220, Math.floor((w * h) / 7500)));
      for (let i = 0; i < count; i++) {
        particles.push(new Particle(w, h));
      }
    };

    // Resize handler
    const resizeCanvas = () => {
      const parent = canvas.parentElement;
      const w = Math.round(parent?.clientWidth || canvas.clientWidth || window.innerWidth * 0.55);
      const h = Math.round(parent?.clientHeight || canvas.clientHeight || window.innerHeight - 65);
      if (w > 0 && h > 0 && (canvas.width !== w || canvas.height !== h)) {
        canvas.width = w;
        canvas.height = h;
        initParticles(w, h);
      }
    };

    resizeCanvas();
    const resizeObserver = new ResizeObserver(() => {
      resizeCanvas();
    });
    if (canvas.parentElement) {
      resizeObserver.observe(canvas.parentElement);
    }

    let mouseX = -1000;
    let mouseY = -1000;

    const handleMouseMove = (e: MouseEvent) => {
      const rect = canvas.getBoundingClientRect();
      mouseX = e.clientX - rect.left;
      mouseY = e.clientY - rect.top;
    };

    const handleMouseLeave = () => {
      mouseX = -1000;
      mouseY = -1000;
    };

    const handleClick = (e: MouseEvent) => {
      const rect = canvas.getBoundingClientRect();
      const clickX = e.clientX - rect.left;
      const clickY = e.clientY - rect.top;

      shockwaves.push({
        x: clickX,
        y: clickY,
        radius: 10,
        maxRadius: 280,
        opacity: 1,
      });

      // Blast impulse to particles
      particles.forEach((p) => {
        const dx = p.x - clickX;
        const dy = p.y - clickY;
        const dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < 260 && dist > 1) {
          const force = ((260 - dist) / 260) * 12;
          p.vx += (dx / dist) * force;
          p.vy += (dy / dist) * force;
        }
      });
    };

    canvas.addEventListener('mousemove', handleMouseMove);
    canvas.addEventListener('mouseleave', handleMouseLeave);
    canvas.addEventListener('click', handleClick);

    const animate = () => {
      if (particles.length === 0 && canvas.width > 0 && canvas.height > 0) {
        initParticles(canvas.width, canvas.height);
      }

      ctx.fillStyle = '#090d16';
      ctx.fillRect(0, 0, canvas.width, canvas.height);

      // Draw connections
      ctx.save();
      const maxConnectDist = 170;
      for (let i = 0; i < particles.length; i++) {
        for (let j = i + 1; j < particles.length; j++) {
          const dx = particles[i].x - particles[j].x;
          const dy = particles[i].y - particles[j].y;
          const distSq = dx * dx + dy * dy;

          if (distSq < maxConnectDist * maxConnectDist) {
            const dist = Math.sqrt(distSq);
            const factor = 1 - dist / maxConnectDist;
            const opacity = factor * 0.55;
            ctx.beginPath();
            ctx.moveTo(particles[i].x, particles[i].y);
            ctx.lineTo(particles[j].x, particles[j].y);
            ctx.strokeStyle = `rgba(168, 85, 247, ${opacity})`;
            ctx.lineWidth = factor > 0.6 ? 1.4 : 0.9;
            ctx.stroke();
          }
        }
      }
      ctx.restore();

      // Shockwaves
      if (shockwaves.length > 0) {
        ctx.save();
        for (let i = shockwaves.length - 1; i >= 0; i--) {
          const sw = shockwaves[i];
          sw.radius += 5.5;
          sw.opacity -= 0.022;

          if (sw.opacity <= 0 || sw.radius >= sw.maxRadius) {
            shockwaves.splice(i, 1);
            continue;
          }

          ctx.beginPath();
          ctx.arc(sw.x, sw.y, sw.radius, 0, Math.PI * 2);
          ctx.strokeStyle = `rgba(168, 85, 247, ${sw.opacity})`;
          ctx.lineWidth = 2.5;
          ctx.stroke();

          if (sw.radius > 12) {
            ctx.beginPath();
            ctx.arc(sw.x, sw.y, sw.radius - 10, 0, Math.PI * 2);
            ctx.strokeStyle = `rgba(56, 189, 248, ${sw.opacity * 0.7})`;
            ctx.lineWidth = 1.5;
            ctx.stroke();
          }
        }
        ctx.restore();
      }

      // Update & draw particles
      particles.forEach((p) => {
        p.update(canvas.width, canvas.height, mouseX, mouseY);
        p.draw(ctx);
      });

      animationFrameId = requestAnimationFrame(animate);
    };

    animate();

    return () => {
      resizeObserver.disconnect();
      canvas.removeEventListener('mousemove', handleMouseMove);
      canvas.removeEventListener('mouseleave', handleMouseLeave);
      canvas.removeEventListener('click', handleClick);
      cancelAnimationFrame(animationFrameId);
    };
  }, []);

  return (
    <div
      style={{
        position: 'absolute',
        inset: 0,
        width: '100%',
        height: '100%',
        backgroundColor: '#090d16',
        overflow: 'hidden',
      }}
    >
      <canvas
        ref={canvasRef}
        style={{
          position: 'absolute',
          inset: 0,
          width: '100%',
          height: '100%',
          display: 'block',
          cursor: 'crosshair',
        }}
      />
      {/* HUD Badge */}
      <div
        style={{
          position: 'absolute',
          bottom: '28px',
          left: '28px',
          right: '28px',
          pointerEvents: 'none',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'flex-end',
          zIndex: 10,
        }}
      >
        <div
          style={{
            background: 'rgba(15, 23, 42, 0.85)',
            backdropFilter: 'blur(14px)',
            border: '1px solid rgba(255, 255, 255, 0.12)',
            borderRadius: '12px',
            padding: '12px 18px',
            maxWidth: '400px',
            boxShadow: '0 8px 24px rgba(0, 0, 0, 0.3)',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
            <span
              style={{
                width: '8px',
                height: '8px',
                borderRadius: '50%',
                background: '#10b981',
                display: 'inline-block',
                boxShadow: '0 0 8px #10b981',
              }}
            />
            <span
              style={{
                fontSize: '12px',
                fontWeight: 800,
                textTransform: 'uppercase',
                letterSpacing: '0.06em',
                color: '#38bdf8',
              }}
            >
              Mô phỏng động lực học hạt (Interactive Lab)
            </span>
          </div>
          <p style={{ margin: 0, fontSize: '12px', color: '#94a3b8', lineHeight: 1.5 }}>
            Rê chuột để đẩy hạt • Nhấp chuột để kích hoạt sóng xung kích gợn sóng
          </p>
        </div>
      </div>
    </div>
  );
};
