// Authored visual output. No computed physics values or animation code.
export type Point = [number,number];
export type Label = {id:string; title:string; field?:string; parameter?:string; unit?:string; input?:{participant:string;key:string;unit:string}; box:[number,number,number,number]; anchor:Point; color:string};
export type Flow = {field:string;color:string;points:Point[]};
const purple='#7558df', blue='#2683dc', teal='#0a9c8b';
function layout(mobile:boolean){
 const p=mobile?{source:[70,340],r:[70,230],r1:[200,100],r2:[365,230],r3:[365,430],left:290,right:440,bottom:560,w:480,h:620}:{source:[90,290],r:[90,165],r1:[260,80],r2:[500,210],r3:[500,350],left:390,right:610,bottom:425,w:720,h:480};
 const [sx,sy]=p.source,[rx,ry]=p.r,[ax,ay]=p.r1,[bx,by]=p.r2,[cx,cy]=p.r3;
 const wire=`M${sx} ${sy-25}V${ry+22} M${rx} ${ry-22}V${ay}H${ax-35} M${ax+35} ${ay}H${p.left}V${by}H${bx-35} M${p.left} ${by}V${cy}H${cx-35} M${bx+35} ${by}H${p.right}V${p.bottom}H${sx}V${sy+25} M${cx+35} ${cy}H${p.right}`;
 const svg=`<defs><linearGradient id="violet" x2="0" y2="1"><stop stop-color="#f3efff"/><stop offset="1" stop-color="#ddd3fc"/></linearGradient><linearGradient id="blue" x2="0" y2="1"><stop stop-color="#edf7ff"/><stop offset="1" stop-color="#c3e2fc"/></linearGradient><linearGradient id="teal" x2="0" y2="1"><stop stop-color="#eafaf5"/><stop offset="1" stop-color="#b9eee0"/></linearGradient></defs><path d="${wire}" fill="none" stroke="#78869a" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/><rect x="${ax-35}" y="${ay-13}" width="70" height="26" rx="6" fill="url(#violet)" stroke="${purple}" stroke-width="2"/><rect x="${bx-35}" y="${by-13}" width="70" height="26" rx="6" fill="url(#blue)" stroke="${blue}" stroke-width="2"/><rect x="${cx-35}" y="${cy-13}" width="70" height="26" rx="6" fill="url(#teal)" stroke="${teal}" stroke-width="2"/><rect x="${rx-12}" y="${ry-22}" width="24" height="44" rx="5" fill="#edf0f5" stroke="#7c8aa0" stroke-width="2"/><circle cx="${sx}" cy="${sy}" r="25" fill="#eef8f3" stroke="#36866a" stroke-width="2"/><path d="M${sx-7} ${sy-9}H${sx+7}M${sx} ${sy-16}V${sy-2}M${sx-7} ${sy+10}H${sx+7}" fill="none" stroke="#36866a" stroke-width="2" stroke-linecap="round"/><circle cx="${p.left}" cy="${by}" r="4" fill="#64748b"/><circle cx="${p.right}" cy="${cy}" r="4" fill="#64748b"/>`;
 const labels:Label[]=mobile?[
  {id:'main',title:'Dòng mạch chính',field:'p_source.current',box:[110,306,175,66],anchor:[sx,sy],color:purple},
  {id:'R1',title:'R1',parameter:'R1',unit:'Ω',box:[143,135,132,58],anchor:[ax,ay],color:purple},
  {id:'R2',title:'R2 · Nhánh trên',field:'p_parallel.current_1',box:[287,147,175,65],anchor:[bx,by],color:blue},
  {id:'R3',title:'R3 · Nhánh dưới',field:'p_parallel.current_2',box:[287,457,175,65],anchor:[cx,cy],color:teal}
 ]:[
  {id:'main',title:'Dòng mạch chính',field:'p_source.current',box:[127,257,175,66],anchor:[sx,sy],color:purple},
  {id:'R1',title:'R1',parameter:'R1',unit:'Ω',box:[194,115,132,58],anchor:[ax,ay],color:purple},
  {id:'R2',title:'R2 · Nhánh trên',field:'p_parallel.current_1',box:[412,115,175,65],anchor:[bx,by],color:blue},
  {id:'R3',title:'R3 · Nhánh dưới',field:'p_parallel.current_2',box:[412,375,175,65],anchor:[cx,cy],color:teal}
 ];
 const flows:Flow[]=[
  {field:'p_source.current',color:purple,points:[[sx,sy-25],[sx,ay],[p.left,ay],[p.left,by]]},
  {field:'p_parallel.current_1',color:blue,points:[[p.left,by],[p.right,by],[p.right,cy]]},
  {field:'p_parallel.current_2',color:teal,points:[[p.left,by],[p.left,cy],[p.right,cy]]},
  {field:'p_source.current',color:purple,points:[[p.right,cy],[p.right,p.bottom],[sx,p.bottom],[sx,sy+25]]}
 ];
 for(const label of labels)if(label.id==='R2'||label.id==='R3'){label.title=label.id;label.input={participant:'p_parallel',key:label.id==='R2'?'resistance_1':'resistance_2',unit:'Ω'};}
 return {width:p.w,height:p.h,svg,labels,flows,rLabel:{x:rx+27,y:ry+5}};
}
export const visualDesign={desktop:layout(false),mobile:layout(true),topology:{nodes:['N','S','A','B'],components:[{id:'source',from:'N',to:'S'},{id:'r',from:'S',to:'A'},{id:'R1',from:'A',to:'B'},{id:'R2',from:'B',to:'N'},{id:'R3',from:'B',to:'N'}],conservation:{incoming:'p_source.current',outgoing:['p_parallel.current_1','p_parallel.current_2']}}};
