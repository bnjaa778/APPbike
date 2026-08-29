# APPbike - Router de trabajo para agentes

Este archivo es la entrada principal para trabajar en APPbike. Usalo como
router: antes de editar, identifica el tipo de tarea, abre primero los archivos
indicados y conserva los contratos documentados aqui.

Ultima revision del proyecto: 2026-08-28.

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
- Orden de barra inferior: Inicio, Marketplace, Mapa, Chat, Perfil.
- Cuenta y sincronizacion deportiva estan en Perfil; el boton de la cabecera se
  conserva como acceso adicional. Bicicletas es el flujo secundario `Mi garaje`.
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
| Inicio o Novedades | `HomeScreen.kt` | `MainActivity.kt`, `RemoteConnections.kt`, `LocalDataStore.kt` |
| Clima del encabezado | `HeaderStatusComponents.kt` | `MainActivity.kt`, `DeviceLocationProvider.kt`, `RemoteConnections.kt`, `AppModels.kt` |
| Destinos o enum de pantallas | `AppModels.kt` | `MainActivity.kt` |
| Login, sesion o logout | `Account.kt` | `RemoteConnections.kt`, `MainActivity.kt` |
| Sincronizacion deportiva | `Account.kt` | `SecureTokenStore.kt`, `MainActivity.kt`, `RemoteConnections.kt` |
| Listado Mis bicicletas | `BikesScreen.kt` | `RemoteConnections.loadUserBikes`, `AppModels.kt` |
| Crear bicicleta | `BikesScreen.kt` | `RemoteConnections.registerBike`, backend `AppBikeInternal.php` |
| Editar o eliminar bicicleta | `BikesScreen.kt` | `RemoteConnections.updateBike`, `RemoteConnections.deleteBike` |
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
- La raiz usa una `Column` determinista con cabecera, contenido central y barra
  inferior como hermanos directos. No volver a un `Scaffold` raiz sin repetir
  la auditoria horizontal: su subcomposicion permitia que capas hijas ocultaran
  visualmente la cabecera al cambiar de destino.
- Solo el contenido central usa `clipToBounds()`; cabecera y barra inferior
  conservan su propia prioridad de dibujo.
- La cabecera muestra el emblema monocromo entregado por el usuario en
  `appbike_brand_icon` y el clima local; no contiene un acceso duplicado a
  Perfil y conserva prioridad de dibujo sobre los fondos de las pantallas hijas.
  Debe permanecer visible en los cinco destinos principales.
- El clima usa la posicion actual del dispositivo mientras la actividad esta
  iniciada. Se actualiza cada 15 minutos, reintenta un fallo al minuto y espera
  el permiso de ubicacion sin iniciar solicitudes de red desde Compose.
- `WeatherStatusChip` muestra temperatura, estado e iconografia propia para sol,
  noche, nubes, niebla, lluvia, nieve, tormenta y granizo. El estado actual es
  informativo y no abre enlaces externos: al tocarlo abre un panel desplegable
  animado dentro de la cabecera con el pronostico de seis dias. La atribucion
  visible `Open-Meteo` no debe retirarse.
- La barra inferior solo debe contener:
  - `AppScreen.HOME` como "Inicio"
  - `AppScreen.MARKETPLACE`
  - `AppScreen.ROUTES` como "Mapa"
  - `AppScreen.CHAT`
  - `AppScreen.ACCOUNT` como "Perfil"
- El contenido central usa `HorizontalPager` entre los cinco destinos en el
  mismo orden. Las cinco paginas permanecen compuestas para conservar scroll,
  busquedas y estado, pero cada pantalla recibe `isActive` y difiere su primera
  carga de red hasta ser visible. El polling de Chat solo corre cuando Chat esta
  activo.
- En `ROUTES`, el lienzo del mapa reserva sus gestos horizontales para MapLibre;
  el desplazamiento del pager sigue disponible al iniciar el gesto sobre los
  controles Compose de busqueda, capas y acciones principales.
- El destino actual se conserva durante recreaciones de la actividad. Las
  navegaciones rapidas esperan la pagina asentada antes de sincronizar estado,
  y el boton Atrás del sistema cierra `Mi garaje` hacia Perfil.
- En horizontal, la cabecera usa la firma `APPBIKE   RIDE • CONNECT` en una
  linea y la barra inferior mide 56 dp con iconos de 24 dp sin etiqueta visual.
  Los cinco iconos deben conservar su `contentDescription` completo.
- Los cinco destinos usan la familia Material `Outlined` de manera consistente;
  solo el destino activo muestra label y Mapa conserva mayor peso visual.
