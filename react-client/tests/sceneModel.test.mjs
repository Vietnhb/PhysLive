import test from 'node:test';
import assert from 'node:assert/strict';
import { describeScene, niceStep, formatNumber } from '../src/features/simulation/model/sceneModel.ts';
import { numberLabel } from '../src/features/simulation/model/learningModel.ts';
const run = f => ({durationSeconds:2,frames:Array.from({length:21},(_,i)=>({t:i/10,values:f(i/10)}))});
test('groups numeric output by calculation unit without deciding geometry or apparatus', () => {
  const timeline = {...run(t => ({'a.position':t,'a.force':-20*t,'b.x':Math.cos(t),'b.y':Math.sin(t),'b.angle':t})),
    fieldRanges: {'a.position':{min:0,max:2},'a.force':{min:-40,max:0}}};
  const metadata = Object.fromEntries(Object.keys(timeline.frames[0].values).map(key => [key, {participantId:key[0]}]));
  const scene = describeScene(timeline, [{id:'a',label:'Tính toán A'},{id:'b'}], metadata);
  assert.deepEqual(scene.participants, [{id:'a',label:'Tính toán A',colorIndex:0},{id:'b',label:'b',colorIndex:1}]);
  for (const item of scene.participants) for (const key of ['spring','link','dims','vertical','stationary']) assert.equal(key in item, false);
  assert.equal(scene.fields['a.position'].max,2);
  assert.equal(scene.fields['a.force'].min,-40);
});
test('names, symbols and units are the backend catalog\'s; the client keeps no vocabulary of its own', () => {
  const metadata = {'a.angle':{label:'Góc đo của cảm biến',symbol:'phi',unit:'rad'},'a.novel_field':{label:'Đại lượng mới',symbol:'z',unit:'J'}};
  const scene = describeScene(run(t=>({'a.angle':t,'a.novel_field':t*2})),[],metadata);
  assert.equal(scene.fields['a.angle'].label,'Góc đo của cảm biến');
  assert.equal(scene.fields['a.angle'].symbol,'phi');
  assert.equal(scene.fields['a.angle'].unit,'rad');
  assert.equal(scene.fields['a.novel_field'].symbol,'z');
  const bare = describeScene(run(t=>({'a.kinetic_energy':t})),[]);
  assert.deepEqual([bare.fields['a.kinetic_energy'].label,bare.fields['a.kinetic_energy'].symbol,bare.fields['a.kinetic_energy'].unit],['Kinetic energy','Kinetic energy','']);
  assert.equal('kind' in bare.fields['a.kinetic_energy'],false);
  assert.equal(scene.fields['a.novel_field'].unit,'J');
});
test('the signed plan selects readings and metadata extraction does not mutate input', () => {
  const timeline = run(t=>({'a.current':t,'a.voltage':2*t}));
  const before=JSON.stringify(timeline);
  const scene = describeScene(timeline,[],{},[{field:'a.voltage',label:'Điện áp trên tụ',object:'Tụ điện'}]);
  assert.deepEqual(scene.observables,['a.voltage']);
  assert.equal(scene.fields['a.voltage'].caption,'Điện áp trên tụ');
  assert.equal(JSON.stringify(timeline),before);
});
test('unknown fields stay available and tiny solver values do not disappear', () => {
  const scene=describeScene(run(t=>({'a.custom':t,'a.elapsed_time':t})),[],{'a.elapsed_time':{unit:'s'}});
  assert.equal(scene.fields['a.custom'].label,'Custom');
  assert.equal(scene.fields['a.elapsed_time'].max,2);
  assert.equal(formatNumber(5.04e-29,4),'5.04×10⁻²⁹');
  assert.equal(niceStep(300,6),50);
});

