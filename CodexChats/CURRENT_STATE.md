# Estado actual de APPbike

Actualizado: 2026-08-11.

## Aplicación

- Android nativo con Jetpack Compose, paquete `com.example.appbike`.
- Entrada: `MainActivity.kt`.
- Destinos principales: Mapas, Bicicletas, Marketplace y Chat.
- API pública: `https://api.zizzio.cl/APIS/AppBikeExternal.php`.
- La sesión usa UUID como identidad interna y Bearer token cifrado con Android
  Keystore. `nombre_de_usuario` es únicamente la identidad visible.

## Funciones conectadas

### Cuenta

- Login por correo o nombre de usuario.
- El formulario de acceso aparece antes del panel promocional cuando no hay
  sesión y una identidad no UUID devuelta por el servidor se rechaza.
- `AccountSession.username` parsea `nombre_de_usuario`.
- Cuentas antiguas sin nombre consultan `user.get` y deben completar
  `user.username.update`.
- Cuenta muestra un acceso a ajustes si las notificaciones están desactivadas.
- La sincronización deportiva está pausada por decisión de producto. Strava,
  Garmin y Wahoo se muestran como tarjetas no interactivas con cinta diagonal
  `PRÓXIMAMENTE`; Android no ejecuta acciones OAuth mientras siga detenida.
- El bloque deportivo aparece antes de `Tu actividad`, de modo que las tres
  plataformas se descubren juntas antes de los listados propios extensos.
- Al autenticar correctamente se borran de inmediato el usuario y la contraseña
  escritos. El contenido de perfil se reinicia por `userId`, por lo que una
  respuesta tardía de otra cuenta no puede conservarse en pantalla.

### Bicicletas

- El listado y detalle validan BikeID y propietario contra la cuenta activa.
- Crear exige los cinco campos de identidad y una fotografia multipart `foto`.
- El detalle permite editar nombre, marca, modelo, tipo y numero de serie; la
  foto y los IDs existentes se conservan si la respuesta es parcial.
- Eliminar usa confirmacion irreversible y solo retira bicicleta, mantenciones y
  reservas locales cuando `bike.delete` termina correctamente.
- Formularios, errores y confirmaciones se reinician al cambiar `userId` para no
  exponer datos privados entre sesiones.

### Diseño visual

- Identidad oscura grafito/verde eléctrico con superficies de alto contraste.
- Campos de texto y búsqueda usan contorno LED verde-azul, con mayor intensidad
  al recibir foco.
- La barra inferior usa una sola familia de iconos Material tintables para
  Mapas, Bicicletas, Marketplace y Chat.
- El icono de lanzamiento es una marca vectorial propia de bicicleta sobre
  grafito y verde electrico; ya no empaqueta el recurso generico de Android.
- Tipografía, radios, espaciado, tarjetas y estados comparten tokens centrales
  en `ui/theme/` y `CommonComponents.kt`.
- `ui/theme/Color.kt` es la fuente única de color; los recursos morado/teal de
  la plantilla fueron retirados y no quedan advertencias Lint de recursos
  visuales, modificadores Compose ni autoboxing de contadores.
- La cabecera raíz tiene prioridad de dibujo sobre los fondos decorativos y se
  mantiene visible también en Marketplace. Los placeholders de búsqueda usan
  un contraste de 9,16:1 sobre la superficie elevada.
- Con fuente Android de 160 % o más, la cabecera usa una firma compacta, la
  barra inferior muestra `Bicis`/`Tienda` sin perder sus nombres semánticos.
- Las tarjetas deportivas cambian a reflow vertical a escala grande; las cintas
  `PRÓXIMAMENTE` conservan tamaño visual y no invaden el texto. Los estados
  vacíos priorizan su CTA y Chat permite desplazamiento a 200 %.
- Cada fondo decorativo se recorta a los límites de su pantalla para no pintar
  sobre cabecera o navegación raíz.
- En horizontal, la cabecera usa una firma de marca en una línea y la barra
  inferior mide 56 dp con iconos sin etiqueta visual; las descripciones
  semánticas siguen anunciando Mapas, Bicicletas, Marketplace y Chat.
- Cabecera, contenido y navegación son hermanos directos en la raíz. Solo el
  contenido central usa recorte, evitando que MapLibre o los fondos Compose
  oculten barras persistentes durante la navegación horizontal.
- La marca se anuncia como un único encabezado, cada tarjeta deportiva como un
  único mensaje y `Agregar bicicleta` como un botón etiquetado. Cuenta ya no
  muestra rótulos con apariencia de pestaña cuando no existe una acción real.