- `ACCOUNT` y `ROUTES` son destinos principales. `BIKES` se superpone como
  `Mi garaje` desde Perfil sin desmontar el pager. `SYNC` aliasa Perfil y
  `CREATE_PUBLICATION` aliasa Marketplace por compatibilidad.
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
- `CyclingRouteSource`
- `CyclingRoutePreview`
- `WeatherCondition`
- `WeatherSnapshot`
- `MeetupEvent`
- `UserChat`
- `StoredMessage`
- `ChatMessagePage`
- `ChatSyncMetadata`
- `ChatNotificationSyncMetadata`
- `SyncPlatform`
- `SocialPost`

Regla: si el backend cambia un campo, actualiza el modelo y el parser al mismo
tiempo.

Las publicaciones sociales de `SocialPost` son actualmente una capacidad local
por `userId`: aceptan una URI persistente de foto o video y se muestran en
Perfil/Inicio. No inventar acciones remotas hasta que exista el contrato social
del backend.

### `RemoteConnections.kt`

Capa remota centralizada con `HttpURLConnection` y `org.json`.

- JSON POST al `API_URL` para cuenta, bicicletas, mantenciones, Marketplace y
  Juntas regionales.
- El clima actual y el pronostico de seis dias usan GET publicos a Open-Meteo
  mediante `loadCurrentWeather` y `loadWeatherForecast`; nunca adjuntan el
  Bearer de APPbike. El endpoint gratuito directo se reserva para
  desarrollo/no comercial. Antes de un lanzamiento comercial, enrutarlo por
  backend o usar el endpoint de cliente contratado sin incluir credenciales
  del proveedor en el APK.
- Los trayectos ciclistas de desarrollo usan un GET publico sin Bearer al demo
  FOSSGIS/OSRM de OpenStreetMap mediante `loadCyclingRoute`. Antes de un
  lanzamiento comercial, enrutarlo por backend o desplegar una instancia propia;
  el demo no tiene SLA y nunca debe recibir el token APPbike.
- Solo chat conserva endpoints por path pendientes.
- Errores visibles deben pasar por `RemoteConnections.userFriendlyError`.
- No hacer llamadas HTTP directamente desde pantallas nuevas si ya existe o
  corresponde una funcion aqui.

## Flujo de navegacion

1. Después del logo, una sesion local con nombre se verifica mediante `user.get`
   en `Dispatchers.IO`. Si backend la acepta abre `AppScreen.HOME`; si no existe
   o el token fue rechazado/expiró, limpia la sesión y muestra
   `UnauthenticatedAccessScreen` a pantalla completa, sin cabecera ni barra
   inferior. Una identidad registrada pero sin nombre abre `AppScreen.ACCOUNT`
   para completar ese dato. Un timeout o error 5xx no debe cerrar sesión.
2. Inicio muestra un hero de aventura con cuatro imágenes JPG entregadas por el
   usuario (`home_hero_truck`, `home_hero_peloton`, `home_hero_ridge` y
   `home_hero_trail`) en rotación automática,
   historias y un feed social/comunitario de juntas activas. Marketplace es
   independiente y nunca se mezcla en Inicio. El acceso de mapa abre
   `AppScreen.ROUTES`.
3. La barra inferior navega a Inicio, Marketplace, Mapa, Chat y Perfil; tambien
   se puede cambiar entre ellos mediante desplazamiento horizontal sin recargar
   automaticamente las pestañas.
4. El destino Perfil de la barra inferior abre `AppScreen.ACCOUNT`; la cabecera
   no duplica ese acceso.
5. `AccountScreen` contiene identidad, rendimiento disponible, una vista previa
   visible de bicicletas, posts y rutas/juntas propias, `Mi garaje` y las tarjetas
   deportivas pausadas para una sesion activa.
   `UnauthenticatedAccessScreen` contiene el login sobre el fondo MTB
   `auth_mtb_background.png`; un acceso correcto vuelve automaticamente a Inicio
   y un error permanece en ese formulario.
6. `Mi garaje` abre `AppScreen.BIKES` como flujo secundario desde Perfil. Si no
   hay sesion, Bicicletas conserva el CTA para ir a Cuenta.
7. Chat requiere sesion y no muestra conversaciones sin cuenta; su estado vacío
   ofrece un CTA que abre Cuenta.
8. Al cerrar sesion se limpian bicicletas, mantenciones y reservas en memoria y
   se abre Cuenta.

