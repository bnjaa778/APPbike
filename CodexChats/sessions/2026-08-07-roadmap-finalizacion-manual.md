# Informe de sesión — roadmap manual de finalización

Fecha: 2026-08-07

## Objetivo

Crear un roadmap para que un equipo humano pueda terminar APPbike sin depender
de IA ni de información retenida en una conversación.

## Evidencia revisada

- `AGENTS.md` completo: arquitectura, contratos e invariantes de Android.
- `CodexChats/CURRENT_STATE.md` y `docs/PROJECT_REPORT.md`: estado técnico,
  validaciones realizadas y pendientes externos.
- `docs/BACKEND_IMPLEMENTATION_REPORT.md`: seguridad, PostGIS, listados propios,
  Chat, FCM/OAuth y pruebas de aceptación del servidor.
- `docs/MAP_MARKETPLACE_CHAT.md` y
  `CodexChats/notifications/OUTSIDE_APP_NOTIFICATIONS.md`: comportamiento actual
  de comunidad, caché, listener y límites de las notificaciones locales.
- Código: `RemoteConnections.kt` ya contiene clientes para `location.reverse`,
  listados propios, Chat y deporte; no demuestra que los contratos remotos estén
  completos. No existe directorio `.github` y la aplicación continúa en versión
  `1`/`1.0`.

## Resultado

Se creó `docs/ROADMAP_FINALIZACION_MANUAL.md`. El plan establece:

- versión 1.0 cerrada por fases 0–6, con deporte deliberadamente fuera del
  alcance mientras siga pausado;
- autenticación y autorización antes de migraciones de funciones privadas;
- geografía/moneda/portadas y contenido propio antes de retirar fallbacks;
- verificación de Chat con dos cuentas y FCM como requisito para push con la
  aplicación cerrada;
- CI, matriz QA, firma, beta y rollback como requisitos de publicación.

## Próximo paso humano

El responsable de producto y el líder técnico deben abrir, asignar y estimar las
issues de la fase 0. No se debe activar OAuth ni inventar acciones de registro,
recuperación o FCM antes de aprobar sus contratos de backend.
