# Sesión 2026-08-06 - Protección de fuentes de fotografías

## Objetivo

Cerrar el riesgo de que rutas privadas del backend se convirtieran en URLs
públicas y demostrarlo tanto en el helper como en los parsers Android reales.

## Diagnóstico

- Una bicicleta sin `photo_id` pasaba `photo_path` a `publicPhotoUrl`, que
  anteponía `https://api.zizzio.cl/` incluso a
  `BikesPhotos/PersonalBikesPhotos/...`.
- Una foto comunitaria descrita como objeto con `path` seguía la misma ruta.
- Strings con URL HTTPS se aceptaban desde cualquier host y también se admitía
  HTTP, abriendo una solicitud desde el dispositivo fuera del dominio previsto.

## Cambios realizados

- Se reemplazó el fallback por `safePublicPhotoUrl`.
- La función rechaza rutas internas conocidas, servidores `/srv`/`/var`, rutas
  Windows, barras invertidas, traversal, esquemas locales, cleartext y dominios
  externos.
- Se permiten rutas relativas públicas bajo el API y HTTPS en `zizzio.cl` o sus
  subdominios.
- `photo_id` conserva prioridad y nunca se transforma en URL directa.
- Strings y objetos del array `photos` aplican la misma validación.

## Verificación

- 33 pruebas unitarias, 0 fallos; dos pruebas nuevas cubren fuentes válidas e
  inválidas.
- 10 pruebas instrumentadas, 0 fallos y 1 omisión esperada sin credenciales de
  Chat.
- `bikeFromJson` dejó `imageUri` vacío ante
  `BikesPhotos/PersonalBikesPhotos/42.jpg`.
- `marketplaceFromJson` omitió un objeto cuya ruta estaba bajo
  `/srv/internal-auth/public/AppBikeInternal`.
- `:app:assembleDebug` y `:app:lintDebug` finalizaron correctamente.

## Pendientes

- Backend debe poblar `photo_id` de portada y eliminar rutas internas de todas
  las respuestas públicas.
- Si en el futuro se adopta CDN, su host HTTPS debe documentarse y autorizarse de
  forma explícita; no aceptar hosts arbitrarios.

## Siguiente paso

Continuar la auditoría local de estados visuales residuales y mensajes que aún
describan funciones conectadas como placeholders.