No agregues botones "Volver" a Inicio, Marketplace, Mapa, Chat o Perfil como
flujo principal. `Mi garaje` y los demas flujos secundarios internos pueden
tener volver/cerrar.

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
- Una sesion persistida con UUID se valida una vez al arrancar con `user.get`.
  Solo `401`, codigos remotos de token o mensajes inequívocos de token/sesion
  invalida/expirada autorizan limpiar Cuenta y token; errores de conectividad o
  servidor conservan la sesion y permiten entrar a Inicio.
- Login envia:
  - `action=login`
  - `usuario`
  - `password`
- Login acepta ID desde `user.id`, `user_id` o `id`.
- Login acepta correo o nombre de usuario en `usuario` y parsea
  `nombre_de_usuario` desde la raiz o `user`.
- Login rechaza cualquier identidad devuelta que no sea UUID antes de guardar
  la sesion. En Cuenta sin sesion, el formulario de acceso aparece antes del
  panel promocional para que la accion principal quede visible de inmediato.
- En la raiz sin sesion, `UnauthenticatedAccessScreen` reemplaza por completo la
  pantalla de perfil y oculta cabecera/navegacion. Debe mostrar `Iniciar sesión`
  y `Crear cuenta` sobre el fondo original `auth_mtb_background.png`.
- El backend desplegado no ofrece aun una accion de registro. `Crear cuenta`
  explica esa limitacion y no debe inventar endpoints ni enviar credenciales
  hasta que el contrato de alta segura exista.
- Una cuenta antigua sin nombre pasa por `user.get` y debe completar
  `user.username.update` antes de depender de su identidad visible.

Al cambiar cuenta, verificar:

- Login exitoso y error visible si falla.
- Persistencia de sesion.
- Limpieza en logout.
- Carga de bicicletas para la cuenta activa.
- Que ninguna pantalla muestre datos privados de otra cuenta.
- Los estados de perfil, Chat y notificaciones deben estar asociados al
  `userId` activo. Una respuesta iniciada por una cuenta anterior debe cancelarse
  o descartarse al cambiar sesion.
- Las corrutinas de UI usan `runSuspendCatching` para llamadas suspendibles:
  convierte errores normales en `Result`, pero nunca intercepta
  `CancellationException`.

## Sincronizacion deportiva

- La sincronizacion no es pantalla principal; vive en `AccountScreen`.
- Plataformas: Strava, Garmin y Wahoo.
- La funcion esta detenida intencionalmente hasta una futura etapa del producto.
- Las tarjetas son informativas y no tienen botones. Solo Strava muestra la cinta
  diagonal `PROXIMAMENTE`; Garmin y Wahoo conservan el estado `Vinculacion en
  pausa` sin presentar una promesa de disponibilidad.
- El bloque deportivo se muestra antes de `ProfileContentSection`/`Tu actividad`
  para que Strava, Garmin y Wahoo permanezcan juntas y no queden despues de
  listados propios extensos.
- Mientras este detenida, `AccountScreen` no llama
  `sports.connections.list`, `sports.oauth.start`, `sports.oauth.complete` ni
  `sports.connection.delete`. Las funciones remotas se conservan sin uso en
  `RemoteConnections.kt` para la reactivacion futura.
- Callback Android: `appbike://oauth/callback`; `MainActivity` es `singleTask`.
- Si llega un callback antiguo mientras la funcion sigue detenida, Android lo
  consume sin completar OAuth ni marcar una plataforma como conectada.
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
bicicletas. Android valida tambien que cualquier `user_id` no vacio devuelto por
el listado coincida con la cuenta activa; una respuesta mezclada se rechaza
completa y nunca se pinta ni se guarda.

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

### Editar y eliminar bicicleta

- El detalle expone `Editar datos de la bicicleta` y `Eliminar bicicleta`.
- Editar reutiliza `BikeFormDialog`, conserva foto/IDs y actualiza solamente
  nombre, marca, modelo, tipo y numero de serie.
- `RemoteConnections.updateBike` envia `action=bike.update`, `bike_id`,
  `user_id`, `bike_custom_name`, `bike_brand`, `bike_model`, `bike_type` y
  `serial_number`.
- Una respuesta de actualizacion con otro `bike_id` o con propietario ajeno se
  rechaza; los campos omitidos no eliminan foto ni identidad ya validadas.
- Eliminar exige una confirmacion destructiva explicita y llama
  `action=bike.delete` con `bike_id`.
- La lista, mantenciones y reservas locales se retiran solamente despues de una
  respuesta remota exitosa. Un fallo permanece visible y permite reintentar.
