# Mapa, Marketplace y Chat

## Archivos

- `AppModels.kt`: `GeoPoint`, `CyclingRoutePreview`, `MeetupEvent`, `UserChat`, `StoredMessage`,
  `ChatMessagePage`, `ChatSyncMetadata` y campos remotos de
  `ProductPublication`.
- `LocalDataStore.kt`: ubicación, chats, mensajes y metadata por usuario.
- `RemoteConnections.kt`: contratos HTTP y parsers.
- `MapScreen.kt`: mapa, búsqueda, selector de destino, trayectos independientes,
  bottom sheet y creación de juntas.
- `CyclingNavigation.kt`: distancia geodesica y respaldo directo desacoplado de
  las Juntas.
- `HomeScreen.kt`: hero de aventura con cuatro imágenes locales en rotación,
  historias y feed social/comunitario de juntas y publicaciones personales;
  Marketplace permanece fuera de Inicio.
- `MarketplaceScreen.kt`: búsqueda, grid, fotos y creación.
- `ChatScreen.kt`: tabs, listado, conversación y sincronización.
- `ChatNotifications.kt`: detección global, aviso dentro de la app, canal Android
  y comprobación diferida con WorkManager.
- `MainActivity.kt`: conexión de las pantallas con la sesión.
- `DeviceLocationProvider.kt`: permiso y lectura puntual de la ubicación Android.
- `docs/LOCATION_BACKEND_HANDOFF.md`: contrato detallado para API externa,
  API interna, tablas, consultas geográficas y datos iniciales.

La navegación raíz compone cabecera, contenido y barra inferior como hermanos
directos. El contenido central se recorta para contener MapLibre y los fondos
decorativos, mientras las barras persistentes conservan prioridad visual. En
horizontal, la cabecera se compacta en una línea y la navegación usa 56 dp con
  iconos compactos y mantiene la semántica completa de los cinco destinos:
  Inicio, Marketplace, Mapa, Chat y Perfil. Un `HorizontalPager` mantiene las
  cinco paginas montadas para conservar estado; cada una difiere su primera
  carga hasta activarse. Bicicletas queda como `Mi garaje` secundario desde Perfil.
  En Mapa el gesto iniciado sobre el lienzo queda para el paneo de MapLibre; al
  iniciar sobre controles Compose se conserva el swipe entre destinos.

## Mapa y juntas regionales

El `MapView` expone una descripcion en espanol con la cantidad de juntas
cercanas visibles. Una prueba instrumentada con estilo offline agrega una junta
despues de que el mapa queda listo y consulta la capa renderizada; esto protege
la actualizacion tardia de la fuente GeoJSON. En la validacion real del
2026-08-06, Osorno mostro el marcador azul, el detalle remoto y su foto Base64.

La pantalla mantiene MapLibre Native OpenGL 13.4.1. Dentro del pager crea el
`MapView` en modo TextureView solo al primer ingreso y baja su FPS fuera de
pantalla. Un boton circular con el
icono universal de capas abre el menu `Mapa`/`Satélite`; la interfaz inicial deja
solo ese control y una lupa bajo la cabecera. `Mapa` usa Liberty de OpenFreeMap y
`Satélite` World Imagery de Esri con una capa de etiquetas de referencia. Cambiar de modo reutiliza el
mismo `MapView`, aplica `setStyle` sobre el mapa activo, conserva el centro
vigente y vuelve a instalar las tres fuentes GeoJSON de APPbike. Un identificador
de solicitud descarta callbacks tardios de estilos anteriores. El logo textual
de MapLibre esta oculto, pero el control
pequeño de atribucion sigue activo y cada fuente satelital declara sus creditos.
La migracion API 37 se valido con la regresion de fuente tardia y un render real
del mapa tanto en Android 16 como en el AVD `APPbike_API_37` Android 17, sin
error de carga nativa. El
marcador verde representa la ubicación local guardada y no se
mueve al desplazar el mapa. Las juntas con coordenadas válidas usan marcador
azul y abren su detalle al tocarlas.

`Trayecto` y `Junta` aparecen como acciones distintas. `Trayecto` funciona sin
sesion, reutiliza el buscador de lugares para elegir un destino independiente y
solicita una geometria de bicicleta al demo FOSSGIS/OSRM de OpenStreetMap. La
`LineLayer` sigue el camino devuelto por calles, el marcador azul identifica el
destino y la camara encuadra la geometria completa. Distancia y duración vienen
del ruteador. La selección se añade al historial de lugares recientes.

