# Estado actual de APPbike

Actualizado: 2026-08-28.

## Aplicación

- Android nativo con Jetpack Compose, paquete `com.example.appbike`.
- Entrada: `MainActivity.kt`.
- Destinos principales: Inicio, Marketplace, Mapa, Chat y Perfil. Bicicletas se
  abre como `Mi garaje` secundario desde Perfil.
- Después del logo, una sesion completa se verifica con `user.get`: si backend
  la acepta abre Inicio; sin sesion o con token rechazado se limpia la identidad
  y aparece un acceso MTB a pantalla completa, sin Perfil, cabecera ni barra
  inferior. Una cuenta válida sin nombre abre Cuenta para completar identidad.
  Timeouts/5xx no expulsan.
- Inicio presenta un carrusel de descubrimiento y feed de cards diferenciadas
  para juntas y publicaciones activas.
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
  Garmin y Wahoo se muestran como tarjetas no interactivas; solo Strava lleva la
  cinta diagonal `PRÓXIMAMENTE`, mientras Garmin y Wahoo indican pausa sin
  prometer disponibilidad. Android no ejecuta acciones OAuth mientras siga detenida.
- Perfil muestra una vista previa de bicicletas, posts locales y rutas/juntas
  propias antes de ofrecer el acceso completo a `Mi garaje`; el bloque deportivo
  aparece después y mantiene juntas las tres plataformas.
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
- La barra inferior usa cinco iconos Material Outlined coherentes. Mapa tiene
  mayor peso visual, solo el destino activo muestra texto y los cinco conservan
  nombres semánticos completos.
- Launcher, variante redonda y cabecera usan la copia exacta del símbolo blanco
  sobre negro entregado por el usuario en `appbike_brand_icon.png`.
- El acceso sin sesion usa `auth_mtb_background.png`, una fotografía vertical
  original de MTB generada para APPBIKE con degradado oscuro legible. Presenta
  login y `Crear cuenta`; esta ultima explica que el alta remota sigue pendiente
  y no transmite credenciales a un endpoint inexistente.
- Entre la marca y Cuenta, la cabecera muestra el clima de la posicion GPS con
  temperatura, estado e iconos propios para sol/noche, nubes, niebla, lluvia,
  nieve, tormenta y granizo. Actualiza cada 15 minutos y acredita de forma
  visible a Open-Meteo. Al tocarlo abre dentro de la cabecera un panel
  desplegable animado con el pronostico de seis dias; no abre paginas externas.
- Cuenta conserva el icono anterior `PersonOutline`, su indicador verde de
  sesion, un objetivo tactil de 48 dp y descripcion accesible. El clima mantiene
  intacto su espacio contiguo.
- El splash de plataforma queda negro; después `LaunchBrandScreen` anima 40 LED
  blancos que convergen desde las cuatro esquinas, revela el mismo logo
  compartido con el acceso y muestra `Iniciando sesión…` debajo. La app se
  compone debajo del splash para validar la sesión en paralelo y entra a la
  interfaz a los 5.000 ms. El logo completo no se adelanta a la animación.
- Tipografía, radios, espaciado, tarjetas y estados comparten tokens centrales
  en `ui/theme/` y `CommonComponents.kt`.
- `ui/theme/Color.kt` es la fuente única de color; los recursos morado/teal de
  la plantilla fueron retirados y no quedan advertencias Lint de recursos
  visuales, modificadores Compose ni autoboxing de contadores.
- La cabecera raíz tiene prioridad de dibujo sobre los fondos decorativos y se
  mantiene visible también en Marketplace. Los placeholders de búsqueda usan
  un contraste de 9,16:1 sobre la superficie elevada.
- Con fuente Android de 160 % o más, la cabecera usa una firma compacta y la
  barra inferior conserva nombres semánticos sin rotular destinos inactivos.
- Las tarjetas deportivas cambian a reflow vertical a escala grande; la cinta
  única de Strava `PRÓXIMAMENTE` conserva tamaño visual y no invade el texto. Los estados
  vacíos priorizan su CTA y Chat permite desplazamiento a 200 %.
