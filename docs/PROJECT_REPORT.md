# APPbike - Estado tecnico

Revision: 2026-08-06.

## Resumen

APPbike es una app Android nativa en Kotlin/Jetpack Compose. La navegacion
principal mantiene cuatro destinos: Mapas, Bicicletas, Marketplace y Chat. La
cuenta, el contenido propio y las conexiones deportivas viven en la pantalla
secundaria Cuenta.

Funcionan contra el backend desplegado:

- login UUID, bicicletas por usuario, detalle, fotos y mantenciones;
- ubicacion inicial, confirmacion, correccion en vivo e historial;
- MapLibre/OpenFreeMap y Juntas regionales;
- Marketplace regional, precios enteros y fotos por ID.

Android ya contiene clientes y UI para contenido propio, Chat, geografia de
servidor, moneda persistida, Bearer auth y OAuth deportivo. Algunas capacidades
quedan a la espera de las acciones del backend detalladas en
`docs/BACKEND_IMPLEMENTATION_REPORT.md`.

## Stack

- AGP 9.3.1, Kotlin integrado/Compose Compiler 2.4.10 y Gradle 9.6.1.
- compile/target SDK 37, min SDK 24, Java/Kotlin 11.
- Compose BOM 2026.06.01, MapLibre OpenGL 13.4.1 y ExifInterface 1.4.2.
- AndroidX Core 1.19.0, Lifecycle 2.11.0 y WorkManager 2.11.2.
- `HttpURLConnection`, `org.json`, coroutines y SharedPreferences.
- Android Keystore AES-GCM para un `access_token` opcional.

## Arquitectura vigente

### Raiz y navegacion

`MainActivity.kt` mantiene sesion, destino actual, bicicletas y conexiones
deportivas. Gestiona el deep link `appbike://oauth/callback` y abre Chat con la
conversacion creada desde una junta o publicacion. Los datos semilla historicos
y `SyncScreen.kt` fueron eliminados.

La identidad grafito/verde electrico se verifico visualmente con fuente Android
al 130 % y 200 %. A partir de 160 %, cabecera y barra inferior usan copia
compacta sin perder semantica, las tarjetas deportivas adoptan reflow vertical
y los estados vacios priorizan el CTA. Los
fondos decorativos quedan recortados a su pantalla y no pueden cubrir la
cabecera raiz.

La raiz usa cabecera, contenido y barra inferior como hermanos directos; solo
el contenido central se recorta. En horizontal, la marca se compacta en una
linea y la navegacion pasa a 56 dp con iconos de 24 dp y semantica completa.
La auditoria real confirmo el reflow en 360 dp horizontal y 540 dp vertical para
Mapas, Bicicletas, Marketplace, Chat y Cuenta.

### Cuenta

`Account.kt` valida IDs UUID y guarda solo identidad no secreta en preferencias.
`SecureTokenStore.kt` cifra con Android Keystore el token que el backend pueda
devolver. Sin sesión, el login aparece antes del panel promocional. Los backups
de nube/transferencia excluyen identidad, token, ubicaciones y cachés de Chat.
`ProfileContent.kt` implementa:

- publicaciones propias por estado;
- juntas propias actuales y anteriores;
- detalle, edicion, fotos, estado/completar y eliminacion.

Las conexiones Strava/Garmin/Wahoo estan detenidas por decision de producto.
Cuenta muestra tarjetas informativas con cinta `PROXIMAMENTE`, sin botones ni
llamadas OAuth. Los clientes remotos se conservan para una reactivacion futura.
La seccion aparece antes de `Tu actividad`, manteniendo las tres plataformas
como un bloque continuo antes del contenido propio.
Los rótulos estáticos con apariencia de pestaña fueron retirados de Cuenta. La
marca y cada plataforma deportiva se agrupan en un único anuncio semántico.

### Bicicletas

