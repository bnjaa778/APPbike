# Sesión 2026-08-21 - Rediseño UX/UI deportivo integral

## Objetivo

Transformar progresivamente APPbike en una experiencia móvil deportiva, social,
visual y fluida, con navegación principal persistente, un único acceso a Mapa,
Juntas integradas, feed con variedad editorial y una base de diseño reutilizable.

## Estado inicial

- Proyecto Android nativo con Jetpack Compose y MapLibre.
- La raíz ya utiliza `HorizontalPager` para Inicio, Bicicletas, Marketplace y
  Chat, pero Mapa y Cuenta siguen siendo destinos secundarios.
- El árbol de trabajo contiene cambios locales previos en navegación, Inicio,
  componentes, tema y pruebas; forman parte del estado a preservar.
- La sesión, autenticación, aislamiento por usuario, contratos de fotos,
  Marketplace, Juntas y Chat se consideran invariantes funcionales.

## Resultado entregado

- La navegación principal quedó en el orden Inicio, Marketplace, Mapa, Chat y
  Perfil. La barra y el `HorizontalPager` están sincronizados; las cinco páginas
  permanecen montadas y difieren su primera carga hasta activarse.
- Bicicletas se conserva sin pérdida funcional como `Mi garaje`, un flujo
  secundario abierto desde Perfil sobre el pager persistente.
- Se consolidaron tokens de movimiento, tamaño, elevación y color, además de
  tarjetas métricas y skeletons compartidos. Se retiraron los cinco vectores de
  navegación que ya no tenían consumidores.
- Inicio usa un hero más compacto, un carrusel de exploración y cards distintas
  para Juntas y Marketplace. Marketplace conserva ubicación, filtros, búsqueda
  y grilla entre pestañas. Chat diferencia Junta/Compra y permite volver al mapa
  desde conversaciones sociales relacionadas.
- Perfil prioriza identidad, `Mi garaje`, métricas honestas y actividad antes de
  las integraciones deportivas pausadas. Los datos que el backend no entrega se
  muestran como ausencia explícita, nunca como cifras simuladas.
- Mapa es el destino central, usa TextureView, se crea al primer ingreso y baja
  a 4 FPS fuera de pantalla. Una Junta abre un bottom sheet; con ubicación
  confirmada ofrece distancia directa, ETA a 15 km/h, línea estimada y apertura
  de navegación ciclista externa.
- `RemoteImageLoader` agregó una caché LRU de bitmaps decodificados de 24 MB,
  además de la caché de bytes existente, para evitar decodificaciones repetidas.

## Decisiones de producto

- No se inventaron participantes, dificultad, desnivel, estadísticas deportivas
  ni un RSVP inexistente. `Quiero participar` reutiliza Chat.
- La línea del mapa se etiqueta como estimación directa; no se presenta como una
  ruta calle a calle. Un futuro proveedor puede reemplazar la geometría a través
  del modelo separado `MeetupRoutePreview`.
- La navegación externa usa una URL universal de Google Maps con
  `travelmode=bicycling`, origen y destino, sin credenciales en el APK.
- Los contratos de sesión, propiedad, fotos, moneda, ubicación y aislamiento por
  usuario se conservaron sin cambios remotos.

## Evidencia y pruebas

- Auditoría inicial: `CodexChats/audits/2026-08-21-redesign-baseline/`.
- Capturas posteriores del acceso a 100 % y 200 % de fuente:
  `CodexChats/audits/2026-08-23-redesign-after/`.
- `:app:testDebugUnitTest`: 59 pruebas, 0 fallos.
- `:app:connectedDebugAndroidTest`: 33 pruebas, 0 fallos y 2 omisiones esperadas
  por credenciales de Chat y permiso de notificaciones del AVD.
- `:app:assembleDebug` y `:app:assembleDebugAndroidTest`: correctos.
- `:app:lintDebug`: correcto, sin errores; quedan cuatro avisos informativos de
  versiones/ecosistema y forma del launcher, ninguno introducido por el rediseño.
- `git diff --check`: correcto.

## Límites externos

- El backend aún no ofrece ruteo ciclista, actividad agregada ni RSVP; la UI
  mantiene estados honestos hasta que existan esos contratos.
- La inspección posterior de pantallas autenticadas requiere una cuenta de
  prueba válida. La sesión persistida del AVD había expirado, por lo que el
  recorrido visual posterior se limitó al acceso; los flujos internos quedaron
  cubiertos por pruebas unitarias e instrumentadas.

## Siguiente paso

Probar la build en un teléfono físico con una cuenta de prueba y, cuando exista
un contrato de ruteo propio, sustituir la línea directa por geometría ciclista
real sin cambiar la arquitectura del bottom sheet.
