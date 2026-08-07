# Sesión 2026-08-05 - Endurecimiento funcional y validación

## Objetivo

Continuar el trabajo después del rediseño visual, corregir riesgos funcionales
de alto impacto, ampliar la seguridad de las pruebas y dejar el APK ejecutado en
el emulador para la siguiente revisión.

## Diagnóstico

- Un Bearer guardado se aplicaba a todas las solicitudes del gateway. Un token
  vencido podía romper login, Mapas y Marketplace aunque fueran flujos públicos.
- Los dos listados del perfil se cargaban como una sola operación: el fallo de
  uno descartaba también el resultado correcto del otro.
- El fallback regional de `*.mine.list` no se activaba ante una acción ausente
  cuando había token, pese al contrato de compatibilidad documentado.
- La prueba remota de Chat fallaba en cualquier AVD sin cuenta y la prueba del
  almacén cifrado podía borrar el token real del usuario.

## Cambios realizados

- Política central de autorización por acción en `RemoteConnections.kt`.
- Mensajes específicos para errores HTTP sin cuerpo JSON y metadatos de estado
  en `RemoteConnectionException`.
- Fallback regional limitado a estados compatibles con acción no desplegada;
  no enmascara un 401 con token ni errores de servidor/transporte.
- Carga paralela e independiente de publicaciones y juntas propias, con banner
  de error y acción de reintento.
- Pruebas instrumentadas seguras para sesión ausente y token preexistente.
- Nueva cobertura unitaria de acciones públicas/privadas y fallback de perfil.
- Login y bicicletas validan UUID/propiedad antes de exponer datos; errores de
  listado ofrecen reintento y las fechas usan validación calendario estricta.
- Creación de Juntas/Marketplace bloquea doble envío y distingue éxito JSON de
  fallo posterior de foto para evitar duplicados. Recargas y detalles descartan
  respuestas antiguas y listados públicos ignoran IDs vacíos.
- Chat conserva el borrador hasta éxito, serializa sincronizaciones con `Mutex`,
  rechaza identidades vacías/cross-chat y permite que la copia remota repare la
  caché obsoleta.
- Fotos locales se muestrean con orientación EXIF; subidas y descargas Android
  se limitan a 20 MB. Backup excluye sesión, ubicaciones y caché privada.
- Cuenta sin sesión prioriza el formulario de acceso antes del panel de campaña.
- Se corrigió compatibilidad de ajustes de notificación en Android 7 y del tipo
  de Foreground Service de Chat en Android 10–13.

## Backend observado

La consulta directa al gateway el 2026-08-05 mostró HTTP 200 para listados y
detalles públicos de Marketplace/Juntas, Base64 en `junta.photo.get`, campos
geográficos y de moneda en Marketplace, y ausencia de rutas internas en la
muestra. `location.search` quedó sin resultados, `location.resolve` respondió y
`location.reverse` devolvió HTTP 400. Las acciones privadas sin token devolvieron
HTTP 401, por lo que falta comprobarlas con una sesión válida.

## Verificación

- `:app:testDebugUnitTest :app:assembleDebug`: correcto.
- `:app:connectedDebugAndroidTest`: correcto; 8 pruebas, 0 fallos y 1 omisión
  esperada por ausencia de credenciales reales de Chat.
- 23 pruebas unitarias finalizadas sin fallos.
- `:app:lint`: correcto.
- APK instalada y ejecutada en `Small_Phone`.
- Mapas, Bicicletas, Marketplace, Chat, login LED y las cintas deportivas se
  inspeccionaron con APPbike al frente.
- Logcat sin excepción fatal ni ANR de APPbike.
- Capturas finales: `appbike-map-ready.png`, `appbike-bikes.png`,
  `appbike-marketplace.png`, `appbike-chat.png`,
  `appbike-account-login-final.png` y `appbike-sports-final.png`.

## Pendientes

- Recorrido autenticado de cuenta, bicicletas, perfil propio y Chat.
- Prueba de conversación/notificaciones con dos cuentas y dos dispositivos.
- Validación física del diseño LED en otra densidad de pantalla.
- Completar comportamiento remoto de ubicación antes de retirar los fallbacks.

## Siguiente paso

Recoger la revisión del usuario con la app abierta y continuar la mejora por
pantalla; usar una cuenta de prueba cuando se pase a los flujos privados.
