# Design QA - ubicación en Mapas (archivo CodexChats)

## Evidencia

- Fuente visual:
  `C:/Users/Xinerdev/AppData/Local/Temp/codex-clipboard-40a7a9ca-1521-4c99-ac8e-4edfc0f401dc.png`.
- Implementación final:
  `C:/Users/Xinerdev/StudioProjects/APPbike/build/appbike-second-launch.png`.
- Comparación completa combinada:
  `C:/Users/Xinerdev/StudioProjects/APPbike/build/design-qa-comparison.png`.
- Comparación enfocada de cabecera, búsqueda y ubicación:
  `C:/Users/Xinerdev/StudioProjects/APPbike/build/design-qa-top-comparison.png`.
- Viewport de implementación: 1080 x 2424 px, emulador Android
  `sdk_gphone16k_x86_64`.
- Estado comparado: pantalla Mapas con ubicación ya confirmada y persistida,
  sin diálogo abierto.

La referencia incluye un marco físico de teléfono y la implementación es una
captura directa de Android. La comparación completa conserva esa diferencia de
presentación; la comparación enfocada normaliza la región superior del producto.

## Superficies de fidelidad

- Tipografía: se conserva la jerarquía APPBIKE, subtítulo, búsqueda y etiqueta
  de ubicación. La implementación usa la tipografía Material del proyecto; el
  peso de `Ubicación actual` mejora la lectura respecto a la referencia.
- Espaciado y composición: búsqueda de ancho completo, ubicación inmediatamente
  debajo, mapa como superficie principal, botón `+` centrado y navegación
  persistente. No hay controles primarios recortados ni desplazamiento horizontal.
- Colores: verde APPbike en borde/acento, marcador y acción flotante; superficies
  cálidas y mapa Liberty conservados.
- Imágenes y recursos: mapa MapLibre/OpenFreeMap real, iconos de Material y pin
  proveniente del recurso de MapLibre tintado con el verde de APPbike. No se usan
  placeholders visuales.
- Copia: `Buscar junta` y `Ubicación actual` coinciden con el objetivo. Se usa la
  ubicación seleccionada real en vez de texto fijo.

## Interacciones verificadas

- Solicitud de permiso en el primer ingreso.
- Lectura de ubicación simulada en Puerto Varas.
- Diálogo `¿Esta ubicación está bien?` con ambos botones solicitados.
- Apertura de corrección manual.
- Búsqueda explícita de `Plaza de Armas Puerto Montt`.
- Selección y persistencia de latitud, longitud y etiqueta.
- Segundo ingreso sin repetir permiso ni confirmación.
- Desplazamiento del mapa sin alterar `selected_location`.
- Pin conservado en su coordenada geográfica después de mover la cámara.
- Logcat revisado sin `FATAL EXCEPTION`.

## Historial de comparación

1. Primera implementación: el recurso de marcador conservaba el rojo original
   de MapLibre, una diferencia P1 frente al acento verde de la referencia y del
   producto.
2. Corrección: el recurso oficial de MapLibre se renderizó a un bitmap nuevo con
   tinte `BikeGreen`.
3. Evidencia posterior: `appbike-second-launch.png` y
   `design-qa-comparison.png` muestran el pin verde, la ubicación bajo la
   búsqueda y el mapa centrado en las coordenadas guardadas.

## Hallazgos finales

No quedan diferencias P0, P1 o P2 accionables frente al objetivo visual.

Como P3 aceptable, la etiqueta puede ser más larga que `Puerto Varas` cuando el
usuario elige un lugar específico; el componente aplica una sola línea con
elipsis en pantallas estrechas. El mensaje `Método no permitido` pertenece al
backend todavía pendiente y también está presente en la referencia visual; no
es una diferencia de implementación de diseño.

final result: passed

---

# Design QA - detalle de publicación de Marketplace

## Evidencia

