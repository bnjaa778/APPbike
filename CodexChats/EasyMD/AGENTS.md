# EasyMD - Router de trabajo para agentes (CodexChats)

Este archivo concentra el contexto operativo y tecnico del puente EasyMD entre
el Codex del PC principal y el Codex del servidor. Usalo como primera fuente de
verdad cuando la tarea trate del puente. No reconstruyas el contexto desde el
historial del chat si la respuesta ya esta aqui.

Ultima revision: 2026-07-21.

## Objetivo

EasyMD permite que dos instalaciones de Codex trabajen por turnos sobre un
mismo proyecto:

1. Un equipo envia una tarea Markdown al endpoint PHP.
2. El endpoint la guarda en una cola protegida por token.
3. El monitor del equipo destino reclama la tarea.
4. EasyMD guarda un archivo `.md` local y puede ejecutar Codex automaticamente.
5. El Codex receptor realiza el trabajo y puede responder mediante la misma
   cola con el rol de destino opuesto.

El mismo `.exe` se usa en ambos equipos. Solo cambian el rol, la carpeta de
trabajo y la ruta local de `codex.exe`.

## Regla de oro

- No publiques, registres en consola ni escribas el token en documentacion.
- Carga siempre el token desde `%APPDATA%\EasyMD\config.json`.
- No reclames tareas con `next` solo para inspeccionarlas: `next` cambia su
  estado de `queued` a `claimed`.
- Para inspeccionar usa `list`; para devolver una tarea atascada usa `update`
  con estado `queued`.
- Antes de reemplazar el PHP desplegado, genera otro `index.php` desde el `.exe`
  configurado con el token correcto.
- Despues de cambiar el codigo C#, recompila el ejecutable y verifica tanto la
  GUI como el modo `--watch`.
- No modifiques archivos Android para resolver problemas del puente.

## Estado actual

- Fuente principal: `CodexChats/EasyMD/source/EasyMDGui.cs`.
- Manifest UAC: `CodexChats/EasyMD/source/EasyMDGui.exe.manifest`.
- Ejecutable vigente: `CodexChats/EasyMD/bin/EasyMD-auto5.exe`.
- Copia de compatibilidad temporal: `dist/EasyMD-auto5.exe`; no eliminarla
  mientras un monitor iniciado desde esa ruta siga activo.
- SHA-256 del binario central recompilado:
  `8C179796129B7AB7054CF72BE4937110958AA159930C474F93B55269A63E0582`.
- SHA-256 de la copia de compatibilidad que seguía activa desde `dist`:
  `693278890409D2B41436556ED07828E475FCF87F60E34F779F5FF5DAC9457A0D`.
- Guia corta para usuario: `CodexChats/EasyMD/EASYMD_HANDOFF.md`.
- Endpoint esperado: `https://api.zizzio.cl/easymd/index.php`.
- Version que debe informar el PHP generado: `1.3.0-compat`.
- La comunicacion HTTP y la entrega de mensajes ya fueron comprobadas.
- En este PC la configuracion observada usa rol `desktop`, proyecto `APPbike`,
  la URL indicada arriba y el inbox `Documents\EasyMD Inbox`. No asumas que el
  servidor comparte rutas locales con este equipo.
- `EasyMD-auto5.exe` espera a que `codex exec` termine, captura su mensaje final,
  responde automaticamente al equipo de origen y luego marca la tarea original
  como `done`.
- Las respuestas se publican con `priority=response`; el monitor receptor las
  guarda y cierra sin volver a ejecutar Codex, evitando ciclos infinitos.
- `EasyMD-auto5.exe` incorpora selector de `codex.exe`, autodeteccion, pruebas
  locales, logs por ejecucion y captura mediante `--output-last-message`.

Los binarios anteriores (`EasyMD.exe`, `EasyMD-compat.exe`,
`EasyMD-fixed.exe`, `EasyMD-auto.exe`, `EasyMD-auto2.exe`,
`EasyMD-auto3.exe` y `EasyMD-auto4.exe`) son historicos. No los recomiendes ni
los distribuyas como version vigente.

## Router rapido

