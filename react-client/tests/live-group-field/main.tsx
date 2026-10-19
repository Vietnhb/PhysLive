
import {createRoot} from 'react-dom/client';
import SvgPixiScene from '../../src/features/simulation/components/SvgPixiScene';
import result from '../../../tmp/live-group-field/fixture-waves-generated.json';
import '../../src/features/simulation/styles/simulation.css';
const spec=result.simulationSpec;
createRoot(document.getElementById('root')!).render(<main style={{width:960}}><h1>Live visual provider — verified waves fixture</h1><p>Original backend output; no visual code edits.</p><SvgPixiScene program={spec.visualProgram} timeline={spec.solverTimeline} parameters={Object.fromEntries(result.parameters.map(p=>[p.name,p.value]))} fieldMeta={spec.solverFieldMeta} models={spec.physicsModels} observables={spec.observables} verificationStatus={result.validation.status} onRenderError={message=>{document.documentElement.dataset.renderError=message;}}/></main>);