`BikesScreen.kt` carga por `user.bikes.list`, consulta `bike.get` antes del
detalle, descarga fotos mediante `bike.photo.get` y carga mantenciones de forma
independiente. Android rechaza listados con `user_id` de otra cuenta, ofrece
reintento tras error y valida fechas calendario reales `AAAA-MM-DD`.

### Ubicacion, mapa y Juntas

`DeviceLocationProvider.kt` obtiene GPS y geocodifica sin limitar la busqueda a
Chile. `GeoPoint` conserva pais, area administrativa, region y moneda cuando se
conocen. `LocalDataStore` persiste esos campos y hasta ocho ubicaciones recientes.

El selector Compose acepta Unicode, busca tras debounce y evita el ANR causado
por una ventana `AlertDialog` separada. Al recibir el primer foco limpia una
ubicacion previa para que la nueva busqueda la reemplace sin insertar texto en
su etiqueta. La capa remota intenta primero `location.search`/`location.reverse`;
usa servicios del dispositivo y un fallback temporal de Nominatim mientras la
resolucion inversa del backend siga incompleta.

MapLibre representa usuario, juntas y punto seleccionado con fuentes GeoJSON y
`SymbolLayer`; ya no usa `MarkerOptions` deprecado. El mapa publico muestra solo
juntas activas. El historial pasado aparece solo en el perfil del creador. Las
respuestas antiguas de búsqueda/detalle se descartan; creación bloquea doble
envío y reporta por separado un fallo de foto después de crear la junta.
La resolución de ubicación también usa un identificador de compromiso para que
una respuesta remota tardía no deshaga una corrección posterior del usuario.

El `MapView` anuncia en espanol la cantidad de juntas visibles. Crear una junta
o contactar a su organizador sin sesion abre Cuenta desde la navegacion raiz.
La ubicacion actual usa la API cancelable compatible de AndroidX. La fecha de una
junta se codifica dentro del contrato textual remoto, pero el modelo la separa
del cuerpo visible y la reconstruye una sola vez al editar.

### Marketplace

Crear una publicacion y contactar a un vendedor abren Cuenta cuando no hay
sesion, sin dejar CTA deshabilitados que prometan iniciar sesion. El recorrido
real del 2026-08-06 cargo la grilla y un detalle de Puerto Montt.
Sin portada, el detalle usa un placeholder de 180 dp; con foto conserva el hero
de 340 dp.

Marketplace conserva una ubicacion independiente de Juntas, radio de 40 km,
busqueda y carga visible minima de 450 ms. El formulario:

- restringe producto a nuevo/usado/reacondicionado;
- muestra prefijo monetario fijo;
- agrupa miles y envia unidades enteras (`10.000` -> `10000`);
- envia `currency`, pais, area y coordenadas separadas.

La moneda de una publicacion se toma del registro, no de la ubicacion de busqueda
actual. El listado ya no ejecuta `marketplace.get` por tarjeta: espera un
`photo_id` de portada. `RemoteImageLoader.kt` comparte cache LRU, timeouts y
downsampling para fotos comunitarias, aplica orientación EXIF local y limita
subidas/descargas a 20 MB. Marketplace descarta respuestas antiguas e IDs vacíos,
ofrece reintento ante un fallo vacío y evita duplicar una creación cuyo único
fallo fue la foto.
Las fuentes legacy pasan por una política segura: rutas internas, traversal,
archivos locales, HTTP y hosts ajenos se omiten; solo se aceptan `photo_id`,
rutas relativas públicas o HTTPS bajo Zizzio.
La cabecera raíz conserva prioridad visual sobre el fondo de Marketplace, por
lo que marca y perfil siguen visibles durante carga y estados vacíos.
Los listados privados de perfil rechazan la respuesta completa si un elemento no
incluye el propietario activo; las altas validan el propietario devuelto y usan
éxito parcial si la entidad ya existe pero el detalle es inconsistente.

### Chat

