import { useState } from 'react';
import { createRoot } from 'react-dom/client';
import Original from '/src/features/simulation/components/SvgPixiScene';
import NoPixels from './SvgPixiSceneNoPx';
import baseline from '../../../output/physlive-circuit-live-test/visual-role-complete/generated-response.json';
import fresh from '../../../output/physlive-circuit-live-test/visual-no-px/generated-response.json';
import '/src/styles/global.css';
import '/src/features/simulation/styles/learning.css';
import '/src/features/simulation/styles/simulation.css';
import '/src/shared/theme/themeStore';
function App(){
 const [noPixels,setNoPixels]=useState(true),[newDesign,setNewDesign]=useState(false);
 const result=newDesign?fresh:baseline, spec=result.simulationSpec;
 const Player=noPixels?NoPixels:Original;
 const parameters=Object.fromEntries(spec.parameters.map(p=>[p.name,p.value]));
 return <main style={{maxWidth:960,margin:'0 auto',padding:20}}>
  <h2>Thử visual với cùng dữ liệu vật lý</h2>
  <div style={{display:'flex',gap:20,flexWrap:'wrap',marginBottom:16}}>
   <label><input type="checkbox" checked={noPixels} onChange={e=>setNoPixels(e.target.checked)}/> Bỏ kiểm tra hình học/pixel</label>
   <label><input type="checkbox" checked={newDesign} onChange={e=>setNewDesign(e.target.checked)}/> Đầu ra visual mới</label>
  </div>
  <Player key={`${noPixels}-${newDesign}`} program={spec.visualProgram} timeline={spec.solverTimeline} parameters={parameters} models={spec.physicsModels} fieldMeta={spec.solverFieldMeta} observables={spec.observables} verificationStatus={result.validation.status}/>
 </main>;
}
createRoot(document.getElementById('root')!).render(<App/>);
