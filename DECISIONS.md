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

## Resolución de restricciones incompatibles
- El doble salto usa al menos 0,65 s entre golpes: además del mínimo de 0,5 s, evita que tres círculos estén simultáneamente activos (1,1 s de aproximación más hasta 168 ms de ventana). Así se puede garantizar una distancia de 150dp con solo dos zonas seguras en pantallas de 320dp.
- La fórmula D=max(0,5*airtime,2*(Tobs-t0)) puede mover el vértice después del obstáculo en un acierto tardío. Se conserva D y se usa una parábola asimétrica por tramos para mantener el vértice en Tobs.
- La exigencia de que nunca se crucen visualmente las cajas incluso antes de un toque tardío no es compatible con todas las dimensiones y ventanas dadas. Ejemplo: Fácil, 130 BPM, toque +168 ms: el obstáculo está a 22,8dp del centro del personaje al iniciar el salto, menos que los semianchos combinados. Se juzga la colisión en el encuentro, como requiere la fórmula de timing; el acierto neutraliza y disuelve inmediatamente el obstáculo. El test de geometría garantiza altura al encuentro, no una ausencia de solapamiento anterior al toque.
- El rendimiento de 60/120 fps y la latencia Bluetooth necesitan un dispositivo real. Se solicita 120 Hz al sistema; no se afirma una medición que no se ha realizado.
