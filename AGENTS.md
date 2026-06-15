# APPbike - Router de trabajo

Este archivo es la entrada principal para cualquier agente que trabaje en APPbike.
Antes de editar, identifica el tipo de tarea en la tabla de rutas y abre solamente
los archivos indicados.

## Estado actual

- La app usa Jetpack Compose.
- La interfaz principal usa una barra inferior persistente.
- El orden de navegación es: Mapas, Bicicletas, Marketplace y Chat.
- Cuenta y sincronización deportiva comparten una misma pantalla accesible desde
  el botón de perfil de la cabecera.
- La API publica es `https://api.zizzio.cl/APIS/AppBikeExternal.php`.
- Las cuentas usan UUID en formato texto.
- Las bicicletas se cargan por `user_id`.
- El detalle de una bicicleta se vuelve a consultar por `bike_id`.
- Las mantenciones se cargan de forma independiente al bajar dentro del detalle.
- Las fotos se suben junto con `bike.create` usando multipart y el campo `foto`.
- Android esta preparado para descargar fotos mediante `bike.photo.get`.
- El cambio de `internal.txt` que agrega `content_base64` debe estar desplegado en
  el servidor para que las fotos remotas aparezcan.

## Router de tareas

| Si la tarea trata de... | Abrir primero | Revisar despues |
|---|---|---|
| Navegacion, cabecera o barra inferior | `MainActivity.kt` | `AppModels.kt`, `CommonComponents.kt` |
| Login, sesion o sincronizacion deportiva | `Account.kt` | `RemoteConnections.kt`, `MainActivity.kt` |
| Listado "Mis bicicletas" | `BikesScreen.kt` | `RemoteConnections.kt`, `AppModels.kt` |
| Crear bicicleta | `BikesScreen.kt` | `RemoteConnections.kt`, backend `internal.txt` |
| Abrir detalle de bicicleta | `BikesScreen.kt` | `RemoteConnections.loadBikeDetails` |
| Fotos de bicicletas | `RemoteConnections.kt` | `BikesScreen.kt`, backend `internal.txt` |
| Mantenciones pasadas | `BikesScreen.kt` | `RemoteConnections.loadMaintenance` |
| Servicios futuros | `BikesScreen.kt` | `RemoteConnections.bookService` |
| Mapa, ubicacion o juntas | `MapScreen.kt` | `LocalDataStore.kt`, `RemoteConnections.kt`, `docs/MAP_MARKETPLACE_CHAT.md` |
| Marketplace | `MarketplaceScreen.kt` | `RemoteConnections.kt`, `docs/MAP_MARKETPLACE_CHAT.md` |
| Chats o sincronizacion local | `ChatScreen.kt` | `LocalDataStore.kt`, `RemoteConnections.kt`, `docs/MAP_MARKETPLACE_CHAT.md` |
| Modelos o campos JSON | `AppModels.kt` | `RemoteConnections.bikeFromJson` |
| Errores de red o API | `RemoteConnections.kt` | `userFriendlyError` y respuesta del backend |
| Tema visual | `ui/theme/Theme.kt` | `res/values/themes.xml` |
| Permisos Android | `AndroidManifest.xml` | `app/build.gradle.kts` |
| Compilacion o APK | `app/build.gradle.kts` | `local.properties`, SDK y JDK |

Todos los archivos Kotlin principales estan en:

`app/src/main/java/com/example/appbike/`

## Flujo de navegacion

`MainActivity.kt` contiene `AppBikeApp`, que funciona como router de pantallas
mediante `AppScreen`. El `Scaffold` principal es dueño de la cabecera y de la
barra inferior; las pantallas hijas no deben crear otra navegación principal.

Flujo principal:

1. La app inicia en `ROUTES`, presentado como "Mapas".
2. La barra inferior muestra, en este orden: `ROUTES`, `BIKES`, `MARKETPLACE`,
   `CHAT`.
3. El botón de perfil de la cabecera abre `ACCOUNT`.
4. `ACCOUNT` contiene tanto la cuenta como las conexiones deportivas.
5. Si no existe sesion, "Mis bicicletas" envia al usuario a `ACCOUNT`.
6. Al cerrar sesion se limpian bicicletas, recordatorios y reservas en memoria.

No agregues navegación principal ni botones "Volver" a Mapas, Bicicletas,
Marketplace o Chat. Agrega el destino a `AppScreen`, resuélvelo en `AppBikeApp`
y decide explícitamente si pertenece a la barra inferior o a un flujo
secundario.

