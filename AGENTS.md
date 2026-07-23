# APPbike - Router de trabajo para agentes

Este archivo es la entrada principal para trabajar en APPbike. Usalo como
router: antes de editar, identifica el tipo de tarea, abre primero los archivos
indicados y conserva los contratos documentados aqui.

Ultima revision del proyecto: 2026-07-22.

## Regla de oro

- No cambies flujos por intuicion. Lee el archivo principal de la ruta antes de
  editar.
- Mantener las solicitudes de red fuera del hilo principal usando coroutines y
  `Dispatchers.IO`.
- No revertir cambios existentes no relacionados.
- No crear navegacion principal nueva dentro de pantallas hijas.
- Si cambias un contrato remoto, actualiza juntos `AppModels.kt`,
  `RemoteConnections.kt`, la pantalla afectada y este archivo.
- Si cambias mapa, marketplace o chat, revisa tambien
  `docs/MAP_MARKETPLACE_CHAT.md`.
- Después de cada petición nueva, registra objetivo, cambios, pruebas, pendientes
  y siguiente paso en `CodexChats/CHANGELOG.md`; las sesiones sustanciales llevan
  además un informe fechado en `CodexChats/sessions/`.

## Estado actual de la app

- App Android nativa con Jetpack Compose.
- Paquete principal: `com.example.appbike`.
- Archivos Kotlin principales:
  `app/src/main/java/com/example/appbike/`.
- Entrada Android: `MainActivity`.
- Tema Compose: `ui/theme/Theme.kt`.
- Navegacion principal con barra inferior persistente.
- Orden de barra inferior: Mapas, Bicicletas, Marketplace, Chat.
- Cuenta y sincronizacion deportiva estan en una misma pantalla accesible desde
  el boton de perfil de la cabecera.
- API publica base: `https://api.zizzio.cl/APIS/AppBikeExternal.php`.
- Las cuentas usan UUID en texto.
- Las bicicletas se cargan por `user_id`.
- El detalle de bicicleta se consulta por `bike_id` antes de mostrar dialogo.
- Las mantenciones se cargan por separado dentro del detalle.
- Fotos de bicicletas y marketplace se descargan por acciones internas que
  devuelven `content_base64`.

## Router rapido

