from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
import argparse
import os
import subprocess

root = Path(__file__).resolve().parents[2]
studio = Path(os.environ.get("STUDIO_DIR", "/home/binhnguyen/android-studio"))
sdk = Path(os.environ.get("ANDROID_HOME", os.environ.get("ANDROID_SDK_ROOT", "/home/binhnguyen/Android/Sdk")))
kotlin = Path(os.environ.get("BCC_KOTLIN_DIR", studio / "plugins/Kotlin/kotlinc"))
classpath = os.pathsep.join(map(str, [
    root / "app/build/sdk-debug/checks",
    root / "app/build/sdk-debug/classes",
    kotlin / "lib/kotlin-stdlib.jar",
    studio / "plugins/design-tools/lib/layoutlib.jar",
    studio / "lib/*",
    studio / "plugins/android/lib/*",
]))
cases = [
    ("01-home", 0, 0, "tab", "c1"),
    ("02-detail", 0, 0, "detail", "c2"),
    ("03-form", 0, 0, "form", "c2"),
    ("04-registrations", 0, 1, "tab", "c1"),
    ("05-ticket", 0, 1, "ticket", "c1"),
    ("06-coordinator", 1, 0, "tab", "c1"),
    ("07-manage", 1, 1, "manage", "c2"),
    ("08-create", 1, 1, "create", "c1"),
    ("09-assisted", 1, 1, "assisted", "c2"),
    ("10-volunteer", 2, 0, "tab", "c1"),
    ("11-handed-over", 2, 1, "tab", "c1"),
    ("12-donor", 3, 0, "tab", "c1"),
    ("13-pledge", 3, 0, "pledge", "c2"),
    ("14-report", 3, 2, "tab", "c0"),
    ("15-profile", 0, 3, "tab", "c1"),
    ("16-home-small", 0, 0, "tab", "c1", 320, 740, 1),
    ("17-home-large-text", 0, 0, "tab", "c1", 393, 852, 1.3),
    ("18-form-small", 0, 0, "form", "c2", 320, 740, 1.3),
    ("19-landscape", 0, 0, "tab", "c1", 852, 393, 1),
    ("20-campaigns", 1, 1, "tab", "c1"),
    ("21-help", 2, 2, "tab", "c1"),
    ("22-commitments-empty", 3, 1, "tab", "c1"),
    ("23-impact", 0, 2, "tab", "c1"),
    ("24-success", 0, 0, "success", "c1"),
    ("25-ticket-pending", 0, 1, "ticket", "c1"),
    ("26-ticket-waiting", 0, 1, "ticket", "c1"),
    ("27-ticket-received", 0, 1, "ticket", "c1"),
    ("28-ticket-cancelled", 0, 1, "ticket", "c1"),
    ("29-detail-archived", 0, 0, "detail", "c0"),
    ("30-manage-allocated", 1, 1, "manage", "c1"),
    ("31-commitments-active", 3, 1, "tab", "c1"),
    ("32-registrations-history", 0, 1, "tab", "c1"),
]
preview_properties = {
    "25-ticket-pending": ("status", "PENDING"),
    "26-ticket-waiting": ("status", "WAITING"),
    "27-ticket-received": ("status", "RECEIVED"),
    "28-ticket-cancelled": ("status", "CANCELLED"),
    "31-commitments-active": ("pledge", "10"),
    "32-registrations-history": ("filter", "9"),
}


def render(case, output):
    name, *state = case
    image = output / f"{name}.png"
    command = [
        str(studio / "jbr/bin/java"), "-Xmx800m", "-Djava.awt.headless=true",
        f"-Dstudio.dir={studio}", f"-Dsdk.dir={sdk}", "--enable-native-access=ALL-UNNAMED",
        "-cp", classpath, "RenderPreview", str(root), str(image), *map(str, state),
    ]
    if name in preview_properties:
        key, value = preview_properties[name]
        command.insert(1, f"-Dpreview.{key}={value}")
    result = subprocess.run(command, cwd=root, capture_output=True, text=True, timeout=60)
    log = result.stdout + result.stderr
    (output / f"{name}.log").write_text(log, encoding="utf-8")
    ok = (result.returncode == 0 and "Populate: Result{status=SUCCESS" in result.stdout
          and image.is_file() and "CLIPPED TEXT:" not in log and "ERROR " not in log)
    print(name, "PASS" if ok else "FAIL", flush=True)
    if not ok:
        print(log[-2000:], flush=True)
    return ok


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, default=root / "docs/qa")
    args = parser.parse_args()
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    with ThreadPoolExecutor(max_workers=3) as pool:
        results = list(pool.map(lambda case: render(case, output), cases))
    if not all(results):
        raise SystemExit("Some previews failed. See the corresponding logs.")


if __name__ == "__main__":
    main()