| Si la tarea trata de... | Abrir primero | Revisar despues |
|---|---|---|
| Interfaz, botones o configuracion | `EasyMDGui.cs`, `MainForm`, `SetupForm` | `EasyConfig`, `Store` |
| Token | `TokenFactory` | `SetupForm`, `EndpointFactory` |
| Generacion de `index.php` | `EndpointFactory.CreatePhpCompat` | contrato API de este archivo |
| Error HTTP o autenticacion | `EasyApi.Call` | PHP generado y configuracion local |
| Cola o estados | PHP en `CreatePhpCompat` | `MainForm`, `Watcher` |
| Monitor de fondo | `Watcher` | `MainForm.StartWatcher`, `Store` |
| Codex no inicia | `CodexRunner`, `Store.FindCodexCommand` | logs y `workspaceDir` |
| Tarea queda en `claimed` | `Watcher`, accion `next` | boton `Reponer a cola` |
| Compilar otro `.exe` | este archivo, seccion Build | manifest y fuente completa |
| Uso diario o instalacion | `CodexChats/EasyMD/EASYMD_HANDOFF.md` | este router |

## Archivos y responsabilidades

### `EasyMDGui.cs`

Aplicacion WinForms autocontenida para .NET Framework. Contiene:

- `Program`: entrada GUI y modos de linea de comandos.
- `EasyConfig`: valores persistidos por equipo.
- `Store`: rutas locales, lectura/escritura JSON y deteccion de Codex.
- `EasyApi`: cliente HTTP JSON con TLS 1.2 y token.
- `TokenFactory`: creacion del token aleatorio.
- `EndpointFactory`: construccion del `index.php` con token embebido.
- `SetupForm`: asistente de rol, token, endpoint y autoejecucion.
- `MainForm`: controles manuales y salida visible.
- `CodexRunner`: ejecucion bloqueante de Codex, logs y captura del mensaje final.
- `TaskProcessor`: envio automatico de resultados, cierre de tareas y deteccion
  de respuestas para evitar bucles.
- `Watcher`: consulta periodica, guardado de tareas y autoejecucion.

### `EasyMDGui.exe.manifest`

Solicita `requireAdministrator` y activa Common Controls. El `.exe` debe mostrar
el dialogo UAC al abrirse.

### `CodexChats/EasyMD/EASYMD_HANDOFF.md`

Guia breve orientada al usuario. Mantenerla sincronizada cuando cambie el nombre
del binario vigente, la instalacion o los controles principales.

## Configuracion local

La configuracion vive fuera del repositorio:

```text
%APPDATA%\EasyMD\config.json
```

Campos actuales:

| Campo | Uso | Valor inicial |
|---|---|---|
| `role` | Identidad del equipo | vacio hasta configurar |
| `token` | Secreto compartido | vacio hasta crear o pegar |
| `baseUrl` | Carpeta remota del endpoint | `https://api.zizzio.cl/easymd` |
| `project` | Filtro logico de cola | `APPbike` |
| `outDir` | Inbox de tareas `.md` | `%USERPROFILE%\Documents\EasyMD Inbox` |
| `intervalSeconds` | Periodo del monitor | `60` |
| `autoRunCodex` | Reclamar y ejecutar automaticamente | `false` |
| `codexCommand` | Ruta o comando de Codex | autodetectado |
| `codexArguments` | Plantilla de argumentos | `exec --sandbox workspace-write "Lee y ejecuta... {file}"` |
| `workspaceDir` | Proyecto donde trabajara Codex | directorio de inicio |

Roles validos en el flujo actual:

- `desktop`: PC principal; envia normalmente a `server`.
- `server`: servidor; envia normalmente a `desktop`.

`MainForm.SendTask` calcula siempre el destino opuesto. Ambos equipos deben usar
el mismo token, la misma `baseUrl` y el mismo `project`.

## Archivos de ejecucion

EasyMD crea estos archivos fuera del repositorio:

```text
%APPDATA%\EasyMD\config.json
%APPDATA%\EasyMD\watch.pid
%APPDATA%\EasyMD\watch.log
%APPDATA%\EasyMD\watch.err.log
%APPDATA%\EasyMD\codex-runs\<task-id>.out.log
%APPDATA%\EasyMD\codex-runs\<task-id>.err.log
%APPDATA%\EasyMD\codex-runs\<task-id>-<fecha>.final.md
%USERPROFILE%\Documents\EasyMD Inbox\<task-id>-<titulo>.md
```

`watch.err.log` esta reservado como ruta conocida, pero el monitor actual
registra sus excepciones en `watch.log`. Los procesos Codex escriben sus salidas
en `codex-runs` cuando el inicio directo permite redireccion.

## Modos del ejecutable

