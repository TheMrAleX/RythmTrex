#!/usr/bin/env bash
set -euo pipefail
APK="$(realpath "${1:?Uso: scripts/verify-apk.sh ruta.apk}")"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/workspace/tooling/android-sdk}}"
TOOLS="$SDK/build-tools/35.0.0"
"$TOOLS/apksigner" verify --verbose "$APK"
python3 - "$TOOLS/aapt" "$APK" <<'PY'
import re,subprocess,sys,zipfile
binary,apk=sys.argv[1:]
permissions=subprocess.check_output([binary,'dump','permissions',apk],text=True)
used=set(re.findall(r"uses-permission: name='([^']+)'",permissions))
if used!={'android.permission.VIBRATE'}:
    raise SystemExit(f'Unexpected permissions: {sorted(used)}')
badging=subprocess.check_output([binary,'dump','badging',apk],text=True)
for required in ("package: name='app.saltabeat'", "sdkVersion:'26'", "targetSdkVersion:'35'"):
    if required not in badging:raise SystemExit(f'Missing package invariant: {required}')
with zipfile.ZipFile(apk) as archive:
    abis={name.split('/')[1] for name in archive.namelist() if name.startswith('lib/') and name.endswith('.so')}
if abis!={'arm64-v8a'}:raise SystemExit(f'Unexpected native ABIs: {sorted(abis)}')
print('Verified: app.saltabeat, API 26–35, only VIBRATE, only arm64-v8a, valid signature.')
PY