- Todo estado de edicion/eliminacion se reinicia por `account.userId`; nunca
  conservar un dialogo privado al cambiar de cuenta.

### Fotos de bicicletas

- El backend puede devolver rutas internas como
  `BikesPhotos/PersonalBikesPhotos/<photo_id>`.
- Esas rutas no deben exponerse como URL publica.
- Si existe `photo_id`, Android usa `appbike-photo://<photo_id>`.
- `BikeImageFrame` detecta ese esquema y llama
  `RemoteConnections.loadBikePhoto(photoId)`.
- `bike.photo.get` debe responder `photo.content_base64`.
- Si falta `photo_id`, `safePublicPhotoUrl` solo acepta una ruta relativa publica
  bajo `api.zizzio.cl` o HTTPS de `zizzio.cl`/subdominios. Debe rechazar rutas
  internas, traversal, unidades locales, HTTP y hosts ajenos; no relajar esta
  politica para ocultar un backend incompleto.

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
- El `MapView` del pager se crea con `MapLibreMapOptions.textureMode(true)`, se
  activa recien al primer ingreso a Mapa y baja su FPS cuando la pestaña queda
  inactiva. No volver a `SurfaceView` dentro del pager.
- El boton circular de capas abre un menu compacto con `Mapa`, usando Liberty de
  OpenFreeMap, y `Satélite`, con World Imagery mas etiquetas de referencia de
  Esri. Junto a el solo queda visible inicialmente el icono de busqueda.
- El selector reutiliza el mismo `MapView` y cambia el estilo con `setStyle`;
  un identificador descarta callbacks tardios. No volver a recrear el
  `AndroidView` por estilo: en el Samsung podia dejar visible el `SurfaceView`
  anterior aunque el selector ya hubiese cambiado.
- `Trayecto` y `Junta` son acciones separadas sobre el mapa. Crear un trayecto
  no requiere cuenta: abre un selector de destino propio, calcula un camino para
  bicicleta por calles, marca el destino y encuadra la geometria completa. Crear
  una Junta conserva su seleccion/formulario y sigue exigiendo sesion.
- El marcador de Junta abre un `ModalBottomSheet`. `Como llegar` reutiliza el
  planificador generico y calcula el camino ciclista hasta esa Junta; no mezcla
  el destino con los datos ni el flujo de creacion de Juntas.
- `CyclingRoutePreview` aisla origen, destino, distancia, ETA, fuente y geometria.
  La fuente normal es FOSSGIS/OSRM con datos OpenStreetMap. Si falla, Android
  dibuja una linea directa rotulada explicitamente como respaldo y mantiene la
  apertura de Google Maps con `travelmode=bicycling`.
- Cada calculo tiene un `Job` y un identificador de solicitud: se puede cancelar
  mientras carga y una respuesta anterior no reemplaza un trayecto posterior.
  Participantes, dificultad, desnivel y tipo de ciclismo no se inventan si el
  backend no los entrega.
- El logo textual de MapLibre esta desactivado. El control pequeño de atribucion
  permanece activo y las fuentes del estilo satelital declaran sus creditos; no
  eliminar ese acceso informativo.
- El modo satelital limita la camara a zoom 17, dos niveles antes del maximo 19
  declarado por las fuentes raster. En Puerto Varas, Esri devuelve teselas
  grises `Map data not yet available` en 19 y en sectores vecinos de 18 durante
  zoom anclado; no retirar este margen ni permitir sobrezoom.
- En el primer ingreso solicita permisos de ubicacion Android.
- El lienzo de MapLibre desactiva temporalmente el swipe del pager mientras se
  toca/arrastra el mapa; los controles superpuestos lo reactivan para conservar
  la navegacion horizontal entre destinos.
- La lectura actual consulta en paralelo GPS, red y proveedor pasivo mediante
  `LocationManagerCompat.getCurrentLocation` con `CancellationSignal`, elige el
  punto fresco de menor `accuracy` y solo acepta un ultimo punto conocido con
  hasta cinco minutos de antiguedad. No reintroducir `requestSingleUpdate` ni
  volver a aceptar el primer proveedor que responda.
- La ubicacion obtenida es temporal hasta que el usuario la confirma.
- Cada confirmacion incrementa su identificador de compromiso. Una respuesta
  tardia de `location.resolve` no puede reemplazar una ubicacion elegida despues.
- El usuario puede corregirla mediante busqueda explicita de lugares o
  coordenadas.
- Ubicacion confirmada se guarda solo en `LocalDataStore` y se reutiliza en los
  ingresos siguientes.
