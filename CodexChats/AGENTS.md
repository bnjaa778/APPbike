# CodexChats - reglas de continuidad

Esta carpeta conserva el contexto necesario para continuar APPbike sin depender
del historial de una conversación ni de una cuota activa de Codex.

Antes de modificar su contenido:

1. Leer `README.md` y `CURRENT_STATE.md`.
2. Para notificaciones leer `notifications/OUTSIDE_APP_NOTIFICATIONS.md`.
3. Para EasyMD leer `EasyMD/AGENTS.md`; nunca copiar tokens ni `config.json`.
4. Después de cada petición nueva agregar una entrada fechada a `CHANGELOG.md`
   con objetivo, archivos, decisiones, pruebas, pendientes y siguiente paso.
5. Si la sesión contiene trabajo sustancial, crear además un archivo en
   `sessions/AAAA-MM-DD-tema.md`.

Los documentos técnicos que forman parte del producto continúan en `docs/` y
el router principal continúa siendo `/AGENTS.md`. `CodexChats` es el cuaderno de
operaciones, relevo y trabajo tradicional; no debe contener contraseñas, tokens,
keystores ni datos privados de usuarios.
