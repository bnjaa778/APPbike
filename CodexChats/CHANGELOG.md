# Registro de cambios de CodexChats

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
