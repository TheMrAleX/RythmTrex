"""Prepare signing without persisting credentials in the repository or logs."""
import base64
import os
from pathlib import Path
import secrets
import subprocess

names = ('KEYSTORE_BASE64', 'KEYSTORE_PASSWORD', 'KEY_ALIAS', 'KEY_PASSWORD')
present = [bool(os.environ.get(n)) for n in names]
if any(present) and not all(present):
    raise SystemExit('Configure all four signing secrets, or none for an ephemeral key.')
path = Path(os.environ['RUNNER_TEMP']) / 'saltabeat-release.jks'
credentials = {}
if all(present):
    path.write_bytes(base64.b64decode(os.environ['KEYSTORE_BASE64'], validate=True))
else:
    credentials = {'KEYSTORE_PASSWORD': secrets.token_urlsafe(32),
                   'KEY_PASSWORD': secrets.token_urlsafe(32), 'KEY_ALIAS': 'saltabeat'}
    for name, value in credentials.items():
        print(f'::add-mask::{value}')
    subprocess.run(['keytool', '-genkeypair', '-keystore', str(path), '-storetype', 'JKS',
                    '-storepass:env', 'KEYSTORE_PASSWORD', '-keypass:env', 'KEY_PASSWORD',
                    '-alias', credentials['KEY_ALIAS'], '-keyalg', 'RSA', '-keysize', '3072',
                    '-validity', '10000', '-dname', 'CN=Saltabeat, OU=Independent, O=Saltabeat, C=ES'],
                   env={**os.environ, **credentials}, check=True)
path.chmod(0o600)
with open(os.environ['GITHUB_ENV'], 'a', encoding='utf-8') as output:
    output.write(f'KEYSTORE_PATH={path}\n')
    for name, value in credentials.items():
        output.write(f'{name}={value}\n')
