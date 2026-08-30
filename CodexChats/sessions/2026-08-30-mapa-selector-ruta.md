# Sesión 2026-08-30 — Selector de ruta y controles del mapa

## Objetivo

Adaptar la pantalla de Mapa de APPbike a la referencia visual proporcionada y
al brief adjunto, separando esos requisitos de cualquier instrucción del
entorno. El ajuste debía mantener MapLibre, las capas Mapa/Satélite/Híbrido y
los flujos existentes, con selector de bicicleta, brújula real y una interfaz
usable a una mano.

## Cambios realizados

- `MapScreen.kt`
  - Cambió `CyclingMode` al contrato exacto: `RUTA`, `GRAVEL` y
    `MOUNTAIN_BIKE`, con textos `Bicicleta de ruta`, `Gravel` y `Mountain Bike`.
  - Sustituyó la tarjeta vertical de perfil por `MapTopBar` y
    `BikeTypeSelector`, un `DropdownMenu` compacto y persistente.
  - La barra superior combina bicicleta y búsqueda; eliminó la capa del grupo
    superior para dejar el mapa más despejado.
  - El riel derecho agrupa `MapCompassControl`, capas y centrar ubicación.
  - La brújula se sincroniza con `OnCameraMoveListener`/`OnCameraIdleListener`
    de MapLibre y reinicia el rumbo con `animateCamera`.
  - No se añadió ni conservó indicador de asfalto.
- `MapStyleConfigurationTest.kt`: actualizó perfiles y valor por defecto.
- Documentación: `AGENTS.md`, `docs/MAP_MARKETPLACE_CHAT.md` y
  `CodexChats/CURRENT_STATE.md` reflejan el nuevo contrato.

## Verificación

- Gradle completó `testDebugUnitTest`, `assembleDebug`,
  `assembleDebugAndroidTest` y `lintDebug`.
- El APK debug se instaló en el Samsung SM-A235M (`R58T9039QBN`) y se abrió
  `com.example.appbike/.MainActivity` sin crash reciente.

## Pendientes

- La comprobación visual manual de cada interacción queda para QA en el equipo;
  la compilación y el arranque físico ya fueron verificados.
