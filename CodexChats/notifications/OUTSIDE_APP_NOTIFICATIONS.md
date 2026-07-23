# Notificaciones fuera de APPBIKE

## Problema corregido

La primera implementación ejecutaba el sondeo de Chat desde un `LaunchedEffect`
de `AppBikeApp`. Al quedar la actividad en segundo plano Android podía suspender
ese trabajo. WorkManager continuaba, pero su intervalo mínimo de 15 minutos no
servía como recepción inmediata.

## Arquitectura actual

```text
sesión activa
    -> MainActivity inicia ChatNotificationListenerService
    -> Foreground Service remoteMessaging
    -> chat.list cada 6 s
    -> cursor independiente por userId/chatId
       -> app visible: SharedFlow -> banner Compose
       -> app no visible: canal de mensajes -> notificación Android
```

Archivos:

- `ChatNotifications.kt`: servicio, bus, canales, repositorio y Worker.
- `MainActivity.kt`: arranque/parada por sesión y consumo del bus.
- `LocalDataStore.kt`: `chat_notification_sync:{userId}:{chatId}`.
- `AndroidManifest.xml`: servicio y permisos de Foreground Service.

## Permisos y canales

- `POST_NOTIFICATIONS`: solicitado en Android 13+.
- `FOREGROUND_SERVICE`: permiso base.
- `FOREGROUND_SERVICE_REMOTE_MESSAGING`: requerido por targetSdk 36.
- `appbike_new_messages`: importancia alta; nombre y mensaje.
- `appbike_message_listener`: importancia baja, silencioso y persistente.

Si el usuario desactiva notificaciones, Cuenta muestra “Abrir ajustes”. El
listener puede seguir consultando, pero Android no permite mostrar el aviso de
mensaje hasta reactivar el permiso/canal.

## Persistencia y deduplicación

La primera sincronización fija una línea base y no reproduce mensajes antiguos.
Después compara `lastMessageId`, cantidad y versión. Solo notifica mensajes cuyo
`senderId` es distinto del usuario activo. El cursor de notificaciones es
independiente del cursor que usa la conversación visible.

## Límites reales

- El servicio se inicia desde una actividad visible, como exige Android 12+.
- `START_STICKY` permite que Android intente recrearlo si mata el proceso por
  memoria.
- Cerrar la pantalla o deslizar la tarea no solicita detener el servicio.
- `force-stop`, “Detener” desde el administrador de tareas, reinicio sin abrir la
  app o ahorro de batería agresivo pueden impedir el listener local.
- FCM sigue siendo necesario para entrega push garantizada desde backend, sobre
  todo después de reiniciar o forzar detención.

## Diagnóstico

1. Comprobar que existe sesión válida y Bearer token vigente.
2. Confirmar permiso de notificaciones.
3. Ver la notificación persistente del listener.
4. Revisar `dumpsys activity services com.example.appbike`.
5. Revisar Logcat por `SecurityException`,
   `ForegroundServiceStartNotAllowedException`, 401 o token expirado.
6. Probar con dos cuentas y verificar que el mensaje aparece en `chat.list` y
   `chat.messages.list` del receptor.
