import { test } from 'node:test';
import assert from 'node:assert/strict';
import { timelineCsv, scriptJson } from '../src/features/simulation/engine/exportData.ts';

test('CSV preserves samples and quotes arbitrary solver keys', () => {
  const csv = timelineCsv({ durationSeconds: 1, frames: [{ t: 0, values: { 'x,"y': 2 } }, { t: 1, values: { z: 3 } }] });
  assert.equal(csv, 'time_seconds,"x,""y","z"\r\n"0","2",""\r\n"1","","3"');
});
test('exported JSON cannot terminate an HTML script', () => {
  const text = '</script><img src=x onerror=alert(1)>';
  const encoded = scriptJson({ text });
  assert.equal(encoded.includes('<'), false);
  assert.deepEqual(JSON.parse(encoded), { text });
});
