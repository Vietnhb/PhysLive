import { useState } from 'react';
import { createRoot } from 'react-dom/client';
import SvgPixiScene from '../../src/features/simulation/components/SvgPixiScene';
import '../../src/features/simulation/styles/simulation.css';

// Test programs, not lesson templates: the same production renderer executes both unchanged.
const independent = `async function(PIXI, app, api) {
  const background = new PIXI.Graphics(); app.stage.addChild(background);
  const dots = [], labels = [];
  for (let i=0;i<3;i++) {
    const dot = new PIXI.Graphics().circle(0,0,18).fill({color:['#f59e0b','#38bdf8','#a78bfa'][i]});
    const label = new PIXI.Text({text:'',style:{fontFamily:'Arial',fontSize:16,fill:'#e2e8f0'}});
    app.stage.addChild(dot,label); dots.push(dot); labels.push(label);
  }
  const heading = new PIXI.Text({text:'Ba vật độc lập — tác giả không vẽ dây nối',style:{fontFamily:'Arial',fontSize:21,fill:'#ffffff'}}); app.stage.addChild(heading);
  return {update(frame) {
    const w=frame.width,h=frame.height;
    background.clear().rect(0,0,w,h).fill({color:'#0f172a'});
    heading.position.set(24,24);
    for(let i=0;i<3;i++) {
      dots[i].position.set(w*(.2+i*.3),h*(.55-frame.fields['p'+i+'.y']*.18));
      labels[i].text='Vật '+(i+1)+': '+frame.fields['p'+i+'.y'].toFixed(2)+' m';
      labels[i].position.set(dots[i].x-50,dots[i].y+30);
    }
  }};
}`;
const board = `async function(PIXI, app, api) {
  const svg = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 400"><rect width="800" height="400" rx="20" fill="#fff7ed"/><path d="M170 160 H630 V300 H170 Z" fill="none" stroke="#475569" stroke-width="6"/><rect x="360" y="140" width="120" height="40" rx="5" fill="#fdba74" stroke="#9a3412" stroke-width="3"/><text x="35" y="48" font-family="Arial" font-size="25" fill="#7c2d12">Mạch điện — bố cục do tác giả quyết định</text><path d="M145 200 H195 M158 228 H182" stroke="#1e293b" stroke-width="5"/></svg>';
  const texture=await api.svgTexture(svg); const art=new PIXI.Sprite(texture); app.stage.addChild(art);
  const reading=new PIXI.Text({text:'',style:{fontFamily:'Arial',fontSize:22,fill:'#9a3412'}});app.stage.addChild(reading);
  const particle = new PIXI.Graphics().circle(0,0,9).fill({color:'#ea580c'}); app.stage.addChild(particle);
  return {update(frame){
    const k=Math.min(frame.width/800,frame.height/400);art.scale.set(k);art.position.set((frame.width-800*k)/2,(frame.height-400*k)/2);
    reading.text='I = '+frame.fields['p0.y'].toFixed(2)+' A';reading.position.set(art.x+340*k,art.y+85*k);
    particle.position.set(art.x+(170+460*((frame.t/2)%1))*k,art.y+160*k);
  }, destroy(){api.releaseTexture(texture);}};
}`;
const timeline = (factor: number) => ({durationSeconds:4,frames:Array.from({length:81},(_,i)=>({t:i*.05,values:Object.fromEntries([0,1,2].flatMap(n=>[[`p${n}.x`,n],[`p${n}.y`,factor*Math.sin(i*.05*2+n)]]))}))});
function App() {
  const [program,setProgram]=useState('independent');
  const [factor,setFactor]=useState(1);
  const [error,setError]=useState('');
  const code=program==='independent'?independent:program==='board'?board:program==='legacy'?'':'async function() {throw Error("Lỗi thử nghiệm có chủ đích");}';
  return <main style={{maxWidth:1000,margin:'auto',padding:24,fontFamily:'Arial',color:'#0f172a'}}>
    <h1>Kiểm tra FE chỉ chạy mã hình ảnh</h1><p>Dữ liệu và mã thử riêng; dùng renderer thật. Không gọi AI, không lưu vào hệ thống.</p>
    <label>Chọn thiết kế <select aria-label="Chọn thiết kế" value={program} onChange={e=>{setError('');setProgram(e.target.value);}}>
      <option value="independent">Ba vật độc lập</option><option value="board">Mạch điện SVG có chữ</option><option value="error">Mã bị lỗi</option><option value="legacy">Cảnh StageKit cũ</option>
    </select></label>
    <label style={{marginLeft:20}}>Đổi dữ liệu thử <input aria-label="Đổi dữ liệu thử" type="range" min=".2" max="2" step=".1" value={factor} onChange={e=>setFactor(Number(e.target.value))}/></label>
    <SvgPixiScene program={{code,playbackRate:1,...(program==='legacy'?{scene:{drawings:[]}}:{})}} timeline={timeline(factor)} parameters={{factor}}
      verificationStatus="VISUAL_ONLY_UNVERIFIED" onRenderError={setError} />
    <output aria-label="Lỗi renderer">{error}</output>
  </main>;
}
createRoot(document.getElementById('root')!).render(<App />);
