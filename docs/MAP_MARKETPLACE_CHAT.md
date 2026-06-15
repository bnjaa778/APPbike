# Mapa, Marketplace y Chat

## Archivos

- `AppModels.kt`: `GeoPoint`, `MeetupEvent`, `UserChat`, `StoredMessage`,
  `ChatSyncMetadata` y campos remotos de `ProductPublication`.
- `LocalDataStore.kt`: ubicación, chats, mensajes y metadata por usuario.
- `RemoteConnections.kt`: contratos HTTP y parsers.
- `MapScreen.kt`: mapa, búsqueda, selección y creación de juntas.
- `MarketplaceScreen.kt`: búsqueda, grid, fotos y creación.
- `ChatScreen.kt`: tabs, listado, conversación y sincronización.
- `MainActivity.kt`: conexión de las pantallas con la sesión.

## Mapa

La pantalla usa MapLibre Native con el backend OpenGL y el estilo Liberty de
OpenFreeMap:

`https://tiles.openfreemap.org/styles/liberty`

Los datos provienen de OpenStreetMap. No requiere cuenta, API key ni
facturación. MapLibre muestra la atribución de los datos en el mapa.

Se usa el artefacto `android-sdk-opengl`; no reemplazarlo por `android-sdk` en
MapLibre 13, porque ese artefacto usa Vulkan y puede cerrar la app en
emuladores o dispositivos sin una implementación Vulkan estable.

1. Al entrar por primera vez solicita dirección o coordenadas `lat,lng`.
2. La ubicación elegida se guarda solo en `SharedPreferences`.
3. El mapa muestra un puntero central fijo. Tocar o desplazar cambia el centro.
4. Al confirmar el centro se consulta un radio fijo de 40 km.
5. Los resultados se dibujan como markers.
6. La búsqueda superior agrega `q` a la petición.

Endpoint esperado:

`GET AppBikeExternal.php/juntas?lat={lat}&lng={lng}&radiusKm=40&q={texto}`

Respuesta:

```json
{"ok":true,"juntas":[{"id":"...","title":"...","dateTime":"...","description":"...","latitude":0,"longitude":0,"createdBy":"...","createdAt":"..."}]}
```

## Crear junta

1. El botón `+` requiere sesión.
2. Se activa selección por toque y aparece un marker azul.
3. `Cancelar` descarta; `Confirmar` abre el formulario.
4. Se solicitan título, fecha/hora y descripción.
5. Se envían punto y `createdBy`.

Endpoint esperado:

`POST AppBikeExternal.php/juntas/events`

```json
{"title":"...","dateTime":"...","description":"...","latitude":0,"longitude":0,"createdBy":"uuid"}
```

## Marketplace

- Obtiene publicaciones de la ubicación local en un radio de 40 km.
- La barra superior busca mediante `q`.
- Presenta una cuadrícula de dos columnas.
- El botón `+` queda centrado abajo y requiere sesión.
- El formulario replica agregar bicicleta sin número de serie: nombre, marca,
  modelo, tipo, precio, descripción y fotografía.
- La creación usa multipart y el archivo se llama `foto`.
- El listado debe entregar metadata ligera. La app descarga la foto solamente
  cuando la tarjeta se renderiza.

Endpoints esperados:

- `GET AppBikeExternal.php/marketplace?lat&lng&radiusKm=40&q`
- `POST AppBikeExternal.php/marketplace` con `multipart/form-data`
- `POST AppBikeExternal.php` con `action=marketplace.photo.get`

Campos multipart:

- `title`
- `brand`
- `model`
- `product_type`
- `price`
- `description`
- `latitude`
- `longitude`
- `createdBy`
- `foto`: JPEG, PNG o WebP

Respuesta recomendada al crear:

```json
{"ok":true,"post":{"id":"...","title":"...","brand":"...","model":"...","product_type":"...","price":"...","description":"...","latitude":0,"longitude":0,"createdBy":"uuid","createdAt":"...","images":[{"photo_id":"..."}]}}
```

Respuesta del listado:

```json
{"ok":true,"posts":[{"id":"...","title":"...","brand":"...","model":"...","product_type":"...","price":"...","description":"...","latitude":0,"longitude":0,"createdBy":"uuid","createdAt":"...","images":[{"photo_id":"..."}]}]}
```

La app convierte cada `photo_id` en
`appbike-market-photo://<photo_id>` y solicita el contenido al abrir la tarjeta:

```json
{"action":"marketplace.photo.get","photo_id":"..."}
```

Respuesta:

```json
{"ok":true,"photo":{"photo_id":"...","mime_type":"image/webp","content_base64":"..."}}
```

Entidad:

```json
{"id":"...","title":"...","brand":"...","model":"...","product_type":"...","description":"...","price":"...","latitude":0,"longitude":0,"images":[{"photo_id":"..."}],"createdBy":"uuid","createdAt":"..."}
```

## Chats

Tabs:

- `SOCIAL`: juntas, amigos y grupos.
- `MARKETPLACE`: conversaciones asociadas a publicaciones.

Flujo:

1. Sin sesión no se consulta ni muestra información.
2. Con sesión se carga primero el listado local.
3. Se solicita el listado remoto y se actualiza la caché.
4. Al abrir un chat se leen mensajes locales.
5. Solo se consulta al servidor si local está vacío, tiene menos mensajes o una
   versión inferior.
6. La descarga usa `after={lastMessageId}` para pedir la diferencia.
7. Se guardan mensajes y metadata después de descargar o enviar.

Endpoints esperados:

- `GET AppBikeExternal.php/chats?userId={uuid}`
- `GET AppBikeExternal.php/chats/{chatId}/messages?userId={uuid}&after={id}`
- `POST AppBikeExternal.php/chats/{chatId}/messages`

Metadata local:

- último ID de mensaje
- cantidad de mensajes
- versión remota
- instante de última sincronización

## Pendientes de backend

- Implementar rutas REST/path-info anteriores en `external.txt`.
- Crear tablas `juntas/events`, `marketplace_posts`, `marketplace_images`,
  `chats`, `chat_participants` y `chat_messages`.
- Implementar geocodificación `GET AppBikeExternal.php/locations/search?q`.
- Aplicar consulta geoespacial de 40 km y ordenar por distancia/fecha.
- Validar que `createdBy`, participantes y remitente correspondan a la sesión.
- Aceptar subida multipart de fotos de Marketplace con el campo `foto`.
- Implementar `marketplace.photo.get` con `content_base64`.
- Entregar `messageCount`, `version` y `updatedAt` en cada chat.
- Entregar mensajes incrementales posteriores a `after`.
- Añadir paginación y límites a juntas, Marketplace y mensajes.
