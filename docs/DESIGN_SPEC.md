FASE 3 – DISEÑO VISUAL (SPEC CERRADA, OBLIGATORIA)
Dispositivo de referencia 360x800dp. Verifica también 320x640, 411x914 y fontScale 1.3. Si es viable usa Paparazzi/Roborazzi para renderizar PNG a /docs/screens y corrige lo que se vea mal. Cada pantalla con @Preview. PROHIBIDOS los componentes Material por defecto visibles (TopAppBar, Button, Card con elevación): construye componentes propios con estos tokens.

Estética: "synthwave atardecer" suave, plana y limpia, con formas redondeadas. Sin degradados chillones fuera de los indicados.
Colores:
  skyTop #1A1033, skyBottom #4B1F6F, sun #FF7A59 (con borde interior #FFB36B), skyline #2A1650, ground #0E0A1F, groundLine #6C4BB5
  runner #FFD166 (cuerpo), runnerDark #C9942A (sombras), eyes #1A1033
  obstacle #FF5A6E (pinchos), #5ED3F3 (caja), #B58CFF (muro); todos con borde 3dp de tono 25% más oscuro
  pad #120C26, padBorder blanco 10% alpha, circle #2EE6C5, ring #FFFFFF 90%
  perfect #FFD166, great #2EE6C5, good #8DA2FF, miss #FF5A6E
  onBg #FFFFFF, onBgMuted blanco 65%, surface #231544, surfaceHigh #2E1C58, accent #2EE6C5
