# APPbike — plan técnico de rediseño UX/UI

Revisión: 2026-08-23. Implementado y verificado localmente.

## Resultado buscado

APPbike debe permitir que un rider abra la app, descubra contenido útil, cambie
de contexto sin esperar ni perder su posición, encuentre una Junta en el Mapa y
continúe hacia ella con una orientación clara. El rediseño conserva Android
nativo, Jetpack Compose, MapLibre, la identidad grafito/verde y todos los
contratos de privacidad y backend existentes.

La línea base visual y sus hallazgos están en
`CodexChats/audits/2026-08-21-redesign-baseline/REPORT.md`.

## Hallazgos de arquitectura

- `MainActivity.kt` ya usa `HorizontalPager`, pero solo para Inicio, Bicicletas,
  Marketplace y Chat. Mapa y Cuenta desmontan el pager al abrirse.
- Los estados de scroll, filtros, búsquedas y cargas viven en `remember` dentro
  de cada pantalla. Como Pager puede desechar páginas, volver a una pestaña puede
  reconstruirla y repetir sus `LaunchedEffect` de red.
- Home, Marketplace, Map, Chat y Profile disparan entre sí hasta siete cargas
  remotas si se componen simultáneamente sin una puerta de activación.
- `MapView` usa su superficie OpenGL predeterminada. Para convivir con
  transiciones y recorte de Pager debe usar el modo `TextureView`, sin recrearse
  al cambiar estilo.
- Las listas ya son `LazyColumn`/`LazyVerticalGrid` donde importa, y las imágenes
  tienen caché LRU de bytes y downsampling; falta cachear bitmaps decodificados.
- El backend no ofrece ruteo ciclista, participantes, desnivel ni métricas
  deportivas. La UI no inventará esos datos.

## Arquitectura objetivo

### Navegación raíz

Orden de cinco destinos principales:

1. `HOME` — Inicio/Feed.
2. `MARKETPLACE` — compra, venta y servicios.
3. `ROUTES` — Mapa unificado con Juntas.
4. `CHAT` — conversaciones sociales y de Marketplace.
5. `ACCOUNT` — Perfil deportivo y contenido propio.

`BIKES` pasa a ser el Garage secundario, accesible desde Perfil y conservando
todo su flujo actual. `SYNC` continúa como alias de Perfil y
`CREATE_PUBLICATION` como compatibilidad de Marketplace.

La raíz mantiene el pager compuesto incluso al superponer un flujo secundario.
Las cinco páginas se conservan en composición para retener scroll y estado, pero
cada pantalla recibe `isActive` y solo realiza su primera carga pesada al
activarse. Volver a una pestaña no refresca por sí solo.

Actualizar datos queda separado de navegar:

- primera activación;
- pull-to-refresh o acción explícita;
- cambio real de ubicación/cuenta;
- evento de creación, edición o eliminación;
- polling de una conversación solo mientras Chat está visible.

### Mapa dentro del pager

- Construir un único `MapView` con `MapLibreMapOptions.textureMode(true)`.
- Mantenerlo montado después de su primera activación y reducir su trabajo
  cuando esté fuera de pantalla.
- Conservar pan libre dentro del mapa. El swipe global se mantiene en las demás
  pestañas; en Mapa se prioriza el gesto cartográfico y se permite salir por
  navegación inferior o gesto controlado desde el borde.
- Mapa y Satélite siguen reutilizando la misma instancia y callbacks protegidos
  por identificador.
- Un fallo de estilo muestra fondo oscuro, explicación y `Reintentar`; nunca una
  superficie clara vacía.

### Estado por pantalla

- Home: contenido, error, última carga y `LazyListState` persistentes.
- Marketplace: ubicación, query, resultados, filtros, detalle y grid state
  persistentes.
- Mapa: ubicación confirmada, cámara, estilo, búsqueda, Junta seleccionada y
  modo de ruta persistentes.
- Chat: categoría, conversación, borrador, scroll y caché por `userId`; polling
  suspendido cuando la pestaña no está activa.
- Perfil: scroll, filtros y contenido propio persistentes por `userId`.

### Ruta a una Junta

Mientras no exista un proveedor de ruteo desplegado:

- calcular distancia geodésica entre ubicación confirmada y punto de Junta;
- estimar tiempo a una velocidad ciclista documentada y etiquetarlo como
  estimación directa;
- dibujar una línea de previsualización en MapLibre, diferenciada de una ruta
  calle-a-calle;
- ofrecer apertura de navegación ciclista externa mediante un Intent seguro;
- mantener un modelo `MeetupRoutePreview` separado para reemplazar luego la
  geometría directa por una ruta real, con desnivel, superficie y ciclovías.

