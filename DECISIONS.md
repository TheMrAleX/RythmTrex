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
- Con calibración positiva los eventos llegan hasta 300 ms después de su tiempo de juicio corregido. El motor concede ese margen adicional de entrega antes de declarar un círculo perdido; no amplía las ventanas de acierto. Hay un segundo bot que entrega los eventos a su hora real con ±300 ms. Un salto corregido puede estar ya avanzado cuando llega un toque tardío por calibración; la puntuación usa el timestamp corregido, nunca el frame.
- Un segundo salto conserva la altura del salto en curso al despegar, manteniendo el vértice de 96dp y evitando un salto visual brusco al suelo.
- Fredoka es una fuente variable OFL: se fijan explícitamente los ejes de peso a 700/600 para evitar que Android use el peso inicial fino. La marca se adapta al ancho disponible cuando aumenta fontScale; el resto de textos sigue la escala del sistema y las pantallas se desplazan verticalmente.
- El scroll del escenario se calcula como 168dp × tiempos musicales acumulados; así el parallax no salta durante una transición de BPM. Cada obstáculo conserva su velocidad de encuentro.
- El sol se centra horizontalmente (la spec menciona tanto 38% como centrado horizontal; se prioriza esta última indicación).

- Se elimina del manifest el permiso de firma interno que AndroidX añade por defecto para receivers no exportados. La app no registra receivers propios; el manifest de release se inspecciona para dejar únicamente VIBRATE. El flujo de persistencia y menús se prueba también con API 26.
- Las advertencias de orientación vertical y de ausencia de x86 en ChromeOS corresponden a los requisitos explícitos de orientación y release ARM64, y se mantienen documentadas. No se desactiva lint globalmente.

- La cuenta atrás inicial usa cuatro clics al BPM seleccionado; la reanudación usa tres. El metrónomo de calibración usa ocho clics a 100 BPM y el volumen de efectos. Los efectos usan cuatro voces preasignadas, permitiendo mezclar tic y jingle simultáneamente.

- El Muro se resuelve como dos segmentos de 56×72dp ligados por los números 1/2. Un único bloque continuo que durase al menos 0,65 s en el punto del corredor sería incompatible con un airtime de 0,45 s y el mínimo de separación entre círculos. La pareja conserva el requisito de acertar dos saltos sin generar un tramo sólido insuperable.
- El pad incluye la barra de navegación dentro del límite de 240–320dp; el generador recibe su altura útil descontando ese inset. En 320dp hay al menos 192dp horizontales entre los extremos aleatorios, suficiente para garantizar 150dp incluso con un pad compacto.

## Validación en GitHub
- La primera ejecución CI (39097010514) falló en setup-android antes de Gradle. Los logs requieren autenticación y la API está bloqueada; la metadata pública permite identificar el paso. Se fija CLI Android 12 (11076708), la misma versión con checksum oficial ya validada localmente, y Ubuntu 24.04 para evitar cambios del runner ubuntu-latest. Se actualizan checkout/setup-java a v5, con Node 24.
