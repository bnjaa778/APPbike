# Roadmap manual para finalizar APPbike

Fecha: 2026-08-07
Audiencia: desarrollador Android, desarrollador backend, QA y responsable de
producto.
Propósito: terminar y publicar APPbike sin depender de IA ni de conocimiento
que solo exista en conversaciones anteriores.

Este plan parte del estado confirmado en `docs/PROJECT_REPORT.md`,
`docs/BACKEND_IMPLEMENTATION_REPORT.md`,
`docs/MAP_MARKETPLACE_CHAT.md` y `CodexChats/CURRENT_STATE.md`. No es una lista
de ideas: cada fase entrega un contrato, código comprobable o una decisión de
producto que desbloquea la siguiente.

> Progreso al 2026-08-11: el workflow local
> `.github/workflows/android-verify.yml` ya cubre pruebas unitarias, Lint y APK
> debug en JDK 17/SDK 37. La fase 0 sigue abierta hasta tener su primera
> ejecucion remota exitosa, cuentas de prueba, tablero y rama de release.

## 1. Punto de partida y definición de terminado

La rama de trabajo compila con Gradle 9.6.1, AGP 9.3.1, SDK 37 y genera APK
debug. Mapas/Juntas y Marketplace públicos ya responden; bicicletas, cuenta,
perfil, chat y notificaciones tienen cliente Android. Sin embargo, varias
lecturas privadas solo han sido comprobadas sin token (HTTP 401),
`location.reverse` falla para coordenadas válidas y la entrega push con la app
cerrada sigue pendiente de FCM.

La versión 1.0 se considera terminada únicamente cuando se cumpla todo lo
siguiente:

1. Una persona puede crear una cuenta, recuperar el acceso y usar la app sin
   intervención manual del equipo.
2. Todas las mutaciones y lecturas privadas se autorizan por Bearer y por
   propietario en el servidor; `user_id` del cuerpo nunca autoriza por sí solo.
3. Mapas, Marketplace y el perfil propio funcionan con región, distancia,
   moneda y portadas resueltas por el servidor, sin depender de filtrar una
   región completa en el teléfono.
4. Dos usuarios de prueba pueden crear contenido, contactar, conversar y recibir
   una notificación push aun después de reiniciar el dispositivo.
5. Las pruebas automatizadas, la matriz manual, la accesibilidad y la entrega
   firmada superan los criterios de la fase 6.

La sincronización Strava/Garmin/Wahoo **no bloquea esta versión** mientras el
producto mantenga la decisión de dejarla como `PRÓXIMAMENTE`. Si se decide
activarla, se ejecuta la fase 7 antes de anunciarla al usuario. No crear rutas
de actividad, pantallas o flujos nuevos que no estén definidos en los contratos
actuales.

## 2. Cómo ejecutar el plan sin IA

- Abrir cada tarea como una issue con: objetivo, contrato afectado, archivos,
  criterios de aceptación, plan de reversión y responsable. Una tarea no se
  inicia si no puede escribirse su prueba de aceptación.
- Trabajar con una rama por issue y revisión de otra persona para cambios de
  seguridad, migraciones SQL, autenticación, fotos, Chat o permisos Android.
- Mantener `AGENTS.md` como router técnico y registrar cada entrega en
  `CodexChats/CHANGELOG.md`. Las sesiones relevantes deben dejar un informe en
  `CodexChats/sessions/` con comandos, datos de prueba y resultado.
- No copiar secretos, tokens, contraseñas, archivos `local.properties`, claves
  de firma ni credenciales FCM al repositorio, a tickets públicos o a capturas.
- No modificar contratos por intuición. Cuando cambie una API, actualizar en el
  mismo cambio el modelo Android, `RemoteConnections.kt`, la pantalla que lo
  consume, pruebas y el documento de contrato.

### Roles mínimos

| Rol | Responsabilidad |
|---|---|
| Responsable de producto | Decide alcance 1.0, reglas de alta/recuperación y si se activa OAuth. |
| Backend | Gateway, PostgreSQL/PostGIS, autenticación, contratos, FCM y datos de prueba. |
| Android | Cliente Kotlin/Compose, almacenamiento seguro, UI, migraciones de contrato y APK. |
| QA | Matriz de dispositivos, pruebas cruzadas de dos cuentas, regresiones, evidencia y aprobación de release. |
| Revisor técnico | Revisa seguridad, SQL, concurrencia y cambios de contrato antes de fusionar. |