| Si la tarea trata de... | Abrir primero | Revisar despues |
|---|---|---|
| Navegacion, cabecera o barra inferior | `MainActivity.kt` | `AppModels.kt`, `CommonComponents.kt` |
| Destinos o enum de pantallas | `AppModels.kt` | `MainActivity.kt` |
| Login, sesion o logout | `Account.kt` | `RemoteConnections.kt`, `MainActivity.kt` |
| Sincronizacion deportiva | `Account.kt` | `SecureTokenStore.kt`, `MainActivity.kt`, `RemoteConnections.kt` |
| Listado Mis bicicletas | `BikesScreen.kt` | `RemoteConnections.loadUserBikes`, `AppModels.kt` |
| Crear bicicleta | `BikesScreen.kt` | `RemoteConnections.registerBike`, backend `AppBikeInternal.php` |
| Detalle de bicicleta | `BikesScreen.kt` | `RemoteConnections.loadBikeDetails` |
| Fotos de bicicletas | `BikesScreen.kt` | `RemoteConnections.loadBikePhoto`, backend `AppBikeInternal.php` |
| Mantenciones pasadas | `BikesScreen.kt` | `RemoteConnections.loadMaintenance`, `createMaintenanceReminder` |
| Servicios futuros | `BikesScreen.kt` | `RemoteConnections.bookService` |
| Mapa, ubicacion o juntas | `MapScreen.kt` | `DeviceLocationProvider.kt`, `LocalDataStore.kt`, `RemoteConnections.kt`, `docs/MAP_MARKETPLACE_CHAT.md`, `docs/LOCATION_BACKEND_HANDOFF.md` |
| Marketplace | `MarketplaceScreen.kt` | `MarketplacePricing.kt`, `RemoteConnections.kt`, `LocalDataStore.kt`, `docs/MAP_MARKETPLACE_CHAT.md` |
| Contenido propio del perfil | `ProfileContent.kt` | `Account.kt`, `RemoteConnections.kt`, `docs/BACKEND_IMPLEMENTATION_REPORT.md` |
| Fotos de Marketplace | `MarketplaceScreen.kt` | `RemoteConnections.loadMarketplacePhoto` |
| Chats | `ChatScreen.kt` | `LocalDataStore.kt`, `RemoteConnections.kt`, `docs/MAP_MARKETPLACE_CHAT.md` |
| Notificaciones de chat | `ChatNotifications.kt` | `MainActivity.kt`, `LocalDataStore.kt`, `AndroidManifest.xml`, `CodexChats/notifications/OUTSIDE_APP_NOTIFICATIONS.md` |
| Cache local | `LocalDataStore.kt` | pantalla consumidora |
| Modelos o campos JSON | `AppModels.kt` | parser correspondiente en `RemoteConnections.kt` |
| Errores de red o API | `RemoteConnections.kt` | `userFriendlyError` y pantalla afectada |
| Tema visual | `ui/theme/Theme.kt` | `ui/theme/Color.kt`, `ui/theme/Type.kt`, `res/values/themes.xml` |
| Permisos Android | `AndroidManifest.xml` | `app/build.gradle.kts` |
| Compilacion o dependencias | `app/build.gradle.kts` | `gradle/libs.versions.toml`, `local.properties` |
| Documentacion de proyecto | `AGENTS.md` | `docs/PROJECT_REPORT.md`, `docs/MAP_MARKETPLACE_CHAT.md` |
| Continuidad manual e historial Codex | `CodexChats/README.md` | `CodexChats/CURRENT_STATE.md`, `CodexChats/CHANGELOG.md` |
| Puente Codex EasyMD | `CodexChats/EasyMD/AGENTS.md` | `CodexChats/EasyMD/source/EasyMDGui.cs`, `CodexChats/EasyMD/EASYMD_HANDOFF.md` |

## Arquitectura actual

### `MainActivity.kt`

- `MainActivity.onCreate` activa edge-to-edge y renderiza `APPbikeTheme`.
- `AppBikeApp` mantiene el estado raiz:
  - `currentScreen`
  - `accountSession`
  - `bikes`
  - `reminders`
  - `bookings`
  - `platforms`
- `Scaffold` principal es duenio de cabecera y barra inferior.
- La cabecera muestra marca y boton de perfil.
- La barra inferior solo debe contener:
  - `AppScreen.ROUTES` como "Mapas"
  - `AppScreen.BIKES`
  - `AppScreen.MARKETPLACE`
  - `AppScreen.CHAT`
- `ACCOUNT` es flujo secundario accesible desde la cabecera.
- `HOME`, `SYNC` y `CREATE_PUBLICATION` existen como compatibilidad/alias, pero
  no son destinos principales activos.
- Hay listas semilla de productos/rutas/juntas/mensajes que hoy no alimentan
  las pantallas conectadas; tratarlas como residuo historico, no como fuente de
  datos.

### `AppModels.kt`

Contiene modelos simples usados por UI y capa remota:

- `AccountSession`
- `Bike`
- `ProductPublication`
- `MaintenanceReminder`
- `ServiceBooking`
- `BikeMaintenanceData`
- `RoutePost`
- `RideMeetup`
- `ChatMessage`
- `GeoPoint`
- `MeetupEvent`
- `UserChat`
- `StoredMessage`
- `ChatMessagePage`
- `ChatSyncMetadata`
- `ChatNotificationSyncMetadata`
- `SyncPlatform`

Regla: si el backend cambia un campo, actualiza el modelo y el parser al mismo
tiempo.

### `RemoteConnections.kt`

Capa remota centralizada con `HttpURLConnection` y `org.json`.

