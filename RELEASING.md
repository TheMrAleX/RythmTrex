# Publicar Saltabeat

Requisitos: JDK 17, SDK 35 y permisos de escritura al repositorio.

```sh
./gradlew :engine:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
git push -u origin main
git tag v1.0.0
git push origin v1.0.0
# Si el tag ya existe localmente, omite git tag.
gh run list --workflow release.yml
gh run watch
gh release view v1.0.0
```

El workflow compila con R8 y shrinkResources, verifica la firma y publica
`Saltabeat-v1.0.0-arm64-v8a.apk` y su `.sha256` con notas generadas.
La app solo contiene código JVM; abiFilters restringe cualquier dependencia nativa
futura a ARM64. No se incluyen bibliotecas nativas innecesarias.

## Firma estable para actualizaciones

Configura en GitHub → Settings → Secrets and variables → Actions estas cuatro
secrets antes del primer tag: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`,
`KEY_PASSWORD`. `KEYSTORE_BASE64` es el archivo JKS codificado con base64 en una
sola línea (por ejemplo `base64 -w0 mi-clave.jks` en Linux). Guarda una copia privada
segura de tu clave. Nunca hagas commit de claves ni contraseñas.

Si no hay ninguna secret, el workflow genera una clave efímera RSA de 3072 bits y
un APK firmado e instalable. Esa clave se elimina al terminar: un APK generado en
otra ejecución con otra clave no podrá instalarse encima sin desinstalar el anterior.
Para actualizaciones utiliza siempre tu clave estable. Si solo hay algunas secrets,
el workflow falla explícitamente para evitar cambiar la identidad de firma por error.

Para firmar localmente, exporta `KEYSTORE_PATH` (ruta absoluta), `KEYSTORE_PASSWORD`,
`KEY_ALIAS` y `KEY_PASSWORD` en tu entorno privado y ejecuta:

```sh
./gradlew :app:assembleRelease
$ANDROID_HOME/build-tools/35.0.0/apksigner verify --verbose app/build/outputs/apk/release/app-release.apk
```

## Evidencia local y limitación de la API

Se generó y verificó un APK release firmado en
`/workspace/artifacts/Saltabeat-v1.0.0-arm64-v8a.apk`, con su `.sha256`.
`scripts/verify-apk.sh` comprueba firma, VIBRATE como único permiso, SDK y ARM64.
La API `api.github.com` está bloqueada por el proxy de este entorno; una consulta
`gh api` devuelve Forbidden, aunque Git HTTPS y la web de github.com funcionan.
El workflow puede publicar desde el runner de GitHub con su propio GITHUB_TOKEN.
La clave efímera de la compilación local difiere de la que crea el workflow: para
instalar una encima de otra hace falta usar una clave estable o desinstalar primero.

Para revisar el resultado sin CLI: https://github.com/TheMrAleX/RythmTrex/releases/tag/v1.0.0
