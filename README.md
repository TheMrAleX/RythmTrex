# Saltabeat

Un juego Android nativo e infinito de saltos al ritmo, con Beto como protagonista.
Funciona íntegramente offline: sin cuenta, red, anuncios ni analytics; solo solicita
VIBRATE. Música sintetizada, personaje y arte originales. No está afiliado a ningún
juego existente.

## Cómo jugar

1. En **Jugar**, elige Fácil (90–130 BPM), Normal (100–160) o Difícil (115–180).
2. Tras la cuenta atrás, toca el núcleo verde cuando el anillo blanco llegue a él.
   El personaje salta automáticamente. Los toques alejados del círculo no penalizan.
3. Perfecto / Genial / Bien dan 300 / 200 / 100 puntos antes del multiplicador.
   Llegar demasiado pronto rompe el círculo. Fallar cuesta una vida al encuentro.
4. Tienes tres vidas, 1,2 segundos de invencibilidad y recuperas una vida cada 25
   de combo. El multiplicador sube cada diez aciertos, hasta x5.
5. En los muros aparecen dos círculos numerados; acierta ambos. **Práctica** permite
   elegir 70–180 BPM y etapa sin perder vidas.

La primera partida incluye un tutorial interactivo omitible. La calibración se
ofrece al abrir la app y se puede repetir desde Ajustes: toca ocho pulsos, guarda
la mediana de tu desfase y afínala en pasos de 5 ms (±300 ms). Se recomienda volver
a calibrar al cambiar de salida de audio, especialmente con Bluetooth.

## Aspecto y capturas

La especificación original está en [docs/DESIGN_SPEC.md](docs/DESIGN_SPEC.md).
Los PNG de Roborazzi están en [docs/screens](docs/screens):

- Inicio: cielo violeta, sol coral, Beto amarillo y botones gruesos turquesa/blanco.
- Juego: puntuación y corazones arriba, mundo de skyline y pad inferior con anillos.
- Pausa y resultados: velo sobre el mundo y paneles redondeados, sin elevación Material.
- Ajustes/calibración: controles propios, volúmenes separados y ajuste de latencia.

Los tests revisan 360×800, 320×640, 411×914 y fontScale 1,3. Cada pantalla incluye
un `@Preview`. Los PNG son renders de JVM; no sustituyen la revisión en un teléfono.

## Compilar y probar

Requiere JDK 17, Android SDK Platform 35 y Build Tools 35.0.0.

```sh
./gradlew :engine:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

En el entorno cloud Linux, `scripts/bootstrap-cloud.sh` prepara herramientas fuera
del checkout y ejecuta esos checks. Después, `source /workspace/tooling/saltabeat-env.sh`
activa las rutas para nuevas terminales. Se conservan las verificaciones TLS y los
checksums oficiales de Gradle, JDK y Android command-line tools.

`engine:test` incluye límites exactos de juicio, vidas/multiplicador, determinismo,
600 obstáculos en cada una de 1.000 semillas, inversión del mapa de tempo, síntesis
PCM y bots de diez minutos en Difícil, también con eventos entregados con ±300 ms.
`app:testDebugUnitTest` ejecuta Robolectric, el flujo de menús/persistencia y capturas
Roborazzi. Los reportes se guardan en `engine/build/reports` y `app/build/reports`.

## Arquitectura del tiempo

- `engine`: Kotlin puro. `Tempo` convierte segundos↔tiempos con transiciones continuas
  de ocho tiempos. Cada etapa contiene 64 tiempos; el BPM sube cuatro hasta su tope.
- `Patterns`: PRNG con semilla, dos posiciones seguras alternadas, mínimo 0,65 segundos
  entre golpes, horizonte de notas limitado. `Game` juzga y puntúa, sin dependencias Android.
- `Synth`: PCM original a 44,1 kHz, bombo, caja, hi-hat, bajo y arpegio, cuatro progresiones
  elegidas por semilla. Capas por etapa y efectos en el mismo flujo.
- `BeatAudio`: un hilo con prioridad de audio escribe `AudioTrack` mono de 16 bits.
  `AudioTimestamp` proyecta el frame presentado al instante actual; el contador de
  reproducción es el segundo recurso, y el reloj monotónico es el respaldo si no
  funciona la salida. La pausa conserva el punto y reconstruye el flujo al reanudar.
- Los eventos `MotionEvent.eventTime` se convierten desde uptime a ese mismo reloj
  y restan la calibración. No se usa el tiempo del frame para juzgar un toque.
- Una Activity, menús con ViewModel/StateFlow y DataStore. `withFrameNanos` solo solicita
  dibujar; posiciones y animación musical se derivan del reloj. El HUD se dibuja dentro
  del Canvas y las actualizaciones de accesibilidad se limitan a eventos de juego.

## Ajustar patrones y dificultad

Edita los parámetros de `Difficulty` y los intervalos de `Patterns.next()` en
`engine/src/main/kotlin/app/saltabeat/engine/Engine.kt`. `Game(seed, difficulty)`
reproduce una carrera; conserva la semilla para reproducir una incidencia. El modo
Práctica usa un `fixedBpm` y una etapa fija. Ejecuta de nuevo todos los tests al
cambiar ventanas, velocidades o geometría. Las resoluciones de restricciones
incompatibles y limitaciones comprobadas se explican en [DECISIONS.md](DECISIONS.md).

## Releases y licencia

Versión 1.0.0 (código 1), mínimo Android 8.0 / API 26. Release ARM64 con R8 y recursos
reducidos. Consulta [RELEASING.md](RELEASING.md) para firma estable, secrets, tags y
comandos de publicación. Código MIT; Fredoka se distribuye bajo OFL, incluida en
[docs/licenses/Fredoka-OFL.txt](docs/licenses/Fredoka-OFL.txt).