- JSON POST al `API_URL` para cuenta, bicicletas, mantenciones, Marketplace y
  Juntas regionales.
- Solo chat conserva endpoints por path pendientes.
- Errores visibles deben pasar por `RemoteConnections.userFriendlyError`.
- No hacer llamadas HTTP directamente desde pantallas nuevas si ya existe o
  corresponde una funcion aqui.

## Flujo de navegacion

1. La app inicia en `AppScreen.ROUTES`, presentado como Mapas.
2. La barra inferior navega a Mapas, Bicicletas, Marketplace y Chat.
3. El boton de perfil abre `AppScreen.ACCOUNT`.
4. `AccountScreen` contiene perfil, login y tarjetas deportivas.
5. Si no hay sesion, Bicicletas muestra CTA para ir a Cuenta.
6. Chat requiere sesion y no muestra conversaciones sin cuenta.
7. Al cerrar sesion se limpian bicicletas, mantenciones y reservas en memoria.

No agregues botones "Volver" a Mapas, Bicicletas, Marketplace o Chat como flujo
principal. Solo los flujos secundarios internos pueden tener volver/cerrar.

En Bicicletas, el mensaje central sin sesion debe ser exactamente:
"Inicia sesión para ver tus bicicletas guardadas". El CTA de inicio de sesion
puede permanecer debajo del mensaje.

## Cuentas y sesion

Archivos:

- `Account.kt`
- `AppModels.kt`
- `RemoteConnections.kt`
- `MainActivity.kt`

Contrato:

- `AccountSession.userId` es `String`.
- `AccountSession.username` refleja `nombre_de_usuario`, es nullable y solo se
  usa como identidad visible; el UUID sigue siendo la identidad interna.
- El ID debe ser un UUID valido.
- `AccountStore` guarda `user_id`, `email` y `username` en SharedPreferences. Si login
  devuelve `access_token`, `SecureTokenStore` lo cifra con Android Keystore;
  nunca guardar el token en texto ni en `LocalDataStore`.
- No guardar password.
- Una sesion numerica o no UUID se considera invalida y se elimina al cargar.
- Login envia:
  - `action=login`
  - `usuario`
  - `password`
- Login acepta ID desde `user.id`, `user_id` o `id`.
- Login acepta correo o nombre de usuario en `usuario` y parsea
  `nombre_de_usuario` desde la raiz o `user`.
- Una cuenta antigua sin nombre pasa por `user.get` y debe completar
  `user.username.update` antes de depender de su identidad visible.

Al cambiar cuenta, verificar:

- Login exitoso y error visible si falla.
- Persistencia de sesion.
- Limpieza en logout.
- Carga de bicicletas para la cuenta activa.
- Que ninguna pantalla muestre datos privados de otra cuenta.

## Sincronizacion deportiva

- La sincronizacion no es pantalla principal; vive en `AccountScreen`.
- Plataformas: Strava, Garmin y Wahoo.
- Los botones ya no simulan conexion local. Usan
  `sports.connections.list`, `sports.oauth.start`, `sports.oauth.complete` y
  `sports.connection.delete`.
- Callback Android: `appbike://oauth/callback`; `MainActivity` es `singleTask`.
- El backend todavia no desplego esas acciones. Hasta entonces la UI muestra un
  error visible y nunca marca una plataforma como conectada.
- Los secretos y refresh tokens de proveedores pertenecen al backend, no al APK.
- `SyncScreen.kt` fue eliminado; `AppScreen.SYNC` queda solo como alias hacia
  Cuenta por compatibilidad.

## Mis bicicletas

### Listado

`BikesScreen` observa `account.userId` y llama:

`RemoteConnections.loadUserBikes(userId)`

Endpoint:

`action=user.bikes.list`

Campos enviados:

- `user_id`
- `limit`
- `offset`

No reemplazar por `bike.list` sin filtro. Cada cuenta debe ver solo sus
bicicletas.

