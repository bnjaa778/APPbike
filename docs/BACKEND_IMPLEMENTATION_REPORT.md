# APPbike - Implementación pendiente del backend

Fecha del contrato: 2026-07-22. Estado remoto revalidado: 2026-08-06.

Este documento es el relevo operativo para el equipo que mantiene
`AppBikeExternal.php`, `AppBikeInternal.php` y `AppBikeCommunity.php`. Android ya
consume los campos y acciones descritos aquí con compatibilidad hacia el backend
actual. No se debe cambiar el nombre del gateway público ni exponer las APIs
privadas.

## 1. Estado comprobado

Prueba real contra `https://api.zizzio.cl/APIS/AppBikeExternal.php` del
2026-08-06, que reemplaza la fotografía de despliegue del 2026-07-22:

- `marketplace.list`, `junta.list`, `marketplace.get` y `junta.get` responden
  HTTP 200 con las colecciones y objetos esperados.
- `junta.photo.get` responde HTTP 200 con `content_base64`.
- `location.search` devuelve sugerencias normalizadas con `latitude`,
  `longitude`, país, área, región y `currency`; `location.resolve` responde
  HTTP 200. `location.reverse` responde HTTP 400 `unknown_region` incluso para
  coordenadas válidas de Santiago.
- `user.bikes.list`, `marketplace.mine.list`, `junta.mine.list`, `chat.list` y
  `sports.connections.list` sin token responden HTTP 401. Esto confirma que son
  privadas, pero no permite validar su resultado con una sesión real.
- El tester publico anuncia `bike.update` y `bike.delete`; Android ya consume
  ambas acciones con validacion de ID/propietario. El tester no anuncia ninguna
  accion para crear cuenta ni recuperar/restablecer contrasena.
- La muestra de Marketplace contiene `latitude`, `longitude`, `country_code`,
  `administrative_area`, `currency` y la clave `photo_id`, y ya no contiene
  `photo_folder_path`. Los listados observados no tenían portada poblada; el
  detalle sí contiene el array `photos`. La muestra de Junta sí tenía `photo_id`
  y su descarga Base64 fue válida.

Las secciones siguientes siguen siendo el contrato objetivo para todo punto que
no haya sido validado expresamente en esta comprobación.

Marketplace y Juntas existentes deben seguir funcionando durante la migración.
Las acciones nuevas se agregan a la allowlist pública, al switch privado, al
tester y al router del backend en el mismo despliegue.

## 2. Autenticación y autorización obligatorias

`user_id` no autentica a una persona: cualquiera puede falsificarlo. El backend
debe emitir un token opaco o JWT después de `login`:

```json
{
  "ok": true,
  "user": { "id": "uuid", "email": "correo" },
  "access_token": "secreto",
  "expires_in": 3600
}
```

Android guarda el token cifrado con Android Keystore y lo envía como:

```http
Authorization: Bearer <token>
```

Implementación requerida:

1. El gateway debe reenviar `Authorization` a la API privada.
2. La API privada valida firma/hash, expiración, revocación y usuario.
3. En cada mutación, el usuario autenticado debe coincidir con el propietario
   real leído desde PostgreSQL. No confiar en el `user_id` del body.
4. Aplicar esto a `update`, `delete`, cambios de estado, completar juntas y
   subir/eliminar fotos.
5. Las lecturas privadas `*.mine.list`, Chat y conexiones deportivas también
   exigen token.
6. Nunca registrar tokens, códigos OAuth ni refresh tokens en logs.

Se puede mantener temporalmente `user_id` en los cuerpos para compatibilidad,
pero solo como dato que se compara contra el actor autenticado.

### 2.1 Alta y recuperacion de cuenta

Para que un usuario nuevo pueda completar el onboarding sin provision manual,
el backend debe definir antes que Android lo implemente:

- una accion de registro con correo, nombre de usuario, contrasena y reglas de
  verificacion;
- inicio y confirmacion de recuperacion de contrasena con token de un solo uso;
- limites de frecuencia, expiracion, mensajes neutrales contra enumeracion de
  cuentas y contrato de errores;
- incorporacion coordinada al gateway, allowlist/switch privado y tester.

No inventar nombres de acciones ni guardar contrasenas/tokens de recuperacion en
Android mientras este contrato no exista.

## 3. Geografía, región y moneda

### 3.1 Modelo

Habilitar PostGIS y agregar, como mínimo, a las cuatro tablas comunitarias de
cada esquema regional:

