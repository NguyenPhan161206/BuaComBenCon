import subprocess
import glob
import sys
from pathlib import Path

studio = 'C:/Program Files/Android/Android Studio'
classes = 'app/build/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes'
shims = 'tools/qa/shims'
qa_classes = 'tools/qa/classes'

# 1. Compile DomainChecks
Path(qa_classes).mkdir(parents=True, exist_ok=True)
compile_cp = f"{classes};{shims};D:/Android/Sdk/platforms/android-36/android.jar"
with open('tools/qa/args.txt', 'w', encoding='utf-8') as f:
    f.write(f"-classpath\n{compile_cp}\n-jvm-target\n11\n-d\n{qa_classes}\ntools/qa/DomainChecks.kt\n")

print("Compiling DomainChecks.kt...")
res = subprocess.run(['kotlinc.cmd', '@tools/qa/args.txt'], capture_output=True, text=True)
if res.returncode != 0:
    print("Compile error:\n", res.stderr)
    sys.exit(res.returncode)

# 2. Run DomainChecks
jars = [
    qa_classes,
    classes,
    shims,
    'C:/Users/Quang Hao/scoop/apps/kotlin/current/lib/kotlin-stdlib.jar',
    f"{studio}/plugins/design-tools/lib/layoutlib.jar"
]
jars.extend(glob.glob(f"{studio}/lib/*.jar"))
jars.extend(glob.glob(f"{studio}/plugins/android/lib/*.jar"))
run_cp = ";".join(jars)

with open('tools/qa/java_args.txt', 'w', encoding='utf-8') as f:
    f.write(f"-cp\n\"{run_cp}\"\nDomainChecks\ndocs/qa\n")

print("Running DomainChecks on JVM...")
res = subprocess.run(['java', '@tools/qa/java_args.txt'], capture_output=True, text=True)
print("STDOUT:\n", res.stdout)
if res.stderr:
    print("STDERR:\n", res.stderr)
sys.exit(res.returncode)