- El marcador de usuario queda anclado a las coordenadas guardadas; mover el
  mapa no cambia ni persiste otra ubicacion.
- Al tocar la lupa se despliegan sobre el mapa un buscador y la fila de ubicacion
  con superficies semitransparentes. La fila permite corregir y ofrece `Precisar`
  para solicitar de nuevo permiso fino y recalcular el GPS.
- Centro inicial si no hay ubicacion: Santiago.
- Radio fijo: 40 km.
- Busqueda filtra localmente la lista regional devuelta por el backend.
- Crear junta requiere sesion. Si no existe, la accion abre Cuenta desde la
  navegacion raiz; no mostrar una accion sin efecto ni un dialogo informativo.
- Las recargas y aperturas de detalle usan identificadores de solicitud: una
  respuesta antigua no puede reemplazar la ubicacion, busqueda o junta vigente.
- El punto elegido para crear se geocodifica antes de enviar. Si la consulta no
  responde, conserva la coordenada exacta y hereda solo la metadata regional de
  la ubicacion confirmada.
- Crear y subir foto bloquea doble envio. Si la junta se creo pero fallo la foto,
  la UI cierra el formulario, informa el exito parcial y refresca para evitar una
  segunda junta duplicada.
- Mientras el backend no tenga un campo propio de fecha, Android codifica
  `Fecha y hora: <valor>` una sola vez al inicio de `description`. El parser
  separa esa metadata del texto visible y las ediciones la reconstruyen; no
  mostrar ni persistir la fecha duplicada.
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
- Si el selector se abre con una ubicacion previa, el primer foco limpia el
  campo antes de recibir texto. La ubicacion anterior sigue disponible en
  `Ubicaciones recientes`; no volver a insertar una busqueda nueva dentro de la
  etiqueta prellenada.
- El mapa publico consulta y muestra solamente juntas `activa`; no debe mostrar
  selectores de juntas activas/pasadas.
- Las juntas `pasada` se reservan para el historial futuro del perfil del
  usuario creador, donde podra revisar sus juntas anteriores y sus detalles.
- Marcador verde: ubicacion guardada. Marcador azul: junta tocable. Ambos usan
  fuentes GeoJSON y `SymbolLayer`; no reintroducir las APIs deprecadas
  `addMarker`/`MarkerOptions`.
- El `MapView` anuncia en espanol cuantas juntas cercanas estan visibles. La
  fuente debe aceptar listas que lleguen despues de cargar el estilo; existe una
  regresion instrumentada offline para ese orden de eventos.
- El detalle permite completar y agregar fotos al propietario. Las acciones
  remotas de estado se conservan para la futura administracion desde perfil,
  pero no se exponen como filtro publico del mapa.
- Contactar al organizador sin sesion abre Cuenta; con sesion conserva el flujo
  `chat.get_or_create`.
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
usa `android.location.Geocoder`, espera 450 ms y comienza desde 3 caracteres.
El dialogo compartido por Mapas y Marketplace se titula "Introduce tu
ubicación" y su ayuda visible dice "Busca una ciudad, dirección o lugar.".
Antes de escribir muestra hasta ocho `Ubicaciones recientes`, compartidas entre
ambos selectores, sin duplicados y ordenadas por uso reciente. Elegir una del
historial solo cambia la ubicacion activa de la pantalla desde la que se abrio;
Mapas y Marketplace siguen siendo independientes.
La capa remota intenta primero `location.search`/`location.reverse`.
`location.search` ya devuelve sugerencias normalizadas; `location.reverse` aun
puede responder `unknown_region` para coordenadas validas. El geocodificador
Android y Nominatim siguen como respaldo temporal con timeout, cache y limite de
frecuencia.
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
- Si no existe portada, el detalle usa un placeholder compacto de 180 dp; con
  fotografia conserva el hero de 340 dp para no empujar el contenido real fuera
  de la primera ventana.
- Crear publicacion requiere sesion y ubicacion. Si falta sesion, la accion abre
  Cuenta desde la navegacion raiz.
- Creacion primero usa JSON y luego sube la foto multipart con campo `foto`.
- El formulario bloquea doble envio. Si la publicacion se creo y solo fallo la
  fotografia, se informa exito parcial y se refresca sin repetir la creacion.
- Las recargas y detalles descartan respuestas antiguas. Los listados ignoran
  elementos sin ID y, si una carga vacia falla, ofrecen `Reintentar` en vez de
  presentar el fallo como un Marketplace realmente vacio.
- Las tarjetas descargan imagen al renderizar.
- Contactar al vendedor sin sesion abre Cuenta; el CTA permanece habilitado
  mientras el detalle no esta cargando.