El detalle de Junta sigue siendo un `ModalBottomSheet`. `Cómo llegar` reutiliza
el mismo planificador genérico sin convertir la Junta en un trayecto guardado ni
mezclar ambos flujos. Si el servicio de ruteo no responde, Android muestra una
línea directa rotulada como respaldo y conserva `Abrir navegación ciclista` con
la URL oficial de Google Maps y `travelmode=bicycling`. Cada solicitud puede
cancelarse y respuestas anteriores se descartan. La interfaz no inventa
participantes, dificultad, desnivel, superficie ni tipo de ciclismo ausentes del
backend.

El ruteador público se reserva para desarrollo y pruebas de bajo volumen: no
recibe el Bearer de APPbike y no ofrece SLA. Antes de publicar comercialmente se
debe usar un proxy del backend o una instancia propia de OSRM y conservar la
atribucion `© OpenStreetMap contributors`.

Las fuentes raster satelitales anuncian zoom 19, pero en sectores de Puerto
Varas World Imagery devuelve en ese nivel una tesela gris `Map data not yet
available`; una tesela puntual conserva imagen en 18, pero el zoom anclado puede
alcanzar sectores vecinos sin cobertura en ese mismo nivel. Por eso la camara de
MapLibre se detiene en 17 y no permite sobrezoom hacia niveles problematicos.

Para conservar acceso por teclado alrededor del `AndroidView`, la lupa, capas y
las acciones `Trayecto`/`Junta` se componen antes del mapa y se dibujan por encima
con `zIndex(1f)`, en la misma esquina superior y dentro de superficies
semitransparentes. `Trayecto`/`Junta` forman un selector segmentado compacto con
dos botones equilibrados, borde sutil y elevación ligera para mantener el mapa
visible bajo el control. Un gesto sobre el lienzo desactiva temporalmente el
swipe del pager para que MapLibre conserve el paneo horizontal; los controles
superiores mantienen disponible el desplazamiento entre pestañas.

El onboarding y la corrección de ubicación no cambian: permisos Android en el
primer ingreso, confirmación antes de persistir y sugerencias en vivo. La capa
remota intenta primero `location.search`/`location.reverse`. La búsqueda remota
ya devuelve lugares normalizados, pero la resolución inversa aún puede responder
`unknown_region`; `android.location.Geocoder` y Nominatim permanecen como
respaldo temporal con cache, rate limit y timeout.
El diálogo muestra hasta ocho ubicaciones recientes antes de escribir. El
historial es compartido para ofrecer accesos rápidos, pero seleccionar un lugar
solo actualiza la ubicación activa de Mapas o de Marketplace según el origen
del diálogo.

Cada confirmación de ubicación tiene un identificador monotónico adicional. Si
`location.resolve` termina después de que el usuario eligió otro lugar, su
resultado se descarta y no puede restaurar la selección anterior.

El selector manual no usa una ventana `AlertDialog`: es un overlay Compose
dentro de la misma pantalla para que MapLibre, el teclado y la selección no
dejen una ventana huérfana sin foco. La entrada declara idioma `es-CL`, conserva
Unicode (`Viña del Mar`, por ejemplo), busca tras 450 ms de pausa y cancela la
consulta anterior cuando cambia el texto. El `Geocoder` local tiene un límite
de 3 s; si no responde o devuelve vacío, la misma búsqueda en vivo usa
automáticamente Nominatim con límite de 8 s, sin esperar Enter. Al tocar una
sugerencia se oculta el teclado y se libera el foco antes de guardar y retirar
el overlay.

La lectura del dispositivo solicita simultaneamente GPS, red y proveedor
pasivo con la API cancelable de AndroidX. Espera hasta 15 s por GPS, compara los
puntos frescos por `accuracy` y evita usar posiciones conocidas de mas de cinco
minutos. La fila de ubicacion incorpora `Precisar`, que vuelve a pedir acceso
fino cuando falta y repite la medicion antes de la confirmacion del usuario.

Cuando existe una ubicación previa, el primer foco limpia su etiqueta antes de
aceptar escritura. Así una búsqueda nueva reemplaza el valor anterior en vez de
insertarse dentro de él; la ubicación previa continúa accesible en la lista de
recientes y no se modifica hasta seleccionar una sugerencia.

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

