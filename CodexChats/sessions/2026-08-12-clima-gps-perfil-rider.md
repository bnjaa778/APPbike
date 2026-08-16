# Sesion 2026-08-12 - Clima GPS y perfil rider APPbike

## Objetivo

Mostrar el tiempo del lugar real del dispositivo en el espacio de la cabecera
junto a Cuenta y reemplazar el pictograma de perfil generico por una expresion
visual propia de APPbike.

## Implementacion

- `MainActivity` conserva `WeatherHeaderState` en la raiz y solo mantiene el
  ciclo de actualizacion cuando la actividad esta `STARTED`.
- La ubicacion se obtiene con `DeviceLocationProvider.currentLocation`; la
  consulta remota se ejecuta explicitamente en `Dispatchers.IO`.
- `RemoteConnections.loadCurrentWeather` usa el endpoint publico de Open-Meteo
  sin autorización de APPbike. Se solicitan temperatura, sensacion termica,
  dia/noche, precipitacion, codigo meteorologico y nubosidad.
- `WeatherCondition` mapea los codigos WMO visibles: despejado, parcialmente
  nublado, nublado, niebla, llovizna, lluvia, lluvia helada, nieve, tormenta y
  tormenta con granizo.
- `WeatherStatusChip` dibuja los estados con Canvas, muestra temperatura y
  atribucion, y abre la fuente meteorologica.
- `AppBikeProfileGlyph` representa un rider con casco, visor y estado de sesion.

## Frecuencia y privacidad

- Exito: actualizacion cada 15 minutos.
- Error de GPS/red: reintento cada 60 segundos.
- Sin permiso: estado GPS y nueva comprobacion cada 2 segundos.
- No se persisten coordenadas adicionales para el clima ni se asocian a una
  cuenta. El token APPbike nunca se envia a Open-Meteo.

## Validacion

- 52 pruebas JVM, 0 fallos.
- Compilacion debug correcta.
- Prueba real en Samsung `SM-A235M`: 6 °C, `Noche clara` e icono de luna.
- Accesibilidad: clima, proveedor y perfil rider tienen descripciones completas.
- El acceso rider abre correctamente `Tu perfil`; 0 excepciones fatales y 0 ANR.
- La compilacion final quedo instalada mediante ADB.
- Evidencia: `app/build/outputs/appbike-weather-final.png` y XML contiguo.

## Handoff

El endpoint gratuito directo de Open-Meteo es apropiado para desarrollo y uso
no comercial. Antes de publicar comercialmente, usar el endpoint de cliente
contratado o un proxy backend y no incluir claves de proveedor en el APK. La
atribucion visible debe conservarse.

## Revision posterior

Por solicitud del usuario, el acceso de Cuenta volvio al icono anterior
`PersonOutline`. El clima, el contenedor de 48 dp y todo el espaciado de la
cabecera permanecieron sin cambios. La descripcion anterior del rider queda como
registro historico de la primera iteracion de esta sesion.
