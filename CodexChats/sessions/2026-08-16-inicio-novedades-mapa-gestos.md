# Sesión 2026-08-16 - Inicio, Novedades, mapa y gestos

## Objetivo

Implementar cinco cambios coordinados: destino de arranque dependiente de la
sesión, Inicio con Novedades tipo historias/feed, mapa con controles compactos,
límite de zoom satelital, navegación horizontal y ejecución en teléfono.

## Decisiones

- `HOME` es nuevamente un destino principal. `ROUTES` se mantiene como mapa
  secundario abierto desde Inicio y se considera seleccionado bajo la misma
  entrada inferior.
- Una cuenta se considera lista para Inicio cuando existe una sesión local
  válida, `nombre_de_usuario` no está vacío y `user.get` acepta el token. Un
  rechazo inequívoco limpia sesión; fallos transitorios de red/servidor no.
- Novedades no consume las listas semilla históricas: consulta en
  `Dispatchers.IO` las juntas y publicaciones activas del radio/región actual.
- El gesto principal no se instala en `ROUTES`, evitando competir con el paneo
  de MapLibre.
- El máximo de cámara satelital es 17. Aunque ambas fuentes Esri declaran
  `maxzoom: 19`, la validación en Puerto Varas descargó una tesela de fotografía
  correcta en 18 y `Map data not yet available` en 19; el gesto anclado tambien
  alcanzó teselas vecinas grises en 18. El margen de dos niveles evita mostrar
  esos sectores problemáticos.

## Archivos principales

- `app/src/main/java/com/example/appbike/MainActivity.kt`
- `app/src/main/java/com/example/appbike/HomeScreen.kt`
- `app/src/main/java/com/example/appbike/MapScreen.kt`
- `app/src/main/java/com/example/appbike/CommonComponents.kt`
- `app/src/main/res/drawable/ic_nav_home.xml`
- `app/src/test/java/com/example/appbike/AppNavigationTest.kt`
- `app/src/test/java/com/example/appbike/MapStyleConfigurationTest.kt`
- `AGENTS.md`, `docs/MAP_MARKETPLACE_CHAT.md`, `docs/PROJECT_REPORT.md`
- `CodexChats/CURRENT_STATE.md`, `CodexChats/CHANGELOG.md`

## Validación

- Compilación Kotlin correcta.
- 56 pruebas unitarias, sin fallos ni omisiones.
- Lint correcto, 0 errores y 4 avisos informativos de versiones/launcher.
- `assembleDebug` correcto; APK final de 62.928.018 bytes.
- Instalación con `adb install -r` correcta en `SM-A235M` (`R58T9039QBN`),
  preservando sesión y preferencias.
- El arranque abrió Novedades para la sesión válida. Se validaron mediante ADB
  los swipes Inicio → Bicicletas → Marketplace y, en Mapas, los controles
  compactos, la expansión semitransparente y el menú de capas.
- El estrés de doble toque reprodujo teselas grises con topes 19 y 18. La fuente
  directa confirmó fotografía en una tesela 18 y ausencia en 19; sectores
  vecinos justificaron el tope conservador 17 del APK final.
- No hubo `FATAL EXCEPTION`, ANR ni fallo de carga nativa de MapLibre.
- La sesión guardada del Samsung respondió `token de acceso no es valido o
  expiro`. Tras instalar la validación de arranque, la app eliminó las entradas
  de identidad sin exponer valores (`user_id_entries=0`, `email_entries=0`),
  mantuvo el proceso activo y dejó Cuenta como destino.

## Evidencias

- `app/build/outputs/goal-2026-08-16/home.png`
- `app/build/outputs/goal-2026-08-16/swipe-bike.png`
- `app/build/outputs/goal-2026-08-16/swipe-market.png`
- `app/build/outputs/goal-2026-08-16/map-compact.png`
- `app/build/outputs/goal-2026-08-16/map-layers.png`

La prueba instrumentada aislada del límite compila. Su ejecución iniciada con
la pantalla ya bloqueada agotó 15 segundos antes de crear el `MapView`; fue un
bloqueo de ciclo de vida/ventana (`NotificationShade` enfocada), no una caída ni
una aserción incorrecta. El teléfono se apagó también justo antes de la captura
posterior al último estrés, aunque el proceso final siguió vivo.

## Pendientes

No quedan cambios de implementación. Es opcional repetir la captura del zoom 17
con el teléfono desbloqueado y la pantalla activa.