Cada recarga y apertura de detalle lleva un identificador monotónico. Si cambia
la ubicación, la búsqueda o el marcador antes de terminar una respuesta, el
resultado antiguo se descarta y no puede sobrescribir la pantalla vigente. Al
crear, Android geocodifica el punto exacto elegido; ante fallo conserva las
coordenadas y usa la región de la ubicación confirmada como respaldo. El botón
se bloquea durante los dos pasos. Si el JSON creó la junta pero falló `foto`, se
informa un éxito parcial, se cierra el formulario y se refresca para no duplicar.

Crear una junta o contactar a su organizador requiere sesion. Sin cuenta activa,
ambas acciones abren Cuenta mediante la navegacion raiz; los CTA publicos no se
deshabilitan cuando su texto promete iniciar sesion.

Mientras el backend comunitario no expone un campo de fecha propio, Android
codifica `Fecha y hora: <valor>` al inicio de `description`. El parser retira esa
linea del texto visible y la conserva en `MeetupEvent.dateTime`; editar vuelve a
codificarla exactamente una vez para no duplicarla ni perderla.

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

Crear una publicacion o contactar a un vendedor abre Cuenta si no existe sesion.
El recorrido real del 2026-08-06 cargo la grilla y el detalle de publicaciones
activas de Puerto Montt; las tarjetas conservan placeholder cuando el listado no
entrega un `photo_id` de portada util.
En el detalle, ese estado sin portada ocupa 180 dp en lugar del hero de 340 dp;
cuando existe una fotografia se conserva la altura protagonista completa.

Marketplace público solicita exclusivamente `publication_status=activa`,
mantiene búsqueda y filtros locales por categoria sobre la respuesta y filtra a
40 km cuando el registro
contiene coordenadas codificadas. Los filtros `activa`, `pausada`, `vendida` y
`en_revision` se retiran de esta vista, pero sus acciones remotas se conservan
para la futura administración de publicaciones propias desde el perfil.
La cabecera raíz de marca/clima conserva prioridad de dibujo sobre el fondo de
Marketplace y permanece visible durante carga, listado y estado vacío.
El fondo decorativo se recorta a los límites del contenido. Con fuente Android
al 200 %, búsqueda, ubicación, moneda y navegación conservan jerarquía; los
nombres largos se eliden sin reducir el tamaño de texto configurado.
La misma prioridad se conserva navegando Inicio → Marketplace → Mapa → Chat →
Perfil en horizontal; el cambio de destino no debe ocultar ni retrasar el repintado
de la marca o del clima.

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
La resolución remota también comprueba el identificador de la elección vigente
antes de persistir país, área o moneda derivados.

El formulario usa un selector cerrado para `product_status`: `nuevo`, `usado`
o `reacondicionado`. El precio conserva únicamente dígitos, muestra un prefijo
monetario no editable y agrupa miles según la moneda derivada de la ubicación.
Así, `10.000` se envía como el entero `10000`. La UI muestra, entre otras
equivalencias, `$ ... CLP` para Chile y `$ ... ARS` para Argentina. Android ya
envía y parsea `currency`; la respuesta observada el 2026-08-06 ya conserva el
campo. Los registros antiguos o respuestas parciales usan la moneda derivada de
la ubicación de creación como fallback.

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

El formulario bloquea controles mientras crea y evita el doble envío. Si el
primer paso tuvo éxito y solo falla la foto, muestra que la publicación existe y
refresca sin repetirla. Recargas y detalles descartan respuestas antiguas; los
listados sin ID se ignoran y un fallo con lista vacía ofrece `Reintentar` en vez
de simular un resultado legítimamente vacío.

Android no hace consultas N+1: espera `photo_id`/`cover_photo_id` en cada
elemento y muestra un placeholder cuando la publicación no tiene portada. La
muestra del 2026-08-06 incluyó la clave `photo_id`, aunque sin un valor útil. El
detalle devuelve `publication` y su array `photos`. Las referencias internas
conservan el ID padre:

```text
appbike-market-photo://<publication_id>/<photo_id>
appbike-junta-photo://<junta_id>/<photo_id>
```

No exponer `photo_folder_path` ni `file_path` como URL pública. La descarga usa
`content_base64` desde la acción `photo.get` correspondiente. Android limita
subidas/descargas a 20 MB, muestrea bitmaps grandes y respeta EXIF en archivos
locales; el backend puede aplicar su límite más estricto.

