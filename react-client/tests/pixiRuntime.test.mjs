import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import vm from 'node:vm';
import { sampleTimeline } from '../src/features/simulation/model/svgScene.ts';
import { PIXI_WORKER, PIXI_BRIDGE } from '../src/features/simulation/engine/pixiRuntime.ts';

const source = readFileSync(new URL('../src/features/simulation/components/SvgPixiScene.tsx', import.meta.url), 'utf8');
const timeline = {durationSeconds: 1, frames: [
  {t: 0, values: {'arbitrary.position': 0}}, {t: 1, values: {'arbitrary.position': 10}},
]};
function runtime() {
  const messages = []; let tick;
  const app = {screen: {width: 600, height: 400}, stage: {children: [], addChild(item) {this.children.push(item);}},
    init: async () => {}, render() {}, renderer: {resize(width, height) {app.screen = {width, height};}}};
  const context = vm.createContext({Proxy, PIXI: {Application: function() {return app;}, Texture: {from: bitmap => bitmap}},
    performance: {now: () => 0}, postMessage: message => messages.push(message), close() {},
    setInterval: callback => {tick = callback;}});
  vm.runInContext(sampleTimeline.toString() + '\n' + PIXI_WORKER, context);
  return {app, messages, context, send: data => context.onmessage({data}), tick: () => tick()};
}
test('runs generated PixiJS functions without a predefined object or scene inventory', async () => {
  const host = runtime();
  await host.send({type: 'start', timeline, parameters: {}, code: `async function(PIXI, app, api) { 
    const visuals = []; for (let i = 0; i < 7; i++) {const item = {i}; visuals.push(item); app.stage.addChild(item);}
    return {update(frame) { for (const item of visuals) item.x = frame.fields['arbitrary.position'] + item.i; }};
  }`});
  host.tick(); host.tick();
  await host.send({type: 'seek', t: 0.5}); host.tick();
  assert.equal(host.app.stage.children.length, 7);
  assert.deepEqual(host.app.stage.children.map(item => item.x), [5,6,7,8,9,10,11]);
  assert.equal(host.messages.some(item => item.type === 'ready'), true);
});

test('the author controls colours, labels and transforms without runtime overrides', async () => {
  const host = runtime();
  await host.send({type: 'start', timeline, parameters: {}, code: `async function(PIXI, app, api) {
    const item = {colour: '#ff00ff', label: 'Theo thiết kế riêng'}; app.stage.addChild(item);
    return {update(frame) {item.x = 123 + frame.fields['arbitrary.position'] * 7; item.rotation = 0.37;}};
  }`});
  await host.send({type: 'seek', t: 0.5}); host.tick();
  assert.equal(host.app.stage.children[0].x, 158);
  assert.equal(host.app.stage.children[0].rotation, 0.37);
  assert.equal(host.app.stage.children[0].colour, '#ff00ff');
  assert.equal(host.app.stage.children[0].label, 'Theo thiết kế riêng');
  await host.send({type: 'seek', t: 1}); host.tick();
  assert.equal(host.app.stage.children[0].x, 193);
});
test('parameter edits replace backend fields and ranges without regenerating visual code', async () => {
  const host = runtime();
  await host.send({type: 'start', timeline, parameters: {}, code: `async function(PIXI, app, api) { 
    const item = {}; app.stage.addChild(item);
    return {update(frame) {item.x = frame.fields['arbitrary.position']; item.range = api.getFieldRanges()['arbitrary.position'].max;},
      resize() {item.layoutRange = api.getFieldRanges()['arbitrary.position'].max;}};
  }`});
  const revised = {durationSeconds: 1, frames: [{t:0,values:{'arbitrary.position':20}}, {t:1,values:{'arbitrary.position':40}}]};
  await host.send({type:'data', timeline:revised, parameters:{any:3}}); host.tick();
  assert.equal(host.app.stage.children[0].x, 20);
  assert.equal(host.app.stage.children[0].range, 40);
  assert.equal(host.app.stage.children[0].layoutRange, 40);
});