- Cada fondo decorativo se recorta a los límites de su pantalla para no pintar
  sobre cabecera o navegación raíz.
- En horizontal, la cabecera usa una firma de marca en una línea y la barra
  inferior compacta conserva descripciones completas para los cinco destinos.
- Un `HorizontalPager` sincronizado con la barra cambia Inicio/Marketplace/Mapa/
  Chat/Perfil. Las cinco paginas quedan montadas, sus estados se conservan y las
  cargas de red se difieren hasta la primera activacion.
- El destino actual se conserva durante recreaciones, una navegación rápida no
  es reemplazada por el asentamiento de una página anterior y el botón Atrás
  del sistema cierra `Mi garaje` hacia Perfil.
- Cabecera, contenido y navegación son hermanos directos en la raíz. Solo el
  contenido central usa recorte, evitando que MapLibre o los fondos Compose
  oculten barras persistentes durante la navegación horizontal.
- La marca se anuncia como un único encabezado, cada tarjeta deportiva como un
  único mensaje y `Agregar bicicleta` como un botón etiquetado. Cuenta ya no
  muestra rótulos con apariencia de pestaña cuando no existe una acción real.
- En Mapas, el orden de teclado conserva buscador, búsqueda, ubicación, creación,
  mapa y navegación. Los objetivos interactivos comprobados mantienen al menos
  48 dp en su eje menor.
- El lienzo de MapLibre reserva el gesto horizontal para desplazar el mapa; el
  pager se reactiva cuando el gesto comienza en búsqueda, capas o Trayecto/Junta.
- Inicio rota las cuatro fotografías entregadas por el usuario con sus dimensiones
  JPG originales; los fondos decorativos agregan un brillo cálido naranja de baja
  intensidad inspirado en la referencia visual.
- Las notificaciones de Chat usan el emblema APPBIKE como icono grande y una
  variante monocroma transparente de la marca como icono pequeño; el banner dentro
  de la app usa el mismo recurso compartido del login y splash.

### Mapas y juntas

- La lectura de ubicacion usa `LocationManagerCompat`, cancelacion real y
  solicitudes paralelas a GPS/red/pasivo; selecciona el punto fresco de mejor
  precision y descarta posiciones conocidas de mas de cinco minutos. La fila
  ofrece `Precisar` para repetir la medicion fina. La
  fecha embebida de una junta se separa del cuerpo visible y se reconstruye una
  sola vez al editar.
- El mapa anuncia en espanol la cantidad de juntas visibles y actualiza su fuente
  aunque la respuesta llegue despues del estilo. En Osorno se verificaron el
  marcador azul, el detalle remoto y la fotografia real.
- Crear una junta o contactar al organizador sin sesion abre Cuenta.

- MapLibre OpenGL con marcador fijo para la ubicación elegida, boton circular de
  capas `Mapa`/`Satélite`, cambio de estilo sobre el mismo `MapView`, logo textual
  oculto y atribucion informativa conservada. La lupa despliega buscador y
  ubicacion semitransparentes; la camara satelital se detiene en zoom 17 para no
  entrar a teselas grises que Esri devuelve localmente en zoom 18/19.
- Mapa es el destino central. Usa TextureView dentro del pager, se inicializa al
  primer ingreso y baja a 4 FPS al quedar inactivo.
- Tocar una Junta abre un bottom sheet. Con ubicacion confirmada muestra distancia
  y ETA; `Cómo llegar` calcula y dibuja un camino ciclista por calles. El mismo
  mapa ofrece `Trayecto` como función separada de `Junta`, sin exigir cuenta, con
  selector de destino, marcador, encuadre completo y cancelación. Si el demo de
  ruteo falla usa una línea directa explícitamente rotulada como respaldo. No
  inventa participantes, dificultad, desnivel ni tipo de ciclismo ausentes del
  backend.
- Solicitud inicial de ubicación, confirmación, corrección manual y sugerencias
  en vivo con Unicode.
- El primer foco del selector limpia la etiqueta previa; la nueva búsqueda no se
  inserta dentro del nombre de la ubicación actual.
