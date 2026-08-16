# Informe de sesion - mapa satelital, GPS e identidad APPbike

Fecha: 2026-08-12

## Objetivo

Modernizar el mapa y la identidad visible de APPbike: conservar el mapa de
calles, sumar una alternativa satelital, mejorar la exactitud de la ubicacion,
retirar el rotulo grande de MapLibre y mostrar una marca propia desde el primer
instante del arranque.

## Implementacion

- `MapScreen.kt` incorpora `MapStyleMode` y un selector `Mapa`/`Satélite`.
- El modo de calles conserva Liberty de OpenFreeMap. El modo satelital usa los
  tiles oficiales `World_Imagery` y `World_Boundaries_and_Places` de Esri con
  atribuciones declaradas dentro del estilo MapLibre.
- Cambiar de modo reutiliza el mismo `MapView`, llama `setStyle` sobre el mapa
  activo y vuelve a cargar marcadores de usuario, juntas y punto seleccionado.
  Un identificador impide que un callback tardio reinstale un estilo anterior.
- `UiSettings.isLogoEnabled=false` elimina el texto de MapLibre observado en la
  captura, mientras `isAttributionEnabled=true` conserva el boton informativo.
- `DeviceLocationProvider` ya no devuelve el primer proveedor. Ejecuta en
  paralelo GPS, red y pasivo, compara `Location.accuracy`, respeta cancelacion y
  limita el fallback conocido a cinco minutos.
- La UI agrega `Precisar`; si falta permiso fino lo solicita y luego presenta la
  nueva posicion para confirmacion antes de persistirla.

## Identidad y arranque

- Se genero con ImageGen integrado un emblema original sin texto: una A
  geometrica construida como bicicleta, con grafito `#06100C`, verde electrico
  `#20D980` y acento azul `#48B8FF`.
- El PNG final vive en
  `app/src/main/res/drawable-nodpi/appbike_brand_icon.png` y se usa en launcher,
  cabecera y splash.
- `ic_appbike_round.xml` proporciona la variante redonda del launcher.
- `appbike_splash.xml` cubre Android 7-11; `values-v31/themes.xml` configura el
  splash nativo de Android 12+.
- Una transicion Compose mantiene la marca durante 900 ms en el arranque frio.
  Esto elimina el intervalo oscuro observado en el Samsung entre el splash
  nativo y la interfaz principal.
- La barra inferior reemplaza `DirectionsBike` por `PedalBike`.

## Evidencia

- `:app:testDebugUnitTest`: correcto.
- `MapStyleConfigurationTest`: 2 pruebas, 0 fallos y 0 errores.
- `LocationQualityTest`: 3 pruebas, 0 fallos y 0 errores; cubre precision frente
  a actualidad, desempate temporal y ausencia de `accuracy`.
- `:app:lintDebug`: `No issues found`.
- `:app:assembleDebug`: correcto; APK debug de 64.630.951 bytes.
- El APK final se instalo en el Samsung `SM-A235M` (`R58T9039QBN`) mediante
  `adb install -r`: `Success`.
- El dispositivo mostro el emblema, `PedalBike`, `Precisar`, el selector y solo
  el boton de atribucion. `Mapa` y `Satélite` se renderizaron y se pudo volver a
  calles sobre el mismo `MapView`.
- `Precisar` propuso `Mirador de Puerto Varas` con coordenadas
  `-41.33296, -72.96802`. La confirmacion quedo sin aceptar y el relanzamiento
  comprobo que `Fundo Libertad` continuaba persistida.
- El arranque frio mostro el emblema centrado antes de la interfaz.
- Evidencias: `app/build/outputs/appbike-final-default.png`,
  `appbike-final-satellite.png`, `appbike-map-final-return.png`,
  `appbike-location-result.png` y `appbike-launch-brand.png`.
- Logcat del proceso: 0 `FATAL EXCEPTION` y 0 ANR.
- La fuente World Imagery respondio HTTP 200 con `image/jpeg` y su metadata
  oficial declara cobertura satelital/aerea y los creditos requeridos.

## Cierre

La validacion fisica quedo completa al reaparecer el Samsung. No fue necesario
alterar ni eliminar datos del AVD. El APK final permanece en
`app/build/outputs/apk/debug/app-debug.apk` y esta instalado en el dispositivo.
