
import {useState} from 'react';
import {createRoot} from 'react-dom/client';
import SvgPixiScene from '../../src/features/simulation/components/SvgPixiScene';
import resolver from '../../../backend/src/main/resources/prompts/teacher-frame-resolver.js?raw';
import '../../src/features/simulation/styles/simulation.css';
const timeline={durationSeconds:1,frames:[{t:0,dt:0,values:{'opaque.x':5.04e-29}},{t:1,dt:1,values:{'opaque.x':123.123456789}}]};
const presentation={version:1 as const,frameResolver:resolver,parameters:{m:3.123456789},fieldRanges:{'opaque.x':{min:5.04e-29,max:123.123456789}},initialFrame:timeline.frames[0],scene:{durationSeconds:1,fields:{'opaque.x':{key:'opaque.x',participantId:'opaque',quantity:'x',label:'Original label',symbol:'Q',unit:'J',min:5.04e-29,max:123.123456789}},constants:{},participants:[{id:'opaque',label:'Original body',colorIndex:0}],observables:['opaque.x'],parameters:{m:{label:'Original m',unit:'kg'}}}};
const code=`async function(PIXI,app,api){
 const svg='<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 200"><defs><linearGradient id="gradient"><stop stop-color="red"/><stop offset="1" stop-color="blue"/></linearGradient><path id="shape" d="M20 80H300V150H20Z"/></defs><rect width="400" height="200" fill="white"/><use href="#shape" fill="url(#gradient)"/><text x="20" y="40" fill="black">Original SVG text</text></svg>';
 const texture=await api.svgTexture(svg); const sprite=new PIXI.Sprite(texture);app.stage.addChild(sprite);
 return {update(frame){sprite.width=frame.width; sprite.height=frame.height;},destroy(){api.releaseTexture(texture)}};
}`;
function App(){const [bad,setBad]=useState(false);return <main style={{width:800}}><h1>Deterministic renderer regression fixture</h1><button onClick={()=>setBad(true)}>Reject unsupported SVG</button><SvgPixiScene key={String(bad)} program={{code:bad?code.replace('<defs>','<script>bad</script><defs>'):code,playbackRate:1}} timeline={timeline} presentation={presentation} parameters={presentation.parameters} verificationStatus="VERIFIED_BY_LAWS"/></main>}
createRoot(document.getElementById('root')!).render(<App/>);
