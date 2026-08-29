# Sesión 2026-08-28 — Pulido de navegación y perfil rider

## Objetivo

Atender los ocho ajustes solicitados: renovar las fotos del hero, limpiar el
login, separar el gesto de mapa del pager, reubicar las acciones del mapa,
identificar las notificaciones con la marca, mostrar actividad directamente en
Perfil, hacer más atractiva la vinculación deportiva y acercar los fondos a la
referencia cálida tipo Strava.

## Cambios realizados

- Se instalaron las cuatro fotos entregadas en `drawable-nodpi` como JPG con sus
  dimensiones originales y se retiraron los cuatro recursos PNG antiguos del
  paquete. Los originales quedaron como respaldo recuperable fuera del proyecto.
- Se eliminó el texto de persistencia local bajo el formulario de acceso.
- Se añadió una región de gesto para el pager: el lienzo MapLibre informa que el
  swipe está desactivado durante el paneo, mientras búsqueda, capas y
  Trayecto/Junta lo reactivan.
- Trayecto y Junta ahora viven en la esquina superior de controles con fondo
  semitransparente.
- Las notificaciones usan una variante monocroma transparente derivada exactamente
  de `appbike_brand_icon` para el icono pequeño y el logo original como icono
  grande; el banner Compose usa `AppBrandLogo`.
- `AccountScreen` carga bicicletas por `userId` cuando Perfil se activa y
  `ProfileContentSection` muestra bicis, posts y rutas/juntas sin obligar a abrir
  Mi garaje. Las respuestas tardías se protegen con el ID de usuario esperado.
- La sección deportiva usa acentos naranja/azul/verde; solo Strava muestra la
  cinta `PRÓXIMAMENTE`, sin reactivar OAuth.
- El fondo común incorpora un brillo naranja radial de baja intensidad.

## Verificación

- `:app:compileDebugKotlin` correcto.
- `:app:testDebugUnitTest` correcto.
- `:app:assembleDebug` correcto.
- `:app:assembleDebugAndroidTest` correcto.
- `:app:lintDebug` correcto. Persisten únicamente avisos informativos de
  versiones, forma del launcher y sugerencias KTX ya conocidas.
- `:app:connectedDebugAndroidTest` correcto: 41 pruebas en `APPbike_API_35`
  (Android 15), 0 fallos y 2 omitidas por precondiciones.
- La inspección en teléfono físico queda pendiente; el AVD sí está validado.

## Pendientes

- Confirmar en AVD/teléfono el paneo de MapLibre, el swipe iniciado sobre los
  controles, el render del nuevo hero y el icono de notificación.