### Detalle

Al tocar una tarjeta:

1. No abrir directamente el objeto resumido.
2. Llamar `RemoteConnections.loadBikeDetails(account, bike)`.
3. Usar endpoint `action=bike.get` con `bike_id`.
4. Validar que el `bike_id` devuelto coincida.
5. Validar que `user_id` pertenezca a la cuenta activa si viene informado.
6. Conservar imagen y datos locales utiles si faltan en la respuesta.

Mientras carga, se muestra "Cargando informacion de la bicicleta...".

### Crear bicicleta

`BikeFormDialog` exige:

- Nombre personalizado.
- Marca.
- Modelo.
- Tipo.
- Numero de serie.
- Fotografia.

`RemoteConnections.registerBike` envia:

- `action=bike.create`
- `user_id`
- `bike_custom_name`
- `bike_brand`
- `bike_model`
- `bike_type`
- `serial_number`
- multipart `foto`

No cambiar el nombre multipart `foto`.

### Fotos de bicicletas

- El backend puede devolver rutas internas como
  `BikesPhotos/PersonalBikesPhotos/<photo_id>`.
- Esas rutas no deben exponerse como URL publica.
- Si existe `photo_id`, Android usa `appbike-photo://<photo_id>`.
- `BikeImageFrame` detecta ese esquema y llama
  `RemoteConnections.loadBikePhoto(photoId)`.
- `bike.photo.get` debe responder `photo.content_base64`.

Si la app muestra "No fue posible cargar la imagen", comprobar primero que el
backend desplegado tenga `content_base64` en `bike.photo.get`.

### Mantenciones

- No recargar todas las bicicletas para actualizar mantenciones.
- Dentro de `BikeDetailDialog`, al bajar cerca de 12% se activa `snapshotFlow`.
- Se llama solo `RemoteConnections.loadMaintenance(account, bike)`.
- La app reemplaza mantenciones y reservas de ese `bike_id`.
- El boton "Actualizar mantenciones" repite la consulta.

Endpoints usados:

- `maintenance.past.list`
- `maintenance.future.list`
- `maintenance.past.create`
- `maintenance.future.create`

Fechas: `AAAA-MM-DD`.

## Mapa y juntas

Archivos:

- `MapScreen.kt`
- `LocalDataStore.kt`
- `RemoteConnections.kt`
- `docs/MAP_MARKETPLACE_CHAT.md`

Estado actual:

- Pantalla Compose con `AndroidView` para `MapView`.
- Usa MapLibre Native OpenGL.
- Estilo: `https://tiles.openfreemap.org/styles/liberty`.
- En el primer ingreso solicita permisos de ubicacion Android.
- La ubicacion obtenida es temporal hasta que el usuario la confirma.
- El usuario puede corregirla mediante busqueda explicita de lugares o
  coordenadas.
- Ubicacion confirmada se guarda solo en `LocalDataStore` y se reutiliza en los
  ingresos siguientes.
- El marcador de usuario queda anclado a las coordenadas guardadas; mover el
  mapa no cambia ni persiste otra ubicacion.
- La fila bajo la busqueda muestra la ubicacion actual y permite corregirla.
- Centro inicial si no hay ubicacion: Santiago.
- Radio fijo: 40 km.
- Busqueda filtra localmente la lista regional devuelta por el backend.
- Crear junta requiere sesion.
- El selector manual de ubicacion se dibuja como overlay Compose dentro de la
  ventana de la pantalla. No volver a convertirlo en `AlertDialog`: al cerrar
  un dialogo de plataforma con el teclado y MapLibre activos puede quedar una
  ventana sin foco y provocar ANR al seleccionar una sugerencia.
- La busqueda en vivo espera 450 ms, cancela la consulta anterior al seguir
  escribiendo y no intercepta `CancellationException`. El `Geocoder` local
  tiene limite de 3 s; si no responde o no encuentra lugares, se consulta
  automaticamente el respaldo remoto con limite de 8 s, sin exigir Enter.
  Al elegir un resultado primero oculta el teclado y quita el foco, luego
  persiste la ubicacion.
