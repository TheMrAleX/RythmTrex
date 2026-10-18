#!/usr/bin/env bash
# Reproducible Linux cloud prerequisites; never modifies tracked application files.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TOOLS=/workspace/tooling
mkdir -p "$TOOLS"
JDK="$TOOLS/jdk-17.0.14+7"
if [ ! -x "$JDK/bin/javac" ]; then
  BASE='https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.14%2B7'
  ARCHIVE='OpenJDK17U-jdk_x64_linux_hotspot_17.0.14_7.tar.gz'
  curl -fsSL "$BASE/$ARCHIVE" -o "$TOOLS/jdk.tar.gz"
  curl -fsSL "$BASE/$ARCHIVE.sha256.txt" -o "$TOOLS/jdk.sha256"
  python3 - "$TOOLS" <<'PY'
from pathlib import Path
import hashlib,sys
p=Path(sys.argv[1])
assert hashlib.sha256((p/'jdk.tar.gz').read_bytes()).hexdigest()==(p/'jdk.sha256').read_text().split()[0]
PY
  tar xf "$TOOLS/jdk.tar.gz" -C "$TOOLS"
fi
export JAVA_HOME="$JDK"
export ANDROID_HOME="$TOOLS/android-sdk"
export ANDROID_USER_HOME="$TOOLS/android-user"
export GRADLE_USER_HOME="$TOOLS/gradle-home"
mkdir -p "$ANDROID_USER_HOME" "$GRADLE_USER_HOME"
# Respect the platform proxy and installed system CA trust, including enterprise CAs.
PROXY_OPTIONS="$(python3 - <<'PY'
import os,urllib.parse
u=urllib.parse.urlsplit(os.environ.get('HTTPS_PROXY',''))
if u.hostname:
 print(f'-Dhttps.proxyHost={u.hostname} -Dhttps.proxyPort={u.port or 80} -Dhttp.proxyHost={u.hostname} -Dhttp.proxyPort={u.port or 80}')
PY
)"
TRUST_OPTIONS=''
if [ -f /etc/ssl/certs/java/cacerts ]; then
  TRUST_OPTIONS='-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts'
fi
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} $PROXY_OPTIONS $TRUST_OPTIONS -Duser.home=$TOOLS/java-home"
mkdir -p "$TOOLS/java-home"
if [ ! -x "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ]; then
  curl -fsSL https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -o "$TOOLS/cmdtools.zip"
  curl -fsSL https://dl.google.com/android/repository/repository2-1.xml -o "$TOOLS/android-repository.xml"
  python3 - "$TOOLS" <<'PY'
from pathlib import Path
import xml.etree.ElementTree as E,hashlib,sys,zipfile,shutil
p=Path(sys.argv[1]);root=E.parse(p/'android-repository.xml').getroot()
archives=[a for a in root.iter('complete') if a.findtext('url')=='commandlinetools-linux-11076708_latest.zip']
if not archives:raise SystemExit('Pinned tools not listed by Google; update pin using the official repository index.')
c=archives[0].find('checksum')
assert hashlib.new(c.attrib.get('type','sha1'),(p/'cmdtools.zip').read_bytes()).hexdigest()==c.text.strip()
with zipfile.ZipFile(p/'cmdtools.zip') as z:z.extractall(p/'android-extract')
dest=p/'android-sdk/cmdline-tools/latest';dest.parent.mkdir(parents=True,exist_ok=True)
shutil.move(str(p/'android-extract/cmdline-tools'),dest)
for f in (dest/'bin').iterdir():f.chmod(0o755)
PY
fi
set +o pipefail
yes 2>/dev/null | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$ANDROID_HOME" --licenses >/dev/null
set -o pipefail
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$ANDROID_HOME" 'platforms;android-35' 'build-tools;35.0.0' 'platform-tools'
printf 'sdk.dir=%s\n' "$ANDROID_HOME" > "$ROOT/local.properties"
ENV_FILE="$TOOLS/saltabeat-env.sh"
for name in JAVA_HOME ANDROID_HOME ANDROID_USER_HOME GRADLE_USER_HOME JAVA_TOOL_OPTIONS; do
  printf 'export %s=%q\n' "$name" "${!name}"
done > "$ENV_FILE"
printf 'export PATH=%q:$PATH\n' "$JAVA_HOME/bin:$ANDROID_HOME/platform-tools" >> "$ENV_FILE"
cd "$ROOT"
./gradlew --no-daemon :engine:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
