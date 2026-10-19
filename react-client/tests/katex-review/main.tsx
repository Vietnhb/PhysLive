import React, { useState } from 'react';
import { createRoot } from 'react-dom/client';
import { Theme } from '@radix-ui/themes';
import '@radix-ui/themes/styles.css';
import { FormulaReview } from '../../src/features/simulation/components/SimulationReview';
import { CapabilitiesEditor } from '../../src/features/reviewer/components/CapabilitiesEditor';
import rcCircuit from '../../../backend/src/main/resources/schemas/library/capabilities/electricity/rc_circuit.json';
import '../../src/features/reviewer/styles/reviewer.css';
import MathFormula from '../../src/shared/ui/MathFormula';
import type { IntentResult } from '../../src/features/simulation/api/simulationUnderstandingApi';

const capability: Record<string, unknown> = { ...rcCircuit, title: 'Nạp điện cho tụ' };
const intent = { formulas: [{ modelId: 'circuit', label: 'Nguồn điện và mạch ngoài', canonical: ['I = E/(R_total + r)', 'R_total = R1 + R2*R3/(R2+R3)'], derived: ['U = E - I*r', 'P = U*I'] }, { modelId: 'pendulum', label: 'Con lắc', canonical: ['d2theta/dt2 = -g/L*sin(theta)', 'omega0 = sqrt(g/L)'], derived: ['Chu kỳ: T = 2*pi*sqrt(L/g)'] }] } as IntentResult;
function App() {
  const [capabilities, setCapabilities] = useState([capability]);
  const [dark, setDark] = useState(false);
  return <Theme appearance={dark ? 'dark' : 'light'}><main style={{ maxWidth: 900, margin: 'auto', padding: 24, color: 'var(--gray-12)' }}>
    <button onClick={() => setDark(!dark)}>Đổi sáng / tối</button>
    <h1>Công thức cho giáo viên và học sinh</h1>
    <p>Trang thử nghiệm dùng các thành phần thực tế. Chỉnh sửa ở đây chỉ thay đổi dữ liệu trong trang này.</p>
    <section style={{ padding: 20, border: '1px solid var(--gray-6)', borderRadius: 12 }}><FormulaReview intent={intent} /></section>
    <h2>Giáo viên chỉnh sửa và xem trước</h2>
    <div className="reviewer-academic" style={{ '--ink': 'var(--gray-12)', '--muted': 'var(--gray-11)' } as React.CSSProperties}><CapabilitiesEditor capabilities={capabilities} objectTypes={['body']} onChange={setCapabilities} /></div>
    <details><summary>Dữ liệu AST hiện tại (chỉ trong trang thử)</summary><pre data-testid="stored-ast">{JSON.stringify(capabilities[0].execution, null, 2)}</pre></details>
    <h2>Cú pháp chưa hỗ trợ vẫn giữ nguyên</h2><MathFormula equation={'Công thức mới: a = b +'} block />
  </main></Theme>;
}
createRoot(document.getElementById('root')!).render(<App />);
