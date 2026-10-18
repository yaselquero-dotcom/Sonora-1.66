# Sonora 1.2 — Android

Reproductor Android en español con interfaz premium oscura, acentos violeta/menta, biblioteca local, búsqueda de emisoras de radio y catálogo de música independiente Jamendo.

## Funciones
- Importación múltiple de archivos locales con selector seguro de Android.
- Reproducción, pausa y siguiente/anterior en la biblioteca.
- Búsqueda de radios con la API pública de Radio Browser; solo añade streams HTTPS.
- Catálogo de Jamendo mediante API oficial, que requiere un `client_id` propio.
- UI adaptada a móvil y navegación Inicio / Radio / Explorar / Biblioteca.
- Favoritos y biblioteca persistentes entre sesiones.
- Pantalla de privacidad y ajustes con borrado confirmado de datos de Sonora (no borra los archivos originales).
- Rechazo de tráfico HTTP sin cifrar mediante `usesCleartextTraffic="false"`; las fuentes online se filtran para aceptar solo HTTPS.

## Activar catálogo de Jamendo
1. Crea una aplicación en https://developer.jamendo.com/
2. Copia el `client_id`.
3. Abre `app/src/main/java/com/sonora/music/MainActivity.java`.
4. Sustituye `YOUR_JAMENDO_CLIENT_ID` en la constante `JAMENDO_CLIENT_ID` por tu identificador.
5. Compila y prueba la app.

La radio online usa `https://all.api.radio-browser.info/` y muestra emisoras con URL HTTPS. La disponibilidad y los derechos de cada emisión dependen de la emisora; el usuario debe respetar las condiciones aplicables.

## Compilar el APK
1. Instala Android Studio y un JDK compatible.
2. Abre esta carpeta como proyecto.
3. Acepta instalar Android SDK Platform 35 y espera a que Gradle sincronice.
4. Conecta un teléfono Android o usa un emulador.
5. Para instalar y probar, pulsa Run. Para crear el APK, usa **Build > Build APK(s)**.
6. El APK de depuración normalmente aparece en `app/build/outputs/apk/debug/app-debug.apk`.

## Estado y límites
Este paquete es el código fuente del proyecto, no un APK compilado. En el entorno de generación no hay Android SDK ni Gradle instalado, por lo que no puedo compilar/verificar un APK instalable aquí. Se recomienda probar el comportamiento de las distintas emisoras en un dispositivo real. La reproducción en segundo plano con controles de notificación, carátulas, cola avanzada, gestión de permisos de streams y pruebas de seguridad automatizadas siguen pendientes. Esta versión todavía requiere compilación y pruebas reales en Android.

- Paquete: `com.sonora.music`
- Versión: 1.2
- Android mínimo: 6.0 (API 23)


## Alternativa: compilar el APK en GitHub Actions
Este proyecto incluye `.github/workflows/android.yml`. Para usarlo:
1. Crea un repositorio en GitHub y sube el contenido de esta carpeta (no el ZIP como un único archivo).
2. Abre la pestaña **Actions** del repositorio.
3. Ejecuta **Build Sonora APK** con **Run workflow** (o espera a que termine el primer push a `main`).
4. Abre la ejecución completada y descarga el artefacto **Sonora-debug-apk**.
5. Extrae el ZIP descargado; dentro estará `app-debug.apk`. Pásalo al teléfono e instálalo. Android puede pedir permiso para instalar apps de esa fuente.
6. Para habilitar catálogo Jamendo, configura el client ID en el código antes de compilar.

La compilación en GitHub requiere una cuenta y repositorio de GitHub. No se ha ejecutado ni verificado desde este entorno.


## Cambios de la versión 1.2
- Se conserva la biblioteca y el estado de favoritos usando almacenamiento privado de la aplicación.
- Se añade pantalla de privacidad y opción confirmada para borrar los registros guardados por Sonora.
- Se mejora la jerarquía visual de las filas de canciones y el estado de favorito.
- **Validación pendiente:** no se ha podido ejecutar Gradle/Android SDK en este entorno; no se declara compilación ni prueba funcional completada.

## Lista de pruebas antes de publicar
1. Compilar en GitHub Actions y confirmar que se genera `app-debug.apk`.
2. Instalar en Android 6+ y comprobar Inicio, Radio, Explorar y Biblioteca.
3. Importar un archivo local, cerrar y abrir la app; comprobar que sigue en biblioteca y se reproduce.
4. Marcar y desmarcar favoritos, reiniciar y comprobar que se conservan.
5. Borrar biblioteca desde Ajustes y verificar que el archivo original permanece en el teléfono.
6. Buscar emisoras con Internet apagado y comprobar que la app muestra un error sin cerrarse.
7. Probar emisoras HTTPS disponibles/no disponibles, llamadas entrantes, cambio de app y pantalla apagada.
8. Revisar privacidad de URLs, licencias del catálogo y permisos antes de distribuir.

## v1.3 — revisión técnica (pendiente de compilación real)

