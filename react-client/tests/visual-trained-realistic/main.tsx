// Replay AIService.generate unchanged; only the visual provider has been replaced.
import { createRoot } from 'react-dom/client';
import { MemoryRouter } from 'react-router-dom';
import axios from 'axios';
import axiosClient from '../../src/shared/api/client';
import { useSessionStore } from '../../src/shared/auth/sessionStore';
import SimulationWorkspace from '../../src/features/simulation/pages/SimulationWorkspace';
import fixtures from './results.json';
const sample=Math.max(1,Math.min(3,Number(new URLSearchParams(window.location.search).get('sample'))||1));
const selected=fixtures[sample-1];
const generated=selected.generated;
document.title=`PhysLive · train linh kiện thật · lần ${sample}`;
import '../../src/styles/global.css';
import '../../src/shared/theme/themeStore';

const originalAdapter = axios.getAdapter(axiosClient.defaults.adapter!);
axiosClient.defaults.adapter = async config => {
  if (config.method === 'get' && config.url === '/simulation/saved/visual-replay') {
    const result=structuredClone(generated);
    return {data:result,status:200,statusText:'OK',headers:{},config};
  }
  return originalAdapter(config);
};
async function start() {
  if(!generated){createRoot(document.getElementById('root')!).render(<main style={{padding:24}}><h1>Lần sinh {sample}: SVG không đạt kiểm tra</h1><p>Giữ nguyên đầu ra lỗi; không sửa tay thành hình đẹp.</p><nav>{[1,2,3].map(n=><a key={n} href={`?sample=${n}`} style={{marginRight:20}}>Lần {n}</a>)}</nav><pre style={{whiteSpace:'pre-wrap'}}>{selected.raw}</pre></main>);return;}
  try {
    const profile = await axiosClient.get('/user/me');
    useSessionStore.getState().setUser(profile.data);
    createRoot(document.getElementById('root')!).render(
      <MemoryRouter initialEntries={['/workspace?simulationId=visual-replay']}>
        <SimulationWorkspace/>
      </MemoryRouter>
    );
  } catch {
    document.getElementById('root')!.textContent='Hãy đăng nhập vào PhysLive trước khi mở bản replay.';
  }
}
void start();



