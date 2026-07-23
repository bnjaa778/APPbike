# Mapa, Marketplace y Chat

## Archivos

- `AppModels.kt`: `GeoPoint`, `MeetupEvent`, `UserChat`, `StoredMessage`,
  `ChatMessagePage`, `ChatSyncMetadata` y campos remotos de
  `ProductPublication`.
- `LocalDataStore.kt`: ubicación, chats, mensajes y metadata por usuario.
- `RemoteConnections.kt`: contratos HTTP y parsers.
- `MapScreen.kt`: mapa, búsqueda, selección y creación de juntas.
- `MarketplaceScreen.kt`: búsqueda, grid, fotos y creación.
- `ChatScreen.kt`: tabs, listado, conversación y sincronización.
- `ChatNotifications.kt`: detección global, aviso dentro de la app, canal Android
  y comprobación diferida con WorkManager.
- `MainActivity.kt`: conexión de las pantallas con la sesión.
- `DeviceLocationProvider.kt`: permiso y lectura puntual de la ubicación Android.
- `docs/LOCATION_BACKEND_HANDOFF.md`: contrato detallado para API externa,
  API interna, tablas, consultas geográficas y datos iniciales.

## Mapa y juntas regionales

La pantalla mantiene MapLibre Native OpenGL con el estilo Liberty de
OpenFreeMap. El marcador verde representa la ubicación local guardada y no se
mueve al desplazar el mapa. Las juntas con coordenadas válidas usan marcador
azul y abren su detalle al tocarlas.

El onboarding y la corrección de ubicación no cambian: permisos Android en el
primer ingreso, confirmación antes de persistir y sugerencias en vivo. La capa
remota intenta primero `location.search`/`location.reverse`; mientras esas
acciones no existan usa `android.location.Geocoder` y Nominatim como respaldo
temporal con cache, rate limit y timeout.
El diálogo muestra hasta ocho ubicaciones recientes antes de escribir. El
historial es compartido para ofrecer accesos rápidos, pero seleccionar un lugar
solo actualiza la ubicación activa de Mapas o de Marketplace según el origen
del diálogo.

El selector manual no usa una ventana `AlertDialog`: es un overlay Compose
dentro de la misma pantalla para que MapLibre, el teclado y la selección no
dejen una ventana huérfana sin foco. La entrada declara idioma `es-CL`, conserva
Unicode (`Viña del Mar`, por ejemplo), busca tras 450 ms de pausa y cancela la
consulta anterior cuando cambia el texto. El `Geocoder` local tiene un límite
de 3 s; si no responde o devuelve vacío, la misma búsqueda en vivo usa
automáticamente Nominatim con límite de 8 s, sin esperar Enter. Al tocar una
sugerencia se oculta el teclado y se libera el foco antes de guardar y retirar
el overlay.

La API de comunidad ya está desplegada y todas sus acciones se envían por
`POST` JSON a `AppBikeExternal.php`:

- `junta.list`: requiere `region`; admite `junta_status`, `limit` y `offset`.
- `junta.get`: requiere `junta_id` regional.
- `junta.create`: requiere `region`, `user_id`, `location`, `title` y
  `description`; la app envía `junta_status=activa`.
- `junta.status.update`: requiere `junta_id` y `junta_status`.
- `junta.complete`: requiere `junta_id` y mueve la junta a `pasada`.
- `junta.photo.upload`: multipart con `junta_id` y campo `foto`.
- `junta.photo.get`: requiere juntos `junta_id` y `photo_id`.

El mapa público solicita exclusivamente `junta_status=activa` y no presenta
selectores de estado. Cuando el propietario marca una junta como realizada,
la app refresca la lista y esa junta deja de mostrarse. El contrato para listar
juntas `pasada` se conserva para el futuro historial del perfil del creador,
donde podrá consultar sus juntas anteriores y sus detalles.

Para conservar coordenadas dentro del campo de backend existente, las juntas
creadas por Android usan:

```text
REGION:<latitud>,<longitud>|<etiqueta legible>
```

Ejemplo: `LAS:-40.574401,-73.114802|Punto de encuentro en Osorno`. El parser
acepta este formato y aplica el radio local de 40 km. Registros regionales
antiguos sin coordenadas siguen siendo válidos en la API, pero no pueden
dibujarse como punto en el mapa.

Los IDs `LAS-...`/`LAN-...` se conservan sin recalcular región en operaciones de
detalle, estado o fotos. Si la ubicación no contiene un prefijo regional, la
integración actual usa el valor de backend `LAS`.

## Marketplace regional