### Estimación de referencia

Con un desarrollador Android, uno backend y apoyo QA, las fases 0 a 6 requieren
aproximadamente 10 a 14 semanas de calendario. Es una estimación para planificar
capacidad; al cerrar la fase 0 se reemplaza por estimaciones de issues. La fase
7 es opcional y se estima por proveedor, no como un bloque único.

## 3. Orden obligatorio de trabajo

| Fase | Resultado | Dependencia | Esfuerzo orientativo |
|---|---|---|---|
| 0 | Línea base reproducible, backlog y entornos de prueba | Ninguna | 2–3 días |
| 1 | Onboarding seguro y autorización real | Fase 0 | 1–2 semanas |
| 2 | Geografía, región y listados públicos escalables | Fase 1 | 2–3 semanas |
| 3 | Administración privada multirregional y fotos | Fases 1–2 | 1–2 semanas |
| 4 | Chat autenticado y push FCM | Fases 1 y 3 | 2–3 semanas |
| 5 | Calidad, rendimiento y accesibilidad de cierre | Fases 1–4 | 1–2 semanas |
| 6 | Firma, distribución y monitoreo de producción | Fase 5 | 1 semana |
| 7 | OAuth deportivo, un proveedor a la vez | Decisión explícita de producto | 1–2 semanas por proveedor |

No iniciar una fase posterior si la anterior no tiene evidencia de aceptación.
Puede prepararse código aislado en paralelo, pero no integrarse ni retirar un
fallback antes de que su contrato remoto esté desplegado y probado.

## 4. Fase 0 — Preparar una línea base reproducible

**Objetivo:** que cualquier programador pueda compilar, probar y diagnosticar
la misma versión antes de cambiar comportamiento.

### Trabajo

1. Crear un tablero de issues con las fases de este documento, prioridad,
   responsable, dependencia y fecha de revisión semanal.
2. Elegir y documentar la rama de release. La rama actual
   `redesign/purple-dark-ui` debe revisarse y fusionarse a la rama de release
   elegida; no publicar desde una rama personal sin revisión.
3. Crear dos cuentas de prueba aisladas, una publicación activa con foto,
   una junta activa con foto y una bicicleta descartable. Documentar dónde se
   regeneran, nunca sus contraseñas ni tokens.
4. Publicar y comprobar la primera ejecución de la integración continua ya
   preparada en `.github/workflows/android-verify.yml`; ejecuta
   `:app:testDebugUnitTest`, `:app:lintDebug` y `:app:assembleDebug` con JDK 17
   y SDK 37. La ejecución instrumentada queda en un job con emulador o
   dispositivo gestionado y se añade al mismo control de cambios cuando sea
   estable.
5. Corregir `CodexChats/MANUAL_WORKFLOW.md` si contiene rutas de SDK/JDK que no
   correspondan al equipo actual; debe usar variables o explicar cómo definirlas.
6. Registrar versión inicial, SHA, tamaño del APK y resultados de prueba. El
   `versionCode=1` y `versionName=1.0` actuales son la referencia de partida.

### Aceptación

- Un clon limpio en Windows con JDK 17 construye `app-debug.apk` sin editar
  código.
- La CI falla si fallan las pruebas unitarias, Lint o el ensamblado debug.
- QA puede iniciar sesión con dos cuentas de prueba y reproducir datos sin usar
  datos personales.
- Existe un issue priorizado para cada fase y una rama de release protegida o
  con revisión obligatoria.

## 5. Fase 1 — Onboarding, sesión y autorización de propietario

**Objetivo:** eliminar la provisión manual de cuentas y cerrar el riesgo de que
un UUID enviado por el cliente autorice acciones de otra persona.

### Backend

1. Especificar y aprobar nombres, cuerpos JSON, respuestas y códigos de error
   para registro, inicio de recuperación y confirmación de recuperación. No
   inventar nombres en Android antes de aprobar esa especificación.
2. Implementar en el gateway, allowlist, switch privado y tester las acciones
   aprobadas. Aplicar límites de frecuencia, expiración, token de un uso y
   mensajes neutrales que no enumeren cuentas.
3. Confirmar que `login` emite un Bearer opaco o JWT con expiración, y que el
   gateway reenvía `Authorization` a la API privada.