```text
EasyMD-auto5.exe
EasyMD-auto5.exe --watch
EasyMD-auto5.exe --export-endpoint <ruta-index.php>
EasyMD-auto5.exe --self-test-codex
EasyMD-auto5.exe --self-test-roundtrip
```

- Sin argumentos abre la interfaz.
- `--watch` ejecuta el ciclo oculto del monitor.
- `--export-endpoint` genera el PHP usando el token guardado; falla si falta.
- `--self-test-codex` ejecuta Codex localmente y valida la captura final.
- `--self-test-roundtrip` valida en un proyecto temporal el ciclo completo de
  cola, Codex, respuesta y cierre sin involucrar al otro equipo.

## Token

La interfaz permite crear uno o usar uno existente. Para crear uno solicita:

- cinco pares de numeros;
- dos frases de exactamente ocho caracteres.

`TokenFactory` combina esos valores con nombre de equipo, usuario, fecha UTC,
un GUID y 64 bytes criptograficamente aleatorios. Aplica SHA-512 y devuelve
Base64 URL-safe sin relleno. No intentes reproducir el token a partir de los
datos introducidos: el componente aleatorio hace que cada resultado sea unico.

El PHP generado incluye el token como `EASYMD_TOKEN`. El cliente lo envia en la
cabecera `X-EasyMD-Token`. El endpoint acepta tambien `Authorization: Bearer`,
`token` en JSON y `token` por query, pero el cliente oficial usa la cabecera.

## Contrato HTTP

Todas las operaciones usan `POST` JSON a:

```text
{baseUrl}/index.php
```

Cabeceras:

```text
Content-Type: application/json; charset=utf-8
X-EasyMD-Token: <token de config.json>
```

Acciones:

| Accion | Campos relevantes | Efecto |
|---|---|---|
| `status` o `ping` | `action` | Devuelve version y conteos por estado |
| `push` | `source`, `target`, `project`, `title`, `markdown`, `priority` opcional | Crea tarea `queued` |
| `list` | filtros `project`, `target`, `source`, `status` | Lista hasta 50, nuevas primero |
| `next` | `target`, `project`, `claimer` | Reclama la primera tarea `queued` coincidente |
| `done` | `id`, `by`, `note` opcional | Cambia la tarea a `done` |
| `update` | `id`, `status`, `by`, `note` opcional | Cambia a un estado permitido |

Estados permitidos:

```text
queued -> claimed -> done
queued/claimed -> cancelled
claimed -> queued
```

El ultimo cambio se usa para recuperar tareas que un monitor reclamo pero no
pudo ejecutar. Los estados aceptados por el endpoint son `queued`, `claimed`,
`done` y `cancelled`.

Esquema de tarea:

```json
{
  "id": "easymd-AAAAMMDD-HHMMSS-8hex",
  "created_at": "UTC ISO-8601",
  "updated_at": "UTC ISO-8601",
  "source": "desktop|server",
  "target": "server|desktop",
  "project": "APPbike",
  "title": "Titulo",
  "priority": "normal",
  "status": "queued|claimed|done|cancelled",
  "claimed_at": null,
  "completed_at": null,
  "claim_count": 0,
  "markdown": "Instrucciones",
  "history": []
}
```

## Persistencia del endpoint PHP

El endpoint guarda `tasks.json` y `tasks.lock` bajo bloqueo exclusivo con
`flock`. Prueba estas carpetas en orden y usa la primera escribible:

1. `easymd/_data` junto a `index.php`.
2. `easymd_data` como hermano de la carpeta `easymd`.
3. `sys_get_temp_dir()/easymd_<hash>`.

Al elegir una carpeta crea y elimina `.write-test`, y trata de crear `.htaccess`
para impedir acceso web. Esta estrategia fue agregada porque el hosting devolvia
`data_dir_unavailable` al no poder crear `_data`.

El PHP `1.3.0-compat` evita sintaxis moderna innecesaria y agrega fallbacks para
`http_response_code`, `hash_equals` y generacion aleatoria. No vuelvas a activar
el bloque PHP moderno que aun permanece debajo del `return CreatePhpCompat(...)`
sin verificar primero la version real de PHP del hosting.

## Monitor de fondo

`Iniciar fondo` lanza el mismo ejecutable con `--watch` y guarda el PID. El ciclo:

1. Lee de nuevo `config.json` en cada iteracion.
2. Si `autoRunCodex=false`, no llama a `next` y no reclama tareas.
3. Si esta activo, llama a `next` para el rol y proyecto locales.
4. Guarda la tarea recibida como Markdown en `outDir`.
5. Si es una respuesta (`priority=response`), la guarda y marca `done` sin
   ejecutar otro Codex.
