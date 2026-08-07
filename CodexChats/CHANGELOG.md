# Registro de cambios de CodexChats

## 2026-08-07 - Publicacion de la actualizacion Android en GitHub

Objetivo:

- Publicar en GitHub la actualizacion local vigente de APPbike, verificando que
  el proyecto genere un APK debug instalable en Android.

Cambios:

- Se prepara la entrega completa de los cambios de aplicacion, pruebas,
  documentacion y toolchain presentes en la rama de trabajo.
- La entrega funcional se confirmo como `3d5f00d` y se publico en GitHub en la
  rama `redesign/purple-dark-ui`.

Pruebas:

- `:app:assembleDebug` correcto con Gradle 9.6.1; se generó el APK debug
  instalable.

Pendientes:

- Ninguno para la entrega Android actual.

Siguiente paso:

- Instalar el APK debug o ejecutar la configuracion Android desde la rama
  publicada.

## 2026-08-06 - Edicion y eliminacion segura de bicicletas

Objetivo:

- Cerrar el mayor hueco funcional Android respaldado por el backend: administrar
  una bicicleta existente desde su detalle.

Cambios:

- `RemoteConnections` incorpora `bike.update` y `bike.delete` con normalizacion
  de campos, BikeID obligatorio y validacion estricta de propietario.
- El formulario de bicicleta sirve tambien para editar; conserva foto, IDs y
  metadata si la respuesta remota es parcial.
- El detalle ofrece acciones reales de edicion y eliminacion. Eliminar requiere
  confirmacion irreversible, evita doble envio y solo limpia listas locales tras
  exito remoto; los errores permanecen visibles para reintentar.
- Estados de formulario, detalle y confirmacion se reinician por `userId`.
- La revision del tester publico confirma que todavia no existe contrato para
  crear cuenta ni recuperar contrasena; el requisito se documento para backend.

Pruebas:

- 40 pruebas unitarias, 0 fallos.
- 24 pruebas instrumentadas en Android 17/API 37, 0 fallos y 2 omisiones
  externas esperadas.
- Las 3 regresiones Compose nuevas cubren formulario editable, acciones del
  detalle y confirmacion destructiva.
- `assembleDebug`, `assembleRelease` y `lintDebug` correctos; Lint informa
  `No issues found`.

Pendientes:

- Repetir update/delete contra una bicicleta descartable con credenciales
  reales; la ausencia de cuenta de prueba impide mutar datos remotos con seguridad.
- Definir en backend registro y recuperacion de cuenta antes de exponer esos CTA.
- Probar Chat entre dos cuentas/dispositivos y completar contrato FCM.

Siguiente paso:

- Instalar el APK actualizado, dejar la app abierta y continuar con el siguiente
  contrato local respaldado por el servidor.

## 2026-08-06 - Migracion completa a API 37 y toolchain actual

Objetivo:

- Cerrar las siete advertencias de versiones restantes y preparar APPbike para
  Android 17 sin mantener una solucion deliberadamente antigua.

Cambios:

- El proyecto migro a Gradle 9.6.1, AGP 9.3.1, Kotlin/Compose Compiler 2.4.10,
  Compose BOM 2026.06.01, Core 1.19.0 y Lifecycle 2.11.0.
- `compileSdk` y `targetSdk` subieron a 37; la plataforma Android 37 revision 2
  se instalo durante la compilacion.
- AGP 9 usa Kotlin integrado: se retiro `org.jetbrains.kotlin.android` y el DSL
  `kotlinOptions` obsoleto.
- MapLibre OpenGL subio a 13.4.1. Chat usa `PrimaryTabRow`, los parsers JSON son
  exhaustivos y cinco suites Compose adoptaron el runner v2.
- Los binarios nativos precompilados de MapLibre/AndroidX se declaran como
  simbolos conservados para que el empaquetado debug/release no emita avisos.

Pruebas:

- 11 regresiones focalizadas Compose/MapLibre, 0 fallos.
- 36 pruebas unitarias, 0 fallos.
- 21 pruebas instrumentadas repetidas en Android 17/API 37, 0 fallos y 2
  omisiones externas esperadas.
- `:app:assembleDebug`, `:app:assembleRelease` y `:app:lintDebug` correctos.
- Lint: `No issues found`.
- APK target 37 reinstalado; ubicacion, mapa MapLibre, perfil, campos LED y
  cintas deportivas verificados primero en `Small_Phone` Android 16 y despues
  en el AVD independiente `APPbike_API_37` Android 17, sin crash, ANR ni error
  de carga nativa.

Pendientes:

- Repetir cuenta, bicicletas, perfil privado y Chat con credenciales reales.
- Probar Chat entre dos dispositivos y completar backend FCM, geografia,
  portadas y listados propios multirregionales.

Siguiente paso:

- Continuar las validaciones funcionales privadas cuando exista una cuenta de
  prueba; la app queda abierta en las cintas deportivas.

## 2026-08-06 - Limpieza KTX y dependencias compatibles

Objetivo:

- Eliminar las advertencias de codigo restantes sin convertir el cierre visual
  en una migracion riesgosa del toolchain Android.

Cambios:

- SharedPreferences usa `androidx.core.content.edit`; URI y Bitmap migraron a
  las extensiones KTX correspondientes.
- AndroidX ExifInterface se actualizo de 1.3.7 a 1.4.2.
- Se probaron Core 1.19, Lifecycle 2.11 y MapLibre 13.4.1, pero se mantuvieron
  sus versiones compatibles: las dos primeras exigen API 37/AGP 9.1+ y la
  ultima exige metadata Kotlin 2.2.
- Lint bajo de 26 a 7 advertencias. Todas las restantes describen el proximo
  salto coordinado de AGP, Kotlin, Compose, API, Core, Lifecycle y MapLibre; no
  corresponden a defectos de codigo o recursos.

Pruebas:

- 36 pruebas unitarias, 0 fallos.
- 21 pruebas instrumentadas, 0 fallos y 2 omisiones externas esperadas.
- `:app:assembleDebug`, `:app:assembleRelease` y `:app:lintDebug` correctos.
- Arranque real, ubicacion confirmada y cintas deportivas verificadas en
  `Small_Phone`; sin `FATAL EXCEPTION` ni ANR.

Pendientes:

- Repetir cuenta, bicicletas, perfil privado y Chat con credenciales reales.
- Probar Chat entre dos dispositivos y completar backend FCM, geografia,
  portadas y listados propios multirregionales.
- Migrar el toolchain completo en una iteracion aislada cuando se adopte API 37.

Siguiente paso:

- Mantener la app abierta para la siguiente mejora; la sincronizacion deportiva
  permanece intencionalmente pausada.