4. En cada lectura privada y mutación, obtener el actor desde el token y
   compararlo con el propietario real de PostgreSQL. Cubrir bicicletas,
   mantenciones, publicaciones, juntas, fotos, estados, Chat y deportes.
5. Añadir respuestas seguras y consistentes: 400, 401, 403, 404, 409 y 429. No
   exponer SQL, rutas de disco, tokens ni trazas.

### Android

1. Cuando el contrato esté desplegado, agregar los flujos de registro y
   recuperación en `Account.kt` y sus clientes en `RemoteConnections.kt`.
2. Conservar UUID como identidad interna y `nombre_de_usuario` como presentación.
   Guardar solo `user_id`, correo, nombre y Bearer cifrado en Keystore; jamás la
   contraseña o un token de recuperación.
3. Manejar expiración/revocación de token: borrar sesión privada, detener el
   listener y llevar a Cuenta con un mensaje claro. No mostrar datos de la
   sesión anterior mientras se resuelve el cambio.
4. Añadir pruebas unitarias y de interfaz para registro, recuperación, token
   inválido, logout y aislamiento entre dos cuentas.

### Aceptación

- Una cuenta nueva se registra, inicia sesión, cierra sesión y recupera el
  acceso sin soporte del equipo.
- Una mutación deliberada con token de A y `user_id` de B devuelve 403 y no
  cambia datos.
- Tokens vencidos/revocados devuelven a Cuenta sin filtrar caché, perfiles,
  notificaciones ni diálogos de otra cuenta.

## 6. Fase 2 — Geografía, regiones y descubrimiento público

**Objetivo:** que región, moneda, distancia y paginación sean responsabilidad
del servidor y funcionen con datos reales más allá de una sola región.

### Backend

1. Respaldar la base y crear migraciones aditivas para PostGIS, columnas
   `latitude`, `longitude`, `geo`, `country_code`, `administrative_area` y
   `currency` donde corresponda. Crear índices GIST y la fuente autoritativa de
   regiones/alias definida en `BACKEND_IMPLEMENTATION_REPORT.md`.
2. Ejecutar un backfill verificable del formato legado
   `REGION:<lat>,<lng>|<etiqueta>`; mantener nula la geografía que no pueda
   parsearse en vez de inventar coordenadas.
3. Implementar `location.reverse` para coordenadas válidas y completar
   `location.search`/`location.resolve` con caché, proveedor controlado,
   timeout, límite de frecuencia y atribución. El proveedor externo se consulta
   solo desde el backend.
4. Aplicar `ST_DWithin`, orden por distancia/fecha, búsqueda parametrizada y
   paginación estable a `marketplace.list` y `junta.list`.
5. Entregar latitud, longitud, región, distancia, moneda y `photo_id` de portada
   por elemento. No entregar Base64 ni rutas internas en listados.

### Android

1. Probar creación, búsqueda, lista y detalle con las nuevas respuestas; mantener
   los contratos de selección y cancelación de ubicación actuales.
2. Retirar el filtrado regional/local y el respaldo directo a Nominatim solo
   después de una versión Android desplegada que valide el backend nuevo. No
   enviar nunca Bearer a un geocodificador externo.
3. Confirmar que una publicación conserva su moneda al cambiar la ubicación de
   búsqueda y que las juntas conservan su región al editarse.

### Aceptación

- `location.reverse` resuelve Santiago y otras coordenadas válidas sin
  `unknown_region`.
- Una consulta de 40 km excluye un registro lejano, ordena por `distance_km` y
  pagina sin duplicados.
- `Viña del Mar` conserva Unicode de extremo a extremo.
- Una tarjeta nueva muestra portada desde `photo_id` sin hacer una consulta de
  detalle por cada elemento.

## 7. Fase 3 — Perfil, contenido propio y fotos comunitarias

**Objetivo:** permitir que cada propietario administre de forma segura todas
sus publicaciones y juntas, incluso si viven en regiones distintas.

### Trabajo

1. Implementar `marketplace.mine.list` y `junta.mine.list` autenticados,
   multirregionales y con filtros de estado. Cada elemento debe incluir el
   `user_id` del actor; una respuesta sin propietario es un error de contrato.
2. Hacer transaccionales los cambios de estado, completar, editar, borrar y
   administrar fotos. Al mover datos entre tablas se conserva ID, fecha,
   geografía, moneda y fotos.