6. Para una tarea normal, ejecuta Codex y espera su salida final.
7. Publica la respuesta al rol de origen con el ID de la tarea original.
8. Marca la tarea original `done` solo despues de publicar la respuesta.
9. Espera al menos 10 segundos o el intervalo configurado.

Ante una excepcion registra el error y espera 60 segundos. Si no pudo publicar
la respuesta, deja la tarea `claimed` para no perder el resultado ni afirmar una
finalizacion inexistente; puede recuperarse con `Reponer a cola`.

Importante: cerrar la ventana principal no equivale a detener el monitor. Usa
`Detener fondo`, que finaliza el PID almacenado.

## Autoejecucion de Codex

Configuracion recomendada:

```text
Ejecutar Codex al recibir tarea: activado
Comando Codex: ruta absoluta al codex.exe real
Argumentos: exec --sandbox workspace-write "Lee y ejecuta la tarea EasyMD guardada en: {file}"
Carpeta trabajo: raiz del proyecto correspondiente a ese equipo
```

Variables disponibles en comando y argumentos:

```text
{file} {workspace} {id} {title} {role} {project}
```

Resolucion de `codex.exe`:

1. Busca recursivamente en `%LOCALAPPDATA%\OpenAI\Codex\bin`.
2. Busca `codex.exe` en `PATH`.
3. Intenta buscar bajo `Program Files\WindowsApps`.
4. Si no encuentra nada conserva `codex` y el lanzamiento mostrara el error.

En este PC se encontro Codex en una ruta con este patron:

```text
%LOCALAPPDATA%\OpenAI\Codex\bin\<version>\codex.exe
```

No copies la ruta exacta de otro equipo. En el servidor usa el boton `...` para
seleccionar su propio `codex.exe` y luego `Probar Codex` antes de iniciar fondo.
Los alias de `WindowsApps` pueden devolver acceso denegado; preferir el binario
real bajo `%LOCALAPPDATA%`.

`CodexRunner` ejecuta Codex oculto, espera su salida, captura stdout/stderr y
agrega `--output-last-message` cuando la plantilla usa `exec`. Los logs incluyen
comando configurado, comando resuelto, argumentos y workspace. Si Codex falla,
EasyMD envia una respuesta de error y cierra la tarea solo si esa respuesta fue
publicada correctamente.

La plantilla historica exacta sin `--sandbox` se migra en memoria a
`workspace-write`. Plantillas personalizadas conservan sus permisos; EasyMD no
las amplia silenciosamente.

## Archivo entregado a Codex

Cada tarea reclamada genera un Markdown con:

- titulo e ID;
- origen, destino, proyecto y fecha;
- instruccion de realizar el trabajo;
- indicacion de terminar con una respuesta final clara;
- Markdown original enviado por el otro Codex.

El token y la ruta de `config.json` no se copian al archivo de tarea. Codex no
opera la cola directamente; `TaskProcessor` se encarga del relevo.

## Protocolo de relevo para el Codex ejecutado

Cuando Codex es iniciado desde una tarea EasyMD debe completar el trabajo y
producir una respuesta final util:

1. Leer por completo el `.md` recibido y este router si la tarea afecta EasyMD.
2. Ejecutar y verificar el trabajo solicitado en `workspaceDir`.
3. Preparar una respuesta Markdown breve con resultado, archivos cambiados,
   pruebas, pendientes y la siguiente accion concreta para el otro equipo.
4. Si no puede completar el trabajo, responder explicando el bloqueo; no dejar
   que la unica evidencia quede en un log local.

Codex no debe leer `config.json`, usar el token, llamar `push` ni cerrar tareas.
EasyMD captura el ultimo mensaje con `--output-last-message`, crea una tarea de
respuesta con `priority=response` y cierra la original. El monitor receptor
guarda esa respuesta en el inbox y la marca `done` sin generar otra respuesta.

## Controles de la interfaz