- Estados: `activa`, `vendida`, `pausada`, `en_revision`.
- No mostrar los filtros Activas/Pausadas/Vendidas/En revision en la vista
  publica. Conservar sus contratos y logica para el futuro perfil, donde el
  propietario administrara y buscara sus publicaciones por estado.
- El perfil implementa filtros de publicaciones propias por estado y juntas
  actuales/anteriores, con detalle, edicion, fotos, estado y eliminacion. Intenta
  `marketplace.mine.list`/`junta.mine.list` y usa un fallback regional hasta que
  el backend despliegue los listados multirregionales.
- Los listados `*.mine.list` deben incluir `user_id` en cada elemento. Android
  rechaza la respuesta completa si falta el propietario o no coincide con la
  cuenta activa; no filtrar silenciosamente una violacion de aislamiento.
- Crear una junta/publicacion valida el propietario devuelto. Si el primer paso
  ya creo la entidad pero el propietario es inconsistente, se reporta exito
  parcial y se refresca para evitar un duplicado.

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
  observado el 2026-08-06 ya la devuelve; los registros antiguos o respuestas
  sin ese campo usan el fallback correspondiente a la ubicacion de creacion.

Acciones POST vigentes:

- `marketplace.create|list|get|update|delete`
- `marketplace.status.update`
- `marketplace.photo.upload|list|get|delete`

El listado regional actual no incluye fotos. Android ya no ejecuta una consulta
`marketplace.get` por tarjeta: espera `photo_id`/`cover_photo_id` en el listado y
usa placeholder mientras el backend no lo devuelva. La app convierte la foto en
`appbike-market-photo://<publication_id>/<photo_id>`.
`marketplace.photo.get` requiere juntos `publication_id` y `photo_id`.
Las URLs legacy encontradas dentro de `photos` pasan por
`safePublicPhotoUrl`; una fuente rechazada no se renderiza y conserva placeholder.

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
- Sin sesion muestra `Iniciar sesión` y navega a Cuenta desde ese CTA.
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
- Al enviar, guarda mensajes y metadata local. Lectura y escritura de caches de
  Chat se realizan en `Dispatchers.IO`, no en el hilo de Compose.
- La recarga del listado usa un mutex por usuario. Cada conversacion usa su
  propio mutex para serializar sincronizacion, polling, recarga manual y envio;
  cambiar de chat cancela el efecto visual anterior sin bloquear otro chat.
- Para mensajes con el mismo `createdAt`, dos IDs enteros se comparan de forma
  numerica; IDs no numericos conservan orden lexicografico estable.
- El borrador se conserva mientras envia y solo se limpia al confirmar el
  servidor; un fallo de red no borra lo escrito. La sincronizacion usa un
  `Mutex` para impedir dos descargas simultaneas.
- Chats y mensajes remotos con ID vacio se rechazan antes de entrar a Compose o
  a la cache. Un mensaje antiguo sin `chatId` adopta el ID solicitado, pero un
  `chatId` distinto se rechaza. Al combinar, la version remota mas reciente
  reemplaza campos antiguos del mismo mensaje local.
- Participantes parsean `nombre_de_usuario` y mensajes parsean
  `sender_nombre_de_usuario`.
- `ChatNotificationListenerService` es un Foreground Service `remoteMessaging`:
  consulta cambios cada 6 segundos fuera de la actividad, publica en
  `ChatNotificationEventBus` si la app está visible y muestra notificacion
  Android si queda en segundo plano.
- El listener se inicia con sesion y se detiene en logout. Debe conservar su
  notificacion persistente de baja importancia.
- Cada `MessageNotificationEvent` lleva `recipientUserId`; el centro y Compose
  lo comparan con la sesion actual. Logout limpia las notificaciones existentes.
- El Intent/PendingIntent de una notificacion tambien incluye
  `EXTRA_RECIPIENT_USER_ID`; `MainActivity` no abre destinos sin destinatario ni
  de otra cuenta. La identidad del PendingIntent combina usuario y chat.
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

Al leer cache, se descartan chats sin ID y mensajes vacios o pertenecientes a
otra conversacion. Los XML de backup excluyen identidad, token cifrado,
ubicaciones y caches de Chat tanto de nube como de transferencia de dispositivo.

No usarlo para datos remotos globales ni para secretos.

## Tema visual

Archivos:

- `ui/theme/Color.kt`
- `ui/theme/Theme.kt`
- `ui/theme/Type.kt`
- `res/values/themes.xml`

Reglas:

