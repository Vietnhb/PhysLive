import { useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';
import SvgPixiScene from '../../src/features/simulation/components/SvgPixiScene';
import { recomputeSimulation, type GeneratedSimulationResult } from '../../src/features/simulation/api/simulationUnderstandingApi';
import generated from './generated-response.json';
import input from './visual-input.json';
import extraCases from './cases.json';
import '../../src/features/simulation/styles/simulation.css';
import './style.css';

const cases=[{id:'circuit',title:'Mạch điện hai nhánh',generated,input},...extraCases];
function CaseView({selected}:{selected:typeof cases[number]}) {
  const result=selected.generated as unknown as GeneratedSimulationResult;
  const input=selected.input;
  const [initial]=useState(()=>Object.fromEntries(result.parameters.map(p=>[p.name,p.value])));
  const [values,setValues]=useState(initial);
  const [run,setRun]=useState({values:initial,timeline:result.simulationSpec.solverTimeline!,validation:result.validation});
  const [busy,setBusy]=useState(false);
  const [error,setError]=useState('');
  const [rendererError,setRendererError]=useState('');
  const [version,setVersion]=useState(0);
  useEffect(()=>{
    if(values===initial) return;
    const controller=new AbortController();
    const timer=setTimeout(()=>{
      setBusy(true);setError('');
      recomputeSimulation(result,values,controller.signal).then(computed=>{
        if(!controller.signal.aborted) {setRun({values,timeline:computed.solverTimeline,validation:computed.validation});setVersion(v=>v+1);}
      }).catch(cause=>{if(!controller.signal.aborted)setError(cause?.response?.data?.message||String(cause));})
        .finally(()=>{if(!controller.signal.aborted)setBusy(false);});
    },200);
    return()=>{clearTimeout(timer);controller.abort();};
  },[values]);
  function downloadInput() {
    const url=URL.createObjectURL(new Blob([JSON.stringify(input,null,2)],{type:'application/json'}));
    const a=document.createElement('a');a.href=url;a.download='system-visual-input.json';a.click();URL.revokeObjectURL(url);
  }
  return <>
    <section className="demo-intro"><strong>{selected.id==='circuit'?'Đề hệ thống đã xác nhận':'Đề thử nghiệm'}</strong><p>{result.description}</p>
      <div className="facts"><span>{input.solverTimeline.frames.length} mốc solver</span><span>Schema {result.schemaVersion}</span><span>Hình do Codex dựng</span></div></section>
    <section className="controls" aria-label="Thông số thử nghiệm">
      {result.parameters.filter(p=>Number(p.max)>Number(p.min)).map(p=><label key={p.name}><span>{p.label} <b>{values[p.name].toFixed(1)} {p.unit==='ohm'?'Ω':p.unit}</b></span>
        <input aria-label={p.label} type="range" min={p.min} max={p.max} step={p.step} value={values[p.name]} onChange={event=>setValues({...values,[p.name]:Number(event.target.value)})}/></label>)}
      <button onClick={()=>setValues({...initial})}>Mặc định</button>
    </section>
    <p className="status" role="status">{busy?'Backend đang tính lại…':error?error:'Số liệu cập nhật từ solver backend; hình giữ nguyên mã do Codex dựng.'}</p>
    <SvgPixiScene key={version} program={result.simulationSpec.visualProgram!} timeline={run.timeline} parameters={run.values}
      parameterInfo={result.parameters} models={result.simulationSpec.physicsModels}
      fieldMeta={result.simulationSpec.solverFieldMeta as never} verificationStatus={run.validation.status}
      onRenderError={setRendererError}/>
    {rendererError&&<p role="alert">Lỗi renderer: {rendererError}</p>}
    <details><summary>Xem input ban đầu và nguồn dữ liệu</summary><p>{selected.id==='circuit'?'Kế hoạch đã lưu của hệ thống.':'Đề thử nghiệm do Codex soạn; dùng schema hiện được BE phê duyệt.'} Input được lấy tại AIClient.visual của AIService.generate, giữ đủ timeline. BE nhận đầu ra visual nguyên vẹn.</p>
      <p>Input gồm nguyên đề, giải thích, kế hoạch, schema, metadata, timeline và kiểm định.</p>
      <button onClick={downloadInput}>Tải input đầy đủ</button><pre>{JSON.stringify(input.confirmedPlan,null,2)}</pre></details>
    <footer>Chỉ thử nghiệm. Thay tham số gọi API tính lại; không gọi model visual bên ngoài và không lưu đè bài trong thư viện.</footer>
  </>;
}
function App(){
  const [selected,setSelected]=useState(0);
  return <main className="model-demo">
    <header><span className="eyebrow">KHU THỬ NGHIỆM · INPUT TỪ BE</span><h1>Thử 7 bài mô phỏng</h1><p>Codex đóng vai model visual. BE tính vật lý; FE chạy nguyên mã hình. Mỗi bài có bối cảnh riêng.</p></header>
    <nav className="case-picker" aria-label="Chọn bài mô phỏng">{cases.map((c,i)=><button key={c.id} aria-pressed={selected===i} onClick={()=>setSelected(i)}>{c.title}</button>)}</nav>
    <CaseView key={cases[selected].id} selected={cases[selected]}/>
  </main>;
}
const demoRoot=createRoot(document.getElementById('root')!);
demoRoot.render(<App/>);
import.meta.hot?.dispose(()=>demoRoot.unmount());