## 2026-08-06 - Interacciones honestas y metadata de Juntas

Objetivo:

- Continuar la meta funcional global auditando stubs, acciones vacias,
  compatibilidad Android y estados reales sin fotografia.

Cambios:

- La tarjeta resumida dentro del detalle de bicicleta ya no parece pulsable;
  solo las tarjetas de listado conservan accion.
- El panel del selector de ubicacion consume toques sin exponerse como una
  accion vacia a accesibilidad. El rotulo `Requerido` del nombre de usuario dejo
  de ser un boton deshabilitado.
- La lectura actual de ubicacion migro de `requestSingleUpdate` a
  `LocationManagerCompat.getCurrentLocation` con cancelacion.
- Juntas centraliza la metadata `Fecha y hora:`: el parser la separa del cuerpo
  visible y crear/editar la codifican exactamente una vez.
- El detalle de Marketplace sin portada reduce su placeholder de 340 a 180 dp;
  una publicacion con foto conserva el hero completo.
- Se retiro logging diagnostico de fotos y las dependencias directas se movieron
  al catalogo de versiones.

Pruebas:

- 36 pruebas unitarias, 0 fallos.
- 21 pruebas instrumentadas, 0 fallos y 2 omisiones externas esperadas.
- `:app:assembleDebug`, `:app:assembleRelease` y `:app:lintDebug` correctos.
- Lint: 0 errores y 26 advertencias no visuales.

Pendientes:

- Repetir cuenta, bicicletas, perfil privado y Chat con credenciales reales.
- Probar Chat entre dos dispositivos y completar backend FCM, geografia,
  portadas y listados propios multirregionales.

Siguiente paso:

- Mantener la meta activa y usar una cuenta de validacion cuando exista; la
  sincronizacion deportiva permanece intencionalmente pausada.

## 2026-08-06 - CTAs de sesion y comunidad real

Objetivo:

- Continuar la meta funcional global con contenido real de Marketplace y Juntas,
  corrigiendo cualquier accion publica que no completara el flujo anunciado.

Cambios:

- Marketplace cargo publicaciones activas de Puerto Montt y abrio su detalle
  remoto; las tarjetas conservaron placeholder porque el listado no entrego una
  portada util.
- Mapas cargo una junta activa de Osorno, dibujo su marcador azul y mostro el
  detalle con fotografia Base64 del backend.
- Los CTA de crear publicacion, contactar vendedor, crear junta y contactar
  organizador abren Cuenta cuando no existe sesion. Ya no quedan botones
  deshabilitados o dialogos informativos sin continuidad.
- El `MapView` anuncia en espanol la cantidad de juntas cercanas visibles.
- Se agrego una regresion MapLibre offline que demuestra que una junta recibida
  despues de cargar el estilo actualiza la fuente y la capa renderizada.

Pruebas:

- 33 pruebas unitarias, 0 fallos.
- 17 pruebas instrumentadas, 0 fallos y 2 omisiones externas esperadas por Chat
  autenticado y notificaciones del AVD.
- `:app:assembleDebug`, `:app:assembleRelease` y `:app:lintDebug` correctos;
  Lint conserva 0 errores y 30 advertencias no visuales.
- Recorrido ADB real de Puerto Montt y Osorno, incluidos ambos detalles, foto de
  junta y los cuatro saltos a Cuenta, sin `FATAL EXCEPTION` ni ANR.

Pendientes:

- Validar con credenciales reales cuenta, bicicletas, perfil privado y Chat
  entre dos dispositivos.
- Poblar portadas de Marketplace, completar `location.reverse`, geografia de
  servidor, listados `*.mine.list` multirregionales y FCM.

Siguiente paso:

- Repetir los flujos privados con una cuenta de prueba cuando este disponible;
  mantener la sincronizacion deportiva visualmente pausada hasta reactivarla.

## 2026-08-06 - Reemplazo seguro de ubicación y contrato remoto revalidado

Objetivo:

- Continuar la meta funcional global validando los flujos públicos contra el
  backend real y corregir el primer defecto reproducible en el emulador.

Cambios:

- El selector compartido de Mapas y Marketplace limpia la etiqueta previa al
  recibir el primer foco. Escribir `Osorno` ya no produce una dirección mezclada
  como `MountaiOsornon View`.
- La ubicación anterior permanece en `Ubicaciones recientes` y solo se persiste
  un cambio al elegir una sugerencia.
- Se añadió una regresión instrumentada que parte con `Mountain View` y exige que
  la primera edición muestre únicamente `Osorno`.
- Se revalidaron `location.*`, listados/detalles públicos, foto de Junta y rechazo
  HTTP 401 de lecturas privadas sin token.
- La documentación distingue que `location.search` ya devuelve resultados,
  mientras `location.reverse` aún falla con `unknown_region` para Santiago.

Pruebas:

- 33 pruebas unitarias, 0 fallos.
- 14 pruebas instrumentadas, 0 fallos y 2 omisiones esperadas por credenciales
  reales de Chat y condiciones de notificación del AVD.
- `:app:assembleDebug`, `:app:assembleRelease` y `:app:lintDebug` correctos;
  Lint conserva 0 errores y 30 advertencias no visuales.
- Recorrido ADB real: `Osorno` se reemplazó completo, el teclado se cerró al
  elegir la sugerencia, Marketplace quedó en CLP y no hubo FATAL ni ANR.

Pendientes:

- Validar con credenciales reales los recorridos privados de cuenta, bicicletas,
  perfil, Chat y notificaciones entre dos dispositivos.
- Corregir en backend `location.reverse`, poblar portadas de Marketplace e
  implementar geografía/distancia de servidor y FCM.

Siguiente paso:

- Continuar la meta funcional con una cuenta de validación cuando esté
  disponible, manteniendo los fallbacks públicos activos mientras tanto.

## 2026-08-06 - Accesibilidad operativa con TalkBack y teclado

Objetivo:

- Completar la primera fase visual con evidencia de accesibilidad real para
  marca, campos, navegación, mapa, acciones y tarjetas deportivas.

Cambios:

- TalkBack se activó temporalmente en el AVD y confirmó foco visible sobre la
  marca agrupada; luego se restauró su estado desactivado.
- APPBIKE se expone como un único encabezado semántico y cada tarjeta de Strava,
  Garmin o Wahoo como un único anuncio con su estado pausado.
- `Agregar bicicleta` usa todo el bloque como objetivo y conserva etiqueta, rol
  y acción de botón.
- Se retiraron Progreso/Entrenamientos/Actividades de Cuenta porque parecían
  pestañas activas sin ofrecer ninguna función.
