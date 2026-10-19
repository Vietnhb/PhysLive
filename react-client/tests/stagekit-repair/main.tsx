import {createRoot} from 'react-dom/client';
import SvgPixiScene from '../../src/features/simulation/components/SvgPixiScene';
import '../../src/features/simulation/styles/simulation.css';
const snapshots=import.meta.glob('../../../tmp/stagekit-repair/*-authored.json',{eager:true,import:'default'}) as Record<string,any>;
const id=new URLSearchParams(location.search).get('id')!;
const snapshot=snapshots['../../../tmp/stagekit-repair/'+id+'-authored.json'];
const spec=snapshot.simulationSpec;
createRoot(document.getElementById('root')!).render(<main style={{width:'100%'}}><SvgPixiScene program={spec.visualProgram} timeline={spec.solverTimeline} parameters={snapshot.savedParameters} parameterInfo={spec.parameters} fieldMeta={spec.solverFieldMeta} models={spec.physicsModels} observables={spec.observables} presentation={snapshot.presentation} verificationStatus={snapshot.validation.status} onRenderError={message=>{document.documentElement.dataset.renderError=message;}}/></main>);
