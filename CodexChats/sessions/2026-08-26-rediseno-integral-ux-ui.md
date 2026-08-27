# Sesión 2026-08-26 - Rediseño integral UX/UI de APPbike

## Objetivo

Aplicar el brief de rediseño social, deportivo e inmersivo manteniendo la
arquitectura Compose, los flujos existentes y los contratos remotos actuales.

## Cambios

- `HomeScreen.kt` dejó de consultar y mostrar Marketplace. El Home ahora
  presenta un hero de aventura con cuatro imágenes editoriales locales, cambio
  automático cada 4 segundos y crossfade de 300 ms, seguido por historias y un
  feed comunitario de Juntas activas.
- Se añadió `SocialPost` y persistencia local aislada por UUID. Perfil permite
  seleccionar foto, editar bio y publicar una experiencia con foto o video; al
  volver a Inicio, esos posts se incorporan al feed sin mezclarse con ventas.
- `Account.kt` reorganiza Perfil como avatar, identidad, bio, resumen, garaje,
  rendimiento, sincronización pausada y contenido propio. La cabecera superior
  ya no tiene botón de Perfil.
- `MapScreen.kt` conserva el planificador interno y reduce `Trayecto`/`Junta` a
  acciones flotantes semitransparentes de menor peso visual.
- `LaunchBrandScreen` mantiene el logo a tamaño y posición constantes; solo
  aplica fade-out antes de pasar a la app.
- Se actualizaron `AGENTS.md`, `docs/PROJECT_REPORT.md`,
  `docs/MAP_MARKETPLACE_CHAT.md`, `docs/UX_UI_REDESIGN_PLAN.md`,
  `CodexChats/CURRENT_STATE.md` y `CodexChats/CHANGELOG.md`.

## Pruebas

- `:app:compileDebugKotlin`: correcto.
- `:app:testDebugUnitTest`: 59/59 correctas.
- `:app:assembleDebug`, `:app:assembleDebugAndroidTest` y `:app:lintDebug`:
  correctos.
- `:app:connectedDebugAndroidTest`: 38 casos, 0 fallos y 2 omisiones externas
  esperadas de Chat/notificaciones.
- AVD `APPbike_API_35`: instalación correcta, splash y pantalla de acceso
  inspeccionados visualmente, sin `FATAL EXCEPTION` ni ANR en logcat reciente.
- `git diff --check`: correcto.

## Pendientes

- El backend aún no tiene contrato para publicaciones sociales. La primera
  versión local no sincroniza posts entre dispositivos; no se inventó un
  endpoint para ocultar esa limitación.
- Falta validar el compositor y reproducción de video en un teléfono físico
  desbloqueado con contenido real.

## Siguiente paso

Definir el contrato remoto de publicaciones sociales (creación, listado,
propiedad y multimedia) y migrar el almacenamiento local solo cuando ese
contrato esté desplegado y probado.