- Fuente visual objetivo:
  `C:/Users/Xinerdev/AppData/Local/Temp/codex-clipboard-0e5430b4-5ee8-4460-b00f-c568a23e8da4.png`.
- Estado anterior compacto:
  `C:/Users/Xinerdev/AppData/Local/Temp/codex-clipboard-9b6c7b3e-11de-436a-97e0-c6854bda82f0.png`.
- Implementación final:
  `C:/Users/Xinerdev/StudioProjects/APPbike/build/marketplace-detail-final.png`.
- Comparación conjunta:
  `C:/Users/Xinerdev/StudioProjects/APPbike/build/marketplace-detail-comparison.png`.
- Fuente: 197 x 350 px. Implementación: 1080 x 2424 px en emulador Android
  `sdk_gphone16k_x86_64`.
- Normalización: ambas capturas se escalaron proporcionalmente a 1200 px de
  alto y se colocaron en un mismo lienzo. No se hizo comparación pixel a pixel
  porque la fuente es una referencia visual de baja resolución, no una maqueta
  de APPbike con las mismas dimensiones ni el mismo contenido.
- Estado: publicación remota abierta, cuenta compradora iniciada, tema claro y
  acción `Contactar al vendedor` disponible.

La comparación completa permite leer cabecera, imagen, jerarquía de precio y
título, descripción, ubicación y acción inferior. No fue necesario un recorte
adicional porque esos elementos son legibles en el conjunto normalizado.

## Historial de comparación

1. Estado inicial: el detalle aparecía dentro de una tarjeta modal pequeña y
   desplazable. Era una diferencia P1 frente a la referencia, porque reducía la
   fotografía, la información y la acción principal a una fracción de la
   pantalla.
2. Corrección: se reemplazó por un `Dialog` sin ancho predeterminado, de ventana
   completa, con retorno superior, imagen protagonista, información desplazable
   y una acción persistente al pie.
3. Evidencia posterior: `marketplace-detail-final.png` y la comparación conjunta
   muestran que el detalle ocupa toda la pantalla y conserva visibles la imagen,
   el precio, el título, los datos y el contacto.

## Superficies de fidelidad

- Tipografía: Material/Roboto conserva una jerarquía equivalente a la fuente:
  precio destacado, título fuerte, etiquetas secundarias y cuerpo más ligero.
  No hay cortes ni texto superpuesto.
- Espaciado y composición: la imagen ocupa la región superior; el contenido usa
  márgenes consistentes de 20 dp; la acción inferior queda fija y accesible. La
  vista no depende de la barra inferior principal mientras está abierta.
- Colores: se adapta la estructura de la referencia al sistema visual existente
  de APPbike, usando verde para precio, estados, ubicación y CTA sobre superficies
  cálidas de alto contraste.
- Imágenes: se muestra la fotografía real entregada por el backend con recorte
  `ContentScale.Crop`; no se creó ni sustituyó por un recurso artificial. El
  contenido concreto de la foto depende de cada publicación.
- Copia: se mantienen precio y moneda, estado del producto, estado de publicación,
  descripción, ubicación, fecha legible y contacto. `Make Offer` no se replica
  porque APPbike todavía no tiene contrato de ofertas; inventarlo habría creado
  una acción sin comportamiento real.

## Interacciones verificadas

- Apertura desde una tarjeta remota de Marketplace.
- Pantalla completa sin el modal compacto anterior.
- Cierre mediante la flecha superior.
- Acción de contacto visible para una cuenta que no es propietaria.
- Fecha del backend convertida a formato legible.
- Recarga por arrastre en Marketplace conservando la publicación.
- Logcat sin `FATAL EXCEPTION`.

## Hallazgos finales

No quedan diferencias P0, P1 o P2 accionables respecto al objetivo de adoptar
la estructura de detalle de Facebook dentro del lenguaje visual de APPbike.

Como P3, una futura API de ofertas podría justificar un segundo CTA equivalente
a `Make Offer`; se deja fuera hasta que exista un flujo real de negocio.

final result: passed
