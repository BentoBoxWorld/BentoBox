const fs=require('fs'),vm=require('vm'),assert=require('assert');
const elements=new Map();
const element=()=>({dataset:{},children:[],classList:{toggle(){}},appendChild(child){this.children.push(child)},replaceChildren(){this.children=[]},setAttribute(){}});
const document={getElementById(id){if(!elements.has(id))elements.set(id,element());return elements.get(id)},createElement:element,querySelectorAll(){return elements.get('tabs').children}};
const html=fs.readFileSync(process.argv[2],'utf8');
const context=vm.createContext({document,console});
vm.runInContext(html.match(/<script>([\s\S]*?)<\/script>/)[1],context);
for(const mode of vm.runInContext('Object.keys(menus)',context)) {
  vm.runInContext(`render('${mode}')`,context);
  assert.equal(elements.get('gui').children.length,54,mode);
}
for(const mode of ['me','admin']) {
  vm.runInContext(`render('${mode}')`,context);
  assert(elements.get('gui').children[3].innerHTML.includes('回收站'),mode+' recycle entry');
  assert(elements.get('gui').children[45].innerHTML.includes('首页'),mode+' pagination entry');
}
vm.runInContext("render('manage')",context);
for(const [slot,name] of [[32,'活动'],[46,'收入'],[47,'访问'],[48,'黑名单'],[50,'复查'],[51,'删除'],[52,'冻结']]) {
  assert(elements.get('gui').children[slot].innerHTML.includes(name),name+' management slot');
}
vm.runInContext("render('travelPayment')",context);
assert(elements.get('menu-title').textContent.includes('25.00'));
assert(elements.get('gui').children[12].innerHTML.includes('评分'));
assert(html.includes('name="viewport"')&&html.includes('@media(max-width:600px)'));
console.log('PREVIEW_CHECK_PASS: all 54-slot menus, management/recycle/pagination slots, paid teleport preview, responsive rules');
