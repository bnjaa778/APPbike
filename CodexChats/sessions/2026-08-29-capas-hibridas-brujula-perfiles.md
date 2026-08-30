# Sesión 2026-08-29 — Capas híbridas, brújula y perfiles de rodada

## Objetivo

Añadir al mapa de APPbike una tercera capa Híbrido (satélite más calles), una
brújula interactiva y un selector visual de perfil para Ciclista, Bicicleta de
montaña y Bicicleta de gravel, tomando como referencia las capturas entregadas
sin crear una navegación principal nueva.

## Cambios realizados

- `MapScreen.kt` incorpora `HYBRID_MAP_STYLE` con World Imagery y etiquetas de
  Esri más `openmaptiles` de OpenFreeMap para carreteras, senderos y nombres de
  calle.
- La hoja `CAPAS DEL MAPA` ofrece Mapa, Satélite e Híbrido con estado activo,
  descripciones y una jerarquía visual alineada con la estética grafito/verde de
  APPbike.
- `MapCompassControl` lee el rumbo de la cámara, muestra norte/grados y centra
  el bearing a 0° al tocarlo.
- `CyclingModeSheet` ofrece los tres perfiles solicitados. La elección se guarda
  en `LocalDataStore`, se muestra sobre el mapa y se incluye en el panel de ruta;
  la velocidad del fallback directo usa el perfil seleccionado.
- Se actualizaron pruebas y documentación de producto.

## Verificación

- `:app:compileDebugKotlin`: correcto.
- `:app:testDebugUnitTest`: correcto.
- `:app:assembleDebug`: correcto.
- `:app:assembleDebugAndroidTest`: correcto.
- `:app:lintDebug`: correcto, con avisos preexistentes de toolchain y KTX.
- El JSON Híbrido pasó validación de sintaxis con `ConvertFrom-Json`.

## Pendientes

- No hay un dispositivo ADB conectado para validar la carga real de teselas,
  rotación, accesibilidad visual o el gesto completo sobre el mapa.