| Control | Funcion |
|---|---|
| `Configurar / token` | Rol, token, URL, proyecto, inbox y Codex |
| `Generar index.php` | Exporta PHP con el token actual |
| `Iniciar fondo` | Inicia `--watch` oculto |
| `Detener fondo` | Finaliza el PID del monitor |
| `Estado` | Prueba endpoint y muestra conteos |
| `Listar cola` | Lista tareas destinadas al rol local |
| `Marcar terminada` | Cambia a `done` el ID indicado |
| `Reponer a cola` | Cambia a `queued` el ID indicado |
| `Abrir carpeta inbox` | Abre tareas Markdown recibidas |
| `Abrir logs` | Abre `%APPDATA%\EasyMD` |
| `Probar Codex` | Crea tarea local de prueba y lanza Codex |
| `Enviar al otro Codex` | Crea tarea para el rol opuesto |
| `Recibir siguiente` | Reclama manualmente y guarda la siguiente |
| `Cargar .md` | Carga instrucciones desde un archivo |

## Diagnostico por sintomas

### `data_dir_unavailable`

El PHP funciona, pero ninguna carpeta candidata permite escritura. Confirmar que
el servidor tenga desplegado el `index.php` `1.3.0-compat`; despues revisar
permisos de `easymd`, su directorio padre y disponibilidad de la carpeta temporal.

### HTTP 500 sin JSON util

Suele indicar PHP incompatible, error de sintaxis o archivo antiguo desplegado.
Generar de nuevo `index.php` con el ejecutable vigente y comprobar `Estado`.

### `unauthorized`

El token del equipo no coincide con el token embebido en `index.php`. No lo
imprimas para comparar: vuelve a pegar el mismo token en ambos equipos y genera
el endpoint desde esa configuracion.

### `No hay tareas pendientes`

No implica necesariamente un error. Revisar con `Listar cola`:

- que `target` coincida con el rol local;
- que `project` coincida exactamente;
- que la tarea siga en `queued` y no en `claimed`;
- que otro monitor no la haya tomado antes.

### Tarea atascada en `claimed`

El endpoint ya la entrego y no volvera a entregarla. Pegar su ID y usar
`Reponer a cola`, despues corregir Codex y reiniciar el monitor.

### `El sistema no puede encontrar el archivo especificado`

EasyMD recibio la tarea, pero `codexCommand` no existe en ese equipo. Detener el
monitor antiguo, abrir el ejecutable vigente, seleccionar el `codex.exe` real,
guardar, ejecutar `Probar Codex`, iniciar fondo y reponer la tarea.

### Codex abre pero trabaja en otro proyecto

Corregir `workspaceDir`. Debe ser una carpeta existente y contener el proyecto
que ese Codex debe modificar.

### Hay monitor viejo o PID obsoleto

Usar primero `Detener fondo`. Si la interfaz dice que no corre pero existe un
`watch.pid` obsoleto, verificar que ese PID ya no corresponda a EasyMD antes de
eliminar solo ese archivo. No finalizar procesos por nombre de forma masiva.

## Comprobaciones directas seguras

Para diagnostico por PowerShell, cargar la configuracion sin mostrar el token:

```powershell
$easyCfg = Get-Content -LiteralPath (Join-Path $env:APPDATA 'EasyMD\config.json') -Raw | ConvertFrom-Json
$easyUri = $easyCfg.baseUrl.TrimEnd('/') + '/index.php'
$easyHeaders = @{ 'X-EasyMD-Token' = $easyCfg.token }
$easyBody = @{ action = 'status' } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri $easyUri -Headers $easyHeaders -ContentType 'application/json; charset=utf-8' -Body $easyBody
```

Para listar sin reclamar:

```powershell
$easyBody = @{
    action = 'list'
    project = $easyCfg.project
    target = $easyCfg.role
} | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri $easyUri -Headers $easyHeaders -ContentType 'application/json; charset=utf-8' -Body $easyBody
```

No incluyas `$easyCfg`, `$easyHeaders` ni el contenido de `config.json` completo
en la salida de herramientas porque contienen el secreto.

## Build

No hay proyecto `.csproj`. Compilar con el C# compiler de .NET Framework:

```powershell
$easyCsc = 'C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe'
& $easyCsc /nologo /target:winexe /optimize+ `
  /out:'CodexChats\EasyMD\bin\EasyMD-auto5.exe' `
  /win32manifest:'CodexChats\EasyMD\source\EasyMDGui.exe.manifest' `
  /reference:System.dll `
  /reference:System.Drawing.dll `
  /reference:System.Windows.Forms.dll `
  /reference:System.Web.Extensions.dll `
  'CodexChats\EasyMD\source\EasyMDGui.cs'
