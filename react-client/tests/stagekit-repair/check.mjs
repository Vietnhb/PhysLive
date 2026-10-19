import {createServer} from 'vite';
import {createRequire} from 'node:module';
import fs from 'node:fs';
const {chromium}=createRequire(import.meta.url)('C:/Users/kemin/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const root='../tmp/stagekit-repair';
const entries=JSON.parse(fs.readFileSync(root+'/inventory.json')).filter(x=>x.scene&&!x.codeChars&&(!process.env.CHECK_ONLY||x.id===process.env.CHECK_ONLY));
const server=await createServer({cacheDir:'../tmp/stagekit-vite-cache',optimizeDeps:{entries:['tests/stagekit-repair/index.html']},server:{host:'127.0.0.1',port:0},appType:'mpa'});
let browser;
try {
  await server.listen();
  const port=server.httpServer.address().port;
  browser=await chromium.launch({executablePath:'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',headless:true,args:['--use-gl=angle','--use-angle=swiftshader','--enable-unsafe-swiftshader']});
  const checks=[];
  for(const item of entries) for(const width of [960,360]) {
    const page=await browser.newPage({viewport:{width,height:850}});const errors=[];page.on('pageerror',e=>errors.push(e.message));
    await page.goto('http://127.0.0.1:'+port+'/tests/stagekit-repair/index.html?id='+item.id);
    await page.waitForFunction(()=>document.querySelector('[data-ready="true"]')||document.documentElement.dataset.renderError,{},{timeout:30000}).catch(e=>errors.push(e.message));
    const check={id:item.id,width,ready:await page.locator('[data-ready="true"]').count()>0,error:await page.evaluate(()=>document.documentElement.dataset.renderError||''),pageErrors:errors};
    // Seek forward then backward through the real component's controls.
    if(check.ready) {
      const slider=page.locator('input[type="range"]').first();
      if(await slider.count()) for(const proportion of [1,.5,0]) {await slider.evaluate((el,p)=>{const set=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set;set.call(el,String(Number(el.max)*p));el.dispatchEvent(new Event('input',{bubbles:true}));el.dispatchEvent(new Event('change',{bubbles:true}));},proportion);await page.waitForTimeout(120);}
      check.error=await page.evaluate(()=>document.documentElement.dataset.renderError||'');
    }
    await page.screenshot({path:root+'/'+item.id+'-'+width+'.png'});
    checks.push(check);console.log(JSON.stringify(check));await page.close();
  }
  const previous=JSON.parse(fs.readFileSync(root+'/browser-checks.json'));
  const combined=[...previous.filter(c=>!checks.some(n=>n.id===c.id&&n.width===c.width)),...checks];
  fs.writeFileSync(root+'/browser-checks.json',JSON.stringify(combined,null,2));
  if(checks.some(c=>!c.ready||c.error||c.pageErrors.length))process.exitCode=1;
} finally {await browser?.close();await server.close();}
