import {useEffect,useRef,useState} from 'react';
import {createRoot} from 'react-dom/client';
import axiosClient from '../../src/shared/api/client';
import {sampleTimeline} from '../../src/features/simulation/model/svgScene';
import original from '../../../output/physlive-circuit-live-test/visual-role-complete/generated-response.json';
import {visualDesign} from './design';
import './style.css';
import {createTransport,position,type Particle} from './transport';

const format=(n:number)=>new Intl.NumberFormat('vi-VN',{maximumFractionDigits:3}).format(n);
function validate(values:Record<string,number>){
 const t=visualDesign.topology;
 const nodes=new Set(t.nodes);
 if(t.components.some(c=>!nodes.has(c.from)||!nodes.has(c.to)))throw Error('Mạch có đầu nối không hợp lệ');
 const source=t.components.find(c=>c.id==='source')!;
 const seen=new Set([source.to]),queue=[source.to];
 while(queue.length){const n=queue.shift()!;for(const c of t.components.filter(c=>c.id!=='source')){const other=c.from===n?c.to:c.to===n?c.from:null;if(other&&!seen.has(other)){seen.add(other);queue.push(other);}}}
 if(!seen.has(source.from))throw Error('Mạch ngoài chưa nối kín với nguồn');
 for(const l of visualDesign.desktop.labels)if(l.field&&!Number.isFinite(values[l.field]))throw Error('Số đọc chưa có dữ liệu');
 const incoming=values[t.conservation.incoming],outgoing=t.conservation.outgoing.reduce((sum,k)=>sum+values[k],0);
 if(!Number.isFinite(outgoing)||Math.abs(incoming-outgoing)>1e-8*Math.max(1,Math.abs(incoming)))throw Error('Dòng điện chưa bảo toàn ở nút phân nhánh');
}
function App(){
 const [parameters,setParameters]=useState({emf:12,R1:4}),[timeline,setTimeline]=useState(original.simulationSpec.solverTimeline),[running,setRunning]=useState(true),[busy,setBusy]=useState(false),[error,setError]=useState(''),[mobile,setMobile]=useState(false),[frame,setFrame]=useState({t:0,particles:[] as Particle[]});
 const box=useRef<HTMLDivElement>(null),version=useRef(0);
 const playback=useRef({running,busy});playback.current={running,busy};
 const design=mobile?visualDesign.mobile:visualDesign.desktop;
 const values=sampleTimeline(timeline,frame.t);
 const inputValue=(participant:string,key:string)=>{const model=original.simulationSpec.physicsModels.find(m=>m.id===participant);const input=(model?.inputs as Record<string,number|string>)[key];return typeof input==='number'?input:input in parameters?parameters[input as keyof typeof parameters]:values[input];};
 useEffect(()=>{const observer=new ResizeObserver(([e])=>setMobile(e.contentRect.width<600));if(box.current)observer.observe(box.current);return()=>observer.disconnect();},[]);
 useEffect(()=>{let id=0,last=performance.now(),time=0;const transport=createTransport(design.flows);setFrame({t:0,particles:transport.snapshot()});const tick=(now:number)=>{const dt=Math.min(.05,(now-last)/1000);last=now;if(playback.current.running&&!playback.current.busy){time=(time+dt)%timeline.durationSeconds;const data=sampleTimeline(timeline,time);setFrame({t:time,particles:transport.step(dt,data)});}id=requestAnimationFrame(tick);};id=requestAnimationFrame(tick);return()=>cancelAnimationFrame(id);},[timeline,design]);
 useEffect(()=>{const mine=++version.current;const timer=setTimeout(async()=>{setBusy(true);setError('');try{const response=await axiosClient.post('/simulation/compute',{...original,parameters});const next=response.data.solverTimeline;validate(next.frames[0].values);if(mine===version.current){setTimeline(next);setFrame({t:0,particles:[]});}}catch(e){if(mine===version.current)setError(e instanceof Error?e.message:'Không tính được');}finally{if(mine===version.current)setBusy(false);}},250);return()=>clearTimeout(timer);},[parameters]);
 let verification='';try{validate(values);verification='Mạch kín · Dòng điện bảo toàn';}catch(e){verification=e instanceof Error?e.message:'Chưa xác minh';}
 return <main><header><span className="brand">PhysLive <span>THỬ THIẾT KẾ</span></span><span className="preview">Bản xem trước riêng</span></header>
 <section className="intro"><span className="eyebrow">MẠCH ĐIỆN MỘT CHIỀU</span><h1>Dòng điện chia như thế nào?</h1><p>R1 nối tiếp với hai nhánh R2 và R3 mắc song song.</p></section>
 <section className="experiment"><div className="stage-header"><div className="legend"><span style={{color:'#7558df'}}>● Mạch chính</span><span style={{color:'#2683dc'}}>● R2</span><span style={{color:'#0a9c8b'}}>● R3</span></div><button onClick={()=>setRunning(!running)}>{running?'Ⅱ Tạm dừng':'▶ Tiếp tục'}</button></div>
 <div ref={box} className="drawing"><svg role="img" aria-label="Mạch kín với dòng điện chuyển động và nhãn đặt cạnh đúng linh kiện" viewBox={`0 0 ${design.width} ${design.height}`}>
 <g dangerouslySetInnerHTML={{__html:design.svg}}/>
 {frame.particles.map(p=>{const flow=design.flows[p.segment];const [x,y]=position(flow,p.distance);return <circle key={p.id} data-particle={p.id} data-segment={p.segment} cx={x} cy={y} r="3.5" fill={flow.color} stroke="white" strokeWidth="1"/>;})}
 <text x={design.rLabel.x} y={design.rLabel.y} className="resistor-name">r = {format(inputValue('p_source','internal_resistance'))} Ω</text>
 {design.labels.map(l=>{const [x,y,w,h]=l.box, [ax,ay]=l.anchor,px=Math.max(x,Math.min(x+w,ax)),py=Math.max(y,Math.min(y+h,ay));const value=l.field?values[l.field]:parameters[l.parameter as keyof typeof parameters];const unit=l.field?original.simulationSpec.solverFieldMeta[l.field as keyof typeof original.simulationSpec.solverFieldMeta].unit:l.unit;return <g key={l.id}><path d={`M${ax} ${ay}L${px} ${py}`} stroke={l.color} opacity=".35" strokeWidth="1.5"/><rect x={x} y={y} width={w} height={h} rx="10" fill="white" stroke={l.color} strokeOpacity=".2"/><rect x={x+12} y={y+15} width="3" height={h-30} rx="1.5" fill={l.color}/><text x={x+24} y={y+24} className="label-title">{l.title}{l.input?` · ${format(inputValue(l.input.participant,l.input.key))} ${l.input.unit}`:''}</text><text x={x+24} y={y+49} className="label-value" fill={l.color}>{format(value)} <tspan className="label-unit">{unit}</tspan></text></g>;})}
 </svg></div>
 <div className="validation"><span className={error?'bad':'good'}>● {busy?'Đang tính lại…':error||verification}</span><span>Số liệu từ backend PhysLive</span></div></section>
 <section className="controls"><label><span>Suất điện động <b>{format(parameters.emf)} V</b></span><input aria-label="Suất điện động E" type="range" min="0" max="24" step=".1" value={parameters.emf} onChange={e=>setParameters(p=>({...p,emf:Number(e.target.value)}))}/></label><label><span>Điện trở R1 <b>{format(parameters.R1)} Ω</b></span><input aria-label="Điện trở R1" type="range" min=".1" max="20" step=".1" value={parameters.R1} onChange={e=>setParameters(p=>({...p,R1:Number(e.target.value)}))}/></label><button onClick={()=>setParameters({emf:12,R1:4})}>Khôi phục</button></section>
 <p className="note">R3 có điện trở nhỏ hơn R2. Quan sát các dòng điện khi thay đổi nguồn hoặc R1.</p>
 </main>;
}
createRoot(document.getElementById('root')!).render(<App/>);