`ChatScreen.kt` carga cache por usuario, sincroniza listado, descarga mensajes
incrementales, elimina duplicados, persiste metadata y actualiza el resumen al
enviar. IDs remotos vacíos o cross-chat no entran a la caché y la versión remota
repara el registro local del mismo mensaje. El listado usa un mutex por cuenta;
cada conversación serializa sincronización, polling y envío con su propio mutex.
Cambiar de chat cancela el efecto anterior, la persistencia ocurre en
`Dispatchers.IO` y el borrador solo se limpia tras éxito. Marketplace/Juntas
llaman `chat.get_or_create` y abren la conversación. Los IDs enteros se ordenan
numéricamente cuando comparten fecha.

Las notificaciones de sistema incluyen el usuario destinatario en su Intent.
`MainActivity` rechaza accesos de otra cuenta o sin ese dato antes de abrir Chat.
Sin sesión, el estado protegido de Chat ofrece un CTA que abre Cuenta; una
prueba Compose ejecuta ese recorrido.

Acciones primarias:

- `chat.get_or_create`
- `chat.list`
- `chat.messages.list`
- `chat.message.send`

Las rutas REST anteriores son fallback temporal. La comprobacion sin sesion del
2026-08-06 recibio HTTP 401 en `chat.list`, por lo que la verificacion funcional
de Chat requiere una cuenta de prueba activa.

### Capa remota

`RemoteConnections.kt` centraliza transporte, errores, contratos y parsers. Las
mutaciones comunitarias incluyen `user_id`. Login, descubrimiento publico,
detalles, fotos y ubicacion publica omiten Bearer para que un token vencido no
rompa la app; bicicletas, contenido propio, Chat, deportes y mutaciones siguen
autenticados. El token nunca se envia al geocodificador externo.

Los listados envian `lat`, `lng`, `radius_km`, `q`, `limit` y `offset`, paginan
hasta 500 elementos y conservan filtrado local para compatibilidad. El backend
debe asumir distancia y paginacion antes de escalar.

## Estado real del servidor

Verificacion de 2026-08-06:

- `marketplace.list` y `junta.list`: HTTP 200 con colecciones regionales activas.
- `marketplace.get` y `junta.get`: HTTP 200.
- `junta.photo.get`: HTTP 200 con `content_base64`.
- `location.search`: HTTP 200 con sugerencias normalizadas; `location.resolve`:
  HTTP 200; `location.reverse`: HTTP 400 `unknown_region` para Santiago.
- `user.bikes.list`, `marketplace.mine.list`, `junta.mine.list`, `chat.list` y
  `sports.connections.list` sin token: HTTP 401.
  Esto confirma proteccion, no su funcionamiento con una sesion valida.
- La muestra de Marketplace incluyo `latitude`, `longitude`, `country_code`,
  `administrative_area`, `currency` y la clave `photo_id`; no incluyo
  `photo_folder_path`. El `photo_id` del registro probado estaba vacio.

## Pruebas

Unitarias:

- historial de ubicaciones;
- precio entero, formato y moneda por pais;
- validacion UUID;
- propiedad de bicicletas y fechas API reales;
- IDs remotos de Chat y codificación UTF-8 de fuentes;
- parseo del formato de coordenadas comunitarias;
- merge incremental de mensajes sin duplicados;
- politica de autorizacion publica/privada y fallback regional de perfil.
- cancelación de corrutinas, aislamiento de notificaciones, propiedad de
  contenido comunitario privado y orden numérico de IDs de Chat.

Instrumentadas:

- CTA publicos de crear/contactar en Marketplace y Juntas conectados a Cuenta;
- actualizacion tardia de la fuente GeoJSON de juntas con estilo MapLibre
  offline y consulta de la capa renderizada;
- parsers JSON de Marketplace y Chat;
- entrada Unicode `Viña del Mar` y reemplazo de una ubicación previa en el
  selector Compose;
