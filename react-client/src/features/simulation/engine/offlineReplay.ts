import pixiBundle from "../../../../node_modules/pixi.js/dist/webworker.min.js?raw";
import { PIXI_BRIDGE, PIXI_WORKER } from "./pixiRuntime";
import { sampleTimeline, type SolverTimeline } from "../model/svgScene";
import { scriptJson } from "./exportData";

/** Replays captured samples and the original scene; no server or new physics calculation. */
export function offlineReplay(snapshot: Record<string, unknown>, timeline: SolverTimeline, parameters: Record<string, number>): string {
  const spec = snapshot.simulationSpec as Record<string, unknown>;
  const program = spec.visualProgram as { code?: string; playbackRate?: number } | undefined;
  if (!program?.code || !timeline.frames.length) throw new Error("Chưa có cảnh và dữ liệu để xuất.");
  const nonce = crypto.randomUUID();
  const child = `<!doctype html><meta http-equiv="Content-Security-Policy" content="default-src 'none'; script-src 'nonce-${nonce}' 'unsafe-eval'; worker-src blob:; img-src blob: data:; style-src 'unsafe-inline'; connect-src 'none'"><style>html,body{margin:0;width:100%;height:100%;overflow:hidden}canvas{width:100%;height:100%;display:block}</style><canvas></canvas><script nonce="${nonce}">${PIXI_BRIDGE.replace(/<\/script/gi, "<\\/script")}</script>`;
  const start = {
    channel: "pixi-host", type: "start", code: program.code, timeline, parameters,
    fieldMeta: spec.solverFieldMeta, models: spec.physicsModels, observables: spec.observables,
    parameterInfo: spec.parameters, presentation: snapshot.presentation,
    verificationStatus: (snapshot.validation as { status?: string })?.status,
    theme: "light", bundle: pixiBundle,
    runtime: `const sampleTimeline = (${sampleTimeline.toString()});\n${PIXI_WORKER}`,
    limits: { maxCode: 250000, maxTextureSide: 4096, maxTexturePixels: 33554432, timeoutMs: 15000 },
  };
  return `<!doctype html><html lang="vi"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta http-equiv="Content-Security-Policy" content="default-src 'none'; script-src 'nonce-${nonce}'; frame-src 'self' blob: data:; style-src 'unsafe-inline'; connect-src 'none'"><title>PhysLive — Phát lại</title><style>body{font:16px system-ui;margin:16px;color:#172033;background:#f4f6fa}iframe{width:100%;height:65vh;border:1px solid #bbc4d2;background:white}button,input{margin:8px}pre{white-space:pre-wrap}details{margin-top:16px}@media print{iframe{height:400px}button,input{display:none}}</style><h1>PhysLive — Phát lại ngoại tuyến</h1><p id="description"></p><p id="verification"></p><iframe title="Mô phỏng" sandbox="allow-scripts"></iframe><div><button id="play">Tạm dừng / Phát</button><button id="reset">Về đầu</button><input id="seek" aria-label="Thời gian" type="range" min="0" max="${timeline.durationSeconds}" step="any" value="0"><output id="time">0 s</output><button id="print">In / Lưu PDF</button></div><pre id="values"></pre><p id="error" role="alert"></p><details><summary>Tham số đã lưu</summary><pre id="params"></pre></details><script nonce="${nonce}">
const captured=${scriptJson(start)},snapshot=${scriptJson(snapshot)},frame=document.querySelector('iframe');
const send=(type,extra={})=>frame.contentWindow.postMessage({channel:'pixi-host',type,...extra},'*');let playing=true;
document.getElementById('description').textContent=String(snapshot.description||'');
document.getElementById('verification').textContent='Trạng thái vật lý: '+String(snapshot.validation?.status||'Chưa xác minh')+' · Phát lại mẫu đã lưu; không tính lại khi ngoại tuyến.';
document.getElementById('params').textContent=JSON.stringify(captured.parameters,null,2);
frame.onload=()=>{frame.contentWindow.postMessage(captured,'*');send('speed',{speed:${program.playbackRate ?? 1}});send('play',{playing:true});};frame.srcdoc=${scriptJson(child)};
addEventListener('message',event=>{if(event.source!==frame.contentWindow||event.data?.channel!=='pixi-runtime')return;const data=event.data;if(data.type==='error')document.getElementById('error').textContent=data.message;if(Number.isFinite(data.t)){document.getElementById('seek').value=data.t;document.getElementById('time').textContent=data.t+' s';}if(data.sample)document.getElementById('values').textContent=JSON.stringify(data.sample.fields,null,2);});
document.getElementById('play').onclick=()=>{playing=!playing;send('play',{playing});};document.getElementById('reset').onclick=()=>send('seek',{t:0});document.getElementById('seek').oninput=event=>send('seek',{t:Number(event.target.value)});document.getElementById('print').onclick=()=>print();
</script></html>`;
}