- Historial de hasta ocho ubicaciones.
- Ubicación de Juntas independiente de Marketplace.
- Juntas activas con coordenadas; contenido propio se administra en el perfil.
- `Trayecto`/`Junta` se muestran como selector segmentado compacto en la esquina
  superior, con botones equilibrados, borde sutil y transparencia sobre el mapa.
- `PremiumScreenBackground` incorpora `appbike_solar_halo_background`, una
  textura raster abstracta de halo ámbar y atmósfera verde-grafito, con contraste
  controlado para no competir con el contenido.
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
- Búsqueda, filtros locales por categoria, recarga al arrastrar, indicador de
  carga y detalle a pantalla completa; estado y posicion se conservan entre tabs.
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
- Las filas distinguen visualmente Junta/Compra. Un chat social con entidad
  relacionada ofrece `Ver en mapa` y abre el detalle de esa Junta.
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
- El polling se cancela cuando Chat no es la pestaña activa y se reanuda al volver.
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
- La verificación local del 2026-08-23 terminó con 59 pruebas unitarias y 33
  pruebas instrumentadas sin fallos. Dos casos instrumentados se omitieron por
  las condiciones externas esperadas de Chat y notificaciones. `assembleDebug`,
  `assembleDebugAndroidTest` y Lint terminaron correctamente. El proyecto usa
  Gradle 9.6.1, AGP 9.3.1, Kotlin integrado/Compose Compiler 2.4.10,
  compile/target 37, Compose BOM 2026.06.01 y MapLibre 13.4.1. Lint no informa
  errores; conserva cuatro avisos informativos de versiones/ecosistema y forma
  del launcher. Un acceso de notificación para otro usuario fue descartado en
  runtime.

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
- El APK y la matriz actual se validaron en `APPbike_API_35`: 33 casos, 0
  fallos y 2 omisiones externas esperadas. La cobertura incluye navegación de
  cinco destinos, semántica, sheet de Junta, mapa offline y CTA protegidos.
- `RemoteConnections` ya evita adjuntar un Bearer a acciones públicas. Mantener
  esa separación al agregar acciones nuevas para que un token vencido no rompa
  Mapas, Marketplace ni login.
- El clima usa el endpoint gratuito directo de Open-Meteo para desarrollo y uso
  no comercial. Antes de distribuir APPbike comercialmente, contratar el
  endpoint de cliente o crear un proxy backend; nunca incrustar la clave en el
  APK y conservar la atribucion visible.
- Implementar FCM en backend y Android cuando exista el contrato de tokens.

## Automatizacion de verificacion

- `.github/workflows/android-verify.yml` prepara JDK 17, SDK 37 y cache de
  Gradle; al recibir un push o pull request ejecuta pruebas unitarias, Lint y
  `assembleDebug`.
- La ejecucion remota `31551839752` completada el 2026-08-12 UTC verifico en un
  runner limpio JDK 17, SDK 37.0, pruebas unitarias, Lint y `assembleDebug`.
- Cada ejecucion correcta conserva durante 14 dias el artefacto descargable
  `appbike-debug-apk`; es apto para pruebas, no para distribucion firmada.

## Rediseño UX/UI 2026-08-26

- Inicio separa Marketplace y usa un hero local de cuatro fotografías con
  rotación de 4 segundos; el feed mezcla solo Juntas activas y publicaciones
  personales locales del usuario activo.
- El acceso superior a Perfil fue eliminado. Perfil ahora ofrece avatar, bio,
  estadísticas honestas y un compositor local de foto/video aislado por UUID.
- Mapa mantiene sus contratos y ruteo interno; `Trayecto` y `Junta` usan
  controles flotantes compactos. Splash usa logo centrado de tamaño constante.
- Build verificada: 59 unitarias y 38 instrumentadas, 0 fallos; 2 omisiones
  externas esperadas en Chat/notificaciones. `assembleDebug`,
  `assembleDebugAndroidTest`, `lintDebug` y `git diff --check` correctos.
- Evidencia visual del AVD: `CodexChats/audits/2026-08-26-redesign-after-launch.png`
  y `CodexChats/audits/2026-08-26-redesign-account.png`.
- Pendiente: backend social remoto y recorrido autenticado en teléfono físico
  desbloqueado.
