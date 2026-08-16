# Sesión 2026-08-16 - Acceso MTB sin sesión

## Objetivo

Separar el acceso inicial de la pantalla de Perfil. Una cuenta guardada y
aceptada debe entrar a Novedades; solo la ausencia de sesión o el rechazo del
token debe mostrar login y creación de cuenta sobre una identidad MTB.

## Decisiones

- `UnauthenticatedAccessScreen` se compone en la raíz y retorna antes de crear
  cabecera, contenido principal o barra inferior.
- La validación `user.get` no cambia: un rechazo inequívoco borra identidad y
  token; timeouts/5xx conservan la sesión.
- Una cuenta válida sin `nombre_de_usuario` sigue entrando a Cuenta para
  completar la identidad obligatoria; no se trata como token inválido.
- El fondo es una fotografía vertical original, sin texto ni marcas, generada
  con la herramienta integrada de imágenes y guardada como
  `app/src/main/res/drawable-nodpi/auth_mtb_background.png`.
- El backend comprobado no anuncia registro. `Crear cuenta` permanece visible
  pero informa la dependencia; Android no inventa una acción ni envía secretos.

## Archivos principales

- `app/src/main/java/com/example/appbike/Account.kt`
- `app/src/main/java/com/example/appbike/MainActivity.kt`
- `app/src/main/res/drawable-nodpi/auth_mtb_background.png`
- `app/src/androidTest/java/com/example/appbike/AuthenticationGatewayInstrumentedTest.kt`
- `AGENTS.md`, `docs/PROJECT_REPORT.md`
- `CodexChats/CURRENT_STATE.md`, `CodexChats/CHANGELOG.md`

## Generación del fondo

- Modo: herramienta integrada de generación de imágenes.
- Prompt final resumido: fotografía realista vertical de un ciclista MTB en un
  sendero andino al amanecer, montañas y niebla, tonos grafito/verde, sujeto en
  el tercio superior y espacio oscuro para UI; sin texto, logos ni marcas.
- Salida de proyecto: `auth_mtb_background.png`, 864 × 1821 px.

## Validación

- 56 pruebas unitarias, sin fallos ni omisiones.
- Lint: 0 errores y 4 advertencias conocidas de versiones/launcher.
- `assembleDebug` y `assembleDebugAndroidTest` correctos; APK final de
  65.746.046 bytes.
- `AuthenticationGatewayInstrumentedTest` en `SM-A235M`: `OK (1 test)`.
- La app sin sesión mostró el fondo MTB, marca, formulario y desplazamiento con
  teclado, sin Perfil, clima ni navegación inferior. Captura:
  `app/build/outputs/auth-gateway-2026-08-16/auth-gateway.png`.
- El usuario completó el login real y la interfaz abrió Novedades. Una nueva
  instalación `-r` y reinicio conservaron esa cuenta y volvieron directamente a
  Inicio, confirmando ambos lados de la condición de arranque.
- Instalación final ADB correcta; proceso vivo, `MainActivity` enfocada, 0
  `FATAL EXCEPTION` y 0 ANR.

## Pendientes externos

La creación real de cuentas necesita que backend defina y despliegue acción,
campos, validación, verificación y errores seguros. Hasta entonces el CTA no
debe simular éxito ni almacenar contraseñas.