## Cuentas

Archivos:

- `Account.kt`
- `AppModels.kt`
- `RemoteConnections.kt`
- `MainActivity.kt`

Contrato:

- `AccountSession.userId` es `String`, no `Long`.
- El ID debe ser un UUID valido.
- `AccountStore` guarda `user_id` y `email` en SharedPreferences.
- Una sesion numerica antigua se considera invalida y se elimina.
- Login llama la accion `login` con `usuario` y `password`.

Al cambiar login, verifica siempre:

- Respuesta `user.id`, `user_id` o `id`.
- Persistencia de la sesion.
- Carga de bicicletas de la cuenta.
- Limpieza de datos al cerrar sesion.

### Sincronización deportiva

La sincronización ya no es una pantalla principal independiente. Sus tarjetas
se renderizan dentro de `AccountScreen` usando la lista `platforms` mantenida
por `AppBikeApp`.

- Plataformas iniciales: Strava, Garmin y Wahoo.
- Cada tarjeta muestra su estado y permite conectar o quitar la conexión.
- La barra inferior debe permanecer visible mientras se consulta esta sección.

## Interfaz visual

- Paleta y esquemas de color: `ui/theme/Color.kt` y `ui/theme/Theme.kt`.
- Tipografía: `ui/theme/Type.kt`.
- Contenedores, tarjetas y campos comunes: `CommonComponents.kt`.
- Los iconos de navegación provienen de `material-icons-extended`.
- La cabecera debe respetar `statusBarsPadding()` por el modo edge-to-edge.
- Mantener el color verde como acento y superficies cálidas de alto contraste.

## Mapa, Marketplace y Chat

La especificación, contratos y TODOs de backend están en:

`docs/MAP_MARKETPLACE_CHAT.md`

Reglas:

- La ubicación elegida vive solo en `LocalDataStore`; no enviarla como perfil.
- Consultar juntas y publicaciones con centro, radio de 40 km y búsqueda.
- El mapa usa MapLibre Native OpenGL con OpenFreeMap y no requiere API key.
- No introducir publicaciones o juntas mock en las pantallas conectadas.
- Marketplace crea publicaciones multipart con fotografía en el campo `foto`.
- Los listados de Marketplace deben devolver `photo_id`, no imágenes base64.
- Chat requiere sesión y usa caché separada por `userId` y `chatId`.
- Antes de descargar mensajes comparar cantidad, versión y caché local.

## Mis bicicletas

### Listado

`BikesScreen` observa `account.userId` y llama:

`RemoteConnections.loadUserBikes(userId)`

Endpoint:

`user.bikes.list`

Campos enviados:

- `user_id`
- `limit`
- `offset`

Nunca reemplazar esta consulta por `bike.list` sin filtro. Cada cuenta debe ver
solamente sus bicicletas.

### Detalle

Al tocar una tarjeta no se debe abrir directamente el objeto resumido. Primero
se llama:

`RemoteConnections.loadBikeDetails(account, bike)`

Endpoint:

`bike.get`

La respuesta se valida para asegurar:

- El `bike_id` devuelto coincide con el solicitado.
- El `user_id` pertenece a la cuenta activa.
- La imagen y los datos locales utiles se conservan si faltan en la respuesta.

Mientras carga, se muestra el dialogo "Cargando informacion de la bicicleta...".

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
- Archivo multipart `foto`

No cambiar el nombre multipart `foto`: es el campo esperado por el backend.

## Fotografias

### Problema encontrado

El backend entrega rutas como:

`BikesPhotos/PersonalBikesPhotos/<photo_id>`

La URL construida bajo `https://api.zizzio.cl/` responde HTTP 404 porque esa
carpeta interna no esta publicada por el servidor web.

### Solucion implementada

Android usa una fuente interna:

`appbike-photo://<photo_id>`

Cuando `BikeImageFrame` encuentra ese esquema:

1. Llama `RemoteConnections.loadBikePhoto(photoId)`.
2. Ejecuta `bike.photo.get`.
3. Lee `photo.content_base64`.
4. Decodifica los bytes.
5. Ajusta el bitmap y lo muestra.

El backend de referencia fue actualizado en:

`C:/Users/Xinerdev/Desktop/internal.txt`

`procesar_bike_photo_get` ahora debe agregar:

