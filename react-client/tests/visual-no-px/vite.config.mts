import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react-swc';

// Test server only. Production sources remain unchanged.
export default defineConfig({
  plugins: [{
    name: 'visual-no-pixel-experiment', enforce: 'pre',
    transform(source, id) {
      if (!id.replaceAll('\\', '/').endsWith('/engine/stageKit.ts')) return;
      const start = source.indexOf('    function verify(run:');
      const end = source.indexOf('    host.register?.({', start);
      if (start < 0 || end < 0) throw Error('Cannot isolate visual verifier');
      const semantic = `    function verify(run: (t: number) => void, times: number[]): string[] {
      ensureBuilt();
      const issues: string[] = [];
      const mustShow = watched().length ? watched() : mode === "board" && !instruments.length ? meterKeys() : [];
      for (const meta of mustShow) if (!instruments.some(ins => ins.key === meta.key) && !gauges.some(gauge => gauge.key === meta.key) && !movesWith(meta))
        issues.push(meta.key + ": watched field has no binding on the stage");
      for (const t of times) {
        run(t);
        const values = host.sample(t);
        for (const ins of instruments) {
          if (!ins.key) continue;
          const value = values[ins.key];
          if (!Number.isFinite(value)) issues.push(ins.key + ": non-finite solver value");
          if (ins.readout && ins.readout.text !== instrumentReading(ins, value)) issues.push(ins.key + ": readout differs from solver");
          if (!ins.shown || Math.abs(ins.shown.t - t) > 1e-9 * Math.max(1, t) || Math.abs(ins.shown.fraction - fractionOf(ins, value)) > 1e-6)
            issues.push(ins.key + ": drive differs from solver");
        }
        if (generatedNodes().length > NODE_BUDGET) issues.push("Display object budget exceeded");
      }
      return [...new Set(issues)];
    }
`;
      return { code: source.slice(0, start) + semantic + source.slice(end), map: null };
    }
  }, react()],
  server: { host: 'localhost', port: 5174, strictPort: true, proxy: { '/api': { target: 'http://localhost:8080', changeOrigin: true } } }
});
