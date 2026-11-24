#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
studio_dir="${STUDIO_DIR:-/home/binhnguyen/android-studio}"
sdk_dir="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/home/binhnguyen/Android/Sdk}}"
kotlin_dir="${BCC_KOTLIN_DIR:-$studio_dir/plugins/Kotlin/kotlinc}"
export JAVA_HOME="${JAVA_HOME:-$studio_dir/jbr}"
export PATH="$JAVA_HOME/bin:$PATH"
qa_output="${BCC_QA_OUTPUT:-$PWD/docs/qa}"
mkdir -p "$qa_output"
./tools/build-sdk.sh > "$qa_output/build.log" 2>&1
qa_classes="$PWD/app/build/sdk-debug/checks"
mkdir -p "$qa_classes"
find "$qa_classes" -type f -delete
qa_cp="$PWD/app/build/sdk-debug/classes:$kotlin_dir/lib/kotlin-stdlib.jar:$studio_dir/plugins/design-tools/lib/layoutlib.jar"
for jar in "$studio_dir"/lib/*.jar "$studio_dir"/plugins/android/lib/*.jar; do
  qa_cp+=":$jar"
done
"$kotlin_dir/bin/kotlinc" -classpath "$qa_cp" -jvm-target 11 -d "$qa_classes" tools/qa
java -Djava.awt.headless=true -cp "$qa_classes:$qa_cp" DomainChecks "$qa_output" | tee "$qa_output/domain-checks.log"
python3 - "$qa_output" <<'QR'
import cv2
import sys
from pathlib import Path
output=Path(sys.argv[1])
im=cv2.imread(str(output / 'qr-native-encoder.png'))
value,_,_=cv2.QRCodeDetector().detectAndDecode(im)
assert value=='BC0013',repr(value)
(output / 'qr-check.log').write_text('PASS Independent OpenCV QR decoder: '+value+'\n')
print('PASS Independent OpenCV QR decoder:',value)
QR
if [[ "${1:-}" == "--render" ]]; then
  STUDIO_DIR="$studio_dir" BCC_KOTLIN_DIR="$kotlin_dir" ANDROID_HOME="$sdk_dir" python3 tools/qa/render_all.py --output "$qa_output"
fi