- Buscador, ubicación y Crear junta se componen antes del `AndroidView` de
  MapLibre; el recorrido por teclado ya no omite el campo de búsqueda.
- Se añadieron dos pruebas de regresión semántica y una auditoría con capturas en
  `CodexChats/audits/2026-08-06-accesibilidad-operativa/`.

Pruebas:

- 33 pruebas unitarias, 0 fallos.
- 13 pruebas instrumentadas, 0 fallos y 1 omisión esperada por falta de
  credenciales reales de Chat.
- `:app:assembleDebug`, `:app:assembleRelease` y `:app:lintDebug` correctos;
  Lint conserva 0 errores y 30 advertencias no visuales.
- UI Automator confirmó nombres completos y objetivos de al menos 48 dp en los
  controles principales comprobados.

Pendientes:

- Repetir pronunciación y gestos TalkBack en dispositivo físico.
- Validar los recorridos privados con credenciales y datos remotos reales.

Siguiente paso:

- Continuar la meta funcional global con pruebas externas autenticadas cuando
  estén disponibles las cuentas de validación.

## 2026-08-06 - Reflow horizontal y jerarquía deportiva

Objetivo:

- Completar la auditoría visual en orientación horizontal y en un teléfono de
  540 dp de ancho, manteniendo visibles la navegación raíz y las tres tarjetas
  deportivas pausadas.

Cambios:

- La raíz usa una columna determinista para cabecera, contenido y navegación;
  solo el contenido central se recorta. Esto elimina la desaparición visual de
  marca y perfil que se producía al navegar en horizontal.
- La cabecera horizontal compacta la marca en una línea y la barra inferior usa
  iconos de 24 dp en 56 dp de alto, conservando los nombres completos en
  `contentDescription`.
- Bicicletas dejó de anidar un segundo `Scaffold`, recuperando altura útil y
  evitando duplicar insets.
- `PremiumScreenBackground` recorta únicamente sus glows mediante `clipRect`,
  sin trasladar todo el contenido a una capa gráfica propia.
- En Cuenta, la sincronización deportiva aparece antes de `Tu actividad`; así
  Strava, Garmin y Wahoo quedan descubiertas como un bloque continuo.
- La auditoría horizontal y de ancho alternativo quedó documentada con 19
  capturas en `CodexChats/audits/2026-08-06-orientacion-horizontal/`.

Pruebas:

- 33 pruebas unitarias, 0 fallos.
- 11 pruebas instrumentadas ejecutadas, 0 fallos y 0 omisiones.
- `:app:assembleDebug`, `:app:assembleRelease` y `:app:lintDebug` correctos;
  Lint conserva 0 errores y 30 advertencias no visuales.
- Inspección real en 360 dp horizontal y 540 dp vertical, con Mapas,
  Bicicletas, Marketplace, Chat, Cuenta y las tres cintas visibles.

Pendientes:

- TalkBack y el recorrido autenticado contra servicios externos aún requieren
  validación manual específica; estas capturas no demuestran conformidad WCAG.

Siguiente paso:

- Mantener la app abierta en Cuenta para que el usuario continúe afinando el
  diseño sobre el APK validado.

## 2026-08-06 - Reflow accesible con fuente al 200 %

Objetivo:

- Comprobar el diseño público y el bloque deportivo con escalas de fuente 130 %
  y 200 %, corrigiendo recortes, superposiciones y acciones inaccesibles.

Cambios:

- La cabecera usa copia compacta y conserva el acceso de perfil de 48 dp cuando
  la fuente llega a 160 % o más.
- La barra inferior muestra `Bicis` y `Tienda` con texto grande, pero mantiene
  los nombres completos en sus descripciones semánticas.
- Las referencias de Cuenta se apilan a ancho completo con fuente grande.
- Las tarjetas Strava, Garmin y Wahoo cambian a disposición vertical; sus cintas
  diagonales conservan tamaño visual y dejan libre el contenido escalable.
- Los fondos decorativos se recortan a los límites de cada pantalla y ya no
  pueden cubrir la cabecera raíz.
- Los estados vacíos centran su contenido y, con fuente grande, colocan el CTA
  antes de la explicación. Chat usa una lista desplazable para conservar todo el
  contenido a 200 %.
- La auditoría se amplió a 24 capturas, incluyendo comparaciones antes/después.

Pruebas:

- 33 pruebas unitarias, 0 fallos.
- 11 pruebas instrumentadas, 0 fallos y 1 omisión esperada sin sesión real de
  Chat.
- `:app:assembleDebug`, `:app:assembleRelease` y `:app:lintDebug` correctos;
  Lint mantiene 0 errores y 30 advertencias no visuales.
- El AVD se comprobó a 130 % y 200 %, y se restauró a 100 % al terminar.

Pendientes:

- TalkBack, orientación horizontal y las pantallas privadas aún requieren una
  pasada específica con credenciales y datos remotos reales.

Siguiente paso:

- Continuar la revisión visual con el usuario desde el APK final abierto en el
  bloque deportivo de Cuenta.

## 2026-08-06 - Auditoría visual del flujo público

Objetivo:

- Revisar el recorrido público completo con capturas reales y corregir las
  inconsistencias visuales o de acceso demostrables antes de continuar con
  funciones autenticadas.

Cambios:

- Se capturaron e inspeccionaron Mapas, Bicicletas, Marketplace, Chat, Cuenta,
  foco LED y sincronización deportiva en pausa.
- La cabecera raíz usa prioridad de dibujo explícita; Marketplace ya no puede
  ocultar visualmente la marca y el acceso de perfil con su fondo Compose.
- Chat sin sesión incorpora el CTA `Iniciar sesión`, conectado a Cuenta.
- Los placeholders de búsqueda usan `AppTextSecondary`; sobre
  `AppSurfaceElevated` su contraste calculado subió de 4,16:1 a 9,16:1.
- Se agregó una prueba Compose que pulsa el CTA de Chat y verifica el callback.
- El informe con las diez capturas aceptadas quedó en
  `CodexChats/audits/2026-08-06-diseno-flujo-publico/REPORT.md`.

Pruebas:

- 33 pruebas unitarias, 0 fallos.
- 11 pruebas instrumentadas, 0 fallos y 1 omisión esperada sin una sesión real
  de Chat.
- `:app:assembleDebug`, `:app:assembleRelease` y `:app:lintDebug` correctos;
  Lint mantiene 0 errores y 30 advertencias no visuales.
- En el AVD, Marketplace mostró cabecera, Chat mostró el CTA y tocarlo abrió
  Cuenta. Las capturas se inspeccionaron a resolución 720 × 1280.

Pendientes:

- Las capturas no demuestran comportamiento con TalkBack, ampliación de fuente,
  orientación horizontal ni todas las pantallas privadas.
- El recorrido autenticado aún requiere credenciales reales y datos remotos.

Siguiente paso:

- Extender la auditoría visual a escalado de fuente y anchos alternativos sin
  retirar el estado deportivo `PRÓXIMAMENTE`.

## 2026-08-06 - Limpieza final de residuos visuales

Objetivo:

- Cerrar advertencias visuales heredadas y dejar la primera fase de diseño con
  una única identidad, sin textos provisionales ni recursos de plantilla.

Cambios:

- Se eliminó `res/values/colors.xml`, que solo contenía los colores morado,
  teal, blanco y negro generados por la plantilla y no tenía consumidores.
- El esqueleto de mantenciones ahora comunica una carga real en vez de afirmar
  que la base de datos todavía debía conectarse.
- Los contadores de Bicicletas, Marketplace y navegación usan
  `mutableIntStateOf`, evitando autoboxing en recomposiciones.
- Los modificadores de imágenes de Juntas y Marketplace respetan la convención
  Compose, manteniendo explícitamente los tamaños visuales en cada llamada.
- `ui/theme/Color.kt` queda como fuente única de la identidad grafito/verde LED.

Pruebas:

- 33 pruebas unitarias, 0 fallos.
- 10 pruebas instrumentadas, 0 fallos y 1 omisión esperada por falta de una
  sesión real de Chat; la prueba de notificación sí se ejecutó con permiso.
- `:app:assembleDebug`, `:app:assembleRelease` y `:app:lintDebug` correctos.
- Lint quedó en 0 errores y 30 advertencias no visuales; se eliminaron las 7 de
  recursos sin uso, las 4 de `Modifier` y las 3 de autoboxing.

Pendientes:

- Las 30 advertencias restantes corresponden a KTX, versiones/dependencias,
  catálogo Gradle y el uso diagnóstico de `Log`; no afectan el diseño actual.
- Los flujos autenticados aún requieren credenciales y dos dispositivos reales
  para su comprobación externa completa.

Siguiente paso:

- Continuar la revisión visual junto al usuario desde APPbike ya instalada y
  ejecutándose en Cuenta.

## 2026-08-06 - Protección de fuentes de fotografías

Objetivo:

- Cumplir el contrato que prohíbe exponer rutas internas del servidor y evitar
  que una URL de imagen remota pueda dirigir al dispositivo a un host inseguro.

Cambios:

- `safePublicPhotoUrl` rechaza rutas de `PersonalBikesPhotos`, `AppBikeInternal`,
  `/srv`, `/var`, unidades Windows, traversal, `file:`, HTTP y hosts externos.
- Solo se aceptan rutas relativas públicas bajo `api.zizzio.cl` o HTTPS en
  `zizzio.cl` y sus subdominios.
- `photo_id` conserva prioridad y sigue convirtiéndose a los esquemas internos
  `appbike-photo`, `appbike-market-photo` y `appbike-junta-photo` para descargar
  Base64 mediante la acción correspondiente.
- Fotos comunitarias recibidas como string u objeto pasan por la misma política;
  una fuente insegura se omite y la UI usa su placeholder.

Pruebas:

- 33 pruebas unitarias, 0 fallos.
- 10 pruebas instrumentadas, 0 fallos y 1 omisión esperada por falta de
  credenciales reales de Chat.
- Dos pruebas de parser comprobaron que una bicicleta y una publicación no
  convierten rutas internas en `imageUri`.
- `:app:assembleDebug` y `:app:lintDebug` correctos; Lint sin errores.

Pendientes:

- El backend debe seguir devolviendo `photo_id` y retirar definitivamente
  `photo_folder_path`, `file_path` y rutas internas de sus respuestas.

Siguiente paso:

- Continuar el cierre de residuos visuales y estados engañosos del cliente.

## 2026-08-06 - Integridad de Chat y accesos de notificación

Objetivo:

- Continuar la meta funcional después del cierre visual, eliminar carreras entre
  sincronización/envío y asegurar que una notificación solo abra la cuenta y
  conversación correctas.

Cambios:

- El `PendingIntent` de cada mensaje incluye `recipientUserId`; su identidad,
  request code e ID de notificación también incorporan usuario y chat.
- `MainActivity` descarta accesos sin destinatario o destinados a otra sesión.
- `RemoteConnections` normaliza el `chatId` ausente por compatibilidad, pero
  rechaza una respuesta que declare mensajes de otra conversación.
- La carga inicial, polling, actualización manual y envío se coordinan mediante
  un mutex por chat. Cambiar de conversación cancela el efecto visual anterior
  sin bloquear otra conversación.
- Cachés de chats, mensajes y metadata se leen/escriben en `Dispatchers.IO`; un
  historial grande no se serializa en el hilo de interfaz.
- El botón volver queda bloqueado durante el envío y el borrador solo se limpia
  después de respuesta y persistencia correctas.

Pruebas:

- 31 pruebas unitarias, 0 fallos.
- 8 pruebas instrumentadas, 0 fallos y 1 omisión esperada sin credenciales
  reales de Chat. La prueba de notificación confirmó el destinatario del Intent.
- `:app:assembleDebug` y `:app:lintDebug` correctos; Lint sin errores.
- En `Small_Phone`, un Intent de notificación para otro usuario permaneció en
  Mapas (`FOREIGN_NOTIFICATION_RESULT=map_kept`) y Chat sin sesión mostró su
  estado protegido. Logcat del proceso sin excepción fatal ni ANR.

Pendientes:

- Repetir envío, recepción, cambio rápido de chat y acceso de notificación con
  dos cuentas reales; el AVD no dispone de esas credenciales.

Siguiente paso:

- Continuar el inventario funcional local y mantener OAuth deportivo detenido.

## 2026-08-06 - Aislamiento de sesión, cancelación y ejecución release

Objetivo:

- Cerrar carreras tardías y mezclas potenciales entre cuentas después del
  rediseño, verificar las variantes debug/release y dejar APPbike ejecutándose.

Cambios:

- Las corrutinas de red vuelven a propagar `CancellationException`; una cuenta o
  pantalla reemplazada ya no continúa publicando resultados tardíos.
- Perfil y Chat reinician estado, listas, selección y exclusiones mutuas por
  `userId`; las recargas solapadas de conversaciones se serializan.
- Los eventos de notificación incluyen el destinatario, se descartan si no
  corresponde a la cuenta activa y se limpian al cerrar sesión.
- La resolución remota de una ubicación ya no puede reemplazar una elección más
  reciente en Mapas o Marketplace.
