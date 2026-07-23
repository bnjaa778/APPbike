# CodexChats de APPbike

## Propósito

Esta carpeta reúne el contexto operativo de Codex, el puente EasyMD, decisiones
técnicas, pruebas y procedimientos manuales. Permite continuar el proyecto desde
Android Studio, PowerShell o con otro agente aunque el historial del chat ya no
esté disponible.

## Lectura recomendada

1. `CURRENT_STATE.md`: qué funciona, arquitectura y pendientes reales.
2. `CHANGELOG.md`: cambios realizados desde la petición de centralización.
3. `MANUAL_WORKFLOW.md`: compilar, instalar, probar y diagnosticar sin Codex.
4. `notifications/OUTSIDE_APP_NOTIFICATIONS.md`: listener persistente de Chat.
5. `EasyMD/AGENTS.md`: contrato completo del puente entre equipos.
6. `EasyMD/EASYMD_HANDOFF.md`: uso cotidiano de EasyMD.
7. `sessions/`: informes cronológicos de cada sesión relevante.

## Estructura

```text
CodexChats/
  AGENTS.md
  README.md
  CURRENT_STATE.md
  CHANGELOG.md
  MANUAL_WORKFLOW.md
  notifications/
  sessions/
  references/
  EasyMD/
    AGENTS.md
    EASYMD_HANDOFF.md
    source/
    bin/
    inbox-archive/
```

`EasyMD/inbox-archive` es una fotografía de los Markdown recibidos hasta
2026-07-22. El inbox vivo continúa en `Documents/EasyMD Inbox` porque esa ruta
está guardada en la configuración externa del programa. Ningún archivo archivado
contiene el token de EasyMD.

## Archivos que no se trasladaron

- `AGENTS.md` de la raíz sigue allí porque Android Studio, CLI y agentes lo usan
  como router del repositorio.
- `docs/` conserva documentación del producto y contratos de backend porque
  forman parte de APPbike, no solo del historial de Codex.
- `dist/EasyMD-auto5.exe` se conserva temporalmente como copia de compatibilidad:
  al ordenar la carpeta había dos procesos EasyMD ejecutándose desde esa ruta.
  El fuente central fue recompilado y verificado en
  `EasyMD/bin/EasyMD-auto5.exe`; ambos hashes están en `EasyMD/AGENTS.md`.

## Regla de mantenimiento

Toda petición posterior debe terminar con documentación suficiente para que una
persona pueda reproducir los cambios, conocer qué fue probado y retomar el
siguiente paso sin leer la conversación original.
