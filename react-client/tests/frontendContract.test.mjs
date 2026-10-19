import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import vm from 'node:vm';
import ts from 'typescript';
import { PIXI_BRIDGE } from '../src/features/simulation/engine/pixiRuntime.ts';
import { readVerification } from '../src/features/simulation/model/verification.ts';

const load = (relative, modules, globals = {}) => {
  const source = readFileSync(new URL(relative, import.meta.url), 'utf8');
  const code = ts.transpileModule(source, {compilerOptions: {module: ts.ModuleKind.CommonJS, jsx: ts.JsxEmit.ReactJSX}}).outputText;
  const exports = {};
  vm.runInNewContext(code, {exports, require: id => {
    if (id in modules) return modules[id];
    throw Error('Unexpected dependency: ' + id);
  }, ...globals});
  return exports;
};
const jsx = (type, props) => ({type, props});
function nodes(tree) {
  if (!tree || typeof tree !== 'object') return [];
  if (Array.isArray(tree)) return tree.flatMap(nodes);
  return [tree, ...nodes(tree.props?.children)];
}
const ui = {'react/jsx-runtime': {jsx, jsxs: jsx}, '../model/verification': {readVerification}};

test('focusing and leaving a parameter preserves its full backend precision', () => {
  let draft = null;
  const {SimulationParameterControl} = load('../src/features/simulation/components/SimulationReview.tsx', {
    ...ui, react: {useState: () => [draft, value => {draft = value;}]},
    '../../../shared/ui/MathFormula': {default: () => null},
    '../model/sceneModel': {formatNumber: String, prettyUnit: x => x},
  });
  const value = 1.23456789012345, changes = [];
  const props = {parameter: {name: 'm', label: 'm', unit: 'kg', value, min: 0, max: 3}, value, onChange: v => changes.push(v)};
  const input = () => nodes(SimulationParameterControl(props)).find(n => n.type === 'input' && n.props.type === 'number');
  assert.equal(input().props.value, String(value));
  input().props.onFocus({currentTarget: {select() {}}});
  input().props.onBlur();
  assert.deepEqual(changes, []);
  input().props.onChange({target: {value: '2.3456789012345'}});
  input().props.onBlur();
  assert.deepEqual(changes, [2.3456789012345]);
  const missing = nodes(SimulationParameterControl({...props, parameter: {name: 'm', value}}));
  assert.equal(missing.some(n => n.type === 'input' && n.props.type === 'range'), false);
  assert.equal(missing.find(n => n.type === 'input' && n.props.type === 'number').props.disabled, undefined);
  const oneSided = nodes(SimulationParameterControl({...props, parameter: {name: 'm', value, min: 0}}));
  assert.equal(oneSided.find(n => n.props?.type === 'number').props.disabled, undefined);
});

test('revising a run cancels recomputation and clears its state before new generation', async () => {
  const states = [], effects = [];
  let cursor = 0, pending = [];
  const same = (a,b) => a && b && a.length === b.length && a.every((x,i) => Object.is(x,b[i]));
  const react = {
    useState(initial) {
      const i = cursor++;
      if (!(i in states)) states[i] = initial;
      return [states[i], x => { states[i] = typeof x === 'function' ? x(states[i]) : x; }];
    },
    useRef(initial) { const i = cursor++; if (!(i in states)) states[i] = {current: initial}; return states[i]; },
    useCallback(fn) { cursor++; return fn; },
    useEffect(fn, deps) {
      const i = cursor++;
      if (!same(effects[i]?.deps, deps)) pending.push(() => {
        effects[i]?.cleanup?.(); effects[i] = {deps, cleanup: fn()};
      });
    },
  };
  const simulation = {description:'source', parameters:[{name:'m',value:2,min:0,max:10}],
    presentation:{parameters:{m:2}},
    validation:{status:'VERIFIED_ANALYTICAL'}, simulationSpec:{solverTimeline:{frames:[{t:0,values:{x:0}}]}}};
  const intent = {...simulation,stage:'EXPLAIN'};
  const timers = new Map(); let timerId = 0, revision;
  const hook = load('../src/features/simulation/hooks/useSimulationWorkspace.ts', {
    react, 'react-router-dom':{useSearchParams:()=>[{get:()=>null}]}, axios:{default:{isAxiosError:()=>false}},
    '../api/simulationUnderstandingApi':{
      openGeneratedSimulation:async()=>simulation,
      reviseSimulationIntent:async(_,text)=>{revision = text; return intent;},
      confirmSimulationExplanation:async()=>simulation,
    },
    '../../library/api/libraryApi':{}, '../../../shared/auth/sessionStore':{useSessionStore:()=>null},
    '../../../shared/auth/permissions':{canTeach:()=>false},
  }, {AbortController, window:{setTimeout:fn=>{timers.set(++timerId,fn);return timerId;},clearTimeout:i=>timers.delete(i)}}).useSimulationWorkspace;
  const library = {folders:[],libraryItems:[],setFolders(){},setLibraryItems(){}};
  const render = () => {
    cursor = 0; const model = hook(library); const work = pending; pending = []; work.forEach(fn => fn()); return model;
  };
  let model = render();
  await model.library.onOpen({id:'item',simulationId:'saved'}); model = render();
  model.experiment.updateParameter(simulation.parameters[0], 20); model = render();
  assert.equal(model.experiment.values.m, 20);
  assert.equal(model.preview.recomputing, true);
  assert.equal(timers.size, 1);
  model.explanation.setRevision('  raw revision  '); model = render();
  await model.explanation.handleRevision({preventDefault(){}}); model = render();
  assert.equal(revision, '  raw revision  ');
  assert.equal(timers.size, 0);
  assert.equal(model.preview.recomputing, false);
  assert.equal(model.preview.liveTimeline, null);
  assert.equal(model.library.currentSimulationId, '');
  await model.input.generate(); model = render();
  assert.equal(model.preview.recomputing, false);
  assert.equal(timers.size, 0);
});

