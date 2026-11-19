#!/usr/bin/env bash
# Build the native Java UI using the Android SDK, without a Gradle download.
set -euo pipefail
cd "$(dirname "$0")/.."
sdk_dir="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/home/binhnguyen/Android/Sdk}}"
jdk_dir="${JAVA_HOME:-/home/binhnguyen/android-studio/jbr}"
export PATH="$jdk_dir/bin:$PATH"
bt="$sdk_dir/build-tools/36.0.0"
platform="$sdk_dir/platforms/android-36/android.jar"
out="$PWD/app/build/sdk-debug"
for task_tool in aapt2 d8 zipalign apksigner; do
  test -x "$bt/$task_tool" || { echo "Missing Android build tool: $bt/$task_tool"; exit 1; }
done
test -f "$platform" || { echo "Install Android SDK platform 36."; exit 1; }
mkdir -p "$out/gen" "$out/classes" "$out/dex"
"$bt/aapt2" compile --dir app/src/main/res -o "$out/resources.zip"
python3 - "$out/AndroidManifest.xml" <<'MANIFEST'
import sys
from pathlib import Path
manifest=Path('app/src/main/AndroidManifest.xml').read_text()
manifest=manifest.replace('<manifest ', '<manifest package="vn.edu.buacombencon" android:versionCode="1" android:versionName="1.0-ui" ')
manifest=manifest.replace('<application ', '<uses-sdk android:minSdkVersion="26" android:targetSdkVersion="36"/><application android:debuggable="true" ')
Path(sys.argv[1]).write_text(manifest)
MANIFEST
"$bt/aapt2" link -o "$out/unsigned.apk" -I "$platform" --manifest "$out/AndroidManifest.xml" --java "$out/gen" "$out/resources.zip"
rg --files app/src/main/java "$out/gen" -g '*.java' > "$out/sources.txt"
javac -encoding UTF-8 --release 11 -classpath "$platform" -d "$out/classes" @"$out/sources.txt"
jar cf "$out/classes.jar" -C "$out/classes" .
"$bt/d8" --lib "$platform" --min-api 26 --output "$out/dex" "$out/classes.jar"
python3 - "$out" <<'DEX'
from pathlib import Path
import sys,zipfile
out=Path(sys.argv[1])
with zipfile.ZipFile(out/'unsigned.apk','a') as apk:
    for dex in (out/'dex').glob('*.dex'): apk.write(dex,dex.name)
DEX
"$bt/zipalign" -f -p 4 "$out/unsigned.apk" "$out/aligned.apk"
if [ ! -f "$out/debug.keystore" ]; then
  keytool -genkeypair -keystore "$out/debug.keystore" -storepass android -keypass android -alias androiddebugkey -dname 'CN=Android Debug,O=Android,C=US' -keyalg RSA -keysize 2048 -validity 10000
fi
mkdir -p app/build/outputs/apk/debug
"$bt/apksigner" sign --ks "$out/debug.keystore" --ks-pass pass:android --out app/build/outputs/apk/debug/app-debug.apk "$out/aligned.apk"
"$bt/apksigner" verify --verbose app/build/outputs/apk/debug/app-debug.apk
"$bt/aapt2" dump badging app/build/outputs/apk/debug/app-debug.apk