- El campo es texto Unicode con `hintLocales=es-CL,es`; debe aceptar nombres
  como `Viña del Mar` y mantener disponibles los caracteres del teclado
  espanol. No filtrar ni normalizar el valor visible a ASCII.
- El mapa publico consulta y muestra solamente juntas `activa`; no debe mostrar
  selectores de juntas activas/pasadas.
- Las juntas `pasada` se reservan para el historial futuro del perfil del
  usuario creador, donde podra revisar sus juntas anteriores y sus detalles.
- Marcador verde: ubicacion guardada. Marcador azul: junta tocable. Ambos usan
  fuentes GeoJSON y `SymbolLayer`; no reintroducir las APIs deprecadas
  `addMarker`/`MarkerOptions`.
- El detalle permite completar y agregar fotos al propietario. Las acciones
  remotas de estado se conservan para la futura administracion desde perfil,
  pero no se exponen como filtro publico del mapa.
- Coordenadas de juntas Android se codifican en
  `REGION:<lat>,<lng>|<etiqueta>` dentro de `location`.

Acciones POST vigentes:

- `junta.create|list|get|update|delete`
- `junta.status.update`
- `junta.complete`
- `junta.photo.upload|list|get|delete`

Crear envia `region`, `user_id`, `location`, `title`, `description` y
`junta_status`. La foto se sube despues de crear mediante multipart `foto` y
`junta_id`. `junta.photo.get` requiere juntos `junta_id` y `photo_id`.

No guardar ubicacion como perfil de usuario.

La correccion manual muestra sugerencias mientras el usuario escribe. Ese flujo
usa `android.location.Geocoder`, espera 500 ms y comienza desde 3 caracteres.
El dialogo compartido por Mapas y Marketplace se titula "Introduce tu
ubicación" y su ayuda visible dice "Busca una ciudad, dirección o lugar.".
Antes de escribir muestra hasta ocho `Ubicaciones recientes`, compartidas entre
ambos selectores, sin duplicados y ordenadas por uso reciente. Elegir una del
historial solo cambia la ubicacion activa de la pantalla desde la que se abrio;
Mapas y Marketplace siguen siendo independientes.
La capa remota intenta primero `location.search`/`location.reverse`. Mientras el
backend siga sin esas acciones, el geocodificador Android entrega sugerencias y
Nominatim queda como respaldo temporal con timeout, cache y limite de frecuencia.
No enviar el token APPbike a Nominatim. Retirar el respaldo directo cuando el
endpoint cacheado de backend este desplegado.

## Marketplace

Archivos:

- `MarketplaceScreen.kt`
- `MarketplacePricing.kt`
- `RemoteConnections.kt`
- `LocalDataStore.kt`
- `docs/MAP_MARKETPLACE_CHAT.md`

Estado actual:

- Marketplace guarda su propia ubicacion en `marketplace_location`. Al usarse
  por primera vez copia `selected_location` como valor inicial, pero desde ese
  momento ambas ubicaciones son independientes y persistentes.
- La pantalla incluye un boton para cambiar la ubicacion de Marketplace con el
  mismo buscador de lugares usado por Mapas/Juntas, sin modificar la ubicacion
  de Juntas.
- Requiere una ubicacion propia definida para listar.
- La vista publica lista solamente publicaciones `activa`; aplica radio de 40 km cuando el
  registro contiene coordenadas codificadas.
- Busqueda filtra localmente la respuesta regional.
- Al entrar a Marketplace y al ejecutar una busqueda se muestra un indicador
  circular superpuesto durante al menos 450 ms, incluso si la API responde de
  inmediato. Las respuestas antiguas no deben ocultar el indicador ni
  reemplazar el resultado de una solicitud posterior.