3. Validar propietario antes de subir/borrar fotos; validar contenido real JPG,
   PNG o WebP, tamaño y limpieza de archivos huérfanos recuperable.
4. En Android, probar `ProfileContent.kt` con los listados reales y retirar el
   fallback regional solo tras comprobar datos de al menos dos regiones.
5. Mantener los URI internos de foto y `content_base64`; no abrir rutas internas
   como URL pública ni cambiar el nombre multipart `foto`.

### Aceptación

- A ve sus publicaciones en todos los cuatro estados y sus juntas activas y
  pasadas, sin ver contenido de B.
- A puede editar, cambiar estado, completar, añadir/eliminar foto y borrar su
  contenido; B recibe 403 en cada intento equivalente.
- Ninguna respuesta pública contiene `file_path`, `photo_folder_path` o Base64
  dentro del listado.

## 8. Fase 4 — Chat verificado y notificaciones push

**Objetivo:** hacer demostrable una conversación de dos usuarios y la entrega
de mensajes con la app visible, en segundo plano y después de reiniciar.

### Trabajo de Chat

1. Validar con dos cuentas reales `chat.get_or_create`, `chat.list`,
   `chat.messages.list` y `chat.message.send` a través del gateway. Retirar las
   rutas REST de fallback solo después de una versión móvil verificada.
2. En servidor, imponer membresía, propiedad de la entidad relacionada,
   idempotencia por `client_message_id`, orden incremental e índices de mensajes.
3. Ejecutar pruebas de A/B: crear chat desde Marketplace y Junta, enviar en ambos
   sentidos, reintentar el mismo envío, cambiar de cuenta, cerrar y reabrir la
   app, y validar que las cachés no se mezclan.

### Trabajo de FCM

1. Producto y backend definen en una especificación versionada el registro,
   renovación, revocación y asociación de tokens de dispositivo. No inventar
   acciones ni almacenar tokens FCM sin ese contrato.
2. Backend integra Firebase Admin con credenciales en secret manager, envía solo
   a participantes autorizados y elimina tokens inválidos. El payload incluye
   identificadores mínimos para abrir el chat, no secretos ni contenido sensible
   innecesario.
3. Android incorpora Firebase Messaging, renovación de token, servicio de
   recepción, canal y `PendingIntent` con `recipientUserId` y `chatId`. Mantiene
   el listener actual como respaldo mientras se monitorea la transición.
4. Añadir consentimiento/permisos de notificación, telemetría sin contenido de
   mensajes y prueba de tokens renovados o revocados.

### Aceptación

- Dos dispositivos físicos con A y B intercambian mensajes sin duplicados;
  ningún tercero puede listar o enviar a su chat.
- Un mensaje nuevo genera banner con la app visible, notificación en segundo
  plano y push tras reiniciar el receptor. Un `force-stop` se prueba según las
  limitaciones documentadas de Android/FCM.
- Tocar la notificación abre solo el chat de la cuenta destinataria; cambiar de
  cuenta o cerrar sesión descarta el intento.

## 9. Fase 5 — Calidad, seguridad y accesibilidad de cierre

**Objetivo:** convertir las validaciones parciales existentes en una puerta de
salida repetible para cada release.

### Trabajo

1. Ampliar las suites actuales (18 unitarias y 11 instrumentadas en la rama de
   referencia) con regresiones para todos los criterios anteriores. Separar
   claramente pruebas sin red, contrato contra staging y pruebas físicas.
2. Ejecutar la matriz: API 24, Android 13 con notificaciones, Android 17/API 37,
   teléfono físico; retrato, horizontal, 200 % de fuente, sin red, red lenta y
   cambio de cuenta durante una carga.
3. Repetir auditoría TalkBack, foco de teclado, tamaños táctiles, contraste y
   las cuatro pantallas principales. Conservar evidencia en
   `CodexChats/audits/`.
4. Revisar seguridad: Keystore, reglas de backup, permisos mínimos, validación de
   URLs/fotos, no registrar datos sensibles y revisión de dependencias.
5. Definir umbrales de rendimiento con medición manual: inicio en frío, carga de
   mapa, lista de Marketplace, apertura de chat y envío. Toda regresión debe
   tener issue antes de release.
