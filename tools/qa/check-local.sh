#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
studio_dir="${STUDIO_DIR:-/home/binhnguyen/android-studio}"
sdk_dir="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/home/binhnguyen/Android/Sdk}}"
export JAVA_HOME="${JAVA_HOME:-$studio_dir/jbr}"
export PATH="$JAVA_HOME/bin:$PATH"
./tools/build-sdk.sh > docs/qa/build.log 2>&1
qa_classes="$PWD/app/build/sdk-debug/checks"
mkdir -p "$qa_classes"
qa_cp="$qa_classes:$PWD/app/build/sdk-debug/classes:$studio_dir/plugins/design-tools/lib/layoutlib.jar:$studio_dir/lib/*:$studio_dir/plugins/android/lib/*"
javac -cp "$qa_cp" -d "$qa_classes" tools/qa/DomainChecks.java tools/qa/RenderPreview.java tools/qa/shims/android/os/Build.java
java -Djava.awt.headless=true -cp "$qa_cp" DomainChecks docs/qa | tee docs/qa/domain-checks.log
python3 - <<'QR'
import cv2
from pathlib import Path
im=cv2.imread('docs/qa/qr-native-encoder.png')
value,_,_=cv2.QRCodeDetector().detectAndDecode(im)
assert value=='BC0013',repr(value)
Path('docs/qa/qr-check.log').write_text('PASS Independent OpenCV QR decoder: '+value+'\n')
print('PASS Independent OpenCV QR decoder:',value)
QR
if [[ "${1:-}" == "--render" ]]; then
  STUDIO_DIR="$studio_dir" ANDROID_HOME="$sdk_dir" python3 tools/qa/render_all.py
fi