- En Mapas, el orden de teclado conserva buscador, búsqueda, ubicación, creación,
  mapa y navegación. Los objetivos interactivos comprobados mantienen al menos
  48 dp en su eje menor.

### Mapas y juntas

- La lectura de ubicacion usa `LocationManagerCompat` y cancelacion real. La
  fecha embebida de una junta se separa del cuerpo visible y se reconstruye una
  sola vez al editar.
- El mapa anuncia en espanol la cantidad de juntas visibles y actualiza su fuente
  aunque la respuesta llegue despues del estilo. En Osorno se verificaron el
  marcador azul, el detalle remoto y la fotografia real.
- Crear una junta o contactar al organizador sin sesion abre Cuenta.

- MapLibre OpenGL con marcador fijo para la ubicación elegida.
- Solicitud inicial de ubicación, confirmación, corrección manual y sugerencias
  en vivo con Unicode.
- El primer foco del selector limpia la etiqueta previa; la nueva búsqueda no se
  inserta dentro del nombre de la ubicación actual.
- Historial de hasta ocho ubicaciones.
- Ubicación de Juntas independiente de Marketplace.
- Juntas activas con coordenadas; contenido propio se administra en el perfil.
- Recargas y detalles descartan respuestas antiguas; la creación bloquea doble
  envío y distingue la creación exitosa de un fallo posterior al subir la foto.
- Una resolución remota de ubicación solo se aplica si sigue siendo la selección
  vigente; una respuesta anterior no reemplaza una corrección posterior.

### Marketplace

- El placeholder del detalle sin portada mide 180 dp; las publicaciones con foto
  conservan el hero de 340 dp.
- En Puerto Montt se verificaron la grilla y un detalle remotos. La ausencia de
  portada util mantiene el placeholder sin consultas N+1.
- Crear una publicacion o contactar al vendedor sin sesion abre Cuenta.

- Ubicación independiente y persistente.
- Búsqueda, recarga al arrastrar, indicador de carga y detalle a pantalla completa.
- La cabecera de marca/perfil permanece visible durante la carga y el estado
  vacío de Marketplace.
- Precio entero con símbolo no editable, agrupación de miles y moneda por país.
- Publicaciones y fotos conectadas al backend; nombres de vendedor visibles.
- Errores con lista vacía ofrecen reintento, detalles descartan respuestas
  antiguas y las creaciones parciales no se repiten como duplicados.
- Los listados privados del perfil exigen que cada publicación tenga como
  propietario al usuario activo; datos sin propietario o ajenos se rechazan.

### Chat

- Tabs social y Marketplace.
- Caché separada por usuario/chat.
- Reparación completa al abrir y sincronización incremental cada tres segundos
  dentro de una conversación.
- Participantes y mensajes muestran `nombre_de_usuario` y
  `sender_nombre_de_usuario`.
- Arrastrar hacia abajo recarga la lista.
- IDs remotos vacíos/cross-chat no entran a la caché; el servidor reemplaza la
  versión local obsoleta del mismo mensaje y un fallo de envío conserva el borrador.
- Estado, caché visible y exclusiones mutuas se separan por cuenta. Listado y
  mensajes no se sincronizan en paralelo consigo mismos; IDs numéricos con la
  misma fecha conservan orden numérico.
- Cada conversación tiene su propia exclusión mutua: sincronización, polling y
  envío no pueden sobrescribirse, mientras otra conversación puede continuar.
- El backend puede omitir `chatId` en un mensaje antiguo y Android lo completa;
  si declara otro chat, se rechaza antes de Compose y de la caché. La persistencia
  de historiales se ejecuta en `Dispatchers.IO`.
- Sin sesión, el estado vacío ofrece `Iniciar sesión` y abre Cuenta.

### Notificaciones

- `ChatNotificationListenerService` es un Foreground Service Android de tipo
  `remoteMessaging`.
- Mientras existe una sesión consulta cambios cada seis segundos aunque la
  actividad esté en segundo plano.
- En primer plano envía eventos a Compose y muestra un banner superior.
- En segundo plano publica una notificación de importancia alta con remitente y
  mensaje; tocarla abre el chat.
- Una notificación persistente de baja prioridad, “Escuchando mensajes nuevos”,
  mantiene visible y controlable el listener.
- WorkManager cada 15 minutos sigue como respaldo si el servicio fue retirado.
- Cada evento lleva el `userId` destinatario. Compose y el centro de
  notificaciones descartan eventos de otra sesión, y el logout retira avisos
  pendientes.
- El acceso del sistema guarda ese destinatario en el Intent. `MainActivity`
  descarta notificaciones antiguas sin destinatario o de otra cuenta.
