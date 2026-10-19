import type {Flow,Point} from './design';
export type Particle={id:number;segment:number;distance:number};
export const length=(flow:Flow)=>flow.points.slice(1).reduce((sum,p,i)=>sum+Math.hypot(p[0]-flow.points[i][0],p[1]-flow.points[i][1]),0);
export function position(flow:Flow,distance:number):Point{
 for(let i=1;i<flow.points.length;i++){
  const a=flow.points[i-1],b=flow.points[i],n=Math.hypot(b[0]-a[0],b[1]-a[1]);
  if(distance<=n)return [a[0]+(b[0]-a[0])*distance/n,a[1]+(b[1]-a[1])*distance/n];
  distance-=n;
 }
 return flow.points[flow.points.length-1];
}
export function createTransport(flows:Flow[]){
 let nextId=0,emission=0,branchBalance=0;
 let particles:Particle[]=flows.flatMap((flow,segment)=>Array.from({length:Math.floor(length(flow)/36)},(_,i)=>({id:nextId++,segment,distance:(i+.5)*36})));
 return {
  snapshot:()=>particles.map(p=>({...p})),
  step(dt:number,values:Record<string,number>){
   const main=Math.max(0,values[flows[0].field]||0);
   emission+=main*65*dt/36;
   while(emission>=1){particles.push({id:nextId++,segment:0,distance:0});emission--;}
   particles=particles.filter(p=>{
    let remaining=dt;
    while(remaining>0){
     const flow=flows[p.segment],speed=Math.max(0,values[flow.field]||0)*65;
     if(!speed)break;
     const toEnd=(length(flow)-p.distance)/speed;
     if(remaining<toEnd){p.distance+=remaining*speed;break;}
     remaining-=toEnd;p.distance=0;
     if(p.segment===0){
      branchBalance+=main?Math.max(0,values[flows[1].field]||0)/main:0;
      p.segment=branchBalance>=1?1:2;
      if(p.segment===1)branchBalance--;
     }else if(p.segment===1||p.segment===2)p.segment=3;
     else return false; // Particles leave the external circuit only at the source terminal.
    }
    return true;
   });
   return particles.map(p=>({...p}));
  }
 };
}
