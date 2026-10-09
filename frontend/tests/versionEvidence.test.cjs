const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const ts = require('typescript');
const source = fs.readFileSync(require('node:path').join(__dirname, '../src/utils/versionEvidence.ts'), 'utf8');
const context = { exports: {} };
vm.runInNewContext(ts.transpileModule(source, {compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText, context);
test('compares actual parent and never interprets omitted risks as resolved', () => {
  const a={id:1,versionNo:1,inputContent:'面向学生。做十种功能。',criticalHypotheses:['付费未验证','冷启动']};
  const b={id:2,versionNo:2,inputContent:'无关分支'};
  const c={id:3,versionNo:3,parentReviewId:1,inputContent:'面向学生。只做报名。',criticalHypotheses:['付费未验证','成本']};
  const r=context.exports.compareVersionEvidence(c,[a,b,c]);
  assert.equal(r.parent.id,1); assert.equal(r.added[0],'只做报名');
  assert.equal(r.omittedRisks[0],'冷启动'); assert.equal(r.newRisks[0],'成本');
});
test('legacy reports and missing parents degrade safely', () => {
  assert.equal(context.exports.compareVersionEvidence({id:2,parentReviewId:1},[{id:1}]).unavailable,true);
  assert.equal(context.exports.compareVersionEvidence({id:1},[]).parent,null);
});
