# APPbike - Estado tecnico

Revision: 2026-07-22.

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

- AGP 8.13.2, Kotlin 2.0.21, Gradle 8.13.
- compile/target SDK 36, min SDK 24, Java/Kotlin 11.
- Compose Material 3, MapLibre OpenGL 13.0.2.
- `HttpURLConnection`, `org.json`, coroutines y SharedPreferences.
- Android Keystore AES-GCM para un `access_token` opcional.

## Arquitectura vigente

### Raiz y navegacion

`MainActivity.kt` mantiene sesion, destino actual, bicicletas y conexiones
deportivas. Gestiona el deep link `appbike://oauth/callback` y abre Chat con la
conversacion creada desde una junta o publicacion. Los datos semilla historicos
y `SyncScreen.kt` fueron eliminados.

### Cuenta

`Account.kt` valida IDs UUID y guarda solo identidad no secreta en preferencias.
`SecureTokenStore.kt` cifra con Android Keystore el token que el backend pueda
devolver. `ProfileContent.kt` implementa:

- publicaciones propias por estado;
- juntas propias actuales y anteriores;
- detalle, edicion, fotos, estado/completar y eliminacion.

Las conexiones Strava/Garmin/Wahoo usan acciones OAuth reales; ya no cambian un
boolean localmente. El servidor aun debe desplegarlas.

### Bicicletas

`BikesScreen.kt` carga por `user.bikes.list`, consulta `bike.get` antes del
detalle, descarga fotos mediante `bike.photo.get` y carga mantenciones de forma
independiente. No se alteraron sus contratos en esta fase.

### Ubicacion, mapa y Juntas

`DeviceLocationProvider.kt` obtiene GPS y geocodifica sin limitar la busqueda a
Chile. `GeoPoint` conserva pais, area administrativa, region y moneda cuando se
conocen. `LocalDataStore` persiste esos campos y hasta ocho ubicaciones recientes.

El selector Compose acepta Unicode, busca tras debounce y evita el ANR causado
por una ventana `AlertDialog` separada. La capa remota intenta primero
`location.search`/`location.reverse`; usa servicios del dispositivo y un fallback
temporal de Nominatim hasta que el backend este disponible.

MapLibre representa usuario, juntas y punto seleccionado con fuentes GeoJSON y
`SymbolLayer`; ya no usa `MarkerOptions` deprecado. El mapa publico muestra solo
juntas activas. El historial pasado aparece solo en el perfil del creador.

### Marketplace

Marketplace conserva una ubicacion independiente de Juntas, radio de 40 km,
busqueda y carga visible minima de 450 ms. El formulario:

- restringe producto a nuevo/usado/reacondicionado;
- muestra prefijo monetario fijo;
- agrupa miles y envia unidades enteras (`10.000` -> `10000`);
- envia `currency`, pais, area y coordenadas separadas.

La moneda de una publicacion se toma del registro, no de la ubicacion de busqueda
actual. El listado ya no ejecuta `marketplace.get` por tarjeta: espera un
`photo_id` de portada. `RemoteImageLoader.kt` comparte cache LRU, timeouts y
downsampling para fotos comunitarias.

### Chat

`ChatScreen.kt` carga cache por usuario, sincroniza listado, descarga mensajes
incrementales, elimina duplicados, persiste metadata y actualiza el resumen al
enviar. Marketplace/Juntas llaman `chat.get_or_create` y abren la conversacion.

Acciones primarias:

- `chat.get_or_create`
- `chat.list`
- `chat.messages.list`
- `chat.message.send`

Las rutas REST anteriores son fallback temporal. El servidor real aun responde
HTTP 400 para `chat.list`.

### Capa remota

`RemoteConnections.kt` centraliza transporte, errores, contratos y parsers. Las
mutaciones comunitarias incluyen `user_id` y todas las conexiones al gateway
agregan Bearer cuando hay token. El token nunca se envia al geocodificador
externo.

Los listados envian `lat`, `lng`, `radius_km`, `q`, `limit` y `offset`, paginan
hasta 500 elementos y conservan filtrado local para compatibilidad. El backend
debe asumir distancia y paginacion antes de escalar.

## Estado real del servidor

Verificacion de 2026-07-22:

- `marketplace.list`: HTTP 200, dos registros LAS.
- `junta.list`: HTTP 200, dos registros LAS.
- `chat.list`: HTTP 400.
- `location.search`: HTTP 400.
- `sports.connections.list`: HTTP 400.

Los listados comunitarios exponen `photo_folder_path`; Android lo ignora. El
backend debe retirarlo y entregar solo metadata publica de foto.

## Pruebas

Unitarias:

- historial de ubicaciones;
- precio entero, formato y moneda por pais;
- validacion UUID;
- parseo del formato de coordenadas comunitarias;
- merge incremental de mensajes sin duplicados.

Instrumentadas:

- parsers JSON de Marketplace y Chat;
- entrada Unicode `Viña del Mar` en el selector Compose;
- placeholders base del proyecto.

Comandos obligatorios:

```powershell
$env:JAVA_HOME='C:\Program Files\Unity\Hub\Editor\6000.4.10f1\Editor\Data\PlaybackEngines\AndroidPlayer\OpenJDK'
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
- `currency` persistida;
- portada `photo_id` en listados y retiro de rutas internas;
- `marketplace.mine.list` y `junta.mine.list` multirregionales;
- acciones/tablas de Chat;
- `location.search|reverse|resolve` cacheadas;
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
- Las integraciones OAuth no pueden completarse sin credenciales y registro de
  callbacks por proveedor.