test('equal parameters on like bodies remain independent controls', () => {
  const Control = () => null;
  const {default: Inspector} = load('../src/features/simulation/components/SimulationInspector.tsx', {
    ...ui, react: {useState: initial => [initial, () => {}]},
    '../../../shared/ui/LearningIcon': {default: () => null},
    '../../library/components/SaveSimulationPanel': {default: () => null},
    './SimulationReview': {FormulaReview: () => null, SimulationParameterControl: Control},
  });
  const changes = [], parameters = ['m1', 'm2'].map(name => ({name, value: 2, min: 1, max: 10}));
  const simulation = {parameters, simulationSpec: {physicsModels: [
    {id: 'a', capabilityId: 'same', inputs: {mass: 'm1'}}, {id: 'b', capabilityId: 'same', inputs: {mass: 'm2'}},
  ]}};
  const tree = Inspector({experiment: {simulation, values: {m1: 2, m2: 2}, updateParameter: (p,v) => changes.push([p.name,v])},
    explanation: {}, save: {folders: []}, library: {items: []}, onSelectExample: () => {}});
  const group = nodes(tree).find(n => typeof n.type === 'function' && n.type.name === 'ParameterControls');
  const controls = nodes(group.type(group.props)).filter(n => n.type === Control);
  assert.equal(controls.length, 2);
  controls[0].props.onChange(3.123456789);
  assert.deepEqual(changes, [['m1', 3.123456789]]);
});

test('requests carry the complete backend contract and raw revision text', async () => {
  const sent = [], client = {post: async (...args) => {sent.push(args); return {data: {stage: 'EXPLAIN'}};}};
  const api = load('../src/features/simulation/api/simulationUnderstandingApi.ts', {'../../../shared/api/client': {default: client}});
  const spec = {durationSeconds: 1, parameters: [], physicsModels: [], requiredObjects: [{label:'A',count:1}], futureContractField: {exact: 0.123456789}};
  const simulation = {schemaId:'x',schemaVersion:'1',description:'original',planSignature:'signed',simulationSpec:spec};
  const parameters = {m: -3.123456789}, signal = new AbortController().signal;
  await api.recomputeSimulation(simulation, parameters, signal);
  assert.equal(sent[0][1].simulationSpec, spec);
  assert.equal(sent[0][1].parameters, parameters);
  assert.equal(sent[0][2].signal, signal);
  const intent = {stage:'CLARIFY',description:'original',question:'which?'};
  await api.reviseSimulationIntent(intent, '  raw correction  ');
  assert.equal(sent[1][0], '/simulation/revise');
  assert.equal(sent[1][1].intent, intent);
  assert.equal(sent[1][1].text, '  raw correction  ');
  const timeline = {durationSeconds: 2, frames: [{t: 0, values: {'body.x': 4}}]};
  await api.reportSimulationRenderFailure(simulation, 'runtime failure', parameters, timeline);
  assert.equal(sent[2][1].simulation, simulation);
  assert.equal(sent[2][1].runtime.parameters, parameters);
  assert.equal(sent[2][1].runtime.timeline, timeline);
});

test('historical response mapping preserves the backend result and does not invent a stop condition', () => {
  const api = load('../src/features/simulation/api/simulationApi.ts', {'../../../shared/api/client': {default: {}}});
  const rawResult = {simulationSpec:{visualProgram:{code:'original'}}};
  const response = {simulationRunId:'r',success:true,validationPassed:false,rawResult,time:[0,2],adjustableParams:{m:1.23456789}};
  const mapped = api.normalizeSimulation(response);
  assert.equal(mapped.result, rawResult);
  assert.equal(mapped.parameters, response.adjustableParams);
  assert.equal(mapped.ready, true);
  assert.equal(mapped.valid, false);
  assert.equal(mapped.resolvedEnd, undefined);
  assert.equal(mapped.visualization, undefined);
});

test('SVG policy preserves permitted authored content and rejects unsupported nodes without deleting them', () => {
  const code = PIXI_BRIDGE.slice(PIXI_BRIDGE.indexOf('function svgRoot('), PIXI_BRIDGE.indexOf('async function svgBitmap('));
  const ns = 'http://www.w3.org/2000/svg';
  const attributes = values => Object.entries(values).map(([name,value]) => ({name,localName:name.split(':').at(-1),value}));
  let root;
  const context = {SVG_NS:ns, DOMParser: class {parseFromString() {return {documentElement:root, querySelector:()=>null};}}};
  vm.createContext(context); vm.runInContext(code, context);
  const child = {localName:'use',namespaceURI:ns,attributes:attributes({href:'#body',style:'fill:url(#gradient);stroke:blue'})};
  root = {localName:'svg',namespaceURI:ns,attributes:attributes({viewBox:'0 0 100 100'}),
    querySelectorAll:()=>[child],getAttribute:name=>name==='viewBox'?'0 0 100 100':null};
  const before = JSON.stringify(child);
  assert.equal(context.svgRoot('<original/>'), root);
  assert.equal(JSON.stringify(child), before);
  child.localName = 'script';
  assert.throws(() => context.svgRoot('<original/>'), /SVG_CONTRACT.*<script>.*not modified/);
  assert.equal(child.localName, 'script');
  child.localName = 'use'; child.attributes = attributes({href:'https://example.test/asset.svg'});
  assert.throws(() => context.svgRoot('<original/>'), /external reference/);
  assert.equal(child.attributes[0].value, 'https://example.test/asset.svg');
});