```

El fuente actual emite `CS0162` porque `CreatePhp` retorna inmediatamente la
version compatible y conserva debajo una plantilla moderna inalcanzable. Es un
warning conocido, no un fallo de compilacion. Si se limpia, conservar exactamente
el comportamiento de `CreatePhpCompat`.

Al crear una nueva version:

1. Usa un nombre incremental claro en `dist`.
2. Actualiza `Estado actual` en este archivo.
3. Actualiza `CodexChats/EasyMD/EASYMD_HANDOFF.md`.
4. Calcula y documenta el SHA-256 del binario vigente.
5. No borres binarios anteriores sin peticion explicita.

## Verificacion obligatoria

Despues de cambios en EasyMD:

1. Compilar sin errores.
2. Confirmar que el manifest siga solicitando administrador.
3. Abrir la GUI y comprobar que carga una configuracion existente.
4. Ejecutar `Estado` contra el endpoint sin exponer el token.
5. Ejecutar `Probar Codex` y revisar los logs de salida y error.
6. Ejecutar `--self-test-roundtrip` y comprobar `EASYMD_ROUNDTRIP_OK`.
7. Enviar una tarea de prueba al rol opuesto.
8. Confirmar que el monitor destino la guarda, ejecuta Codex y publica respuesta.
9. Confirmar que el origen guarda la respuesta sin iniciar otro ciclo Codex.
10. Confirmar que tanto la tarea como su respuesta quedan `done`.
11. Si se probo con una tarea real que quedo `claimed`, terminarla o reponerla.

Para cambios solo documentales basta revisar rutas, comandos, nombres de campos
y `git diff`; no es necesario alterar la cola remota.

## Historial util de incidentes

- El primer endpoint devolvio HTTP 500 por sintaxis PHP demasiado moderna.
- La primera correccion alcanzo el servidor, pero fallo al crear `_data`.
- Se agregaron tres ubicaciones de persistencia y prueba real de escritura.
- Con `1.3.0-compat` los mensajes comenzaron a llegar entre ambos equipos.
- Un monitor podia reclamar tareas aunque auto-Codex no estuviera listo; ahora
  no llama a `next` cuando `autoRunCodex=false`.
- En el servidor aparecio: `No se pudo iniciar Codex... El sistema no puede
  encontrar el archivo especificado`. La causa fue la ruta de `codex.exe`, no
  la cola ni el token.
- Se agregaron `Reponer a cola`, `Abrir logs`, `Probar Codex`, selector de
  ejecutable, autodeteccion y logs de procesos en `EasyMD-auto4.exe`.
- La captura muestra que `auto4` podia iniciar Codex pero dejaba la tarea
  `claimed`: EasyMD no esperaba el proceso ni recogia su respuesta.
- `EasyMD-auto5.exe` agrega espera bloqueante, `--output-last-message`, respuesta
  y cierre automaticos, pruebas CLI locales y proteccion anti-bucle mediante
  `priority=response`.

IDs historicos usados durante pruebas, solo como referencia de diagnostico:

```text
easymd-20260721-063217-bbc1a287
easymd-20260721-063306-47f7e6ee
easymd-20260721-064400-e57db6d2
easymd-20260721-065215-b15f4ec5
```

No asumas su estado actual. Consulta con `list` antes de actuar y nunca uses un
ID historico como parte del funcionamiento normal.

## Pendientes y limites conocidos

- Si EasyMD no logra publicar la respuesta, la tarea queda `claimed` y requiere
  revision de logs y recuperacion manual; no hay reintento automatico.
- `codex exec` debe estar autenticado y disponible en cada equipo.
- La cola usa un archivo JSON, adecuada para uso personal y baja concurrencia,
  no para alta carga.
- El token queda en texto dentro de `config.json` y del PHP generado.
- El servidor debe tener permisos de escritura en al menos una carpeta candidata.
- No existe instalador ni actualizador; el artefacto distribuible es un `.exe`.
- El bloque PHP moderno inalcanzable genera un warning de compilacion pendiente
  de limpieza.

## Checklist antes de cerrar una tarea

- El cambio pertenece al puente y no altera APPbike sin necesidad.
- No se expuso el token en codigo, logs, capturas ni respuestas.
- El contrato remoto sigue sincronizado entre `EasyApi` y `EndpointFactory`.
- `next` solo se usa cuando se desea reclamar una tarea.
- Las tareas fallidas pueden recuperarse a `queued`.
- La ruta de Codex y `workspaceDir` son validas en el equipo probado.
- El ejecutable vigente, su hash y la guia corta estan actualizados si aplica.
- Se realizaron las verificaciones proporcionales al cambio.
