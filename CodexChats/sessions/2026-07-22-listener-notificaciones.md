# Sesión 2026-07-22 - Listener de notificaciones

## Objetivo

Hacer que los mensajes generen avisos al dejar APPBIKE en segundo plano y dejar
un relevo completo para trabajo tradicional.

## Diagnóstico

El sondeo rápido dependía de `AppBikeApp`. Al pausar la actividad podía dejar de
ejecutarse. El Worker de respaldo sí sobrevivía, pero Android no permite periodos
menores a 15 minutos para trabajo periódico.

## Implementación

- Servicio persistente `ChatNotificationListenerService`.
- Tipo Android `remoteMessaging` y permisos específicos.
- Ciclo de seis segundos en `Dispatchers.IO`.
- `START_STICKY` para recreación por el sistema.
- Notificación persistente de estado y canal separado de baja importancia.
- Canal de mensajes de importancia alta.
- Bus `SharedFlow` para avisos dentro de la app.
- Inicio al activar sesión; detención al cerrar sesión.
- WorkManager se conserva como segunda capa.
- Acceso a ajustes desde Cuenta si el permiso está deshabilitado.

## Verificación realizada

- `:app:testDebugUnitTest`: correcto.
- `:app:assembleDebug`: correcto.
- `:app:assembleDebugAndroidTest`: correcto.
- Cuatro pruebas instrumentadas correctas: notificación con remitente/mensaje y
  parsers de Marketplace, participantes y `sender_nombre_de_usuario`.
- APK instalado con `install -r` en `emulator-5554`.
- Permiso `POST_NOTIFICATIONS` concedido para la prueba.
- Servicio confirmado antes y después de presionar Inicio:
  `isForeground=true`, `foregroundId=41002`, tipo `0x00000200`
  (`remoteMessaging`).
- Pantalla activa después de Inicio: Nexus Launcher; APPbike permaneció con PID
  y servicio foreground activos.
- Canal `appbike_message_listener`: importancia 2, silencioso, sin badge.
- Canal `appbike_new_messages`: importancia 4, sonido y vibración habilitados.
- No aparecieron `FATAL EXCEPTION`, `SecurityException` ni
  `ForegroundServiceStartNotAllowedException`.

Solo estaba conectado `emulator-5554`. No fue posible repetir todavía el envío
real con dos cuentas/dispositivos. Además, la sesión preservada en el emulador
tenía un Bearer token vencido, por lo que la validación de red necesita un login
nuevo y el segundo dispositivo conectado.

## Pendiente backend

Diseñar registro/revocación de tokens y envío FCM para cubrir reinicios,
`force-stop` y dispositivos con políticas agresivas de batería.