### Reproducción y controles
- Reproducción trasladada a `MediaPlaybackService`, separado de la pantalla para que continúe cuando la actividad no está visible.
- Notificación persistente mientras suena audio, con anterior, reproducir/pausar y siguiente.
- `MediaSessionCompat` expone metadatos y controles al sistema Android.
- Cola basada en la biblioteca local actual; los resultados online se añaden a la biblioteca al iniciar su reproducción.
- Mensajes de error para archivos/enlaces que no se pueden abrir; se puede intentar la pista siguiente manualmente.

### Carátulas
- Se conserva la URL HTTPS de carátula devuelta por Jamendo y se intenta cargar en las filas de resultados.
- La notificación usa una carátula de marca de reserva; la descarga de carátulas se limita a HTTPS y a 3 MB.
- Las radios y archivos sin carátula mantienen el icono de reserva. No hay caché persistente de imágenes todavía.

### Permisos y conexiones
- Permisos declarados: `INTERNET`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` y `WAKE_LOCK`.
- Servicio de reproducción no exportado, tráfico HTTP en claro deshabilitado y copias de seguridad de Android desactivadas.
- La importación de audio usa el selector del sistema y permisos URI persistentes, no acceso general al almacenamiento.
- La reproducción en segundo plano debe iniciarse por acción del usuario y mantener notificación visible.

### Licencias y publicación — bloqueos conocidos
- No existe todavía un sistema de licencia comercial/de activación de Sonora. No afirmar que la app tiene licenciamiento robusto ni publicar una promesa de DRM.
- Jamendo está desactivado por defecto hasta que el desarrollador configure su `client_id`. Antes de distribuir, revisar y aceptar los términos actuales de Jamendo, comprobar la licencia de cada pista y añadir atribución del artista/proveedor y enlace directo a la página de cada pista. No se debe asumir que todas las pistas permiten cualquier uso comercial.
- Radio Browser es un directorio de emisoras; cada emisora controla su flujo y sus derechos. Verificar las condiciones de cada stream y ofrecer contacto/reporte para emisoras no disponibles o no autorizadas.
- Revisar las políticas actuales de Google Play, los avisos de privacidad y el formulario de tipo de servicio en primer plano antes de publicar.

### Estado de verificación
- No se ha ejecutado Gradle/Android SDK en este entorno. La versión 1.3 es una revisión de código y **no está compilada ni probada en un dispositivo**.
- Pruebas pendientes: build limpio; Android 8–15; reproducción con pantalla apagada; controles en notificación y pantalla de bloqueo; llamadas/interrupciones de audio; desconexión de red; URI local revocado; cola vacía; saltos rápidos; errores de stream; batería; accesibilidad y permisos.

## v1.5 — pruebas y firma

La cola de reproducción sigue el orden de la biblioteca. La notificación y sesión multimedia ofrecen controles del reproductor y el servicio intenta saltar a la siguiente pista si una falla. Esto requiere validación en dispositivo; no se declara certificado.

Para obtener una APK de prueba, ejecuta el workflow `Build Sonora APK` en GitHub Actions y descarga `Sonora-debug-apk`. Es solo para pruebas.

Para release firmado, consulta `DISTRIBUTION_AUDIT.md`. La firma se activa únicamente con los secretos `SONORA_KEYSTORE_BASE64`, `SONORA_KEYSTORE_PASSWORD`, `SONORA_KEY_ALIAS`, `SONORA_KEY_PASSWORD` y la variable de repositorio `SONORA_RELEASE_SIGNING_ENABLED=true`. Nunca subas el keystore ni contraseñas al código.


## Estabilización v1.5

Consulta `ESTABILIZACION_Y_PRUEBAS.md` para ver las correcciones, limitaciones conocidas y criterios de aceptación. Esta versión no debe considerarse verificada hasta que compile en CI y supere las pruebas en un dispositivo real.


## Revisión 1.5.1 — diseño y listas locales

Esta revisión mantiene el proyecto Java y las integraciones existentes, y añade una interfaz oscura con acentos violeta/cian inspirada en la referencia visual del proyecto.

### Listas de reproducción locales
- Crear listas, cambiar su nombre y eliminarlas.
- Añadir o quitar canciones de la biblioteca local mediante selección múltiple.
- Reproducir una lista completa o comenzar desde una canción concreta.
- Guardar nombres y referencias a pistas en las preferencias privadas `sonora_playlists`.
- Las listas no copian ni borran los archivos de audio originales.

### Biblioteca local
- Selector de documentos de Android para importar varios archivos de audio.
- Intenta leer el título y el artista desde los metadatos del archivo; si no están disponibles, conserva el nombre del archivo y el texto de reserva.
- La radio y el catálogo online siguen siendo secciones independientes y no se han sustituido.

### Estado de verificación
La revisión estática del código y la validación del XML/ZIP no sustituyen una compilación Android. La compilación, la instalación y las pruebas reales de reproducción deben ejecutarse con Gradle y Android SDK (por ejemplo, mediante el flujo de GitHub Actions incluido).