6. Dividir `RemoteConnections.kt` o `BikesScreen.kt` solo en cambios pequeños
   con pruebas verdes; no mezclar esa refactorización con cambios de contrato.

### Puerta de calidad

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:assembleRelease
.\gradlew.bat :app:connectedDebugAndroidTest
```

Todas las pruebas deben pasar. Una omisión aceptada exige razón, responsable,
fecha de vencimiento y issue enlazada; no se acepta como evidencia de que la
función está terminada.

## 10. Fase 6 — Release de producción

**Objetivo:** publicar un artefacto instalable, rastreable y recuperable.

1. Definir versionado semántico y aumentar `versionCode`/`versionName` en cada
   entrega. El release actual parte de `1`/`1.0`.
2. Crear la clave de firma fuera del repositorio, guardar su acceso en un gestor
   de secretos y configurar firma release en la CI. Verificar certificado,
   `applicationId` y artefacto firmado antes de subirlo.
3. Decidir explícitamente si se activa reducción/obfuscación; si se activa,
   ejecutar pruebas de mapa, serialización JSON, Deep Links, servicio foreground
   y notificaciones sobre el APK/AAB minificado.
4. Completar ficha de tienda, política de privacidad, explicación de ubicación,
   fotos, notificaciones y tratamiento de datos. Validar la declaración contra
   los permisos realmente presentes en el manifest.
5. Publicar primero en distribución interna, después en beta cerrada. Establecer
   rollback: mantener el AAB previo, versión de API compatible, consulta de
   errores y contacto de soporte.
6. Aprobar producción solo con informe QA, resultados CI, contrato backend
   desplegado, evidencia de dos cuentas y lista de incidencias conocidas.

### Aceptación

- AAB/APK firmado se instala desde distribución interna en un dispositivo limpio.
- La actualización conserva o migra datos compatibles y el rollback no rompe el
  backend.
- No hay bloqueantes de seguridad, privacidad, crash, ANR, acceso cruzado ni
  pérdida de mensajes en la beta acordada.

## 11. Fase 7 — Reactivar deporte solo con decisión de producto

Esta fase está fuera de 1.0 mientras las tarjetas sigan informativas. Si se
aprueba, ejecutar un proveedor completo antes de iniciar el siguiente:

1. Registrar credenciales, scopes y callback del proveedor en el backend.
2. Implementar `sports.connections.list`, `sports.oauth.start`,
   `sports.oauth.complete` y `sports.connection.delete` con `state` único,
   expiración, revocación y tokens cifrados solo en servidor.
3. Activar su tarjeta Android, probar deep link
   `appbike://oauth/callback`, errores, cancelación y desconexión.
4. Ejecutar una sincronización idempotente por job con cursor; no hacerlo dentro
   de una respuesta HTTP móvil.
5. Repetir con cuenta real y aprobar antes de anunciar el proveedor. Después
   evaluar Strava, Garmin y Wahoo por separado.

## 12. Tablero operativo y cierre

Cada issue debe avanzar por estas columnas:

```text
Por especificar -> Listo para desarrollar -> En desarrollo -> En revisión
-> En staging -> QA aprobado -> Listo para release -> Publicado
```

Para mover una issue a **QA aprobado** deben adjuntarse enlace al cambio,
contrato o migración, comandos ejecutados, datos de prueba no sensibles,
capturas/logs cuando corresponda y resultado de los criterios de aceptación.
Para mover la aplicación a **Publicado** deben estar cerradas las fases 0 a 6.

### Riesgos y decisiones que no se deben aplazar

- Registro y recuperación de contraseña requieren una decisión de producto y
  contrato de backend; Android no debe simularlos.
- PostGIS, regiones y migración de datos deben pasar por backup, migración
  aditiva y plan de reversión antes de tocar datos productivos.
- FCM necesita proyecto Firebase, secretos de servidor y política de privacidad;
  el sondeo actual no equivale a push garantizado.
- OAuth se mantiene apagado hasta disponer de credenciales, scopes y revisión de
  seguridad por proveedor.
- Las rutas internas de fotos y cualquier clave de firma son datos sensibles;
  una entrega que los exponga se bloquea aunque la interfaz parezca funcionar.

Con este orden, un equipo humano puede finalizar la aplicación partiendo del
repositorio actual, sin depender de decisiones implícitas ni de una herramienta
de IA para descubrir el siguiente paso.