test('reports invalid generated lifecycle as a rendering error, not physics verification', async () => {
  const host = runtime();
  await host.send({type:'start',timeline,parameters:{},code:'async function() { return {}; }'});
  assert.equal(host.messages.at(-1).type, 'error');
  assert.match(host.messages.at(-1).message, /update/);
  assert.equal(host.messages.some(item => item.type === 'VERIFIED'), false);
});
test('runtime isolates code with an opaque iframe, blocked network and worker watchdog', () => {
  assert.match(source, /sandbox="allow-scripts"/);
  assert.doesNotMatch(source, /allow-same-origin/);
  assert.match(source, /connect-src/);
  assert.match(PIXI_BRIDGE, /worker\?\.terminate\(\)/);
  assert.match(PIXI_BRIDGE, /lastHeartbeat > limits.timeoutMs/);
});
test('unknown solver field keys fail loudly instead of silently drawing nothing', async () => {
  const host = runtime();
  await host.send({type: 'start', timeline, parameters: {}, code: `async function(PIXI, app, api) { 
    return {update(frame) { app.stage.x = frame.fields['motion1.position']; }};
  }`});
  assert.equal(host.messages.at(-1).type, 'error');
  assert.match(host.messages.at(-1).message, /Unknown solver field "motion1.position".*arbitrary.position/);
});
test('playback stops at the end of the backend timeline and restarts on play', async () => {
  const host = runtime();
  await host.send({type: 'start', timeline, parameters: {}, code: `async function(PIXI, app, api) { 
    return {update(frame) { app.stage.t = frame.t; }};
  }`});
  await host.send({type: 'seek', t: 0.5}); host.tick();
  assert.equal(host.messages.some(item => item.type === 'ended'), false);
  await host.send({type: 'seek', t: 1}); host.tick();
  assert.equal(host.messages.some(item => item.type === 'ended'), true);
  await host.send({type: 'play', playing: true}); host.tick();
  assert.equal(host.app.stage.t, 0);
});
test('no inferred scene, predefined inventory or automatic fallback is exposed', async () => {
  const host = runtime();
  await host.send({type: 'start', timeline, parameters: {}, code: `async function(PIXI, app, api) {
    const item = {hasKit: 'kit' in api, hasScene: 'scene' in api, privateData: typeof data}; app.stage.addChild(item);
    return {update(frame) {item.x = frame.fields['arbitrary.position'];}};
  }`});
  host.tick(); host.tick();
  assert.equal(host.messages.some(item => item.type === 'ready'), true);
  assert.equal(host.app.stage.children[0].hasKit, false);
  assert.equal(host.app.stage.children[0].hasScene, false);
  assert.equal(host.app.stage.children[0].privateData, 'undefined');
  assert.doesNotMatch(source, /STANDARD_SCENE_CODE|ILLUSTRATED_SCENE_CODE|setMode/);
});
test('generated code cannot modify backend fields, parameters or timeline samples', async () => {
  for (const expression of ["frame.fields['arbitrary.position'] = 99", "frame.parameters.any = 99", "api.timeline.frames[0].values['arbitrary.position'] = 99"]) {
    const host = runtime();
    await host.send({type:'start', timeline, parameters:{any:3}, code:`async function(PIXI,app,api) { return {update(frame) { ${expression}; }}; }`});
    assert.equal(host.messages.at(-1).type, 'error');
    assert.match(host.messages.at(-1).message, /read only|not extensible/);
  }
});
test('backend metadata and values are passed unchanged to the author', async () => {
  const host = runtime();
  const fieldMeta = {'arbitrary.position':{label:'Trường tùy chọn',unit:'rad',rendererRole:'state_value'}};
  await host.send({type:'start',timeline,parameters:{any:3},fieldMeta,code:`async function(PIXI,app,api) {
    const item = {}; app.stage.addChild(item);
    return {update(frame) {item.meta = api.fieldMeta['arbitrary.position']; item.value = frame.fields['arbitrary.position']; item.parameter = frame.parameters.any; item.t = frame.t; item.sample = api.sample(.5)['arbitrary.position'];}};
  }`});
  await host.send({type:'seek',t:.5}); host.tick();
  assert.equal(host.app.stage.children[0].meta.unit, 'rad');
  assert.equal(host.app.stage.children[0].meta.label, 'Trường tùy chọn');
  assert.equal(host.app.stage.children[0].value, 5);
  assert.equal(host.app.stage.children[0].t, .5);
  assert.equal(host.app.stage.children[0].sample, 5);
  assert.equal(host.app.stage.children[0].parameter, 3);
});
test('seeking backwards displays backend state without accumulated simulation', async () => {
  const host = runtime();
  await host.send({type:'start',timeline,parameters:{},code:`async function(PIXI,app,api) {
    const item = {}; app.stage.addChild(item); return {update(frame) {item.x = frame.fields['arbitrary.position'];}};
  }`});
  await host.send({type:'seek',t:1}); host.tick(); assert.equal(host.app.stage.children[0].x, 10);
  await host.send({type:'seek',t:.9}); host.tick(); assert.equal(host.app.stage.children[0].x, 9);
  await host.send({type:'seek',t:.2}); host.tick(); assert.equal(host.app.stage.children[0].x, 2);
});
test('author controls resize and theme changes; no automatic layout is added', async () => {
  const host = runtime();
  await host.send({type:'start',timeline,parameters:{},theme:'LIGHT',code:`async function(PIXI,app,api) {
    const item = {}; app.stage.addChild(item); return {
      update(frame) {item.theme = frame.theme;}, resize(w,h) {item.x = w * .17; item.y = h * .23;}
    };
  }`});
  await host.send({type:'resize',width:300,height:200}); host.tick();
  assert.ok(Math.abs(host.app.stage.children[0].x - 51) < 1e-12);
  assert.equal(host.app.stage.children[0].y, 46);
  await host.send({type:'theme',theme:'DARK'});
  assert.equal(host.app.stage.children[0].theme, 'DARK');
});
test('SVG bridge permits authored text and retains isolation and limits', () => {
  assert.doesNotMatch(PIXI_BRIDGE, /'text','tspan','textPath'/);
  assert.match(PIXI_BRIDGE, /unsupported element/);
  assert.match(PIXI_BRIDGE, /external SVG resources forbidden/);
  assert.doesNotMatch(PIXI_BRIDGE, /sanitize|FORBID_ATTR/);
  assert.match(PIXI_BRIDGE, /maxTexturePixels/);
  assert.doesNotMatch(PIXI_BRIDGE, /luma|inkShare|paper/);
});

