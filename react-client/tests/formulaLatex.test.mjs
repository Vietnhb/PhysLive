import test from 'node:test';
import assert from 'node:assert/strict';
import { readdirSync, readFileSync } from 'node:fs';
import katex from 'katex';
import { astLatex, astEquationLatex, equationLatex, symbolLatex } from '../src/shared/lib/formulaLatex.ts';
import { capabilityMath } from '../src/features/reviewer/model/equationAst.ts';

const render = latex => katex.renderToString(latex, { throwOnError: true, trust: false, output: 'htmlAndMathml' });
test('preserves grouping, signs, nested fractions and powers', () => {
  assert.equal(astLatex(['pow', -2, 2]), '\\left(-2\\right)^{2}');
  assert.equal(astLatex(['neg', ['pow', 'x', 2]]), '-x^{2}');
  assert.equal(astLatex(['sub', 'a', ['sub', 'b', 'c']]), 'a - \\left(b - c\\right)');
  assert.equal(astLatex(['div', 'a', ['div', 'b', 'c']]), '\\frac{a}{\\frac{b}{c}}');
  assert.equal(astLatex(['pow', ['pow', 'a', 'b'], 'c']), '\\left(a^{b}\\right)^{c}');
  assert.equal(astLatex(['pow', 'a', ['pow', 'b', 'c']]), 'a^{b^{c}}');
});
test('renders generic symbols and textbook notation without changing storage grammar', () => {
  for (const name of ['R1', 'omega0_value', 'DeltaT', 'theta', '_private', 'arbitrary_variable']) render(symbolLatex(name));
  for (const source of ["x'' = -omega^2*x", "x'^2 = v0^2", 'd2theta/dt2 = -g/L*sin(theta)', 'dq/dt = I', '0 <= x < L', 'Delta t = t2 - t1', 'U = E - I*r', 'f(x, t) = x + t', '|x| = abs(x)']) {
    assert.match(render(equationLatex(source).latex), /<math/);
  }
  assert.match(astEquationLatex('theta', ['neg', 'omega'], true), /\\frac/);
  assert.equal(equationLatex('v = distance / dt').latex, 'v = \\frac{\\mathrm{distance}}{\\mathrm{dt}}');
});
test('preserves prose and rejects ambiguous or unsafe text rather than guessing', () => {
  assert.equal(equationLatex('Điện áp: U = E - I*r').before, 'Điện áp:');
  assert.equal(equationLatex('F = m*a (định luật Newton)').after, '(định luật Newton)');
  for (const source of ['a = b c', 'a = b +', '<img src=x onerror=alert(1)>', '\\htmlClass{evil}{x}']) assert.throws(() => equationLatex(source));
  assert.throws(() => symbolLatex('x}\\href{evil}{y'));
  const cyclic = ['neg']; cyclic.push(cyclic);
  assert.throws(() => astLatex(cyclic));
  assert.throws(() => astLatex(Infinity));
});
test('renders every actual schema AST and preserves the source schemas', () => {
  const root = new URL('../../backend/src/main/resources/schemas/library/capabilities/', import.meta.url);
  const files = readdirSync(root, { recursive: true }).filter(f => f.endsWith('.json'));
  let formulas = 0, equations = 0, plainText = 0;
  for (const file of files) {
    const capability = JSON.parse(readFileSync(new URL(file.replaceAll('\\', '/'), root), 'utf8'));
    const before = JSON.stringify(capability);
    for (const [phase, map] of Object.entries(capabilityMath(capability))) for (const [name, ast] of Object.entries(map)) {
      assert.match(render(astEquationLatex(name, ast, phase === 'rates')), /<math/, `${file}: ${phase}.${name}`);
      formulas++;
    }
    for (const source of [...(capability.equationSet?.canonical ?? []), ...(capability.equationSet?.derived ?? [])]) {
      let display;
      try { display = equationLatex(source); } catch { plainText++; continue; }
      render(display.latex); equations++;
    }
    assert.equal(JSON.stringify(capability), before);
  }
  assert.ok(files.length >= 86 && formulas > 500);
  console.log(`${files.length} schemas: ${formulas} AST formulas, ${equations} textbook equations rendered; ${plainText} prose/unsupported lines preserved as text`);
});
