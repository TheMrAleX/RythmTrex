# Validación de Saltabeat

## Comprobado en el entorno cloud

- JDK 17 completo, SDK 35, Build Tools 35.0.0, wrapper Gradle 8.11.1 con checksum.
- `scripts/bootstrap-cloud.sh`: instalación/activación y checks de principio a fin.
- 25 tests, cero fallos, cero omitidos: 9 del motor, 2 de PCM, 12 de pantallas y 2
  ejecuciones del flujo completo (API 26 y 35).
- Patrones deterministas, 600 obstáculos en cada una de 1.000 semillas, ventanas
  incluidas sus fronteras, puntuación, vidas, reloj táctil y calibración ±300 ms.
- Bots de 10 minutos en Difícil sin perder vidas, incluida entrega real de eventos
  a ambos extremos de calibración; cobertura del BPM tope.
- Flujo: tutorial → cuenta atrás → juego/acierto → pausa → reanudación en el mismo
  punto → fin de partida → reintento. Ajustes y calibración persisten al crear otro
  ViewModel desde DataStore.
- PNG Roborazzi revisados en `docs/screens`: 360×800, 320×640, 411×914 y fontScale 1,3.
  Ajustes desplaza su contenido cuando no cabe. El juego conserva círculos dentro
  de su zona segura y el HUD se adapta en la pantalla compacta con texto ampliado.
- Lint debug/release sin errores. Persisten advertencias de estilo, recursos sin uso
  y orientación/ABI elegidas expresamente. `allowBackup=false` desactiva backup;
  las reglas de extracción excluyen también los datos en Android 12+.
- APK debug: firma válida, app.saltabeat, API 26–35, únicamente VIBRATE y bibliotecas
  nativas arm64-v8a, comprobado con `scripts/verify-apk.sh`.
- Release local: build con R8/shrinkResources y lintRelease completados; firma v2,
  permisos, SDK, ARM64 y SHA256 verificados. APK de aproximadamente 1,1 MB en
  `/workspace/artifacts/Saltabeat-v1.0.0-arm64-v8a.apk`.

Los informes originales permanecen en `engine/build/reports` y `app/build/reports`.
Las capturas se regeneran al ejecutar `:app:testDebugUnitTest`; no se omiten tests
para que un resultado parezca correcto.

## Límites de la comprobación

No hay teléfono ni emulador Android con audio real conectado. No se ha medido fps,
latencia física, consumo o comportamiento de Bluetooth. La síntesis se prueba como
PCM determinista y acotado; la lógica de foco y ciclo de vida está implementada, pero
una llamada telefónica y el cambio de dispositivo de salida requieren prueba física.
La restauración comprobada corresponde a ajustes/records, no a continuar una carrera
tras muerte del proceso: en ese caso la app abre el menú de forma segura.

Las incompatibilidades entre dimensiones, ventanas tardías y doble salto están
resueltas explícitamente en `DECISIONS.md`. El test de salto verifica el vértice en
el encuentro; no prueba una ausencia de solapamiento anterior a un toque tardío.

## Comprobación recomendada en un teléfono

1. Instalar el APK firmado. Abrir sin red; calibrar y completar tutorial.
2. Jugar en cada dificultad y Práctica 70/180 BPM con etapas 1/4.
3. Revisar ocho pulsos de calibración, ajustes manuales ±300 ms y volumen de música 0.
4. Pausar, bloquear/desbloquear, cambiar de app y recibir una llamada: no debe avanzar
   la canción pausada ni quedar el stream abierto en segundo plano.
5. Cambiar entre altavoz y Bluetooth, recalibrar y comprobar los límites de juicio.
6. Activar texto al 130%, TalkBack y Reducir efectos; revisar controles y anuncios.
7. Confirmar fluidez en pantallas 60/120 Hz y que solo se mantiene encendida al jugar.

## Entorno reutilizable

Se guardaron `install_script` y `start_skill` en el borrador de configuración del
entorno. Ese guardado no aplica ni publica cambios: para restaurarlo en nuevas tareas,
revisar/guardar los ajustes y publicar el entorno desde la interfaz correspondiente.