const teacherResolver = readFileSync(new URL('../../backend/src/main/resources/prompts/teacher-frame-resolver.js', import.meta.url), 'utf8');
test('teacher renderer executes backend sampling contract without local interpolation or extrema', async () => {
 const host=runtime();
 const contract={version:1,frameResolver:teacherResolver,fieldRanges:{opaque:{min:-700,max:4000}}};
 const run={durationSeconds:2,frames:[{t:0,dt:0,values:{opaque:5.04e-29,extra:123.123456789}},{t:1,dt:1,values:{opaque:10,extra:0}},{t:1,dt:0,values:{opaque:-5,extra:99}},{t:2,dt:1,values:{opaque:-3,extra:98}}]};
 const before=JSON.stringify(run);
 await host.send({type:'start',timeline:run,presentation:contract,parameters:{m:3.123456789},code:'async function(PIXI,app,api) { return {update(frame) { app.stage.packet=frame; app.stage.sample=api.sample(.5); app.stage.ranges=api.getFieldRanges(); }}; }'});
 await host.send({type:'play',playing:false});
 for(const [cursor,t,value,dt] of [[.5,0,5.04e-29,0],[1,1,-5,0],[1.5,1,-5,0],[2,2,-3,1],[.2,0,5.04e-29,0]]) {
  await host.send({type:'seek',t:cursor}); host.tick();
  const packet=host.app.stage.packet;
  assert.equal(packet.t,t); assert.equal(packet.dt,dt); assert.equal(packet.fields.opaque,value);
  assert.equal(packet.parameters.m,3.123456789);
  assert.equal(host.messages.at(-1).sample.fields.opaque,value);
 }
 assert.equal(host.app.stage.sample.opaque,5.04e-29);
 assert.equal(host.app.stage.sample.extra,123.123456789);
 assert.equal(host.app.stage.ranges.opaque.max,4000);
 assert.equal(JSON.stringify(run),before);
});
test('iframe bridge preserves the entire backend sample packet',async()=>{
 const sent=[],worker={};
 const context=vm.createContext({worker,stopped:false,lastHeartbeat:0,Date,send:(type,packet)=>sent.push(packet)});
 vm.runInContext(PIXI_BRIDGE.slice(PIXI_BRIDGE.indexOf('worker.onmessage ='),PIXI_BRIDGE.indexOf('worker.onerror =')),context);
 const packet={type:'tick',t:.5,sample:{t:0,dt:0,fields:{opaque:5.04e-29,extra:123.123456789}},futureField:{value:'preserve'}};
 await worker.onmessage({data:packet});
 assert.equal(sent[0],packet);
 assert.equal(sent[0].sample.fields.opaque,5.04e-29);
 assert.equal(sent[0].futureField.value,'preserve');
});
test('teacher contract version failures are explicit and never switch to legacy sampling',async()=>{
 const host=runtime();
 await host.send({type:'start',timeline,presentation:{version:999,frameResolver:teacherResolver},parameters:{},code:'async function(){return {update(){}}}'});
 assert.equal(host.messages.at(-1).type,'error');
 assert.match(host.messages.at(-1).message,/Unsupported teacher presentation contract/);
});