Marketplace público solicita exclusivamente `publication_status=activa`,
mantiene búsqueda local sobre la respuesta y filtra a 40 km cuando el registro
contiene coordenadas codificadas. Los filtros `activa`, `pausada`, `vendida` y
`en_revision` se retiran de esta vista, pero sus acciones remotas se conservan
para la futura administración de publicaciones propias desde el perfil.

Al entrar a la pantalla o ejecutar una búsqueda se muestra un indicador circular
superpuesto durante un mínimo de 450 ms. Cada recarga tiene un identificador;
una respuesta anterior no puede sobrescribir resultados más nuevos ni apagar
prematuramente el indicador de la solicitud vigente.

La grilla admite actualización al arrastrar hacia abajo. Ese gesto conserva las
tarjetas mientras consulta y usa el indicador superior de `PullToRefreshBox`.
Al tocar una tarjeta, el detalle ocupa toda la ventana: imagen principal grande,
precio, título, estado, descripción y ubicación dentro de contenido desplazable,
con contacto o administración fijados al pie. El diálogo compacto anterior ya no
forma parte del flujo.

La ubicación de Marketplace se persiste en `marketplace_location`. En el primer
uso toma una copia de `selected_location`, si existe; luego el botón de ubicación
de Marketplace puede cambiarla sin modificar el mapa de Juntas, y viceversa.

El formulario usa un selector cerrado para `product_status`: `nuevo`, `usado`
o `reacondicionado`. El precio conserva únicamente dígitos, muestra un prefijo
monetario no editable y agrupa miles según la moneda derivada de la ubicación.
Así, `10.000` se envía como el entero `10000`. La UI muestra, entre otras
equivalencias, `$ ... CLP` para Chile y `$ ... ARS` para Argentina. Android ya
envía y parsea `currency`; el backend desplegado todavía no lo conserva. Los
registros antiguos usan CLP como fallback y la migración requerida está en el
informe de backend.

Acciones usadas:

- `marketplace.list`: `region`, `publication_status`, `limit`, `offset`.
- `marketplace.get`: `publication_id`.
- `marketplace.create`: `region`, `user_id`, `location`, `title`,
  `description`, `price`, `product_status`, `publication_status`.
- `marketplace.status.update`: `publication_id`, `publication_status`.
- `marketplace.photo.upload`: multipart con `publication_id` y campo `foto`.
- `marketplace.photo.get`: requiere juntos `publication_id` y `photo_id`.

La creación es obligatoriamente de dos pasos: primero JSON
`marketplace.create`; después, si se eligió imagen, multipart
`marketplace.photo.upload` con el ID regional recibido. El detalle permite al
propietario cambiar estado y agregar más fotos. Cada operación recarga detalle
y listado.

El listado desplegado todavía devuelve `publications` sin fotos. Android no hace
consultas N+1: espera `photo_id`/`cover_photo_id` en cada elemento y muestra un
placeholder mientras el backend no entregue la portada. El detalle devuelve
`publication` y su array `photos`. Las referencias internas conservan el ID padre:

```text
appbike-market-photo://<publication_id>/<photo_id>
appbike-junta-photo://<junta_id>/<photo_id>
```

No exponer `photo_folder_path` ni `file_path` como URL pública. La descarga usa
`content_base64` desde la acción `photo.get` correspondiente.

## Contenido propio en Cuenta

`ProfileContent.kt` muestra publicaciones propias separadas en `activa`,
`pausada`, `vendida` y `en_revision`, y juntas propias actuales/anteriores. El
detalle permite editar, mover estado, completar, agregar fotos y eliminar. La
carga intenta `marketplace.mine.list` y `junta.mine.list`; mientras no existan,
consulta los listados de la región activa y filtra estrictamente por `user_id`.
El fallback no reemplaza el requisito multirregional del backend.

## Chats

### Identidad visible

El backend mantiene el UUID como identificador y autorización, pero la UI usa
`nombre_de_usuario` como identidad visible. Android acepta el campo nullable en
sesiones, Marketplace, Juntas, participantes y mensajes. Los mensajes usan
`sender_nombre_de_usuario`; los participantes conservan el mapa UUID/nombre solo
para resolver la presentación. Un registro antiguo sin nombre muestra
`Usuario de APPBIKE`, nunca el UUID.

Login acepta correo o nombre de usuario. Si `login` o `user.get` devuelve
`nombre_de_usuario=null`, Cuenta exige elegir uno mediante
`user.username.update`. La validación local replica el contrato remoto: 3 a 30
caracteres ASCII, inicio alfanumérico y resto alfanumérico, punto, guion o guion
bajo. La unicidad final siempre la decide el backend.

Tabs:

- `SOCIAL`: juntas, amigos y grupos.
- `MARKETPLACE`: conversaciones asociadas a publicaciones.