Tipografía: intenta descargar e incluir una fuente redondeada OFL (Fredoka o Baloo 2, pesos SemiBold/Bold) en res/font; si no puedes, fuente del sistema con FontWeight.Black. Números de marcador con cifras tabulares. Tamaños: display 56sp Black, score 36sp Black, title 24sp ExtraBold, body 16sp Medium, caption 13sp Medium.
Espaciado en múltiplos de 4dp; padding de pantalla 20dp. Radios: 12, 20, 28, 32 (pad), pill 50%. Movimiento: 150 ms micro, 250 ms estados, 400 ms entradas. Easing FastOutSlowIn; springs con dampingRatio 0.7.
Botones propios "chunky": alto 60dp, pill, borde inferior sólido 4dp 25% más oscuro, al presionar baja 4dp en 80 ms + háptica ligera. Variantes: primario (accent con texto #1A1033), claro (blanco, texto #1A1033), secundario (contorno 2dp blanco 40%).

ÁREAS DE LA PANTALLA DE JUEGO (360x800 de referencia)
  ┌──────────────────────────┐
  │ 12.450  x4        ♥♥♥  ⏸ │  HUD (bajo la barra de estado)
  │ Récord 18.200            │
  │        ◖  sol  ◗         │
  │  skyline en parallax     │
  │                          │
  │   🟡→            ▲  ▮    │  personaje a 22% del ancho; obstáculos entran por la derecha
  │ ──────────¦───────────── │  suelo + marca de salto
  │ ╭──────────────────────╮ │
  │ │ ● ● ● ●   (compás)   │ │  PAD: 34% de la altura (mín 240dp, máx 320dp)
  │ │      ◯ ← círculo     │ │
  │ │ ◯                    │ │
  │ ╰──────────────────────╯ │
  └──────────────────────────┘
- Mundo: ocupa todo lo que queda sobre el pad. Cielo en degradado vertical skyTop→skyBottom. Sol: semicírculo de radio 96dp centrado a 38% del ancho... (centrado horizontal) con la mitad inferior oculta tras el skyline, que late (escala 1.00→1.03 en 120 ms) con cada bombo. Parallax: sol/estrellas 0.1x, skyline (rectángulos redondeados de alturas variadas) 0.3x, suelo 1.0x. Estrellas: 30 puntos de 1–2dp con parpadeo lento.
- Suelo: línea de 4dp color groundLine a 64dp sobre el borde superior del pad; bajo ella el color ground con rayitas horizontales de 12dp que se desplazan a velocidad del mundo. Velocidad del mundo = 280dp/s × (BPM/100).
- Personaje "Beto": cuerpo rectángulo redondeado 44x48dp (radio 14dp) color runner, dos ojos blancos de 10dp con pupila #1A1033 de 5dp que miran hacia delante, mejilla sombreada runnerDark. Piernas: dos pequeños rectángulos de 8x14dp que alternan cada medio tiempo (animación sincronizada al BPM). Squash & stretch: al despegar estira 1.15x vertical; al aterrizar se aplasta 0.85x por 100 ms con polvo (3 partículas). Su sombra: elipse oscura 40% alpha en el suelo que se encoge al subir. Parpadea (alpha 0.3/1 cada 80 ms) durante la invencibilidad.
- Obstáculos: Pinchos = fila de 3 triángulos redondeados de 20x24dp; Caja = cuadrado redondeado 40x40dp con una cara simple (dos puntos) inclinándose levemente al avanzar; Muro = rectángulo redondeado 56x72dp con 2 franjas. Cada uno con borde oscuro y sombra elíptica.
- MARCA DE SALTO: línea punteada vertical tenue (blanco 20%) sobre el suelo en el punto donde el obstáculo debe estar cuando toca tocar (recorre speed × airtime/2 desde el personaje). Ayuda al principiante.
- HUD: puntaje 36sp Black arriba a la izquierda (20dp de margen + inset superior); debajo "Récord N" en caption muted; junto al puntaje, el multiplicador "x4" en una pill color accent (rebota 1.0→1.25→1.0 en 150 ms al subir). A la derecha: corazones de 24dp (llenos color miss, vacíos contorno blanco 30%) y un botón pausa circular de 48dp (surface 60% alpha, icono de 2 barras).
- PAD (contenedor): color pad, borde superior 2dp padBorder, esquinas superiores 32dp, puntitos decorativos blancos 4% alpha en cuadrícula de 32dp. El borde superior late en alpha (10%→35%→10%) en 120 ms con cada tiempo. Fila de 4 puntos de 8dp en la parte superior interna (12dp bajo el borde) como metrónomo del compás: el tiempo actual se ilumina en accent y el primero del compás en blanco. 
- Zona segura para círculos: dentro del pad con margen de 56dp a los lados, 56dp arriba (bajo los puntos del metrónomo) y 56dp + inset de la barra de navegación abajo.
- CÍRCULO: núcleo de radio 36dp color circle con borde blanco 3dp, brillo interior (círculo de 12dp blanco 35% en la esquina superior izquierda). ANILLO de aproximación: trazo 6dp color ring, radio de 108dp → 36dp, lineal, durante 1,1 s (se ajusta a que el radio llegue a 36dp exactamente en la hora de golpe). Aparece con fundido+escala 0.8→1 en 150 ms. En círculos de doble salto, dibuja "1" y "2" (20sp Black, #1A1033) dentro del núcleo. En el instante de golpe perfecto el anillo coincide con el borde del núcleo.
- Feedback de acierto: onda de anillo que se expande 36→80dp y se desvanece en 250 ms, 12 partículas del color del juicio, texto flotante ("¡Perfecto!", "Genial", "Bien") 22sp Black color del juicio que sube 40dp y se desvanece en 600 ms. Háptica ligera (fuerte en Perfecto).
- Feedback de fallo: círculo se vuelve miss, se parte (se encoge y desvanece en 250 ms), texto "Fallo" o "Muy pronto" en miss, sacudida de pantalla de 6dp en 200 ms, flash rojo de 15% alpha en 120 ms, corazón perdido con animación de caída. Si "Reducir efectos" está activo, sin sacudida ni flashes ni pulsos de fondo.
- Combo: texto "COMBO 12" 18sp Black centrado a 24% de altura, aparece solo desde 5 de combo, con pulso en cada acierto; se desvanece al romperse.
- Cambio de etapa: banner "ETAPA 2" 40sp Black que entra por la izquierda y sale por la derecha en 1,2 s, y el cielo cambia gradualmente a otra variante (E2 skyBottom #6B2A7F, E3 #8A2F7A con sol #FF5E78, E4 #2C3E91 con sol #FFC857; luego cicla).

PANTALLAS
1) INICIO: fondo cielo + skyline + suelo con Beto corriendo en bucle sobre el suelo y pad vacío con puntos. Título "Saltabeat" 56sp Black con trazo exterior 4dp color #FF7A59 y relleno blanco; debajo caption "Toca al ritmo. Salta sin parar." Botones apilados con 12dp: "Jugar" (primario), "Práctica" (claro), "Ajustes" (secundario). Muestra "Récord N" bajo el título. Entrada: título cae con spring desde -40dp, botones en cascada con 80 ms.
2) SELECCIÓN DE DIFICULTAD (hoja inferior sobre el inicio, radio 32dp, surface): 3 tarjetas grandes de 72dp (Fácil / Normal / Difícil) con su BPM tope en caption y 1-3 estrellas pequeñas; tocar una inicia la cuenta atrás.
3) CUENTA ATRÁS: "3, 2, 1, ¡YA!" en 96sp Black centrado, escalando 1.4→1 por cada número sincronizado con 4 clics del metrónomo; el pad muestra el primer círculo de práctica sin penalización.
4) JUEGO: descrito arriba.
5) PAUSA (también al salir a segundo plano): velo #1A1033 al 85%, tarjeta centrada (surface, radio 28dp): "Pausa", botones "Seguir" (primario, con cuenta atrás de 3 tiempos), "Reiniciar" (claro) y "Salir" (secundario). Al volver de pausa o de otra app, la canción se reanuda en el punto exacto.
6) FIN DE PARTIDA: el mundo se congela y se oscurece al 60%; tarjeta que sube desde abajo (surface, radio 28dp): título "¡Fin de partida!", puntaje final 56sp Black con conteo animado de 0 al valor en 800 ms, "Nuevo récord" en pill dorada con confeti si aplica, estadísticas en 2 columnas (Mejor combo, Perfectos, Etapa alcanzada, Precisión %). Botones: "Otra vez" (primario) y "Menú" (secundario).
7) TUTORIAL (solo la primera vez, interactivo, 3 pasos con globos de texto 16sp sobre velo): 1 "Un obstáculo se acerca", 2 "Toca el círculo cuando el anillo lo alcance" (el juego se pausa suavemente hasta que lo toca), 3 "¡Sigue el ritmo!". Omitible.
8) CALIBRACIÓN: fondo bg, círculo grande de 120dp en el centro que pulsa a 100 BPM, texto "Toca cada vez que lo veas y escuches pulsar", contador 0/8, al final muestra "Tu ajuste: +42 ms" con botones "Guardar" y "Repetir".
9) AJUSTES: filas de 64dp en tarjeta surface radio 28dp: Música (slider), Efectos (slider), Vibración (switch), Reducir efectos (switch), Calibración de audio (abre la pantalla 8 + ajuste manual ±), Borrar récords (con diálogo), Acerca de (versión, licencia MIT y aviso "No está afiliado a ningún juego existente"). Switches propios: pista 52x32dp, activo accent, círculo blanco 24dp.

RENDIMIENTO
- Un único Canvas con bucle por withFrameNanos; objetos pre-asignados (Paths, Paints, pools de partículas máx. 120), sin asignaciones en el bucle de dibujo. Objetivo 60 fps en gama media, 120 en pantallas que lo permitan (declara preferredRefreshRate/ frame rate hint).
- El estado del marcador se lee solo dentro del DrawScope o en composables mínimos para evitar recomposiciones por frame.
