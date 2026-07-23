# Registro de cambios de CodexChats

## 2026-07-22 - Centralización y listener persistente

Petición: preservar todo el contexto para continuar manualmente y corregir las
notificaciones que no llegaban fuera de la app.

Cambios:

- Se creó `CodexChats` con router, estado, flujo manual, documentación de
  notificaciones y sesiones fechadas.
- Fuente y manual de EasyMD se trasladaron a `CodexChats/EasyMD`.
- Se archivaron 14 tareas/respuestas Markdown del inbox, sin copiar secretos.
- Se copió y después recompiló el ejecutable vigente `EasyMD-auto5.exe` desde su
  nueva ruta; el build terminó con el warning histórico `CS0162` y su SHA-256
  quedó registrado.
- La copia de `dist` se mantiene porque había dos procesos EasyMD activos.
- El sondeo de Chat salió de Compose y pasó a
  `ChatNotificationListenerService`, Foreground Service `remoteMessaging`.
- Se agregó un bus interno para conservar el banner cuando la app está visible.
- Se conservaron WorkManager y su cursor independiente como respaldo.
- Se agregó acceso directo a ajustes cuando las notificaciones están apagadas.
- Se renovó la solicitud de permiso con la clave de migración
  `notifications_listener_v2_asked`.

Verificación:

- Build, tests unitarios y APK de tests instrumentados correctos.
- Listener confirmado como Foreground Service `remoteMessaging` después de
  enviar la actividad al inicio.
- Canales confirmados con importancia baja para estado y alta para mensajes.
- Sin excepciones fatales o de permisos de Foreground Service.
- Prueba cruzada pendiente porque solo estaba conectado el emulador y su token
  remoto conservado había expirado.

Pruebas y resultado final de la sesión se detallan en
`sessions/2026-07-22-listener-notificaciones.md`.
