# Sesion 2026-08-06 - SDK Manager operativo

## Objetivo

Corregir la configuracion del Android SDK en Windows y dejar APPbike lista para
sincronizar con Android Studio y ejecutar Gradle.

## Diagnostico inicial

- `local.properties` apuntaba a `C:\Android\Sdk`, una ruta inexistente.
- El SDK real estaba en
  `C:\Users\Benja(martulalover)\AppData\Local\Android\Sdk`, pero solo tenia
  metadatos parciales y no tenia `sdkmanager`, plataformas ni Build Tools.
- Android Studio tambien conservaba `C:\Android\Sdk` en su configuracion.

## Cambios

- Se instalo `cmdline-tools` 22.0 desde el repositorio oficial de Android.
- Se instalaron `platform-tools` 37.0.1, la plataforma `android-37.0` y Build
  Tools 36.0.0/37.0.0; los archivos descargados se validaron con SHA-1 oficial.
- Se actualizo la ruta del SDK en `local.properties` y en las preferencias de
  Android Studio 2025.1.2/2026.1.3.
- Se corrigio el metadata local de `platforms;android-37.0` para que el gestor
  reconozca el paquete y no lo reinstale como `android-37.0-2`.
- Se configuraron `ANDROID_HOME`, `ANDROID_SDK_ROOT` y el `PATH` de usuario.

## Pruebas

- `sdkmanager --version`: correcto, 22.0.
- `sdkmanager --list`: correcto; enumera la plataforma 37.0, Platform Tools
  37.0.1 y Build Tools 36/37 como instalados.
- `adb version` y `aapt2 version`: correctos.
- `:app:tasks --offline`: `BUILD SUCCESSFUL`.
- `:app:assembleDebug --offline`: falla únicamente por AAR no cacheadas de
  Compose, AndroidX y MapLibre; la configuracion del SDK ya no es la causa.

## Pendientes

- Completar una compilacion online cuando termine la descarga de dependencias
  Gradle faltantes.

## Siguiente paso

Reiniciar Android Studio o abrir una terminal nueva y ejecutar la sincronizacion
del proyecto.