`photo.content_base64`

La accion `bike.photo.get` ya esta permitida por `external.txt`, por lo que no
requiere una ruta publica nueva.

Importante: si la app muestra "No fue posible cargar la imagen", comprobar
primero que el `internal.txt` actualizado este desplegado. La API desplegada
seguia respondiendo sin `content_base64` al realizar la ultima verificacion.

## Mantenciones

La carga de mantenciones no debe recargar todas las bicicletas.

Dentro de `BikeDetailDialog`:

1. El detalle se abre con la informacion basica.
2. Al bajar aproximadamente 12% del contenido se activa `snapshotFlow`.
3. Se llama solamente `RemoteConnections.loadMaintenance(account, bike)`.
4. Se reemplazan las mantenciones de ese `bike_id`.
5. El boton "Actualizar mantenciones" permite repetir la consulta.

Endpoints:

- `maintenance.past.list`
- `maintenance.future.list`
- `maintenance.past.create`
- `maintenance.future.create`

Los listados reciben `user_id` y `bike_id`. Los elementos se relacionan en la
app mediante `bikeId`, no solamente por el nombre de la bicicleta.

Las fechas enviadas deben usar `AAAA-MM-DD`.

## Mapeo de datos

`RemoteConnections.bikeFromJson` traduce:

- `id` -> `Bike.remoteId`
- `user_id` -> `Bike.userId`
- `bike_custom_name` -> `Bike.name`
- `bike_brand` -> `Bike.brand`
- `bike_model` -> `Bike.model`
- `bike_type` -> `Bike.type`
- `serial_number` -> `Bike.serialNumber`
- `photo_id` -> `Bike.photoId`
- `last_maintenance_date` -> `Bike.lastMaintenance`
- `next_maintenance_date` -> `Bike.nextMaintenance`

Cuando el servidor cambie un campo, actualiza juntos el modelo, el parser y la
documentacion de este archivo.

## Backend de referencia

Archivos externos al repositorio Android:

- `C:/Users/Xinerdev/Desktop/tester.txt`
- `C:/Users/Xinerdev/Desktop/external.txt`
- `C:/Users/Xinerdev/Desktop/internal.txt`

Responsabilidades:

- `tester.txt`: pagina piloto para probar acciones y contratos.
- `external.txt`: API publica, validacion de acciones y reenvio.
- `internal.txt`: PostgreSQL, usuarios, bicicletas, fotos y mantenciones.

Estos archivos son referencia/despliegue y no forman parte del APK.

## Verificacion obligatoria

Despues de cambiar cuentas, bicicletas, fotos o mantenciones:

1. Compilar `:app:assembleDebug`.
2. Instalar `app/build/outputs/apk/debug/app-debug.apk`.
3. Iniciar sesion.
4. Abrir "Mis bicicletas".
5. Confirmar que solo aparecen bicicletas de la cuenta.
6. Abrir una bicicleta y comprobar sus campos.
7. Confirmar que la foto se muestra.
8. Bajar hasta mantenciones y comprobar la actualizacion independiente.
9. Revisar que no exista `FATAL EXCEPTION`.

El proyecto requiere Java 11 o superior. En este equipo se verifico con el JDK
y el SDK incluidos en Unity:

- JDK: `C:/Program Files/Unity/Hub/Editor/6000.4.10f1/Editor/Data/PlaybackEngines/AndroidPlayer/OpenJDK`
- SDK: `C:/Program Files/Unity/Hub/Editor/6000.4.10f1/Editor/Data/PlaybackEngines/AndroidPlayer/SDK`

Si se cambia temporalmente `local.properties`, restaurar al terminar:

`sdk.dir=C\:\\Users\\Xinerdev\\AppData\\Local\\Android\\Sdk`

## Reglas para futuros agentes

- Mantener las solicitudes de red fuera del hilo principal.
- No cargar todas las bicicletas para actualizar una sola mantencion.
- No confiar en el nombre de una bicicleta como identificador.
- Validar propiedad con `user_id` antes de mostrar un detalle.
- No guardar contraseñas; solo UUID y correo de la sesion.
- No exponer rutas fisicas internas del servidor como URLs publicas.
- Mostrar errores de API mediante `RemoteConnections.userFriendlyError`.
- No revertir cambios existentes no relacionados.
- Actualizar este router cuando cambie un flujo, endpoint o responsabilidad.