- Los listados privados de publicaciones/juntas rechazan propietarios ausentes o
  ajenos; una creación que devuelve un propietario inconsistente se maneja como
  éxito parcial para impedir reintentos duplicados.
- Mensajes con la misma fecha ordenan IDs numéricos por valor numérico, no como
  texto (`9` antes de `10`).
- Se añadieron pruebas de cancelación, aislamiento de notificaciones, propiedad
  comunitaria y orden de IDs.

Pruebas:

- 30 pruebas unitarias, 0 fallos.
- 8 pruebas instrumentadas, 0 fallos y 1 omisión esperada por ausencia de
  credenciales reales de Chat; la prueba de notificaciones se repitió con el
  permiso concedido y pasó.
- `:app:assembleDebug`, `:app:assembleRelease` y `:app:lintDebug` correctos; Lint
  sin errores (41 advertencias y 3 sugerencias no bloqueantes).
- APK debug reinstalada y abierta en frío en `Small_Phone`; ubicación confirmada,
  MapLibre visible y tarjetas Strava/Garmin/Wahoo verificadas con cinta
  `PRÓXIMAMENTE`. Logcat de APPbike sin excepción fatal ni ANR.

Pendientes:

- Recorrido autenticado con credenciales de prueba reales para bicicletas,
  perfil multirregional, Chat entre dos usuarios y mutaciones privadas.
- Comprobación visual final en un dispositivo físico y otra densidad.

Siguiente paso:

- Continuar la revisión visual desde la app abierta sin reactivar OAuth
  deportivo; usar cuentas de prueba cuando estén disponibles.

## 2026-08-05 - Auditoría funcional, integridad y ejecución final

Objetivo:

- Continuar la meta de terminar APPbike, cerrar riesgos funcionales de la etapa
  visual y dejar la compilación más reciente abierta para revisión.

Cambios:

- Login rechaza identidades no UUID y Cuenta prioriza el formulario de acceso
  antes del panel promocional cuando no existe sesión.
- Bicicletas valida que el backend no mezcle propietarios, muestra reintento y
  exige fechas calendario reales para mantenciones y servicios.
- Juntas y Marketplace bloquean doble envío, distinguen éxito parcial de foto,
  descartan respuestas antiguas e ignoran registros remotos sin ID.
- Marketplace conserva la moneda enviada si la respuesta de creación omite el
  campo y ofrece reintento cuando una carga vacía falla.
- Chat conserva el borrador tras error, serializa sincronización, filtra IDs
  inválidos/cross-chat y actualiza la caché con la copia remota más reciente.
- Fotos locales usan muestreo y EXIF; imágenes Android tienen límite de 20 MB.
- Backups excluyen sesión, token, ubicación y cachés privadas.
- Se corrigió compatibilidad de ajustes de notificación en Android 7 y del
  Foreground Service de Chat en Android 10–13.
- Se añadieron pruebas de propiedad, fechas, identidad remota y codificación.

Pruebas:

- 23 pruebas unitarias, 0 fallos.
- 8 pruebas instrumentadas, 0 fallos y 1 omisión esperada sin credenciales de
  Chat.
- `:app:assembleDebug` y `:app:lintDebug` correctos.
- Recorrido en `Small_Phone`: Mapas, Bicicletas, Marketplace, Chat, login LED y
  tarjetas Strava/Garmin/Wahoo. Logcat sin `FATAL EXCEPTION` ni ANR.
- APK final instalada y `com.example.appbike/.MainActivity` quedó al frente.

Pendientes:

- Recorrido autenticado con una cuenta de prueba real para bicicletas, contenido
  propio, conversación y notificaciones entre dos usuarios/dispositivos.
- Validación final en dispositivo físico y densidad distinta.

Siguiente paso:

- Continuar la revisión visual desde la app abierta; cuando haya credenciales de
  prueba, ejecutar la matriz privada sin cambiar la pausa deportiva.

## 2026-08-05 - Endurecimiento funcional tras el rediseño

Objetivo:

- Continuar desde la etapa visual terminada, detectar fallos de alto impacto y
  dejar el APK ejecutable con una base de pruebas más segura.

Cambios:

- `RemoteConnections` separa acciones públicas y privadas. Login, listados,
  detalles, fotos y ubicación pública ya no adjuntan un Bearer vencido; las
  operaciones privadas siguen autenticadas.
- Los errores HTTP no JSON ahora se convierten en mensajes visibles por estado
  en vez de presentarse como una respuesta inválida genérica.
- El fallback regional de contenido propio se usa ante acciones `mine` todavía
  no desplegadas, sin ocultar un 401 autenticado ni fallos reales del servidor.
- `ProfileContent` carga publicaciones y juntas en paralelo, conserva el
  resultado que sí respondió y ofrece reintento cuando una fuente falla.
- La prueba remota de Chat se omite limpiamente sin sesión de prueba y la prueba
  de `SecureTokenStore` restaura cualquier token previo para no dañar datos del
  dispositivo.
- Se agregaron pruebas unitarias para la política de autorización y el criterio
  de fallback de perfil.
- Se actualizaron el router y los informes con la comprobación directa del
  backend del 2026-08-05.

Pruebas:

- `:app:testDebugUnitTest :app:assembleDebug`: correcto.
- `:app:connectedDebugAndroidTest`: correcto; 10 resultados terminados, con dos
  omisiones esperadas por falta de sesión/permisos.
- La primera corrida instrumentada perdió temporalmente el servidor ADB; el AVD
  siguió activo, ADB se recuperó y la repetición completa terminó correctamente.
- `:app:lint`: correcto.
- APK instalada y abierta en el AVD `Small_Phone`; Mapas y Marketplace públicos
  cargaron sin error, Perfil mostró la cinta deportiva y no hubo
  `FATAL EXCEPTION` ni ANR de APPbike.
- Capturas en `app/build/outputs/appbike-functional-final.png` y
  `appbike-sports-running.png`.

Pendientes:

- El AVD está limpio y no tiene una cuenta de prueba. Bicicletas, contenido
  propio y Chat deben recorrerse de extremo a extremo con una sesión real.
- Validar notificaciones y conversación entre dos dispositivos/cuentas.
- `location.search` respondió sin sugerencias y `location.reverse` HTTP 400; el
  fallback Android debe mantenerse hasta completar el backend.

Siguiente paso:

- Mantener la app abierta para la revisión visual del usuario y, al disponer de
  credenciales de prueba, ejecutar la matriz autenticada completa.

## 2026-08-05 - Diseño integral verde LED y sincronización en pausa

Objetivo:

