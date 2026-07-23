# Flujo manual de trabajo

## Abrir y compilar

1. Abrir `C:\Users\Xinerdev\StudioProjects\APPbike` en Android Studio.
2. Esperar sincronización de Gradle.
3. Usar JDK 17. En este equipo funciona:

```text
C:\Program Files\Unity\Hub\Editor\6000.4.10f1\Editor\Data\PlaybackEngines\AndroidPlayer\OpenJDK
```

Compilación PowerShell:

```powershell
.\gradlew.bat "-Dorg.gradle.java.home=C:\Program Files\Unity\Hub\Editor\6000.4.10f1\Editor\Data\PlaybackEngines\AndroidPlayer\OpenJDK" :app:testDebugUnitTest :app:assembleDebug
```

APK resultante:

```text
app\build\outputs\apk\debug\app-debug.apk
```

## Instalar por ADB

```powershell
$adb = 'C:\Users\Xinerdev\AppData\Local\Android\Sdk\platform-tools\adb.exe'
& $adb devices -l
& $adb -s <dispositivo> install -r 'app\build\outputs\apk\debug\app-debug.apk'
```

`-r` conserva sesión y datos. Para una prueba de primer ingreso se debe borrar
datos explícitamente desde Ajustes o con `pm clear`; eso elimina la sesión local.

## Prueba manual de notificaciones

1. Iniciar sesión y aceptar notificaciones.
2. Confirmar una notificación persistente: “Escuchando mensajes nuevos”.
3. Presionar Inicio sin cerrar APPbike.
4. Enviar un mensaje desde otra cuenta/dispositivo.
5. Esperar hasta 6–12 segundos.
6. Comprobar que aparece el nombre del remitente y el texto.
7. Tocar el aviso y confirmar que abre la conversación.
8. Volver a APPbike y repetir; ahora debe aparecer el banner interno.

Si no llega, revisar `notifications/OUTSIDE_APP_NOTIFICATIONS.md`.

## Logcat útil

```powershell
& $adb -s <dispositivo> logcat -c
& $adb -s <dispositivo> logcat -v brief | Select-String -Pattern 'ChatNotification|ForegroundService|AndroidRuntime|FATAL EXCEPTION'
```

Servicio activo:

```powershell
& $adb -s <dispositivo> shell dumpsys activity services com.example.appbike
```

Canales y permisos:

```powershell
& $adb -s <dispositivo> shell dumpsys package com.example.appbike
& $adb -s <dispositivo> shell dumpsys notification --noredact
```

## EasyMD sin Codex automático

Abrir `CodexChats\EasyMD\bin\EasyMD-auto5.exe`, desactivar “Ejecutar Codex al
recibir tarea” y usar “Listar cola”, “Recibir siguiente” y “Abrir carpeta inbox”.
Leer la tarea Markdown, realizar el trabajo manualmente y conservar el informe en
`CodexChats/sessions`. No leer ni copiar `%APPDATA%\EasyMD\config.json`.