- Un `force-stop`, el botón “Detener” de Android o políticas agresivas del
  fabricante pueden detener cualquier listener local. FCM continúa siendo la
  solución futura para push inmediato administrado por servidor.

## Backend conocido

- Marketplace y Juntas públicos están desplegados. En la comprobación directa
  del 2026-08-06, listados y detalles respondieron HTTP 200 y la foto de Junta
  entregó `content_base64`.
- Backend devuelve nombres en sesión, contenido y Chat.
- UUID permanece en autorización y claves foráneas.
- `location.search` devuelve lugares normalizados y `location.resolve` responde
  HTTP 200. `location.reverse` respondió HTTP 400 `unknown_region` para
  Santiago; el fallback Android sigue siendo necesario.
- Los listados observados ya contienen campos geográficos, `currency` y la clave
  `photo_id`, pero el registro de Marketplace probado no tenía portada útil.
- Las acciones privadas sin token responden HTTP 401. Falta repetir Chat y
  `*.mine.list` con una cuenta de prueba antes de declararlos completos.
- OAuth deportivo queda fuera de la etapa activa mientras la función esté
  pausada. Siguen pendientes geografía de servidor completa, FCM/tokens de
  dispositivo y la comprobación autenticada multirregional.

## Integridad y privacidad local

- Bicicletas devueltas con propietario distinto a la sesión activa se rechazan.
- Publicaciones y juntas privadas exigen propietario exacto. Si el backend crea
  una entidad y luego devuelve un propietario inconsistente, Android refresca y
  evita repetir la creación.
- Las operaciones suspendibles distinguen cancelación de error; cambiar de
  cuenta o pantalla cancela el trabajo anterior sin mostrar un falso fallo.
- Fechas de mantención/servicio exigen una fecha real `AAAA-MM-DD`.
- Fotos locales se muestrean, respetan orientación EXIF y tienen un máximo de
  20 MB para subida/descarga desde Android.
- Las rutas internas de fotos, HTTP, archivos locales y hosts externos se
  descartan. Solo se aceptan `photo_id`, rutas públicas relativas al API o HTTPS
  de Zizzio; una fuente rechazada muestra placeholder.
- Backup de nube y transferencia excluyen identidad, token cifrado, ubicaciones
  y cachés de Chat.
- La UI pública se recorrió en el AVD `Small_Phone` el 2026-08-06 sin excepción
  fatal ni ANR; compilaciones debug/release, 44 pruebas unitarias y 24 pruebas
  instrumentadas terminaron sin fallos. Dos casos se omitieron por las
  condiciones externas esperadas de Chat y notificaciones. El proyecto usa
  Gradle 9.6.1, AGP 9.3.1, Kotlin integrado/Compose Compiler 2.4.10,
  compile/target 37, Compose BOM 2026.06.01 y MapLibre 13.4.1. Lint informa
  `No issues found`. Un acceso de
  notificación para otro usuario fue descartado en runtime.

## Rutas importantes

- Código Kotlin: `app/src/main/java/com/example/appbike/`.
- APK de prueba: `app/build/outputs/apk/debug/app-debug.apk`.
- Contrato mapa/market/chat: `docs/MAP_MARKETPLACE_CHAT.md`.
- Informe backend: `docs/BACKEND_IMPLEMENTATION_REPORT.md`.
- Router completo: `AGENTS.md`.
- EasyMD centralizado: `CodexChats/EasyMD/`.

## Riesgos y pendientes

- Probar el listener con dos dispositivos reales y cuentas diferentes.
- El dispositivo debe conceder notificaciones y no detener manualmente el
  servicio persistente.
- El AVD actual está limpio y sin cuenta; para pruebas autenticadas se debe
  iniciar sesión con credenciales de prueba.
- El APK y la matriz instrumentada ya se validaron en el AVD independiente
  `APPbike_API_37` con Android 17/API 37: 24 casos, 0 fallos y 2 omisiones
  externas esperadas. La app queda abierta en ese AVD sobre las cintas
  deportivas.
- `RemoteConnections` ya evita adjuntar un Bearer a acciones públicas. Mantener
  esa separación al agregar acciones nuevas para que un token vencido no rompa
  Mapas, Marketplace ni login.
- Implementar FCM en backend y Android cuando exista el contrato de tokens.

## Automatizacion de verificacion

- `.github/workflows/android-verify.yml` prepara JDK 17, SDK 37 y cache de
  Gradle; al recibir un push o pull request ejecuta pruebas unitarias, Lint y
  `assembleDebug`.
- La configuracion fue comprobada localmente el 2026-08-11. La primera ejecucion
  remota queda pendiente hasta que se publique una rama con el workflow.
