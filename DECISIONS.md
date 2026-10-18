# Decisiones

## Entorno y arquitectura
- El usuario autoriza crear el proyecto completo; las restricciones de solo setup anteriores no limitan este encargo de desarrollo.
- Se usa el checkout existente y se crea main desde la rama local sin commits.
- AGP 8.9.1, Gradle 8.11.1, Kotlin 2.1.10, Compose BOM 2025.02.00; SDK 35, mínimo 26 y bytecode Java 17.
- Dos módulos: engine (JVM puro) y app. El tiempo musical es la autoridad; no se integra posición por frames.
- Las separaciones de medio tiempo del doble salto contradicen el mínimo absoluto de 0,5 s a BPM >60. Se prioriza jugabilidad: el segundo golpe se separa max(medio tiempo, 0,5 s); la siguiente pareja empieza después del segundo golpe y de la recuperación.
- El Muro de 72dp supera la holgura de un salto de 96dp cerca de los extremos; cada acierto neutraliza su tramo y el muro se representa desactivado mientras se resuelve la pareja. El test verifica la geometría visible de obstáculos sólidos, no oculta colisiones del motor.
- La posición de obstáculos usa la velocidad del BPM en la hora de encuentro (constante por obstáculo); evita teletransportes al cambiar el tempo y conserva x=runnerX+speed*(Tobs-t).
- Los círculos se alternan entre dos puntos seguros separados al menos 150dp; aleatoriedad acotada por semilla. En 320dp se colocan en diagonal para respetar los márgenes y distancia.
- El reloj no resta dos veces latencia: AudioTimestamp ya describe presentación; playbackHeadPosition usa una estimación conservadora y el fallback monotónico conserva el punto de pausa.
