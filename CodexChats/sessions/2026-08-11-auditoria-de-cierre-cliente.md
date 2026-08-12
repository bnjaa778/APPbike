# Auditoria de cierre del cliente Android

Fecha: 2026-08-11 (evidencia CI: 2026-08-12 UTC)

## Objetivo

Contrastar la peticion de terminar diseño y funciones con evidencia actual del
repositorio. Esta auditoria diferencia cliente Android comprobable de requisitos
que no pueden resolverse sin contrato, infraestructura o datos externos.

## Evidencia comprobada

| Area | Evidencia | Estado |
| --- | --- | --- |
| Compilacion del cliente | GitHub Actions `31551839752`: JDK 17, SDK 37.0, unitarias, Lint y APK debug correctos en runner limpio. | Comprobado |
| Diseño Android | Tema grafito/verde, componentes comunes, icono vectorial propio, cabecera y navegacion estan centralizados; existen pruebas semanticas y recorridos AVD documentados. | Cliente implementado; la inspeccion en dispositivo fisico sigue pendiente. |
| Flujos principales | Cuenta, Bicicletas, Mapas/Juntas, Marketplace y Chat tienen pantallas, estados sin sesion, protecciones de propietario y pruebas locales/instrumentadas. | Cliente implementado. |
| Fotos y datos locales | URI seguros, Base64 por accion, limite de tamano, EXIF y claves de cache aisladas se cubren en codigo y pruebas. | Comprobado en cliente. |
| Entrega de pruebas | El workflow conserva el APK debug como `appbike-debug-apk` por 14 dias tras una ejecucion correcta. | Comprobado: ejecucion `31552406399`, artefacto de 35,857,353 bytes. |

## Requisitos que no se pueden declarar terminados desde este repositorio

1. **Alta y recuperacion de cuentas.** El backend no define acciones ni reglas
   aprobadas. Android no debe inventar esos contratos ni almacenar secretos de
   recuperacion.
2. **Autorizacion extremo a extremo.** Las acciones privadas responden 401 sin
   token; se requiere una sesion de prueba para probar Bearer, propiedad y
   limpieza de datos entre dos usuarios.
3. **Geografia de servidor.** `location.reverse` devuelve `unknown_region` para
   Santiago y falta la entrega PostGIS/paginacion multirregional.
4. **Push con proceso cerrado.** La escucha local funciona mientras el proceso
   vive, pero FCM necesita contrato de tokens, proyecto Firebase y backend.
5. **Release de produccion.** Requiere firma, una rama de release, dos cuentas
   de prueba y aprobacion QA en dispositivo fisico; un APK debug no es una
   distribucion firmada.

## Decisiones de alcance

- Strava, Garmin y Wahoo permanecen `PROXIMAMENTE` por decisión vigente de
  producto; no son un defecto mientras no se active OAuth.
- No se reemplazan fallbacks de ubicacion ni se crean formularios de registro
  hasta que el contrato remoto exista y se pruebe de forma autenticada.
- No se amplian iconos, logos o cajas de texto: el sistema visual ya esta
  centralizado y no hay una deficiencia demostrada que justifique mas costo.

## Siguiente paso verificable

La mejora que desbloquea el 100 % de producto requiere coordinacion del
backend/QA con los cinco requisitos externos listados, no una reescritura
adicional del cliente.