```sql
CREATE EXTENSION IF NOT EXISTS postgis;

ALTER TABLE "Region_LAS".market_publis_activas
  ADD COLUMN IF NOT EXISTS latitude double precision,
  ADD COLUMN IF NOT EXISTS longitude double precision,
  ADD COLUMN IF NOT EXISTS geo geography(Point, 4326),
  ADD COLUMN IF NOT EXISTS country_code char(2),
  ADD COLUMN IF NOT EXISTS administrative_area text,
  ADD COLUMN IF NOT EXISTS currency char(3);

CREATE INDEX IF NOT EXISTS market_activas_geo_gix
  ON "Region_LAS".market_publis_activas USING gist (geo);
```

Repetir para `market_publis_inactivas`, `juntas_activas` y `juntas_pasadas`, y
para cada esquema habilitado. `currency` solo corresponde a Marketplace. Usar
un trigger o la escritura de negocio para mantener:

```sql
geo = ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)::geography
```

Durante el backfill se puede extraer el formato legado
`REGION:<lat>,<lng>|<etiqueta>`. Si no es parseable, conservar `location` y dejar
`geo` nulo; nunca inventar coordenadas.

### 3.2 Regiones

Crear una fuente autoritativa, no una lista dispersa en PHP:

```text
community_regions
- code char(3) primary key
- country_code char(2)
- name text
- schema_name text unique
- boundary geography(MultiPolygon,4326) nullable
- active boolean

community_region_aliases
- region_code char(3)
- normalized_alias text
```

Resolución:

1. Buscar `ST_Covers(boundary, point)` cuando exista polígono.
2. Como respaldo, usar `country_code` + área administrativa normalizada.
3. Rechazar creación con región desconocida; no caer silenciosamente en `LAS`.
4. El ID regional se genera solo después de resolver la región.
5. Detalle, fotos y estado siguen enrutándose por el prefijo del ID; nunca se
   recalcula la región de un registro existente.

### 3.3 Acciones de ubicación

Agregar estas acciones POST:

#### `location.search`

Entrada:

```json
{ "action": "location.search", "q": "Viña del Mar", "limit": 5 }
```

Respuesta:

```json
{
  "ok": true,
  "locations": [{
    "label": "Viña del Mar, Valparaíso, Chile",
    "latitude": -33.0245,
    "longitude": -71.5518,
    "country_code": "CL",
    "administrative_area": "Valparaíso",
    "region_code": "VAL",
    "currency": "CLP"
  }]
}
```

#### `location.reverse`

Entrada: `lat`, `lng`. Respuesta: un objeto `location` con el mismo esquema.

#### `location.resolve`

Entrada: `lat`, `lng`, y opcionalmente `country_code`, `administrative_area` y
`label`. Debe resolver `region_code` y `currency` sin depender de texto libre.

El proveedor externo se consulta exclusivamente desde el backend. Guardar caché
normalizada con respuesta, proveedor, instante y expiración; aplicar límite por
IP/usuario, timeout y atribución. No usar el Nominatim público para autocomplete
de alta frecuencia sin un servicio compatible con su política.

## 4. Marketplace

### 4.1 Crear y actualizar

Android ya envía, además del contrato vigente:

```json
{
  "latitude": -40.576401,
  "longitude": -73.114802,
  "country_code": "CL",
  "administrative_area": "Los Lagos",
  "currency": "CLP"
}
```

Validar:

- latitud `[-90,90]`, longitud `[-180,180]`;
- `currency` ISO 4217 permitida para el país/mercado;
- `price` decimal no negativo, interpretado como unidades de la moneda, nunca
  desde separadores localizados;
- `product_status` en `nuevo|usado|reacondicionado`;
- `publication_status` en `activa|pausada|vendida|en_revision`.

La moneda queda fijada al crear. Cambiar la ubicación de búsqueda del usuario no
puede reinterpretar publicaciones existentes.

### 4.2 Listado geoespacial

`marketplace.list` acepta:

```json
{
  "region": "LAS",
  "publication_status": "activa",
  "lat": -40.57,
  "lng": -73.11,
  "radius_km": 40,
  "q": "casco",
  "limit": 100,
  "offset": 0
}
```

Aplicar `ST_DWithin(geo, center, radius_km * 1000)`, búsqueda parametrizada en
título/descripción/estado, orden por distancia y fecha, y paginación estable.
Cada elemento debe incluir:

```text
id, user_id, title, description, price, currency,
product_status, publication_status, latitude, longitude,
country_code, administrative_area, region, location,
distance_km, created_at, photo_id
```