- Terminar la primera etapa visual de APPbike con una identidad llamativa y
  coherente, contornos LED en las cajas de texto y una presentación clara de la
  sincronización deportiva como función futura.

Cambios:

- Se reemplazó la base morada por una paleta grafito y verde eléctrico con
  acentos azul y ámbar en `ui/theme/Color.kt` y `Theme.kt`.
- Se completó la escala tipográfica y el sistema de radios en `Type.kt` y
  `Theme.kt`.
- `CommonComponents.kt` ahora aplica fondos ambientales, tarjetas con borde,
  campos de texto con contorno LED persistente y un brillo reforzado al foco.
- La barra inferior usa una familia uniforme de iconos Material tintables en
  vez de cuatro imágenes de estilos diferentes.
- Perfil presenta Strava, Garmin y Wahoo como tarjetas no interactivas con cinta
  diagonal `PRÓXIMAMENTE` y estado `Vinculación en pausa`.
- `AccountScreen` dejó de ejecutar listados, inicio, finalización o eliminación
  de conexiones deportivas mientras la función siga detenida.
- Se corrigió el contraste de los iconos de barras de sistema en Android 15.
- Se actualizaron `AGENTS.md` y `CodexChats/CURRENT_STATE.md` con la decisión de
  producto y el sistema visual vigente.

Pruebas:

- `:app:assembleDebug` correcto después de la implementación y tras el ajuste
  final de Android 15.
- `:app:test` correcto.
- `:app:lint` correcto; reporte en `app/build/reports/lint-results-debug.html`.
- APK debug instalada y ejecutada en el AVD `Small_Phone`.
- Se revisaron visualmente Mapas, Bicicletas, Marketplace, Chat, Perfil, campos
  LED y las tres cintas deportivas en 720 x 1280.
- Capturas finales en `app/build/outputs/appbike-final-map-wait.png`,
  `appbike-final-sports-header.png`, `appbike-final-sports-intro.png` y
  `appbike-final-live.png`.
- Logcat de arranque revisado sin `FATAL EXCEPTION` ni `AndroidRuntime`.

Pendientes:

- El AVD se reinició con datos limpios por falta de almacenamiento; la revisión
  autenticada requiere iniciar sesión nuevamente con una cuenta de prueba.
- Hacer una pasada final en dispositivo físico para calibrar el brillo LED por
  densidad y continuar la mejora pantalla por pantalla con el usuario.

Siguiente paso:

- Mantener APPbike abierta en el emulador y recoger la siguiente observación
  visual del usuario.

## 2026-08-04 - Apertura en emulador

Objetivo:

- Abrir APPbike en un emulador Android para revision manual.

Cambios:

- No se modifico codigo de la app.
- Se localizo el SDK activo en `C:\Android\Sdk`.
- Se arranco el AVD `Small_Phone`.
- Se instalo y abrio el APK debug en el emulador.

Pruebas:

- `:app:assembleDebug` correcto.
- `adb install -r app/build/outputs/apk/debug/app-debug.apk` correcto.
- `com.example.appbike/.MainActivity` quedo como actividad enfocada.

Pendientes:

- Ninguno.

Siguiente paso:

- Revisar visualmente la app en el emulador ya abierto.

## 2026-08-03 - Sistema oscuro morado consistente

Peticion: corregir y completar el rediseño visual para todas las pestañas,
evitando bordes neon permanentes y glows invasivos, con un dashboard oscuro
modular y acentos morados sutiles.

Cambios:

- Se audito la estructura Compose, navegacion, pestañas, componentes compartidos
  y estilos que causaban bordes morados excesivos.
- Se creo un set centralizado de tokens oscuros/morados en el tema Compose.
- Se agregaron componentes compartidos para tarjetas, buscador, estados vacios,
  errores, loading y dimensiones.
- Se reemplazo el glow global intenso por un brillo morado localizado y suave.
- Se rediseñaron Marketplace, barra inferior, Mapas, Bicicletas, Chat y Cuenta
  usando el mismo sistema visual.
- Se dejo `appBikeTextFieldGlow()` como compatibilidad sin dibujo para retirar
  el borde neon anterior sin romper formularios existentes.
- Se mantuvieron la navegacion, contratos remotos, permisos, formularios,
  autenticacion y servicios existentes.

Verificacion:

- `:app:assembleDebug` correcto durante la implementacion y al cierre.
- `:app:lint :app:test :app:assembleDebug` correcto.
- APK debug instalado en `emulator-5554`.
- Revision visual realizada en 720 x 1280 para Mapas, Bicicletas, Marketplace,
  Chat y Cuenta.
- Capturas guardadas en `app/build/outputs/redesign-review/`.
- Logcat revisado sin `FATAL EXCEPTION` ni `AndroidRuntime` de APPbike.

Pendientes:

- No se verificaron manualmente todos los estados internos de detalle, teclado y
  formularios largos; el build y las pruebas unitarias quedaron correctos.

Siguiente paso:

- Revisar en dispositivo real los formularios con teclado abierto y los detalles
  de publicaciones/bicicletas si se quiere afinar microespaciado.

## 2026-07-29 - Interfaz Cuenta con orbe morado

Peticion: abrir el emulador y replicar en morado la interfaz de referencia con
orbe lateral izquierdo.

Cambios:

- Se ajusto el fondo premium comun para concentrar el brillo morado como orbe
  lateral izquierdo sobre base oscura.
- Se rediseno `AccountScreen` con cabecera "Tu", tabs visuales, hero de progreso
  y tarjetas negras inspiradas en la referencia.
- Se mantuvo la navegacion principal existente y no se agregaron destinos nuevos.

Verificacion:

- `:app:assembleDebug` correcto.
- APK debug instalada en `emulator-5554`.
- App abierta en el emulador y pantalla Cuenta dejada visible.
- Captura guardada en `app/build/outputs/appbike-purple-orb-account.png`.
- Logcat revisado sin `FATAL EXCEPTION` ni `AndroidRuntime` de APPbike.

Pendientes:

- Ajuste fino visual si se quiere igualar todavia mas el espaciado exacto de la
  referencia.

Siguiente paso:

- Revisar la pantalla abierta en el emulador y confirmar si se aumenta o reduce
  el tamaño del orbe morado.

## 2026-07-29 - Logos en navegacion principal

Peticion: reemplazar solamente los iconos de los botones Mapas, Bicicletas,
Marketplace y Chat por los logos de la imagen de referencia.

Cambios:

- Se crearon cuatro assets PNG separados en `app/src/main/assets/navigation/`
  con los nombres solicitados.
- La barra inferior de `MainActivity.kt` ahora usa esos PNG mediante un
  componente comun `NavigationLogoIcon`.