Flujo:

1. Sin sesión no se consulta ni muestra información.
2. Con sesión se carga primero el listado local.
3. Se solicita el listado remoto y se actualiza la caché.
4. Arrastrar la lista hacia abajo repite `chat.list` y conserva la caché visible.
5. Al abrir un chat se pintan primero los mensajes locales.
6. Inmediatamente se pide la primera página completa, sin
   `after_message_id`, para reparar huecos históricos. El cursor no se envía con
   cadena vacía porque el backend responde `after_message_id no es valido`.
7. Con la conversación abierta se consulta cada 3 segundos usando el último ID;
   los mensajes nuevos del otro usuario aparecen sin cerrar ni reabrir.
8. Las páginas se combinan sin duplicados y se guardan mensajes, cantidad,
   versión y último ID después de descargar o enviar.

### Notificaciones de mensajes

- Con sesión activa, `ChatNotificationListenerService` mantiene un Foreground
  Service `remoteMessaging`, revisa `chat.list` cada 6 segundos y descarga solo
  los mensajes posteriores al cursor de notificación aunque la actividad quede
  en segundo plano.
- El cursor `chat_notification_sync:{userId}:{chatId}` es independiente de la
  caché visible del chat. La primera observación establece una línea base y no
  notifica mensajes históricos.
- En primer plano aparece una tarjeta animada en la parte superior durante 5
  segundos con `nombre_de_usuario` y el contenido. El servicio entrega el evento
  a Compose mediante `ChatNotificationEventBus`; al tocarlo abre el chat.
- Fuera de la app se publica en el canal `appbike_new_messages`, de importancia
  alta, con el mismo nombre y mensaje. Tocar la notificación abre la conversación.
- Android 13 o posterior solicita `POST_NOTIFICATIONS` una vez por instalación.
- El canal silencioso `appbike_message_listener` muestra la notificación
  persistente “Escuchando mensajes nuevos”. Android requiere que el listener sea
  visible mientras trabaja fuera de la app.
- WorkManager conserva una comprobación con red cada 15 minutos cuando Android
  retira el servicio. Ese es el mínimo permitido por el planificador periódico.
- La entrega inmediata con el proceso totalmente cerrado requiere que backend
  registre tokens de dispositivo y envíe push mediante FCM. Hasta contar con ese
  contrato, el tiempo real se garantiza mientras el proceso está vivo y el modo
  cerrado queda sujeto al servicio o a la ventana de WorkManager. `force-stop`,
  reinicio sin abrir APPbike o “Detener” desde Android requieren FCM para una
  entrega garantizada.

El parser admite las variantes desplegadas `type`/`chat_type`, participantes
como IDs o como objetos, `last_message` como texto u objeto y metadata de página.
Una prueba real con dos cuentas encontró un caché local con IDs altos que ocultaba
mensajes anteriores; la sincronización completa inicial es deliberada y no debe
reemplazarse por `after={local.lastId}`.

Acciones POST esperadas:

- `chat.get_or_create`
- `chat.list`
- `chat.messages.list`
- `chat.message.send`

Android conserva las rutas REST anteriores como fallback temporal para listar,
descargar y enviar. Desde el detalle de una junta o publicación llama
`chat.get_or_create` y abre directamente la conversación devuelta.

Metadata local:

- último ID de mensaje
- cantidad de mensajes
- versión remota
- instante de última sincronización
- cursor separado para notificaciones

## Estado y pendientes de backend

- Marketplace y Juntas regionales están desplegados en
  `AppBikeCommunity.php` y accesibles mediante la allowlist pública.
- Existen una publicación activa regional y una junta activa con coordenadas
  codificadas para validación desde Android.
- El backend todavía no ofrece columnas geográficas ni consulta de distancia;
  Android codifica coordenadas en `location` y filtra el radio localmente.
- Antes de escalar debe definirse el código regional a partir de ubicación en
  el servidor, devolver latitud/longitud como campos propios y aplicar consulta
  geoespacial paginada.
- La geocodificación aún debe migrarse a un proveedor controlado por backend.
- Las acciones y tablas de Chat ya responden con Bearer auth. Los listados propios
  multirregionales y OAuth deportivo continúan pendientes.
- Chat ya devuelve nombres visibles. Sigue pendiente un contrato FCM de registro,
  revocación de token y push para mensajes nuevos con la app totalmente cerrada.
- El listado comunitario expone `photo_folder_path`; debe retirarse de la API
  pública y reemplazarse por `photo_id` de portada.

La especificación operativa completa está en
`docs/BACKEND_IMPLEMENTATION_REPORT.md`.
