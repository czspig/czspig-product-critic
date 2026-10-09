const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const ts=require('typescript');
function load(storage, post) {
 const code=ts.transpileModule(fs.readFileSync(require('node:path').join(__dirname,'../src/stores/account.ts'),'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText;
 const s={user:{id:7,username:'test',nickname:'测试',email:'test@example.test'},csrf:'test',identityKey:'account:7',anonymousKey:'anon:owned',remaining:0,anonymousProjects:0};
 const ctx={exports:{},localStorage:storage,Event:class {},window:{dispatchEvent(){}},require(name){
  if(name==='vue')return {reactive:v=>v};
  if(name==='@/utils/session')return {setSessionIdentity(){}};
  if(name==='@/api/http')return {setCsrf(){},unwrap:v=>v,http:{post,get:async()=>({data:s})}};
  throw new Error(name);
 }};
 vm.runInNewContext(code,ctx); ctx.exports.applySession(s); return ctx.exports;
}
function storage() {
 const values={};
 Object.defineProperties(values,{getItem:{value:k=>values[k]??null},setItem:{configurable:true,value:(k,v)=>values[k]=v},removeItem:{value:k=>delete values[k]}});return values;
}
test('post-login destinations reject outside and encoded path redirects',()=>{
 const a=load(storage());
 for(const target of ['https://evil.test','//evil.test','/\\evil.test','/%2f%2fevil.test','/account/reset?token=private','/reviews/7\nLocation:evil'])assert.equal(a.safeReturn(target),'/projects');
 assert.equal(a.safeReturn('/drafts/42?from=/ideas/9'),'/drafts/42?from=/ideas/9');
 assert.equal(a.safeReturn('/?result=42'),'/?result=42');
});
test('logout cleanup removes only this account drafts and preserves anonymous and other owners',()=>{
 const s=storage(); s.setItem('czspig-draft-v2:account:7:9:42','owned');s.setItem('czspig-draft-v2:account:8:9:42','other');s.setItem('czspig-draft-v2:anon:owned:9:42','anon');
 const a=load(s);a.clearAccountDrafts();assert.equal(s.getItem('czspig-draft-v2:account:7:9:42'),null);assert.equal(s.getItem('czspig-draft-v2:account:8:9:42'),'other');assert.equal(s.getItem('czspig-draft-v2:anon:owned:9:42'),'anon');
});
test('idempotent server receipts permit recovery after draft migration storage failure',async()=>{
 const s=storage(); const anon='czspig-draft-v2:anon:owned:9:42', target='czspig-draft-v2:account:7:9:42';
 s.setItem(anon,'idea-and-settings');let denied=true;
 const proxy=new Proxy(s,{get(o,k){if(k==='setItem')return (key,value)=>{if(denied&&key===target)throw new Error('disk full');o.setItem(key,value);};return o[k];}});
 const a=load(proxy,async()=>({data:{projects:['9'],migrated:0}}));
 await assert.rejects(a.migrateProjects());assert.equal(s.getItem(anon),'idea-and-settings');denied=false;
 await a.migrateProjects();assert.equal(s.getItem(target),'idea-and-settings');assert.equal(s.getItem(anon),null);
});