- Se conservaron destinos, `onClick`, estados de seleccion, badge de Chat y
  etiquetas existentes.

Verificacion:

- `:app:assembleDebug` correcto.
- Se revisaron dimensiones de los cuatro PNG: 128 x 128 px.

Pendientes:

- Revision visual manual en dispositivo/emulador para afinar el arte final si se
  desea mas fidelidad exacta al adjunto.

Siguiente paso:

- Abrir la app y comprobar la barra inferior en pantalla real.

## 2026-07-25 - Rediseño premium morado/azul/negro

Peticion: cambiar APPbike a una paleta morado, azul y negro, con un diseño mas premium y deportivo, luego intensificar el morado y mejorar las cajas de texto.

Cambios:

- Se reemplazo la paleta verde/arena por una base oscura premium con negro grafito, morado electrico y azul deportivo.
- Se ajusto el morado principal de marca al color exacto `#4E2FB0`.
- Se incorporaron fondos premium con luces radiales moradas y negras, con distribuciones distintas para Cuenta, Bicicletas, Marketplace y Chat.
- Se reforzaron tarjetas, overlays de Mapa y burbujas de Chat con superficies negras, bordes morados y elevacion.
- Se acerco el diseño a la referencia solicitada: luz lateral morada sobre fondo negro, tarjetas negras, tipografia blanca protagonista y acentos blancos/morados.
- Se forzo el acento visible principal del tema oscuro a `#4E2FB0`.
- Se fijo el tema Compose en modo oscuro para mantener la identidad visual consistente.
- Se actualizaron colores de sistema Android en `themes.xml` para status bar, navigation bar y fondo inicial.
- Se ajustaron cabecera y barra inferior con superficies oscuras elevadas, acento violeta y texto deportivo.
- Se agrego un estilo reutilizable para cajas de texto con contenedor oscuro, borde violeta al foco, cursor azul y esquinas mas elegantes.
- Se aplico el nuevo estilo a login, chat, busquedas, ubicacion, creacion de juntas y creacion de publicaciones.

Verificacion:

- `:app:assembleDebug` correcto.
- APK debug instalado en el emulador `emulator-5554`.
- `MainActivity` iniciada con `adb shell am start`.
- Logcat revisado sin `FATAL EXCEPTION` ni `AndroidRuntime` durante el arranque.
- Captura visual guardada en `app/build/outputs/appbike-premium-screen.png`.
- Captura del estilo final de Cuenta guardada en `app/build/outputs/appbike-account-reference-style.png`.

Pendientes:

- Revision visual manual de cada pantalla con datos reales para afinar contraste fino en tarjetas especificas.

Siguiente paso:

- Probar login, Marketplace, Mapa y Chat en dispositivo fisico para ajustar detalles visuales por densidad y brillo de pantalla.

## 2026-07-24 - Gradle 8.13.2 ejecutable

Peticion: actualizar el proyecto a Gradle/AGP 8.13.2 y dejar la app ejecutando.

Cambios:

- Se confirmo que el catalogo usa Android Gradle Plugin 8.13.2 y el wrapper usa Gradle 8.13.
- Se fijo `org.gradle.java.home` al JBR de Android Studio para evitar que `gradlew` use Java 8 desde el PATH.

Verificacion:

- `:app:assembleDebug` correcto.
- APK debug instalado en el dispositivo `R58T9039QBN`.
- `MainActivity` iniciada con `adb shell am start`.
- Logcat filtrado sin errores `AndroidRuntime`, `APPbike` ni `System.err` tras el arranque.

Pendientes:

- Ninguno para la actualizacion Gradle/JDK local.

Siguiente paso:

- Ejecutar pruebas funcionales manuales de los flujos que se quieran validar en dispositivo.

## 2026-07-22 - Centralización y listener persistente

Petición: preservar todo el contexto para continuar manualmente y corregir las
notificaciones que no llegaban fuera de la app.

Cambios:

- Se creó `CodexChats` con router, estado, flujo manual, documentación de
  notificaciones y sesiones fechadas.
- Fuente y manual de EasyMD se trasladaron a `CodexChats/EasyMD`.
- Se archivaron 14 tareas/respuestas Markdown del inbox, sin copiar secretos.
- Se copió y después recompiló el ejecutable vigente `EasyMD-auto5.exe` desde su
  nueva ruta; el build terminó con el warning histórico `CS0162` y su SHA-256
  quedó registrado.
- La copia de `dist` se mantiene porque había dos procesos EasyMD activos.
- El sondeo de Chat salió de Compose y pasó a
  `ChatNotificationListenerService`, Foreground Service `remoteMessaging`.
- Se agregó un bus interno para conservar el banner cuando la app está visible.
- Se conservaron WorkManager y su cursor independiente como respaldo.
- Se agregó acceso directo a ajustes cuando las notificaciones están apagadas.
- Se renovó la solicitud de permiso con la clave de migración
  `notifications_listener_v2_asked`.

Verificación:

- Build, tests unitarios y APK de tests instrumentados correctos.
- Listener confirmado como Foreground Service `remoteMessaging` después de
  enviar la actividad al inicio.
- Canales confirmados con importancia baja para estado y alta para mensajes.
- Sin excepciones fatales o de permisos de Foreground Service.
- Prueba cruzada pendiente porque solo estaba conectado el emulador y su token
  remoto conservado había expirado.

Pruebas y resultado final de la sesión se detallan en
`sessions/2026-07-22-listener-notificaciones.md`.
## 2026-08-04 - Refinamiento de logos inferiores

Objetivo:

- Mejorar definicion y encuadre de los logos de Bicicletas, Marketplace y Chat
  en la barra inferior.

Cambios:

- Se regenero `bicicletas-logo.png` desde la fuente original, con mayor
  definicion y contorno claro para destacar en fondo oscuro.
- Se regenero `marketplace-logo.png` mas pequeno y con limpieza de fondo para
  retirar bordes/relieves blancos no deseados.
- Se regenero `chat-logo.png` mas pequeno, centrado y simetrico dentro del
  lienzo compartido de 256 px.

Pruebas:

- `:app:assembleDebug` correcto.
- APK debug reinstalada en el emulador `emulator-5554`.
- `MainActivity` iniciada con `adb shell am start`.
- Captura visual guardada en
  `app/build/outputs/appbike-nav-logos-refined-balanced-ready.png`.
- Logcat revisado sin `FATAL EXCEPTION` ni `AndroidRuntime`.

Pendientes:

- Ninguno para este refinamiento visual.

Siguiente paso:

- Revisar en dispositivo fisico si se quiere ajustar un par de pixeles por
  densidad real de pantalla.

