# Estado actual de APPbike

Actualizado: 2026-07-22.

## Aplicación

- Android nativo con Jetpack Compose, paquete `com.example.appbike`.
- Entrada: `MainActivity.kt`.
- Destinos principales: Mapas, Bicicletas, Marketplace y Chat.
- API pública: `https://api.zizzio.cl/APIS/AppBikeExternal.php`.
- La sesión usa UUID como identidad interna y Bearer token cifrado con Android
  Keystore. `nombre_de_usuario` es únicamente la identidad visible.

## Funciones conectadas

### Cuenta

- Login por correo o nombre de usuario.
- `AccountSession.username` parsea `nombre_de_usuario`.
- Cuentas antiguas sin nombre consultan `user.get` y deben completar
  `user.username.update`.
- Cuenta muestra un acceso a ajustes si las notificaciones están desactivadas.

### Mapas y juntas

- MapLibre OpenGL con marcador fijo para la ubicación elegida.
- Solicitud inicial de ubicación, confirmación, corrección manual y sugerencias
  en vivo con Unicode.
- Historial de hasta ocho ubicaciones.
- Ubicación de Juntas independiente de Marketplace.
- Juntas activas con coordenadas; contenido propio se administra en el perfil.

### Marketplace

- Ubicación independiente y persistente.
- Búsqueda, recarga al arrastrar, indicador de carga y detalle a pantalla completa.
- Precio entero con símbolo no editable, agrupación de miles y moneda por país.
- Publicaciones y fotos conectadas al backend; nombres de vendedor visibles.

### Chat

- Tabs social y Marketplace.
- Caché separada por usuario/chat.
- Reparación completa al abrir y sincronización incremental cada tres segundos
  dentro de una conversación.
- Participantes y mensajes muestran `nombre_de_usuario` y
  `sender_nombre_de_usuario`.
- Arrastrar hacia abajo recarga la lista.

### Notificaciones

- `ChatNotificationListenerService` es un Foreground Service Android de tipo
  `remoteMessaging`.
- Mientras existe una sesión consulta cambios cada seis segundos aunque la
  actividad esté en segundo plano.
- En primer plano envía eventos a Compose y muestra un banner superior.
- En segundo plano publica una notificación de importancia alta con remitente y
  mensaje; tocarla abre el chat.
- Una notificación persistente de baja prioridad, “Escuchando mensajes nuevos”,
  mantiene visible y controlable el listener.
- WorkManager cada 15 minutos sigue como respaldo si el servicio fue retirado.
- Un `force-stop`, el botón “Detener” de Android o políticas agresivas del
  fabricante pueden detener cualquier listener local. FCM continúa siendo la
  solución futura para push inmediato administrado por servidor.

## Backend conocido

- Marketplace, Juntas y cuatro acciones de Chat están desplegadas.
- Backend devuelve nombres en sesión, contenido y Chat.
- UUID permanece en autorización y claves foráneas.
- Siguen pendientes geografía real, listados propios multirregionales, OAuth
  deportivo y FCM/tokens de dispositivo.

## Rutas importantes

- Código Kotlin: `app/src/main/java/com/example/appbike/`.
- APK de prueba: `app/build/outputs/apk/debug/app-debug.apk`.
- Contrato mapa/market/chat: `docs/MAP_MARKETPLACE_CHAT.md`.
- Informe backend: `docs/BACKEND_IMPLEMENTATION_REPORT.md`.
- Router completo: `AGENTS.md`.
- EasyMD centralizado: `CodexChats/EasyMD/`.

## Riesgos y pendientes

- Probar el listener con dos dispositivos reales y cuentas diferentes.
- El dispositivo debe conceder notificaciones y no detener manualmente el
  servicio persistente.
- El emulador disponible tenía un Bearer token antiguo; para pruebas remotas se
  debe iniciar sesión de nuevo.
- Implementar FCM en backend y Android cuando exista el contrato de tokens.
