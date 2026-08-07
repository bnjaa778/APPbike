# Auditoría combinada — flujo público APPbike

Fecha: 2026-08-06
Dispositivo: `Small_Phone` AVD, 720 × 1280
Objetivo: comprobar que el recorrido público tenga identidad consistente,
acciones claras y legibilidad suficiente antes de probar contenido privado.

## Veredicto

El sistema visual grafito/verde LED es consistente, reconocible y mantiene
objetivos táctiles amplios. La auditoría detectó problemas concretos de cabecera,
acceso desde Chat, contraste y reflow con fuente al 200 %. Todos quedaron
corregidos y recapturados. La evidencia demuestra buen comportamiento en el AVD
observado, no cumplimiento WCAG completo.

## Pasos auditados

1. **Mapas — saludable.** El mapa domina la jerarquía, ubicación y búsqueda son
   claras, y el botón crear tiene un objetivo amplio.

   ![Mapas](01-mapas.png)

2. **Bicicletas sin sesión — saludable.** El mensaje requerido es protagonista,
   explica el beneficio y ofrece acceso directo a Cuenta.

   ![Bicicletas sin sesión](02-bicicletas-sin-sesion.png)

3. **Marketplace antes de corregir — riesgo visual.** El contenido reservaba el
   espacio de cabecera, pero el fondo hijo podía pintarse por encima de marca y
   perfil; el placeholder también era demasiado tenue.

   ![Marketplace antes](03-marketplace.png)

4. **Chat antes de corregir — usable con fricción.** El bloqueo era claro, pero
   el usuario no tenía acción para resolverlo desde la misma pantalla.

   ![Chat antes](04-chat-sin-sesion.png)

5. **Cuenta — saludable.** Título, tabs promocionales y tarjeta de acceso tienen
   jerarquía clara; la pantalla desplaza el resto del formulario.

   ![Cuenta](05-cuenta-login.png)

6. **Foco LED — saludable.** El estado de foco es inequívoco sin depender solo
   de un cambio de texto. La captura aceptada excluye el panel de escritura del
   sistema que apareció en el primer intento.

   ![Foco LED](06-cuenta-led-focus.png)

7. **Deportes pausados — saludable.** Strava, Garmin y Wahoo se reconocen como
   futuras integraciones; cinta, color y texto comunican que no son acciones
   disponibles.

   ![Deportes pausados](07-sports-paused.png)

8. **Marketplace corregido — saludable.** Marca y perfil quedan por encima del
   fondo; el placeholder usa `AppTextSecondary` y alcanza 9,16:1 sobre la
   superficie elevada.

   ![Marketplace corregido](08-marketplace-corregido.png)

9. **Chat corregido — saludable.** `Iniciar sesión` aparece como resolución
   inmediata dentro del estado vacío.

   ![Chat con CTA](09-chat-cta.png)

10. **Resolución del CTA — saludable.** Tocar el acceso de Chat abre Cuenta y
    deja visible el primer campo de autenticación.

    ![Cuenta abierta desde Chat](10-chat-cta-cuenta.png)

11. **Cuenta con fuente 130 % — saludable.** Título, tabs, formulario y
    navegación aumentan sin perder acciones.

    ![Cuenta a 130 %](11-cuenta-font-130.png)

12. **Deportes con fuente 130 % — saludable.** Las tres integraciones mantienen
    marca, explicación, estado y cinta reconocibles.

    ![Deportes a 130 %](12-sports-font-130.png)

13. **Cuenta a 200 % antes del reflow — riesgo alto.** Las referencias se
    forzaban en una fila, la cabecera comprimía la identidad y la barra inferior
    truncaba destinos.

    ![Cuenta a 200 % antes](13-cuenta-font-200.png)

14. **Deportes a 200 % antes del reflow — riesgo alto.** Había palabras partidas
    y cintas superpuestas sobre el contenido de las tarjetas.

    ![Deportes a 200 % antes](14-sports-font-200.png)

15. **Primera corrección de Cuenta — mejora incompleta.** La navegación quedó
    legible, pero las referencias todavía necesitaban una columna completa.

    ![Cuenta a 200 % intermedia](15-cuenta-font-200-corregida.png)

16. **Cuenta a 200 % corregida — saludable.** Las referencias se apilan a ancho
    completo, el perfil conserva 48 dp y la navegación usa copia compacta.

    ![Cuenta a 200 % corregida](16-cuenta-font-200-reflow.png)

17. **Recorrido del bloque deportivo — evidencia de desplazamiento.** La pantalla
    permite alcanzar tarjetas inferiores; esta captura motivó aislar una tarjeta
    completa para revisar sus límites.

    ![Recorrido deportes a 200 %](17-sports-font-200-reflow.png)

18. **Tarjeta deportiva a 200 % — saludable.** Wahoo usa reflow vertical, el
    estado permanece completo y la cinta diagonal no invade el texto.

    ![Tarjeta deportiva a 200 %](18-sports-font-200-card.png)

19. **Mapas a 200 % — saludable.** Cabecera, búsqueda, ubicación, mapa, creación
    y cuatro destinos siguen disponibles; el dato largo se elide de forma segura.

    ![Mapas a 200 %](19-mapas-font-200.png)

20. **Marketplace a 200 % — saludable.** Búsqueda, ubicación/moneda, estado
    vacío, creación y navegación conservan jerarquía.

    ![Marketplace a 200 %](20-marketplace-font-200.png)

21. **Chat a 200 % antes de corregir — riesgo alto.** La explicación ocupaba la
    altura disponible y el CTA quedaba fuera de la ventana.

    ![Chat a 200 % antes](21-chat-font-200.png)

22. **Bicicletas a 200 % antes de corregir — riesgo medio.** El contenido ya era
    desplazable, pero el CTA aparecía después de una explicación inicialmente
    recortada.

    ![Bicicletas a 200 % antes](22-bikes-font-200.png)

23. **Chat a 200 % corregido — saludable.** El CTA se prioriza tras el título y
    la tarjeta completa puede desplazarse para leer la explicación.

    ![Chat a 200 % corregido](23-chat-font-200-corregido.png)

24. **Bicicletas a 200 % corregido — saludable.** Se conserva literalmente el
    mensaje requerido y `Iniciar sesión` queda visible sin reducir la fuente.

    ![Bicicletas a 200 % corregido](24-bikes-font-200-corregido.png)

## Fortalezas

- Cabecera y barra inferior mantienen identidad y destinos previsibles.
- Las tarjetas vacías explican qué falta y por qué importa.
- Verde brillante sobre grafito ofrece contraste alto: texto principal 15,72:1,
  acento verde 11,29:1 y texto secundario 9,16:1 sobre la superficie elevada.
- Los controles observados alcanzan aproximadamente 48 dp o más.
- El reflow conserva la escala solicitada por Android: compacta copia y cambia
  disposición, sin reducir títulos ni explicaciones.
- Las acciones de estados vacíos quedan antes del texto secundario a escala
  grande y el contenido largo conserva desplazamiento.

## Riesgos y límites de evidencia

- Las capturas no prueban orden de lectura o pronunciación con TalkBack.
- Falta repetir en orientación horizontal y otros anchos físicos.
- Sin credenciales no fue posible auditar bicicletas, contenido propio, chats ni
  Marketplace con datos privados reales.
- No se afirma cumplimiento WCAG completo a partir de capturas.

## Recomendaciones siguientes

1. Recorrer formularios y detalles autenticados con datos de prueba.
2. Ejecutar TalkBack en navegación, búsqueda, selectores y diálogos.
3. Repetir en orientación horizontal y un dispositivo de ancho alternativo.