- Arrastrar la grilla hacia abajo ejecuta una recarga real y muestra el indicador
  de `PullToRefreshBox` sin retirar las tarjetas ya visibles.
- El detalle de una publicacion es un `Dialog` de ancho y alto completos, con
  cabecera de retorno, fotografia protagonista, contenido desplazable y accion
  principal fija al pie. No volver al dialogo compacto centrado.
- Crear publicacion requiere sesion y ubicacion.
- Creacion primero usa JSON y luego sube la foto multipart con campo `foto`.
- Las tarjetas descargan imagen al renderizar.
- Estados: `activa`, `vendida`, `pausada`, `en_revision`.
- No mostrar los filtros Activas/Pausadas/Vendidas/En revision en la vista
  publica. Conservar sus contratos y logica para el futuro perfil, donde el
  propietario administrara y buscara sus publicaciones por estado.
- El perfil implementa filtros de publicaciones propias por estado y juntas
  actuales/anteriores, con detalle, edicion, fotos, estado y eliminacion. Intenta
  `marketplace.mine.list`/`junta.mine.list` y usa un fallback regional hasta que
  el backend despliegue los listados multirregionales.

Contrato del formulario de publicacion:

- `product_status` no es texto libre. Las unicas opciones seleccionables son
  `nuevo`, `usado` y `reacondicionado`.
- El precio se edita como digitos de una unidad monetaria entera. El simbolo de
  moneda es un prefijo visual no editable y el codigo de moneda se determina
  desde la ubicacion propia de Marketplace.
- La UI agrupa miles segun la moneda/ubicacion (`1.000`, `10.000`,
  `1.000.000` para CLP y ARS), pero nunca envia esos separadores al backend.
- Ejemplo obligatorio: una entrada visual `10.000` envia el numero `10000`, no
  `10` ni un decimal.
- En Chile se muestra `$` y `CLP`; en Argentina `$` y `ARS` (peso argentino).
  El helper de moneda contiene equivalencias para otros paises y un fallback
  documentado. Android envia `currency` y la conserva en el modelo. El backend
  desplegado aun no la devuelve; los registros antiguos usan fallback CLP.

Acciones POST vigentes:

- `marketplace.create|list|get|update|delete`
- `marketplace.status.update`
- `marketplace.photo.upload|list|get|delete`

El listado regional actual no incluye fotos. Android ya no ejecuta una consulta
`marketplace.get` por tarjeta: espera `photo_id`/`cover_photo_id` en el listado y
usa placeholder mientras el backend no lo devuelva. La app convierte la foto en
`appbike-market-photo://<publication_id>/<photo_id>`.
`marketplace.photo.get` requiere juntos `publication_id` y `photo_id`.

`MarketplacePricing.kt` es la fuente unica para resolver moneda, normalizar la
entrada entera y formatear miles. No volver a convertir texto localizado con
`toDoubleOrNull()`. `currencyCode` forma parte de `ProductPublication`; mantener
sincronizados modelo, parser, creacion, actualizacion y formateador.

## Chat

Archivos:

- `ChatScreen.kt`
- `LocalDataStore.kt`
- `RemoteConnections.kt`
- `docs/MAP_MARKETPLACE_CHAT.md`

Estado actual:

- Requiere sesion.
- Tabs:
  - `SOCIAL`
  - `MARKETPLACE`
- Carga primero cache local por `userId`.
- Luego consulta chats remotos.
- Arrastrar la lista hacia abajo vuelve a consultar `chat.list`.
- Al abrir un chat, pinta inmediatamente los mensajes locales y siempre pide la
  primera pagina remota completa para reparar huecos de caches incrementales
  antiguos.
- Una vez reparado, consulta cada 3 segundos solo los mensajes posteriores al
  ultimo ID visible para que el receptor no tenga que cerrar la conversacion.
- `after_message_id` se omite por completo cuando no existe cursor; el backend
  rechaza expresamente una cadena vacia como cursor invalido.
