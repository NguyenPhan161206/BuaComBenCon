from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
import subprocess
import os
root=Path(__file__).resolve().parents[2]
studio=Path(os.environ.get('STUDIO_DIR','/home/binhnguyen/android-studio'))
sdk=Path(os.environ.get('ANDROID_HOME',os.environ.get('ANDROID_SDK_ROOT','/home/binhnguyen/Android/Sdk')))
cp=f'app/build/sdk-debug/checks:app/build/sdk-debug/classes:{studio}/plugins/design-tools/lib/layoutlib.jar:{studio}/lib/*:{studio}/plugins/android/lib/*'
cases=[('01-home',0,0,'tab','c1'),('02-detail',0,0,'detail','c2'),('03-form',0,0,'form','c2'),('04-registrations',0,1,'tab','c1'),('05-ticket',0,1,'ticket','c1'),('06-coordinator',1,0,'tab','c1'),('07-manage',1,1,'manage','c2'),('08-create',1,1,'create','c1'),('09-assisted',1,1,'assisted','c2'),('10-volunteer',2,0,'tab','c1'),('11-handed-over',2,1,'tab','c1'),('12-donor',3,0,'tab','c1'),('13-pledge',3,0,'pledge','c2'),('14-report',3,2,'tab','c0'),('15-profile',0,3,'tab','c1'),('16-home-small',0,0,'tab','c1',320,740,1),('17-home-large-text',0,0,'tab','c1',393,852,1.3),('18-form-small',0,0,'form','c2',320,740,1.3),('19-landscape',0,0,'tab','c1',852,393,1)]
def run(case):
 name,*args=case
 p=subprocess.run([str(studio/'jbr/bin/java'),'-Xmx800m','-Djava.awt.headless=true',f'-Dstudio.dir={studio}',f'-Dsdk.dir={sdk}','--enable-native-access=ALL-UNNAMED','-cp',cp,'RenderPreview',str(root),f'docs/qa/{name}.png',*map(str,args)],cwd=root,capture_output=True,text=True,timeout=50)
 (root/f'docs/qa/{name}.log').write_text(p.stdout+p.stderr)
 ok=p.returncode==0 and 'Populate: Result{status=SUCCESS' in p.stdout
 print(name,'PASS' if ok else 'FAIL',flush=True)
 if not ok:print((p.stdout+p.stderr)[-2000:],flush=True)
 return ok
with ThreadPoolExecutor(max_workers=3) as pool:
 results=list(pool.map(run,cases))
assert all(results)
