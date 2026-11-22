const mineflayer=require('mineflayer');const fs=require('fs');const sleep=ms=>new Promise(r=>setTimeout(r,ms));let fail=0;const ok=(c,m)=>{console.log((c?'PASS ':'FAIL ')+m);if(!c)fail++};
const con=c=>fs.appendFileSync('server.in',c+'\n');
const b=mineflayer.createBot({host:process.env.BOT_HOST||'127.0.0.1',port:25565,username:'EcBot',auth:'offline',version:'1.21.11'});
b.once('spawn',async()=>{await sleep(2000);
 const p=b.entity.position.floored();const x=p.x+2,y=p.y,z=p.z;
 con(`setblock ${x} ${y} ${z} minecraft:ender_chest`);con('item replace entity EcBot enderchest.0 with minecraft:diamond 5');con('give EcBot minecraft:emerald 7');await sleep(2000);
 const blk=b.blockAt(new (require('vec3'))(x,y,z));ok(blk&&blk.name=='ender_chest','block placed');
 await b.lookAt(blk.position.offset(0.5,0.5,0.5));b.activateBlock(blk);
 const w=await new Promise(r=>{b.once('windowOpen',r);setTimeout(()=>r(null),5000)});
 ok(w&&w.inventoryStart==54,'big window opened ('+(w?w.inventoryStart:'none')+' slots)');
 if(!w){process.exit(1)}
 ok(w.slots[0]&&w.slots[0].name=='diamond'&&w.slots[0].count==5,'vanilla item in slot 0');
 // move emeralds from player inv into slot 40
 const em=w.slots.findIndex((s,i)=>i>=w.inventoryStart&&s&&s.name=='emerald');
 await b.clickWindow(em,0,0);await b.clickWindow(40,0,0);await sleep(500);b.closeWindow(w);await sleep(1500);
 ok(fs.existsSync('plugins/ChickenSMPEnderchest/data/'+b.player.uuid+'.dat')||fs.readdirSync('plugins/ChickenSMPEnderchest/data').length>0,'extra slots saved to file');
 b.activateBlock(blk);const w2=await new Promise(r=>{b.once('windowOpen',r);setTimeout(()=>r(null),5000)});
 ok(w2&&w2.slots[40]&&w2.slots[40].name=='emerald'&&w2.slots[40].count==7,'extra item still there after reopen');
 ok(w2&&w2.slots[0]&&w2.slots[0].name=='diamond','vanilla item still there');
 b.closeWindow(w2);await sleep(1000);
 
 console.log(fail?'SOME FAILED':'ALL PASSED');process.exit(fail?1:0)});
