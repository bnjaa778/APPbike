# Sesión 2026-08-23 - Trayectos independientes en Mapa

## Objetivo

Añadir en Mapa una función para crear un trayecto hacia cualquier destino,
marcar el camino dentro de APPbike y mantenerla completamente separada de la
creación y los datos de Juntas.

## Cambios

- La acción inferior del mapa ahora separa visualmente `Trayecto` y `Junta`.
  Trayecto no requiere sesión; Junta conserva el requisito de cuenta y su
  formulario existente.
- El selector compartido de lugares admite copia específica para destinos. La
  selección se conserva en ubicaciones recientes sin cambiar la ubicación
  confirmada del mapa ni la ubicación propia de Marketplace.
- `CyclingRoutePreview` reemplaza el modelo acoplado a Juntas y conserva origen,
  destino, distancia, ETA, geometría y fuente.
- `RemoteConnections.loadCyclingRoute` consulta fuera del hilo principal el
  demo ciclista FOSSGIS/OSRM con geometría GeoJSON completa y sin enviar el
  Bearer de APPbike.
- MapLibre dibuja el camino por calles mediante `LineLayer`, marca el destino y
  ajusta la cámara a toda la geometría.
- Cada cálculo usa un `Job` cancelable y un identificador monotónico. Una
  respuesta tardía no reemplaza el trayecto vigente.
- Si el ruteador no responde, la UI conserva una línea directa claramente
  rotulada como respaldo y permite abrir navegación ciclista externa.
- `Cómo llegar` de una Junta reutiliza el planificador genérico, pero no mezcla
  sus estados con crear una Junta.

## Pruebas

- Se verificó manualmente el endpoint con un trayecto ciclista de Santiago:
  respuesta HTTP 200, 2.981 m, 766 s y geometría detallada.
- `:app:compileDebugKotlin`, `:app:compileDebugAndroidTestKotlin` y
  `:app:testDebugUnitTest`: correctos; 59 pruebas unitarias.
- Pruebas instrumentadas dirigidas en AVD API 35 y Samsung A23: 13 casos por
  dispositivo, 0 fallos y 0 omisiones.
- La nueva cobertura comprueba acciones separadas, copia del selector de destino
  y parser de distancia, duración y geometría de ruta.
- Matriz instrumentada completa en AVD API 35: 38 casos ejecutados, 0 fallos y
  2 omisiones opcionales de Chat.
- `lintDebug` final correcto con las 4 advertencias históricas y sin advertencias
  nuevas; `assembleDebug` y `git diff --check` correctos.
- APK final instalado en el Samsung A23. `MainActivity` inició con PID `25889`
  y no hubo excepciones fatales ni ANR en el log posterior.
- Una repetición posterior de las pruebas visuales físicas quedó invalidada por
  el bloqueo seguro del teléfono (`Dozing`, sin jerarquía Compose). Las 7
  pruebas no visuales avanzaron y la ejecución física dirigida anterior de
  13/13 permanece como la validación del cambio.

## Dependencia externa

El servidor FOSSGIS es un demo comunitario sin SLA, apto solo para desarrollo y
pruebas ligeras. Antes de un lanzamiento comercial se debe enrutar por backend o
desplegar OSRM propio, mantener la atribución de OpenStreetMap y no incluir
credenciales en el APK.

## Pendientes

- Desbloquear el Samsung y realizar la comprobación táctil con la ubicación real.
  El APK ya está instalado y el proceso fue iniciado detrás del bloqueo.

## Siguiente paso

Desbloquear el teléfono y recorrer `Mapa > Trayecto` con un destino real.