No se presentará la línea directa como una ruta calculada ni se inventarán
instrucciones giro a giro.

## Sistema de diseño

Fuentes únicas:

- color: `ui/theme/Color.kt`;
- tipografía y escala: `ui/theme/Type.kt`;
- shapes y esquema Material: `ui/theme/Theme.kt`;
- espaciado, tamaños, movimiento y componentes: `CommonComponents.kt`.

Se consolidarán:

- espaciado de 4 dp;
- radios 10/16/22/28/pill;
- alturas táctiles mínimas de 48 dp;
- movimiento rápido de 150, 220 y 300 ms;
- tarjetas, pills, métricas, botones de icono, banners y skeletons;
- verde como acción/estado, azul para mapa/rendimiento y naranja para comercio.

Los iconos de navegación usarán una sola familia de trazo reconocido y labels
visibles únicamente en el destino activo; las descripciones internas completas
se mantienen siempre.

## Fases y aceptación

### Fase 1 — sistema de diseño

- Tokens de movimiento/tamaño/elevación y componentes reutilizables.
- Contraste, 48 dp y reflow sin regresiones.

### Fase 2 — navegación inferior

- Cinco destinos en el orden objetivo, Mapa con mayor peso visual y label solo
  en el activo.
- Mapa deja de ser destino secundario seleccionado como Inicio.
- Garage accesible desde Perfil.

### Fase 3 — swipe y persistencia

- Tap y pager sincronizados con transición ligada al dedo.
- Páginas principales conservan estado; cambiar de pestaña no dispara red.
- Pager permanece montado durante Garage y otros flujos secundarios.

### Fase 4 — Feed y Perfil

- Hero más compacto y módulos con jerarquías diferentes usando datos reales.
- Perfil prioriza identidad rider, Garage y actividad antes de integraciones
  pausadas.

### Fase 5/6 — Mapa y Juntas

- Mapa principal único, controles mínimos y estados de carga/error coherentes.
- Marcador abre bottom sheet sin abandonar el mapa.
- La sheet muestra solo datos disponibles; no inventa participantes, dificultad
  ni tipo de ciclismo.

### Fase 7/8 — ruta, distancia y tiempo

- Previsualización directa y ETA documentada.
- Modo de ruta cancelable y CTA hacia navegación ciclista externa.
- Arquitectura lista para geometría de proveedor propio.

### Fase 9/10 — microinteracciones y rendimiento

- Interacciones comunes entre 150 y 300 ms.
- Skeletons discretos, caché de bitmaps decodificados y cargas activadas por
  visibilidad.
- Revisar recomposiciones, FPS del mapa fuera de pantalla y llamadas duplicadas.

## Verificación obligatoria

- Pruebas unitarias de orden de destinos, distancia/ETA y estado de navegación.
- Pruebas Compose de labels/selección, acceso a Garage y sheet de Junta.
- `testDebugUnitTest`, Lint y `assembleDebug` con JDK 17.
- Capturas antes/después en el mismo AVD y viewport.
- Recorrido con fuente 100 %, 160 % y 200 %, orientación vertical/horizontal,
  TalkBack y objetivos táctiles.
- Regresión de MapLibre: estilo tardío, cambio Mapa/Satélite, fuente de Juntas y
  montaje único del `MapView`.

## Dependencias y límites externos

- Un ruteo ciclista real dentro de APPbike requiere backend/proveedor con
  geometría, ETA y términos de uso aptos para producción.
- Kilómetros, tiempo, desnivel, seguidores y logros requieren contratos de
  actividad aún no desplegados; la interfaz usa estados honestos hasta entonces.
- Unirse a una Junta no tiene acción remota propia. La primera versión coordina
  participación mediante el chat existente sin simular un RSVP persistido.

## Delta de rediseño visual — 2026-08-26

- Inicio ya no consulta ni muestra Marketplace: presenta un feed comunitario de
  juntas y publicaciones personales, con un hero de cuatro fotografías de
  ciclismo que rota cada 4 segundos mediante crossfade de 300 ms.
- El acceso superior a Perfil fue retirado. La navegación inferior conserva el
  único acceso principal a Perfil y la cabecera queda reservada para marca y
  clima.
- Perfil prioriza avatar, nombre, bio, resumen y contenido. La foto y bio se
  persisten localmente por UUID. Las publicaciones sociales locales admiten
  foto o video y se incorporan al feed sin mezclarse con ventas de Marketplace.
- Las acciones `Trayecto` y `Junta` del mapa usan un control flotante compacto y
  semitransparente; el planificador sigue funcionando dentro de APPbike y solo
  abre navegación externa tras una acción explícita.
- El splash mantiene el logo centrado a tamaño constante y solo aplica fade-out.
