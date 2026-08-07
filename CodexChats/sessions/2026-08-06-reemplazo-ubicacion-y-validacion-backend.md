# Sesión 2026-08-06 - Reemplazo de ubicación y validación del backend

## Objetivo

Continuar la meta de terminación funcional después de la fase visual, contrastar
los contratos Android con el backend desplegado y corregir un defecto público
reproducible sin depender de credenciales privadas.

## Hallazgo y corrección

En Marketplace, el selector se abría con la dirección actual completa. Tocar el
centro y escribir una ciudad insertaba la búsqueda dentro de esa dirección; en el
AVD se reprodujo `MountaiOsornon View`. `LocationSearchDialog` ahora limpia el
valor prellenado en el primer foco, antes de aceptar teclas. La ubicación anterior
sigue en el historial y no se modifica hasta elegir una sugerencia.

La regresión instrumentada inicia con `Mountain View`, pulsa el campo y escribe
`Osorno`; exige que el valor visible sea exactamente `Osorno`. La comprobación
ADB confirmó además la sugerencia `Osorno, Los Lagos, Chile`, cierre del teclado,
persistencia independiente de Marketplace y moneda CLP.

## Backend revalidado

- `location.search`: HTTP 200 con una sugerencia normalizada que incluye país,
  área administrativa, región y moneda.
- `location.resolve`: HTTP 200.
- `location.reverse`: HTTP 400 `unknown_region` para coordenadas válidas de
  Santiago. Android conservó correctamente su respaldo.
- `marketplace.list|get` y `junta.list|get`: HTTP 200.
- `junta.photo.get`: HTTP 200 con `content_base64` e imagen PNG.
- Las lecturas privadas de bicicletas, contenido propio, Chat y conexiones
  deportivas rechazaron la ausencia de token con HTTP 401.
- Las 21 publicaciones activas recibidas para LAS estaban a unos 83,1 km de
  Osorno; la UI las excluyó correctamente por el radio fijo de 40 km.

## Verificación

- 33 pruebas unitarias: 0 fallos.
- 14 pruebas instrumentadas: 0 fallos, 2 omisiones esperadas por dependencias
  externas de Chat/notificaciones.
- Ensamblado debug y release: correctos.
- Lint debug: 0 errores, 30 advertencias no visuales.
- Logcat del recorrido manual: sin `FATAL EXCEPTION` ni ANR de APPbike.

## Pendientes externos

Los recorridos privados completos requieren credenciales reales y datos de
prueba. El servidor aún debe resolver correctamente `location.reverse`, llenar
portadas en los listados de Marketplace, completar geografía/distancia en base de
datos e implementar FCM. La sincronización deportiva permanece detenida por
decisión de producto.
