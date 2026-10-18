# Saltabeat: reglas de trabajo
- Trabajar de forma autónoma, sin preguntas. Resolver dudas razonablemente y anotarlas en DECISIONS.md.
- Stack obligatorio: Kotlin, Compose, Material 3 como base, una Activity; MVVM/StateFlow en menús; motor Kotlin puro y un Canvas para el juego.
- Offline, solo VIBRATE; arte y audio originales. Consultar docs/DESIGN_SPEC.md antes de escribir cada pantalla.
- No crear worktrees: el entorno ya está aislado. No guardar credenciales ni keystores.
- Hacer commits pequeños por fase y ejecutar tests, lint y builds. No declarar verificaciones que no se han ejecutado.
- Continuar hasta la definición de terminado o un bloqueo externo comprobado; dejar instrucciones exactas.

## Estado
- [x] Fase 0: proyecto y documentación inicial.
- [x] Fase 1: motor y pruebas.
- [x] Fase 2: audio sintetizado y calibración.
- [x] Fases 3–4: pantallas, persistencia y accesibilidad.
- [x] Fase 5: 25 tests sin fallos/omisiones, lint debug/release y ambos APK verificados.
- [ ] Fase 6: publicar y verificar v1.0.0 en GitHub.
- [x] Fase 7: README, licencia, decisiones, especificación y capturas.

No hay dispositivo físico: sonido, Bluetooth y fps requieren comprobación real; ver docs/QA.md.