`photo_id` es solo la portada. No devolver Base64 en el listado y no devolver
`photo_folder_path`, `file_path` ni otra ruta interna. Con esto Android evita una
consulta `marketplace.get` por cada tarjeta.
Android no convierte rutas internas en URL: las descarta. La compatibilidad con
URL directa se limita a HTTPS bajo dominios Zizzio, por lo que cualquier CDN
futura debe agregarse explícitamente al contrato y a la política del cliente.

### 4.3 Contenido propio

Agregar `marketplace.mine.list`:

```json
{
  "action": "marketplace.mine.list",
  "user_id": "uuid",
  "publication_status": "activa",
  "limit": 100,
  "offset": 0
}
```

El usuario real sale del token. Debe consultar todas sus regiones y ambas tablas,
con filtro opcional de estado. Responde `publications` con el mismo modelo del
listado. Cada elemento debe incluir `user_id` y coincidir con el actor autenticado;
Android rechaza toda la respuesta si no puede comprobar esa propiedad. Android
mantiene un fallback regional mientras se despliega esta acción.

## 5. Juntas

`junta.create` y `junta.update` deben aceptar los campos geográficos separados
del apartado 3. El listado acepta `lat`, `lng`, `radius_km`, `q`, `limit` y
`offset`, usa PostGIS y devuelve:

```text
id, user_id, title, description, junta_status,
latitude, longitude, country_code, administrative_area,
region, location, distance_km, created_at, photo_id
```

Agregar `junta.mine.list`, autenticada y multirregional, con filtro opcional
`junta_status=activa|pasada`. Es el origen del historial del creador en el
perfil. Cada junta debe incluir el `user_id` del actor autenticado; no devolver
registros sin propietario. Las juntas pasadas no se mezclan con el mapa público.

`junta.complete`, `junta.status.update`, `junta.update`, `junta.delete` y fotos
deben validar propiedad. Los movimientos entre tablas deben conservar el mismo
ID, moneda si aplica, geografía, fotos y fechas dentro de una transacción.

## 6. Fotos comunitarias

- Mantener multipart bajo el campo exacto `foto`.
- Máximo 5 MB; aceptar JPG, PNG y WebP validando contenido real, no solo MIME.
- Validar propietario antes de subir o borrar.
- `photo.get` puede ser lectura pública de una entidad pública, pero debe usar
  IDs y devolver `content_base64`, nunca una ruta del disco.
- `*.photo.list` y `*.get` devuelven metadata: `photo_id`, MIME, tamaño, orden y
  si es portada.
- Al borrar una entidad, resolver fotos con una estrategia transaccional y
  recuperable; no dejar carpetas huérfanas.

## 7. Chat

### 7.1 Tablas sugeridas

```sql
CREATE TABLE public.appbike_chats (
  id uuid PRIMARY KEY,
  chat_type text NOT NULL CHECK (chat_type IN ('social','marketplace')),
  related_entity_id text,
  title text NOT NULL DEFAULT '',
  version bigint NOT NULL DEFAULT 1,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE public.appbike_chat_participants (
  chat_id uuid REFERENCES public.appbike_chats(id) ON DELETE CASCADE,
  user_id uuid REFERENCES public.users(id) ON DELETE CASCADE,
  joined_at timestamptz NOT NULL DEFAULT now(),
  last_read_message_id bigint,
  PRIMARY KEY (chat_id, user_id)
);

CREATE TABLE public.appbike_chat_messages (
  id bigserial PRIMARY KEY,
  chat_id uuid REFERENCES public.appbike_chats(id) ON DELETE CASCADE,
  sender_id uuid REFERENCES public.users(id),
  client_message_id uuid NOT NULL,
  content text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (sender_id, client_message_id)
);

CREATE INDEX appbike_chat_messages_incremental
  ON public.appbike_chat_messages(chat_id, id);
```

### 7.2 Acciones

#### `chat.get_or_create`

Entrada:

```json
{
  "action": "chat.get_or_create",
  "user_id": "actor",
  "participant_user_id": "vendedor-u-organizador",
  "chat_type": "marketplace",
  "related_entity_id": "LAS-...",
  "title": "Casco MTB"
}
```

Validar que la entidad exista y que `participant_user_id` sea su propietario.
Imponer una clave lógica única por tipo, entidad y conjunto de participantes para
que dos toques no creen chats duplicados. Responder `chat`.

#### `chat.list`

Entrada: `user_id`, `limit`, `offset`. Responde `chats` con:

```text
id, type, title, participants, related_entity_id,
last_message, message_count, version, updated_at
```

Solo miembros pueden leer el chat.

#### `chat.messages.list`

Entrada: `user_id`, `chat_id`, `after_message_id`, `limit`. Responde mensajes
ordenados ascendentemente y metadata `message_count`, `version` y `has_more`.

#### `chat.message.send`