- El parser acepta `type`/`chat_type`, participantes como strings u objetos,
  `last_message` como texto u objeto y metadata de pagina en `ChatMessagePage`.
- Al enviar, guarda mensajes y metadata local.
- Participantes parsean `nombre_de_usuario` y mensajes parsean
  `sender_nombre_de_usuario`.
- `ChatNotificationListenerService` es un Foreground Service `remoteMessaging`:
  consulta cambios cada 6 segundos fuera de la actividad, publica en
  `ChatNotificationEventBus` si la app está visible y muestra notificacion
  Android si queda en segundo plano.
- El listener se inicia con sesion y se detiene en logout. Debe conservar su
  notificacion persistente de baja importancia.
- WorkManager revisa cada 15 minutos si el proceso fue cerrado. Notificacion push
  inmediata con proceso muerto queda pendiente de FCM/backend.

Acciones POST primarias esperadas:

- `chat.get_or_create`
- `chat.list`
- `chat.messages.list`
- `chat.message.send`

Las rutas REST antiguas quedan como fallback transitorio para listar, descargar
y enviar. Crear un chat desde Marketplace/Juntas exige la accion POST nueva.

No mezclar caches entre usuarios. Las claves locales incluyen `userId` y
`chatId`.

## LocalDataStore

Guarda en SharedPreferences:

- `selected_location`: ubicacion independiente del mapa de Juntas.
- `marketplace_location`: ubicacion independiente de Marketplace; se inicializa
  una sola vez desde `selected_location` cuando existe.
- `location_history`: hasta ocho ubicaciones recientes compartidas por los dos
  selectores, sin duplicar coordenadas.
- `chats:{userId}`
- `messages:{userId}:{chatId}`
- `chat_sync:{userId}:{chatId}`
- `chat_notification_sync:{userId}:{chatId}`

No usarlo para datos remotos globales ni para secretos.

## Tema visual

Archivos:

- `ui/theme/Color.kt`
- `ui/theme/Theme.kt`
- `ui/theme/Type.kt`
- `res/values/themes.xml`

Reglas:

- Mantener verde como acento principal.
- Superficies claras calidas y alto contraste.
- La app llama `APPbikeTheme(dynamicColor = false)`.
- Cabecera usa `statusBarsPadding()` por edge-to-edge.
- Iconos de navegacion vienen de `material-icons-extended`.

## Build, dependencias y permisos

Archivos:

- `app/build.gradle.kts`
- `gradle/libs.versions.toml`
- `AndroidManifest.xml`
- `local.properties`

Configuracion actual:

- AGP 8.13.2.
- Kotlin 2.0.21.
- Compose BOM 2024.09.00.
- compileSdk 36.
- targetSdk 36.
- minSdk 24.
- Java/Kotlin target 11.
- MapLibre: `org.maplibre.gl:android-sdk-opengl:13.0.2`.
- WorkManager: `androidx.work:work-runtime-ktx:2.11.2`.
- Permisos Android actuales: `INTERNET`, `ACCESS_COARSE_LOCATION` y
  `ACCESS_FINE_LOCATION`, `FOREGROUND_SERVICE`,
  `FOREGROUND_SERVICE_REMOTE_MESSAGING` y `POST_NOTIFICATIONS` en Android 13+.

No reemplazar MapLibre OpenGL por el artefacto Vulkan sin probar en dispositivo
y emulador.

`local.properties` actual esperado:

`sdk.dir=C\:\\Users\\Xinerdev\\AppData\\Local\\Android\\Sdk`

Si se cambia temporalmente, restaurarlo al terminar.

## Backend de referencia vigente

Rutas reales en el servidor:

- API publica:
  `Z:/var/www/api.zizzio.cl/APIS/AppBikeExternal.php`.
- API privada:
  `Z:/srv/internal-auth/public/AppBikeInternal/AppBikeInternal.php`.
- Modulo privado de comunidad:
  `Z:/srv/internal-auth/public/AppBikeInternal/AppBikeCommunity.php`.
