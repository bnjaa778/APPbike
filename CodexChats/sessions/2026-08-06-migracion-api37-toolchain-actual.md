# Sesion 2026-08-06 - Migracion API 37 y toolchain actual

## Objetivo

Eliminar las siete advertencias de versiones que quedaban despues del cierre
visual y migrar APPbike como un conjunto coherente a Android 17, sin forzar
librerias nuevas sobre un compilador antiguo.

## Migracion aplicada

- Gradle 8.13 -> 9.6.1.
- Android Gradle Plugin 8.13.2 -> 9.3.1.
- Kotlin Android externo 2.0.21 -> Kotlin integrado de AGP 9.
- Compose Compiler 2.0.21 -> 2.4.10.
- Compose BOM 2024.09.00 -> 2026.06.01.
- compile/target SDK 36 -> 37.
- AndroidX Core 1.18 -> 1.19 y Lifecycle 2.10 -> 2.11.
- MapLibre OpenGL 13.0.2 -> 13.4.1.
- Wrapper, scripts y JAR de Gradle regenerados con 9.6.1.

La plataforma Android 37.0 revision 2 se descargo automaticamente con las
licencias aceptadas. El proyecto mantiene minSdk 24 y bytecode Java/Kotlin 11;
el daemon usa el JBR 21 de Android Studio.

## Ajustes de codigo

- Se retiro `org.jetbrains.kotlin.android` y `android.kotlinOptions`; AGP 9
  compila Kotlin de forma integrada.
- `TabRow` de Chat fue reemplazado por `PrimaryTabRow`.
- Cuatro helpers JSON cubren `null` explicitamente bajo Kotlin 2.4.
- Cinco suites Compose usan `androidx.compose.ui.test.junit4.v2`.
- Los `.so` precompilados de MapLibre y AndroidX Graphics se marcan como
  simbolos conservados; AGP deja de intentar un strip imposible y empaqueta el
  mismo binario de forma declarativa.

## Compatibilidad Android 17 revisada

APPbike no usa LAN local, SMS, Bluetooth, Contacts Provider, audio de fondo,
reflexion de `MessageQueue`, mutacion de campos `static final` ni carga dinamica
nativa propia. Los cambios relevantes de Android 17 no exigieron permisos o
codigo adicional. Se conserva el diseño adaptable para pantallas grandes.

## Verificacion

Regresiones focalizadas:

- 11 pruebas Compose Test v2, accesibilidad, ubicacion, MapLibre y CTAs.
- Resultado: 11 correctas, 0 fallos.

Matriz integral:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:connectedDebugAndroidTest :app:assembleDebug :app:assembleRelease :app:lintDebug --no-daemon --console=plain
```

- 36 unitarias, 0 fallos.
- 21 instrumentadas, 0 fallos y 2 omisiones externas esperadas.
- Debug y release correctos.
- Lint: `No issues found`.
- Empaquetado nativo sin advertencias despues de declarar los simbolos.

Runtime:

- APK instalado con `targetSdk=37` en `Small_Phone` Android 16.
- Imagen estable Google APIs Android 17/API 37 instalada y AVD independiente
  `APPbike_API_37` creado con perfil Pixel 5.
- Suite instrumentada repetida en Android 17: 21 casos, 0 fallos y 2 omisiones
  externas esperadas.
- Ubicacion inicial y confirmacion correctas.
- MapLibre 13.4.1 renderizo mapa, marcador y controles.
- Strava, Garmin y Wahoo conservaron cintas `PROXIMAMENTE`, estado en pausa y
  semantica no interactiva.
- `MainActivity` quedo en primer plano.
- Logcat sin `FATAL EXCEPTION`, ANR ni `UnsatisfiedLinkError`.

## Auditoria visual Android 17

1. Inicio y ubicacion: mapa, buscador, jerarquia, confirmacion y barra inferior
   se renderizaron sin recortes; estado saludable.
2. Perfil sin sesion: campos de correo y contrasena conservaron el doble
   contorno LED, espaciado consistente y CTA deshabilitado claro; estado
   saludable.
3. Sincronizacion deportiva: Strava, Garmin y Wahoo mostraron tarjetas
   independientes, estado `Vinculacion en pausa` y cinta diagonal
   `PROXIMAMENTE` completa; estado saludable y no interactivo.

Las capturas confirman composicion, contraste aparente, jerarquia y recortes.
La accesibilidad no se declara completa solo por imagen; la suite instrumentada
complementa esta revision con semantica, acciones y Unicode.

## Evidencia

- APK: `app/build/outputs/apk/debug/app-debug.apk`.
- Mapa API 37: `app/build/outputs/appbike-target37-map.png`.
- Captura final: `app/build/outputs/appbike-final-running.png`.
- Captura final Android 17:
  `app/build/outputs/appbike-final-api37.png`.
- Auditoria visual Android 17:
  `app/build/outputs/design-audit-api37/`.
- Lint: `app/build/reports/lint-results-debug.txt`.

## Pendientes externos

- Credenciales para cuenta, bicicletas, perfil privado y Chat autenticado.
- Segundo dispositivo/cuenta para Chat y notificaciones reales.
- FCM, geografia de servidor, portadas y listados propios multirregionales.
- OAuth deportivo permanece detenido por decision de producto.

## Siguiente paso

Continuar las pruebas privadas cuando existan credenciales; mantener la app
abierta en Android 17 sobre la seccion deportiva pausada.