Entrada: `user_id`, `chat_id`, `content`, `client_message_id`. Validar miembro,
longitud y contenido. `client_message_id` hace el reintento idempotente. Incrementar
la versión del chat y responder `message`.

Android intenta primero estas acciones. Como compatibilidad transitoria conserva
las rutas REST antiguas `/chats/...`, que pueden retirarse después de desplegar y
probar las acciones POST.

## 8. OAuth deportivo

Los botones de Strava, Garmin y Wahoo ya no simulan una conexión local. Android
espera estas acciones autenticadas:

- `sports.connections.list`
- `sports.oauth.start`
- `sports.oauth.complete`
- `sports.connection.delete`

`sports.oauth.start` recibe `provider` y
`redirect_uri=appbike://oauth/callback`, genera un `state` de un solo uso con
expiración y responde:

```json
{
  "ok": true,
  "oauth": {
    "authorization_url": "https://proveedor/...",
    "state": "aleatorio"
  }
}
```

El callback hacia la app debe contener `provider`, `code`, `state` o `error`.
`sports.oauth.complete` intercambia el código exclusivamente desde el servidor.
Guardar access/refresh tokens cifrados en reposo, scopes, expiración y cuenta
externa. Credenciales de cliente solo en variables/secret manager del servidor.

Al desconectar, revocar en el proveedor cuando sea posible y borrar tokens. La
sincronización de actividades debe ser un job idempotente con cursor por proveedor;
no ejecutar llamadas largas dentro de la respuesta HTTP del móvil.

Para habilitar cada proveedor hacen falta sus credenciales, scopes aprobados y
URLs de callback registradas. Sin esos datos el backend debe responder un error
claro, no marcar la plataforma como conectada.

## 9. Gateway y errores

Agregar todas las acciones nuevas a la allowlist. Conservar JSON UTF-8 y formato:

```json
{ "ok": false, "code": "forbidden", "message": "Mensaje seguro" }
```

Códigos HTTP mínimos: 400 validación, 401 token ausente/inválido, 403 propiedad,
404 entidad, 409 conflicto/idempotencia y 429 límite. No enviar SQL, rutas,
credenciales ni stack traces al cliente.

## 10. Orden de despliegue

1. Backup y migraciones aditivas de geografía/moneda.
2. Backfill verificable desde `location` legado.
3. Autenticación Bearer y autorización de propietario.
4. Listados con campos nuevos, portada y sin rutas internas.
5. `location.*` y tabla de regiones.
6. `*.mine.list`.
7. Chat y sus índices.
8. OAuth por proveedor, uno a la vez.
9. Actualizar tester y documentación.
10. Retirar fallbacks solo después de una versión Android desplegada y verificada.

## 11. Datos y pruebas de aceptación

Conservar o crear:

- una publicación activa con precio `10000`, `currency=CLP`, coordenadas reales
  y una foto existente;
- una junta activa con coordenadas reales y una foto;
- un segundo usuario para probar Chat y autorización negativa.

Pruebas obligatorias:

1. `10.000` desde Android termina como `10000` y `CLP`.
2. Cambiar ubicación de búsqueda a Argentina no cambia la moneda guardada.
3. Radio 40 km excluye un punto lejano y ordena por `distance_km`.
4. Búsqueda con `Viña del Mar` conserva `ñ` y devuelve coordenadas.
5. Un usuario no puede editar, completar, borrar ni subir fotos a contenido ajeno.
6. Mover estado conserva ID, geografía, moneda y fotos.
7. Listados no contienen `photo_folder_path` ni Base64; sí contienen `photo_id`.
8. `marketplace.mine.list` incluye los cuatro estados y todas las regiones del actor.
9. `junta.mine.list` incluye activas y pasadas del actor.
10. `chat.get_or_create` es idempotente; un tercero no puede leerlo.
11. Repetir `chat.message.send` con el mismo `client_message_id` no duplica.
12. OAuth rechaza `state` vencido/reutilizado y nunca devuelve tokens del proveedor
    a Android.
13. Tester público cubre camino feliz y 401/403/404 de cada acción nueva.

## 12. Criterio de finalización para el backend

El trabajo se considera terminado cuando las acciones nuevas responden a través
del gateway público, las pruebas anteriores pasan, el tester fue actualizado y
Android puede:

- listar por distancia sin filtrar toda la región localmente;
- mostrar portadas sin N+1;
- administrar contenido propio multirregional;
- abrir un chat desde una publicación o junta y enviar mensajes incrementales;
- conectar/desconectar al menos un proveedor deportivo real;
- rechazar toda mutación de un usuario que no sea propietario.
