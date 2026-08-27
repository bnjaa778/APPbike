# Sesión 2026-08-26 — Splash con logo estable

## Objetivo

Corregir el cambio de tamaño del logo entre la pantalla inicial y “Verificando
sesión…”, restaurando las estrellas blancas que convergen desde las esquinas.

## Cambios realizados

- Se definió un único tamaño `BRAND_LOGO_SIZE = 220.dp` para el splash y la
  validación de sesión.
- Se restauró la animación de 40 partículas blancas, con trayectorias curvas y
  giro alrededor del centro antes de converger al emblema.
- El logo conserva tamaño y posición fijos; solo cambia su opacidad durante la
  formación para evitar el salto visual.

## Verificación

- `:app:compileDebugKotlin`, pruebas unitarias y `:app:assembleDebug`: correctos.
- Capturas tomadas en el emulador durante el arranque: las partículas aparecen
  y el emblema queda centrado al completar la transición.

## Pendiente

La confirmación visual final en el Samsung requiere que el dispositivo esté
desbloqueado.