- Mantener verde como acento principal.
- Mantener la identidad grafito/verde electrico, superficies oscuras y alto
  contraste; los campos usan contorno LED verde-azul con realce al enfocar.
- Los placeholders de campos de búsqueda usan `AppTextSecondary`; no degradar
  su contraste al tono `AppTextMuted` sobre `AppSurfaceElevated`.
- `ui/theme/Color.kt` es la fuente unica de color Compose. No reintroducir los
  recursos morado/teal de la plantilla eliminada.
- La app llama `APPbikeTheme(dynamicColor = false)`.
- Cabecera usa `statusBarsPadding()` por edge-to-edge.
- El espacio entre marca y perfil contiene `WeatherStatusChip`. El boton de
  Cuenta conserva 48 dp y el icono anterior `PersonOutline`, con indicador de
  sesion; ambos mantienen descripciones semanticas completas.
- Los iconos principales de navegacion usan una sola familia Material Outlined;
  cada uno mantiene el `contentDescription` completo del destino y solo el
  activo expone label visual.
- `appbike_brand_icon.png` es la copia exacta del PNG monocromo proporcionado el
  2026-08-12. Es la fuente única para launcher, variante redonda, cabecera y
  revelado de arranque; no regenerarlo, recolorearlo ni reemplazarlo por la
  bicicleta/A verde anterior.
- El splash de plataforma es negro y usa un icono transparente. En arranque
  frio, `LaunchBrandScreen` anima 40 puntos LED blancos desde las cuatro esquinas
  durante 4.500 ms, revela el mismo logo compartido con el login, muestra
  `Iniciando sesión…` debajo y entra a la app a los 5.000 ms. La app se compone
  debajo del splash para que la validación de sesión empiece sin demora.
  No mostrar el logo completo antes de esa convergencia.
- Con `fontScale >= 1.6`, la cabecera usa `RIDE • CONNECT`; la barra inferior
  mantiene nombres semanticos completos y no muestra labels de destinos
  inactivos.
- A escala grande, las tarjetas deportivas usan reflow vertical y reservan
  espacio para la cinta diagonal. La cinta mantiene tamaño visual estable; el
  estado semantico escalable sigue siendo `Vinculacion en pausa`.
- Los estados vacios con accion colocan el CTA antes de la descripcion a escala
  grande. Si el contenido puede superar la ventana, el contenedor debe ser
  desplazable.
- `PremiumScreenBackground` recorta solo sus circulos decorativos mediante
  `clipRect`; no aplicar `clipToBounds()` a todo el contenedor porque introduce
  una capa grafica que puede alterar el orden visual. El recorte estructural
  pertenece al contenido central de `MainActivity.kt`.
- `BikesScreen` no debe anidar otro `Scaffold`; la raiz ya resuelve las barras e
  insets persistentes.
- No mostrar controles con apariencia de pestaña si no existe una acción. Cuenta
  no incluye los antiguos rótulos estáticos Progreso/Entrenamientos/Actividades.
- La marca APPBIKE se expone como un único encabezado semántico; las tarjetas
  Strava/Garmin/Wahoo se exponen como un único anuncio por plataforma, solo el
  anuncio de Strava incluye `Próximamente`, y `Agregar bicicleta` conserva rol y
  etiqueta de botón.
- El splash, login, banner dentro de la app y notificaciones comparten el
  emblema `appbike_brand_icon`; las notificaciones usan además una variante
  monocroma transparente compatible con el icono pequeño de Android.
- En `MapScreen`, las capas Compose de buscador/ubicación y acciones
  `Trayecto`/`Junta` se componen antes de `AndroidView`, usan `zIndex(1f)` y
  comparten la esquina superior con capas/búsqueda sobre fondos semitransparentes.
  Las acciones usan un selector segmentado compacto, con dos botones del mismo
  ancho, borde sutil y transparencia suficiente para conservar el mapa visible.
  Conservar ese orden para que buscador, ubicación y creación sigan disponibles
  por teclado.

- `PremiumScreenBackground` usa `appbike_solar_halo_background` como textura
  ambiental oscura de halo ámbar, con una capa de contraste para mantener la
  lectura. No reemplazarla por fotografías ni colocarla por encima de contenido
  interactivo.

- Una tarjeta resumida solo expone accion de clic cuando recibe un callback. La
  copia mostrada dentro del detalle de bicicleta es estatica.
- El panel del selector de ubicacion consume toques para proteger el scrim, pero
  no anuncia una accion vacia a accesibilidad.

## Build, dependencias y permisos

Archivos:

