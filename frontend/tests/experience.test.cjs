const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const ts = require('typescript');
const path = require('node:path');

function load(storage) {
  const source = fs.readFileSync(path.join(__dirname, '../src/utils/experience.ts'), 'utf8');
  const { outputText } = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 },
  });
  const context = {
    exports: {}, localStorage: storage,
    require(name) {
      if (name === '@/utils/session') return { getSessionId: () => 'test-session' };
      if (name === '@/api/reviewApi') return {};
      throw new Error('Unexpected dependency: ' + name);
    },
  };
  vm.runInNewContext(outputText, context);
  return context.exports;
}

test('untrusted and unrelated return destinations fall back to this project', () => {
  const api = load({});
  for (const from of ['https://example.com', '//example.com', '/ideas/other', '/history?redirect=outside', '/reviews/999']) {
    assert.equal(api.reportReturn(from, '94').path, '/projects/94');
  }
  assert.equal(api.reportReturn('/ideas/94', '94').label, '返回版本对比');
  assert.equal(api.reportReturn('/history?page=2', '94').path, '/history?page=2');
});

test('drafts isolate project/source and retain all review settings', () => {
  const values = new Map();
  const api = load({ setItem: (k, v) => values.set(k, v), getItem: k => values.get(k) || null });
  const a = api.draftKey('94', 94);
  const b = api.draftKey('94', 95);
  const c = api.draftKey('97', 97);
  assert.notEqual(a, b);
  assert.notEqual(a, c);
  assert.notEqual(a, api.draftKey());
  api.writeSavedDraft(a, { content: '来源 V1', mode: 'CLIENT', roastLevel: 1 });
  api.writeSavedDraft(b, { content: '来源 V2', mode: 'MENTOR', roastLevel: 3 });
  assert.equal(api.readSavedDraft(a).content, '来源 V1');
  assert.equal(api.readSavedDraft(a).mode, 'CLIENT');
  assert.equal(api.readSavedDraft(a).roastLevel, 1);
  assert.equal(api.readSavedDraft(b).content, '来源 V2');
  assert.equal(api.readSavedDraft(c), null);
});

test('failed persistence and malformed saved drafts are not reported as saved', () => {
  const denied = load({ setItem() { throw new Error('quota'); }, getItem() { return null; } });
  assert.throws(() => denied.writeSavedDraft('key', { content: 'idea', mode: 'CLIENT', roastLevel: 1 }));
  const ignored = load({ setItem() {}, getItem() { return null; } });
  assert.throws(() => ignored.writeSavedDraft('key', { content: 'idea', mode: 'CLIENT', roastLevel: 1 }));
  const corrupt = load({ getItem() { return '{"content":"idea","mode":"unknown"}'; } });
  assert.throws(() => corrupt.readSavedDraft('key'));
});