La compatibilidad legacy no acepta cualquier `path`: Android descarta rutas de
servidor (`PersonalBikesPhotos`, `AppBikeInternal`, `/srv`, `/var`, unidades
Windows), traversal, `file:`, HTTP y hosts externos. Solo una ruta pública
relativa al API o HTTPS bajo Zizzio puede convertirse en URL. Si se rechaza,
queda el placeholder; nunca se intenta “arreglar” una ruta interna.

## Contenido propio en Cuenta

`ProfileContent.kt` muestra publicaciones propias separadas en `activa`,
`pausada`, `vendida` y `en_revision`, y juntas propias actuales/anteriores. El
detalle permite editar, mover estado, completar, agregar fotos y eliminar. La
carga intenta `marketplace.mine.list` y `junta.mine.list`; mientras no existan,
consulta los listados de la región activa y filtra estrictamente por `user_id`.
El fallback no reemplaza el requisito multirregional del backend.
Cuando la acción privada responde directamente, cada elemento debe traer el
`user_id` exacto de la cuenta activa. Android rechaza la lista completa ante un
propietario ausente o ajeno para no ocultar una violación del contrato. Las
creaciones comprueban la misma propiedad; una incoherencia posterior al alta se
trata como éxito parcial y fuerza recarga para impedir duplicados.

## Chats

Sin sesión, Chat no carga ni muestra conversaciones privadas. Su estado vacío
incluye el CTA `Iniciar sesión`, conectado directamente a Cuenta.
Con fuente grande el CTA aparece antes de la explicación y el estado se aloja
en una lista desplazable, evitando que la acción quede fuera de la ventana al
200 %. Los estados vacíos autenticados usan el mismo respaldo desplazable.

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

Las filas distinguen visualmente `JUNTA` y `COMPRA`. Si un chat social incluye
`relatedEntityId`, la conversación ofrece `Ver en mapa`; la raiz cambia a Mapa y
`RoutesScreen` carga el detalle remoto de esa Junta sin depender del listado
regional ya visible.

Flujo:

1. Sin sesión no se consulta ni muestra información.
2. Con sesión se carga primero el listado local.
3. Se solicita el listado remoto y se actualiza la caché.
4. Arrastrar la lista hacia abajo repite `chat.list` y conserva la caché visible.
5. Al abrir un chat se pintan primero los mensajes locales.
6. Inmediatamente se pide la primera página completa, sin
   `after_message_id`, para reparar huecos históricos. El cursor no se envía con
   cadena vacía porque el backend responde `after_message_id no es valido`.
7. Con la conversación abierta y Chat visible se consulta cada 3 segundos usando
   el último ID; al cambiar de pestaña el efecto se cancela y al volver se reanuda.
8. Las páginas se combinan sin duplicados y se guardan mensajes, cantidad,
   versión y último ID después de descargar o enviar.
9. IDs vacíos o mensajes de otra conversación se rechazan antes de Compose y de
   la caché. Si un mensaje remoto repite un ID local, la versión remota más nueva
   repara contenido, estado y nombre del remitente.
10. El borrador permanece visible durante el envío y solo se limpia tras éxito;
    un fallo conserva el texto. El listado usa un mutex por usuario y cada chat
    usa otro para serializar sincronización, polling, recarga y envío. Cambiar de
    conversación cancela su efecto visual sin bloquear otro chat.
11. Si dos mensajes comparten `createdAt`, sus IDs se ordenan numéricamente
    cuando ambos son enteros (`9` antes de `10`); otros IDs usan orden estable.
12. Un mensaje antiguo sin `chatId` adopta el ID solicitado. Un mensaje que
    declara un chat diferente invalida la página completa antes de UI o caché.
13. Lectura y escritura de chats, mensajes y metadata se hacen en
    `Dispatchers.IO`; la serialización de un historial no bloquea Compose.

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
- Cada evento incluye el UUID destinatario. El centro de notificaciones y
  Compose lo comparan con la sesión actual; eventos atrasados de otra cuenta se
  descartan y el logout elimina notificaciones ya publicadas.
- El Intent del `PendingIntent` conserva el mismo UUID. `MainActivity` descarta
  accesos sin destinatario o de otra sesión antes de cambiar a la pestaña Chat;
  la identidad del PendingIntent combina destinatario y conversación.
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
