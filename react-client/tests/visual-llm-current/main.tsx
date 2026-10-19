// Replay AIService.generate unchanged; only the visual provider has been replaced.
import { createRoot } from 'react-dom/client';
import { MemoryRouter } from 'react-router-dom';
import axios from 'axios';
import axiosClient from '../../src/shared/api/client';
import { useSessionStore } from '../../src/shared/auth/sessionStore';
import SimulationWorkspace from '../../src/features/simulation/pages/SimulationWorkspace';
import generated from '../../../output/physlive-circuit-live-test/current-visual-retest/generated-response.json';
import '../../src/styles/global.css';
import '../../src/shared/theme/themeStore';

const originalAdapter = axios.getAdapter(axiosClient.defaults.adapter!);
axiosClient.defaults.adapter = async config => {
  if (config.method === 'get' && config.url === '/simulation/saved/visual-replay') {
    const result=structuredClone(generated);
    if (new URLSearchParams(location.search).get('omit') === 'R3') result.simulationSpec.visualProgram.scene.annotations = result.simulationSpec.visualProgram.scene.annotations.filter(note => note.field !== 'p_parallel.current_2');
    return {data:result,status:200,statusText:'OK',headers:{},config};
  }
  return originalAdapter(config);
};
async function start() {
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


