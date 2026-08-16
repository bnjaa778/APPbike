# APPbike - Integración vigente de ubicación, Juntas y Marketplace

Última verificación: 2026-07-22.

> Este documento conserva el contrato de compatibilidad del backend regional
> actualmente desplegado. La implementación nueva y autoritativa de geografía,
> moneda, autorización, Chat y OAuth está en
> `docs/BACKEND_IMPLEMENTATION_REPORT.md`.

## Arquitectura desplegada

- Gateway público: `Z:/var/www/api.zizzio.cl/APIS/AppBikeExternal.php`.
- API privada general:
  `Z:/srv/internal-auth/public/AppBikeInternal/AppBikeInternal.php`.
- Módulo privado comunitario:
  `Z:/srv/internal-auth/public/AppBikeInternal/AppBikeCommunity.php`.
- Tester público: `Z:/var/www/api.zizzio.cl/APIS/AppBikeApiTester.html`.

Android consume solamente el gateway público. `AppBikeExternal.php` conserva
CORS, lectura JSON/multipart, allowlist y reenvío; PostgreSQL y movimientos
entre tablas pertenecen al módulo privado.

## Región y ubicación

Los IDs de publicaciones, juntas y carpetas de fotos son regionales:
`REGION-<32 hex>`, por ejemplo `LAS-...`. Detalle, estado y fotos se enrutan por
ese prefijo; Android no recalcula la región después de crear.

En creación siempre se envían `region`, `user_id` y `location`. La integración
actual usa `LAS` cuando la ubicación local no trae un prefijo regional. Para
mantener puntos geográficos mientras el backend conserva `location` como texto,
Android escribe:

```text
REGION:<latitud>,<longitud>|<etiqueta>
```

El cliente puede así dibujar juntas y filtrar 40 km. Como mejora futura, el
backend debe almacenar latitud/longitud o `geography(Point,4326)`, resolver la
región desde coordenadas y aplicar distancia/paginación en PostgreSQL.

La ubicación elegida continúa guardándose solo en `LocalDataStore`; no forma
parte del perfil remoto. El geocodificador Android entrega sugerencias en vivo.
La captura automatica del dispositivo prioriza una medicion GPS fina, consulta
GPS/red/pasivo en paralelo y selecciona el punto fresco con menor radio de
precision; un punto conocido solo se usa durante cinco minutos. La accion
`Precisar` permite repetir esa medicion sin alterar la ubicacion persistida hasta
que el usuario la confirma.
La app intenta primero `location.search`/`location.reverse`. La comprobacion del
2026-08-06 confirma resultados normalizados en `location.search`, mientras
`location.reverse` aun puede responder HTTP 400 `unknown_region` para
coordenadas validas. El dispositivo y Nominatim siguen como respaldo temporal
con cache, timeout y limite de frecuencia.

## Marketplace

Todas las acciones son `POST` a `AppBikeExternal.php`.

### Crear

```json
{
  "action": "marketplace.create",
  "region": "LAS",
  "user_id": "uuid",
  "location": "LAS:-40.576401,-73.114802|Osorno",
  "title": "Casco MTB",
  "description": "...",
  "price": "25000",
  "product_status": "usado",
  "publication_status": "activa"
}
```

La respuesta usa `publication`. `publication_status` admite `activa`,
`vendida`, `pausada` y `en_revision`. Solo `activa` vive en
`market_publis_activas`; las demás viven en `market_publis_inactivas`.

### Precio y moneda

- Android envía `price` como unidades monetarias enteras sin separadores de
  miles. Una entrada visual `10.000` llega como `10000`.
- `marketplace.update` acepta una actualización parcial con
  `publication_id` y `price`; se usó para corregir el registro histórico
  `LAS-631D8FD73C4DB792F08B4DDA9BA09590` de `10.00` a `10000.00`.
- El backend actual no guarda moneda. Android ya envía `currency`, la parsea y
  usa CLP como fallback para registros históricos.
- Pendiente obligatorio: agregar `currency` ISO 4217 (`CLP`, `ARS`, etc.) a
  creación, actualización, detalle y listado. La moneda debe quedar asociada a
  la publicación al crearla y no reinterpretarse si el usuario cambia luego su
  ubicación de búsqueda.