- almacenamiento cifrado probado sin destruir un token existente;
- Chat remoto se omite de forma explicita cuando no hay una sesion de prueba;
- CTA de Chat sin sesión conectado a Cuenta;
- semántica agrupada de marca/tarjetas deportivas y acción etiquetada de
  `Agregar bicicleta`;
- formulario de edicion, acciones reales del detalle y confirmacion destructiva
  de bicicletas;
- placeholders base del proyecto.

Última ejecución local (2026-08-11): 44 pruebas unitarias sin fallos, Lint sin
incidencias y `:app:assembleDebug` correcto con el icono vectorial de APPbike.
La verificación remota de GitHub Actions `31551839752` terminó correctamente
el 2026-08-12 UTC con JDK 17, SDK 37.0, las mismas pruebas unitarias, Lint y
`assembleDebug` desde un runner limpio.
La ejecucion `31552406399` repitio esa verificacion y publico el artefacto
`appbike-debug-apk` (35,857,353 bytes, retencion de 14 dias) para instalar el
mismo APK debug que fue comprobado.
La última matriz instrumentada (2026-08-06) terminó con 24 pruebas sin fallos,
con 2 omisiones esperadas por credenciales reales de Chat y
condiciones de notificación del AVD. La migración coordinada a Gradle 9.6.1,
AGP 9.3.1, Kotlin integrado/Compose Compiler 2.4.10, API 37, Core 1.19,
Lifecycle 2.11, Compose BOM 2026.06.01 y MapLibre 13.4.1 terminó correctamente.
Lint informa `No issues found`. `:app:assembleDebug` y
`:app:assembleRelease` terminaron correctamente. Se recorrieron Mapas,
Bicicletas, Marketplace, Chat y Cuenta en `Small_Phone`, incluida la sección
deportiva en pausa, en retrato, horizontal y 540 dp de ancho, sin
`FATAL EXCEPTION` ni ANR. La matriz instrumentada completa se repitio ademas en
el AVD `APPbike_API_37` con Android 17: 24 casos, 0 fallos y 2 omisiones externas
esperadas. Mapa, contornos LED y cintas deportivas se inspeccionaron tambien en
ese runtime.
Tambien se validaron contra contenido real la grilla/detalle de Marketplace en
Puerto Montt y el marcador/detalle/foto de una junta activa en Osorno. El mapa
anuncia en espanol la cantidad visible y una regresion offline comprueba que una
respuesta tardia actualiza la fuente GeoJSON.

Comandos obligatorios:

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:connectedDebugAndroidTest
.\gradlew.bat :app:assembleDebug
```

APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Pendientes externos verificables

No son stubs ocultos de Android; requieren despliegue o credenciales externas:

- Bearer token emitido por login y validado por propietario;
- PostGIS, region de servidor, distancia y paginacion;
- comportamiento geografico completo y resultados utiles de `location.*`;
- portada `photo_id` poblada para publicaciones con foto;
- `marketplace.mine.list` y `junta.mine.list` multirregionales;
- comprobacion autenticada de Chat y listados propios;
- credenciales/scopes OAuth de Strava, Garmin y Wahoo.

La migracion, contratos, SQL sugerido, seguridad y casos de aceptacion estan en
`docs/BACKEND_IMPLEMENTATION_REPORT.md`.

## Riesgos tecnicos restantes

- `RemoteConnections.kt` y `BikesScreen.kt` siguen siendo grandes. Se redujo
  duplicacion de imagenes y legado, pero una division mayor debe hacerse con
  pruebas de regresion de bicicletas/mantenciones.
- El fallback regional del perfil solo ve la region activa; desaparece cuando
  el backend implemente los listados `mine`.
- Hasta que el listado entregue portada, las tarjetas nuevas pueden mostrar
  placeholder aunque el detalle tenga fotos.
- La integracion OAuth esta pausada; al reactivarla requerira credenciales,
  callbacks y una nueva prueba extremo a extremo por proveedor.
