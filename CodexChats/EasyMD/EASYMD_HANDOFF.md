# EasyMD visual y relevo tradicional

EasyMD ahora se usa con un solo ejecutable visual:

```text
CodexChats/EasyMD/bin/EasyMD-auto5.exe
```

Ese mismo `.exe` se copia al PC principal y al servidor. Al abrirlo pide permisos
de administrador, guarda su configuracion en `%APPDATA%\EasyMD` y permite:

- crear o pegar token,
- generar el `index.php` listo para subir a `api.zizzio.cl/easymd`,
- iniciar y detener el monitor de fondo,
- enviar tareas al otro Codex,
- recibir la siguiente tarea,
- listar la cola,
- marcar tareas como terminadas,
- ejecutar Codex automaticamente al recibir una tarea,
- capturar su respuesta y devolverla al otro equipo automaticamente.

## Primera configuracion

1. Ejecutar `CodexChats/EasyMD/bin/EasyMD-auto5.exe`.
2. Elegir rol:
   - `desktop` en el ordenador principal.
   - `server` en el servidor.
3. Elegir `Crear token nuevo` o `Usar token existente`.
4. Para crear token nuevo:
   - escribir 5 pares de numeros,
   - escribir 2 frases de exactamente 8 caracteres,
   - pulsar `Crear token`,
   - copiar el token generado.
5. Guardar configuracion.

El token generado se pega en el otro ordenador usando `Usar token existente`.

## Autoejecutar Codex

En `Configurar / token`, activar:

```text
Ejecutar Codex al recibir tarea
```

Valores recomendados:

```text
Comando Codex: ruta autodetectada a codex.exe
Argumentos: exec --sandbox workspace-write "Lee y ejecuta la tarea EasyMD guardada en: {file}"
Carpeta trabajo: carpeta del proyecto que debe modificar ese Codex
```

Si aparece `El sistema no puede encontrar el archivo especificado`, usa el boton
`...` junto a `Comando Codex` y selecciona manualmente `codex.exe`. En Windows
suele estar en una ruta similar a:

```text
%LOCALAPPDATA%\OpenAI\Codex\bin\<version>\codex.exe
```

Variables disponibles en argumentos:

```text
{file}
{workspace}
{id}
{title}
{role}
{project}
```

Cuando el monitor recibe una tarea normal:

1. guarda el `.md`,
2. lanza Codex y espera a que termine,
3. captura su ultimo mensaje,
4. envia ese resultado al equipo de origen,
5. marca la tarea original como terminada.

Las respuestas se guardan en el inbox y se cierran sin ejecutar otro Codex, para
que los dos monitores no entren en un ciclo de respuestas. Los logs y el mensaje
final capturado quedan en:

```text
%APPDATA%\EasyMD\codex-runs
```

Si una tarea queda en `claimed`, abre `Abrir logs` y revisa `watch.log` y
`codex-runs`. En `auto5` esto significa que Codex sigue trabajando o que EasyMD
no pudo publicar la respuesta; revisa los archivos `.out.log`, `.err.log` y
`.final.md` antes de usar `Reponer a cola`.

`Probar Codex` valida localmente el lanzamiento y la captura final. Para una
prueba completa desde consola se puede usar:

```text
EasyMD-auto5.exe --self-test-roundtrip
```

Debe finalizar con `EASYMD_ROUNDTRIP_OK`. Usa un proyecto de cola temporal y no
envia la prueba al otro equipo.

## Crear el endpoint PHP

En el equipo donde generaste el token:

1. Abrir `CodexChats/EasyMD/bin/EasyMD-auto5.exe`.
2. Pulsar `Generar index.php`.
3. Guardar el archivo.
4. Subir ese `index.php` a:

```text
https://api.zizzio.cl/easymd/index.php
```

El archivo PHP ya queda con el token escrito dentro.

## Uso diario

- `Iniciar fondo`: deja el monitor oculto revisando tareas.
- `Detener fondo`: detiene el monitor oculto.
- `Estado`: comprueba conexion con el endpoint.
- `Enviar al otro Codex`: manda instrucciones en Markdown al otro equipo.
- `Recibir siguiente`: toma manualmente la siguiente tarea pendiente.
- `Marcar terminada`: cierra una tarea usando su ID.

Las tareas recibidas se guardan en la carpeta configurada como inbox.

## Actualizar desde auto4

En cada equipo:

1. Abrir la version antigua y pulsar `Detener fondo`.
2. Copiar y abrir `EasyMD-auto5.exe`.
3. Entrar a `Configurar / token`, confirmar la ruta local de `codex.exe` y
   guardar. No copies la ruta del otro PC.
4. Pulsar `Probar Codex` y comprobar la respuesta capturada.
5. Pulsar `Iniciar fondo`.

La configuracion y el token existentes se conservan. La plantilla historica
exacta de argumentos se migra a `workspace-write`. El endpoint PHP
`1.3.0-compat` no necesita cambiar para esta actualizacion.

## Seguridad

- No publiques el token.
- No subas claves de base de datos ni passwords en las tareas.
- No compartas el `index.php` generado salvo para instalarlo en tu servidor.