### Listar, detalle y estado

- `marketplace.list`: `region`, `publication_status`, `limit`, `offset`;
  responde `publications`.
- `marketplace.get`: `publication_id`; responde `publication` y `photos`.
  Android ya no consulta detalle por tarjeta: el listado nuevo debe entregar
  `photo_id` de portada; mientras tanto se muestra placeholder.
- `marketplace.status.update`: `publication_id`, `publication_status`.
- `marketplace.update`: `publication_id` y los campos parciales que cambian;
  `price` usa unidades enteras sin formato localizado.

Un cambio de estado mueve el mismo ID entre tablas. La app vuelve a solicitar
detalle y listado después del cambio.

### Fotos

La publicación se crea primero. Después se llama multipart
`marketplace.photo.upload` con `publication_id` y el archivo bajo `foto`.

Para descargar no basta `photo_id`: el backend necesita ambos campos:

```json
{
  "action": "marketplace.photo.get",
  "publication_id": "LAS-...",
  "photo_id": "hash.png"
}
```

La respuesta devuelve `content_base64`. Android usa internamente
`appbike-market-photo://<publication_id>/<photo_id>` y nunca expone
`photo_folder_path` o `file_path` como URL pública.

## Juntas

### Crear

```json
{
  "action": "junta.create",
  "region": "LAS",
  "user_id": "uuid",
  "location": "LAS:-40.574401,-73.114802|Punto de encuentro en Osorno",
  "title": "Salida de prueba",
  "description": "...",
  "junta_status": "activa"
}
```

La respuesta usa `junta`. Estados válidos: `activa` y `pasada`.

### Listar, detalle y completar

- `junta.list`: `region`, `junta_status`, `limit`, `offset`; responde `juntas`.
- `junta.get`: `junta_id`; responde `junta` y `photos`.
- `junta.status.update`: `junta_id`, `junta_status`.
- `junta.complete`: `junta_id`; cambia a `pasada`.

`activa` vive en `juntas_activas`; `pasada` vive en `juntas_pasadas`. Android
refresca detalle y lista después de mover o completar.

### Fotos

`junta.photo.upload` usa multipart con `junta_id` y `foto`. La descarga requiere
`junta_id` y `photo_id`:

```json
{
  "action": "junta.photo.get",
  "junta_id": "LAS-...",
  "photo_id": "hash.webp"
}
```

El URI interno es `appbike-junta-photo://<junta_id>/<photo_id>`.

## Archivos y límites

- Tipos aceptados: JPG, PNG y WebP.
- Tamaño máximo: 5 MB.
- Campo multipart obligatorio: `foto`.
- Marketplace:
  `AppBikeInternal/Region_<REGION>/MarketplacePhotos/<user UUID>/<folder ID>/`.
- Juntas:
  `AppBikeInternal/Region_<REGION>/JuntasPhotos/<user UUID>/<folder ID>/`.

## Verificación realizada

- `marketplace.list/get/status.update` respondieron con datos regionales reales.
- `junta.create/list/get/complete/status.update` se probaron y refrescaron al
  mover el registro entre tablas.
- Para ambas entidades se verificó el ciclo temporal crear → subir foto →
  listar/detalle → `photo.get` → eliminar.
- Los bytes Base64 descargados coincidieron con el archivo subido.
- Los registros temporales se eliminaron.
- Quedaron disponibles como datos semilla una publicación activa y una junta
  activa con coordenadas cerca de la ubicación de prueba.

## Pendientes recomendados

- Resolver región desde coordenadas en el backend en lugar del valor inicial
  `LAS` del cliente.
- Agregar columnas geográficas y ordenar por distancia/fecha en PostgreSQL.
- Trasladar geocodificación a un proveedor controlado y cacheado por backend.
- Aplicar autorización del propietario a cambios de estado, fotos y borrado si
  todavía no se valida mediante sesión/token en el gateway.
- Completar las rutas y tablas de chat, que siguen fuera del módulo comunitario
  desplegado.