- Tester:
  `Z:/var/www/api.zizzio.cl/APIS/AppBikeApiTester.html`.
- Fotos internas:
  `Z:/srv/internal-auth/public/AppBikeInternal/BikesPhotos/PersonalBikesPhotos`.

`AppBikeExternal.php` solo maneja transporte, CORS, formatos, allowlist y
reenvio. PostgreSQL y logica de negocio pertenecen a `AppBikeInternal.php`.
Estas rutas existen en el servidor y no deben asumirse montadas en el PC
Android. Para ubicacion, juntas y Marketplace revisar tambien
`docs/LOCATION_BACKEND_HANDOFF.md`. El contrato completo de trabajo pendiente
del servidor esta en `docs/BACKEND_IMPLEMENTATION_REPORT.md`.

## Riesgos conocidos

- Marketplace, Juntas y las cuatro acciones POST de Chat ya responden en el
  backend desplegado. `location.*`, `*.mine.list` y OAuth deportivo siguen
  pendientes de backend.
- El backend comunitario guarda `location` como texto. Android codifica
  coordenadas dentro de ese campo y filtra 40 km localmente hasta que existan
  columnas y consultas geograficas reales.
- Los listados desplegados todavia exponen `photo_folder_path`; Android lo
  ignora, pero el backend debe dejar de devolver rutas internas.
- El listado Marketplace no devuelve portada, por lo que las tarjetas muestran
  placeholder hasta recibir `photo_id`; no reintroducir consultas N+1.
- `RemoteConnections.kt` y `BikesScreen.kt` siguen siendo archivos grandes;
  extraer por dominio solo con pruebas que preserven los contratos actuales.
- Hay tests unitarios reales para ubicaciones, moneda/precio, UUID y merge
  incremental de Chat; tambien tests instrumentados para parsers JSON y entrada
  Unicode del selector de ubicacion. Los placeholders generados se conservan.
- Si el backend no devuelve `content_base64` para fotos, las imagenes remotas no
  se muestran.
- Sin FCM, `force-stop`, reinicio sin abrir la app o detener manualmente el
  listener dejan la recepción sujeta a la siguiente apertura/ventana permitida;
  no prometer push garantizado en esos casos.

## Verificacion obligatoria por tipo de cambio

### Siempre que se cambie Kotlin, Gradle o Manifest

1. Compilar `:app:assembleDebug`.
2. Revisar que no haya errores de Kotlin/Compose.

### Si se cambia cuenta, bicicletas, fotos o mantenciones

1. Compilar `:app:assembleDebug`.
2. Instalar `app/build/outputs/apk/debug/app-debug.apk`.
3. Iniciar sesion.
4. Abrir Mis bicicletas.
5. Confirmar que solo aparecen bicicletas de la cuenta.
6. Abrir una bicicleta y comprobar campos.
7. Confirmar que la foto se muestra.
8. Bajar hasta mantenciones y comprobar carga independiente.
9. Revisar Logcat por `FATAL EXCEPTION`.

### Si se cambia mapa, marketplace o chat

1. Compilar `:app:assembleDebug`.
2. Abrir Mapas y definir ubicacion.
3. Confirmar que no se bloquea el hilo principal.
4. Probar busqueda con y sin texto.
5. Probar estado sin sesion y con sesion.
6. Confirmar errores visibles usando `userFriendlyError`.
7. Revisar Logcat por `FATAL EXCEPTION`.

## Checklist antes de cerrar una tarea

- El cambio esta limitado al flujo solicitado.
- No se agrego navegacion principal duplicada.
- No se hicieron llamadas de red fuera de `Dispatchers.IO`.
- No se mezclaron datos entre cuentas.
- No se cambio `foto` como campo multipart.
- No se expusieron rutas internas del servidor como URL publica.
- Se actualizaron docs si cambio un contrato.
- Se corrio la verificacion aplicable o se explico por que no fue posible.
