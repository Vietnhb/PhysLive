import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync, readdirSync } from 'node:fs';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { parseFormula, formatFormula, FormulaError, capabilityMath, withPhase } from '../src/features/reviewer/model/equationAst.ts';

function evaluate(ast, env) {
  if (typeof ast === 'number') return ast;
  if (typeof ast === 'string') return env[ast];
  const [op, a, b] = ast; const x = evaluate(a, env); const y = b === undefined ? 0 : evaluate(b, env);
  return { add: x + y, sub: x - y, mul: x * y, div: x / y, pow: Math.pow(x, y), neg: -x, sin: Math.sin(x), cos: Math.cos(x), sqrt: Math.sqrt(x), exp: Math.exp(x), abs: Math.abs(x), log: Math.log(x), asin: Math.asin(x), acos: Math.acos(x), atan: Math.atan(x) }[op];
}

test('parses precedence, associativity and unary minus', () => {
  assert.deepEqual(parseFormula('a + b * c'), ['add', 'a', ['mul', 'b', 'c']]);
  assert.deepEqual(parseFormula('a - b - c'), ['sub', ['sub', 'a', 'b'], 'c']);
  assert.deepEqual(parseFormula('a / b / c'), ['div', ['div', 'a', 'b'], 'c']);
  assert.deepEqual(parseFormula('-x^2'), ['neg', ['pow', 'x', 2]]);
  assert.deepEqual(parseFormula('a^b^c'), ['pow', 'a', ['pow', 'b', 'c']]);
  assert.deepEqual(parseFormula('2 * -3'), ['mul', 2, -3]);
  assert.deepEqual(parseFormula('1e-9 + .5'), ['add', 1e-9, 0.5]);
  assert.deepEqual(parseFormula('E + (U0 - E) * exp(-t / (R * C))'), ['add', 'E', ['mul', ['sub', 'U0', 'E'], ['exp', ['div', ['neg', 't'], ['mul', 'R', 'C']]]]]);
});

test('formats with only the brackets that are needed', () => {
  assert.equal(formatFormula(['div', ['sub', 'E', ['div', 'q', 'C']], 'R']), '(E - q / C) / R');
  assert.equal(formatFormula(['sub', 'a', ['sub', 'b', 'c']]), 'a - (b - c)');
  assert.equal(formatFormula(['pow', ['neg', 'x'], 2]), '(-x)^2');
  assert.equal(formatFormula(['pow', -2, 2]), '(-2)^2');
  assert.equal(formatFormula(['neg', ['add', 'a', 'b']]), '-(a + b)');
  assert.equal(formatFormula(['mul', 'a', ['neg', 'b']]), 'a * -b');
});

test('rejects bad input with a readable message', () => {
  for (const bad of ['', 'a +', '(a', 'a b', 'sinh(x)', 'a $ b', 'sin x', '2 *', 'max(a)', 'if(a, b)', 'sin(a, b)']) assert.throws(() => parseFormula(bad), FormulaError, bad);
});

test('functions of several values parse, print and survive a round trip', () => {
  assert.deepEqual(parseFormula('max(0, m * g - F * sin(alpha))'), ['max', 0, ['sub', ['mul', 'm', 'g'], ['mul', 'F', ['sin', 'alpha']]]]);
  assert.deepEqual(parseFormula('if(x - L, L, x)'), ['if', ['sub', 'x', 'L'], 'L', 'x']);
  for (const ast of [['atan2', 'fy', 'fx'], ['mod', ['mul', 'v', 't'], 'L'], ['min', ['floor', 'a'], ['sign', ['tan', 'b']]], ['if', ['sub', 'a', 'b'], ['neg', 'a'], ['max', 'a', 'b']]])
    assert.deepEqual(parseFormula(formatFormula(ast)), ast);
  assert.throws(() => formatFormula(['if', 'a', 'b']), FormulaError);
});

const LIBRARY = process.env.CAPABILITY_LIBRARY || fileURLToPath(new URL('../../backend/src/main/resources/schemas/library/capabilities/', import.meta.url));
test('every shipped capability formula round-trips', () => {
  const files = readdirSync(LIBRARY, { recursive: true }).filter(name => String(name).endsWith('.json'));
  assert.ok(files.length > 0);
  let formulas = 0;
  for (const file of files) {
    const capability = JSON.parse(readFileSync(join(LIBRARY, String(file)), 'utf8'));
    for (const [phase, map] of Object.entries(capabilityMath(capability))) for (const [name, ast] of Object.entries(map)) {
      const reparsed = parseFormula(formatFormula(ast));
      const env = new Proxy({}, { get: (_, key) => 0.37 + String(key).length * 0.11 });
      const before = evaluate(ast, env), after = evaluate(reparsed, env);
      assert.ok(Object.is(before, after) || Math.abs(before - after) <= 1e-12 * Math.abs(before), `${file} ${phase}.${name}`);
      formulas++;
    }
  }
  console.log(`checked ${files.length} capabilities, ${formulas} formulas`);
});