## 2026-08-04 - Perfil limpio, inputs LED e icono bicicleta

Objetivo:

- Quitar del perfil el bloque de progreso mostrado por el usuario, dar relieve
  morado LED a las cajas de texto y mejorar el contraste del icono de
  Bicicletas.

Cambios:

- Se elimino del flujo de Cuenta el bloque de tarjetas `Mejores tiempos`,
  `Metas` y `Esfuerzo`.
- Se agrego un modificador reutilizable de glow morado para campos de texto y
  se aplico a `AppInput`, busquedas, chat, ubicacion y formularios principales.
- Se amplio el asset `bicicletas-logo.png` dentro de su lienzo y se agrego un
  contorno blanco para separarlo mejor de la barra inferior.

Pruebas:

- `:app:lint` correcto.
- `:app:test` correcto.
- `:app:assembleDebug` correcto.
- APK debug reinstalada en el emulador `emulator-5554`.
- `MainActivity` iniciada con `adb shell am start`.
- Capturas visuales guardadas en
  `app/build/outputs/appbike-profile-clean-led-profile.png` y
  `app/build/outputs/appbike-bike-icon-white.png`.
- Logcat revisado sin `FATAL EXCEPTION` ni `AndroidRuntime`.

Pendientes:

- Ninguno para este ajuste visual.

Siguiente paso:

- Revisar en dispositivo fisico si se quiere ajustar la intensidad del glow.

## 2026-08-04 - Dashboard de progreso Bento morado

Objetivo:

- Implementar una pantalla de progreso movil en modo oscuro, inspirada en la
  referencia, con Bento Grid y acentos morado neon.

Cambios:

- Se rediseño el bloque de progreso de Cuenta en `Account.kt` con tarjeta
  principal de ancho completo, tarjetas pequenas en dos columnas, estados
  pressed/hover/focus y glow morado sutil.
- Se ajusto el fondo premium de Cuenta en `CommonComponents.kt` a base
  `#090909` con gradientes radiales difuminados.
- Se mantuvo la navegacion principal existente sin agregar pestañas nuevas.

Pruebas:

- `:app:lint` correcto.
- `:app:test` correcto.
- `:app:assembleDebug` correcto.
- APK debug reinstalada en el emulador `emulator-5554`.
- `MainActivity` iniciada con `adb shell am start`.
- Capturas visuales guardadas en
  `app/build/outputs/appbike-progress-dashboard-final-top.png` y
  `app/build/outputs/appbike-progress-dashboard-final-grid.png`.
- Logcat revisado sin `FATAL EXCEPTION` ni `AndroidRuntime` durante el arranque.

Pendientes:

- Ninguno para este rediseño.

Siguiente paso:

- Revisar en dispositivo fisico para afinar brillo del glow morado si hace
  falta.

## 2026-08-04 - Iconos de navegacion sin fondo

Objetivo:

- Quitar los bordes/fondos de las fotos de navegacion y agrandar las figuras
  sin alterar las distancias de la barra inferior.

Cambios:

- Se reprocesaron los cuatro PNG de `app/src/main/assets/navigation/` con fondo
  transparente.
- Se recortaron las figuras utiles y se reencuadraron dentro del mismo lienzo
  cuadrado de 256 px para mantener el espacio Compose existente.

Pruebas:

- `:app:assembleDebug` correcto.
- APK debug reinstalada en el emulador `emulator-5554`.
- `MainActivity` iniciada con `adb shell am start`.
- Captura visual guardada en
  `app/build/outputs/appbike-nav-icons-transparent-run.png`.
- Logcat revisado sin `FATAL EXCEPTION` ni `AndroidRuntime` de la app durante
  el arranque.

Pendientes:

- Ninguno para este ajuste visual.

Siguiente paso:

- Validar en dispositivo fisico si se quiere revisar el tamano percibido por
  densidad de pantalla.

## 2026-08-04 - Iconos de navegacion desde imagenes entregadas

Objetivo:

- Reemplazar las imagenes de los accesos Mapas, Bicicletas, Marketplace y Chat
  usando el paquete indicado por el usuario.

Cambios:

- Se resolvio el acceso directo `imagenes app.lnk` hacia el ZIP de Descargas.
- Se generaron PNG cuadrados de 256 px para `app/src/main/assets/navigation/`,
  conservando cada imagen centrada y sin deformarla.
- Se mantuvo la navegacion existente de la barra inferior sin agregar destinos
  nuevos.

Pruebas:

- `:app:assembleDebug` correcto.
- APK debug instalada en el emulador `emulator-5554`.
- `MainActivity` iniciada con `adb shell am start`.
- Captura visual guardada en
  `app/build/outputs/appbike-nav-icons-run.png`.
- Logcat revisado sin `FATAL EXCEPTION` ni `AndroidRuntime` de la app durante
  el arranque.

Pendientes:

- Ninguno para este reemplazo de iconos.

Siguiente paso:

- Validar manualmente en dispositivo fisico si se quiere revisar densidad y
  contraste fuera del emulador.

## 2026-08-06 - SDK Manager operativo

Objetivo:

- Corregir la ruta rota del Android SDK y dejar operativo el SDK Manager para
  sincronizar y compilar APPbike.

Cambios:

- Se alineo `local.properties`, Android Studio y las variables de usuario
  `ANDROID_HOME`/`ANDROID_SDK_ROOT` con
  `C:\Users\Benja(martulalover)\AppData\Local\Android\Sdk`.
- Se instalaron las herramientas oficiales `cmdline-tools` 22.0 y se
  agregaron `platform-tools` 37.0.1, plataformas `android-37.0` y Build Tools
  36.0.0/37.0.0.
- Se corrigio el metadata local de la plataforma 37 para que SDK Manager la
  indexe y Gradle no intente reinstalarla en una carpeta `-2`.
- Se agregaron `platform-tools` y `cmdline-tools\latest\bin` al `PATH` de
  usuario.

Pruebas:

- `sdkmanager --version`: correcto, 22.0.
- `sdkmanager --list`: muestra instalados `platforms;android-37.0`,
  `platform-tools`, Build Tools 36 y Build Tools 37.
- `adb version` y `aapt2 version`: correctos.
- `:app:tasks --offline`: correcto; Gradle configura el modulo sin pedir SDK.
- `:app:assembleDebug --offline`: no completa porque faltan AAR de Compose,
  MapLibre y AndroidX en cache; no es un error de SDK.

Pendientes:

- Ejecutar una compilacion online cuando la red termine de descargar las AAR
  faltantes.

Siguiente paso:

- Reiniciar Android Studio o abrir una terminal nueva para heredar las
  variables y ejecutar Sync Project with Gradle Files.
