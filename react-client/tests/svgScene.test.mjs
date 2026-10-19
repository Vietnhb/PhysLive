import test from 'node:test';
import assert from 'node:assert/strict';
import {sampleTimeline} from '../src/features/simulation/model/svgScene.ts';
test('existing non-teacher playback retains interpolation',()=>{
 const timeline={durationSeconds:2,frames:[{t:0,values:{'first.x':0,'second.x':10}},{t:2,values:{'first.x':4,'second.x':20}}]};
 assert.deepEqual(sampleTimeline(timeline,1),{t:1,'first.x':2,'second.x':15});
 assert.deepEqual(sampleTimeline(timeline,20),{t:2,'first.x':4,'second.x':20});
});