- `app/build.gradle.kts`
- `gradle/libs.versions.toml`
- `AndroidManifest.xml`
- `local.properties`

Configuracion actual:

- AGP 9.3.1 y Gradle 9.6.1.
- Kotlin integrado de AGP; no volver a aplicar `org.jetbrains.kotlin.android`.
- Plugin Compose Compiler 2.4.10 y Compose BOM 2026.06.01.
- compileSdk 37.
- targetSdk 37.
- minSdk 24.
- Java/Kotlin target 11.
- AndroidX Core 1.19.0 y Lifecycle 2.11.0.
- MapLibre: `org.maplibre.gl:android-sdk-opengl:13.4.1`.
- AndroidX ExifInterface: `1.4.2` para orientar fotos locales sin depender de la
  implementacion de plataforma.
- WorkManager: `androidx.work:work-runtime-ktx:2.11.2`.
- Dependencias directas pertenecen al catalogo `gradle/libs.versions.toml`; no
  volver a declarar coordenadas/versiones literales en `app/build.gradle.kts`.
- Permisos Android actuales: `INTERNET`, `ACCESS_COARSE_LOCATION` y
  `ACCESS_FINE_LOCATION`, `FOREGROUND_SERVICE`,
  `FOREGROUND_SERVICE_REMOTE_MESSAGING` y `POST_NOTIFICATIONS` en Android 13+.

No reemplazar MapLibre OpenGL por el artefacto Vulkan sin probar en dispositivo
y emulador.
No bajar API/AGP/Kotlin/Compose por separado: el toolchain actual se migro como
un conjunto y usa Kotlin integrado de AGP 9.

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

- Verificacion directa de 2026-08-06: los listados y detalles publicos de
  Marketplace/Juntas responden; `junta.photo.get` entrega Base64;
  `location.search` devuelve resultados normalizados y `location.resolve`
  responde HTTP 200. `location.reverse` aun responde HTTP 400 `unknown_region`
  para coordenadas validas de Santiago, por lo que el respaldo sigue activo.
- Las lecturas privadas sin token (`*.mine.list` y Chat) responden HTTP 401, como
  corresponde. Su funcionamiento autenticado debe probarse nuevamente con una
  cuenta de prueba; un emulador limpio no permite concluir que esten completos.
- La sincronizacion deportiva esta detenida por decision de producto. La
  disponibilidad de OAuth del backend se vuelve a evaluar solo al reactivarla.
- El backend comunitario guarda `location` como texto. Android codifica
  coordenadas dentro de ese campo y filtra 40 km localmente hasta que existan
  columnas y consultas geograficas reales.
- La muestra de Marketplace de 2026-08-06 ya incluyo `latitude`, `longitude`,
  `country_code`, `administrative_area`, `currency` y la clave `photo_id`, sin
  `photo_folder_path`. El registro probado no tenia un `photo_id` util, por lo
  que las tarjetas aun pueden mostrar placeholder; no reintroducir consultas N+1.
- La validacion visual real del 2026-08-06 mostro publicaciones activas de Puerto
  Montt y una junta activa de Osorno con marcador azul, detalle y foto Base64.
- Las acciones publicas no deben heredar un Bearer vencido. La politica central
  de `RemoteConnections` omite autorizacion en login, descubrimiento, detalles,
  fotos y resolucion publica; mutaciones, bicicletas, perfil, Chat y deportes
  siguen autenticados.
- `RemoteConnections.kt` y `BikesScreen.kt` siguen siendo archivos grandes;
  extraer por dominio solo con pruebas que preserven los contratos actuales.
- Hay tests unitarios reales para ubicaciones, moneda/precio, UUID, propiedad de
  bicicletas, fechas, IDs remotos, autorizacion, codificacion de fuentes y merge
  incremental de Chat; tambien tests instrumentados para parsers JSON y entrada
  Unicode del selector de ubicacion. Los placeholders generados se conservan.
- Si el backend no devuelve `content_base64` para fotos, las imagenes remotas no
  se muestran.
- Android limita fotos subidas y descargadas a 20 MB, decodifica con muestreo y
  aplica EXIF a imagenes locales para evitar picos de memoria y orientacion
  incorrecta. El backend puede imponer un limite menor.
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
7. Editar una bicicleta descartable y confirmar que conserva foto y propietario.
8. Abrir eliminar, comprobar la advertencia y cancelar salvo que el dato sea de prueba.
9. Confirmar que la foto se muestra.
10. Bajar hasta mantenciones y comprobar carga independiente.
11. Revisar Logcat por `FATAL EXCEPTION`.

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
