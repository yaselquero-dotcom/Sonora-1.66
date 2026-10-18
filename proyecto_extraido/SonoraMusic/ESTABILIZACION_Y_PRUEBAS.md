# Sonora 1.5 — estabilización y criterios de aceptación

## Correcciones de código de esta revisión

- Se corrigieron los imports de `MediaSessionCompat`, `PlaybackStateCompat`, `MediaMetadataCompat` y `MediaStyle` para utilizar los paquetes de Android Support Media compatibles con la dependencia `androidx.media:media`.
- Se añadió la consulta segura del estado del reproductor para evitar excepciones cuando `MediaPlayer` ya no está en un estado válido.
- Se evita reiniciar el contador de pistas fallidas en cada `onPrepared`, lo que podía producir saltos repetidos indefinidamente cuando una pista se preparaba pero fallaba después.
- La actualización asíncrona de carátulas vuelve al hilo principal antes de consultar la cola o actualizar la notificación y evita actualizarla si la pista ya cambió o el servicio se destruyó.
- Se añadió el permiso de notificaciones y la solicitud en Android 13 o posterior. Si el usuario lo deniega, la reproducción no debe depender de que lo conceda; la visibilidad de la notificación puede variar según Android.
- Se evita lanzar búsquedas remotas con consultas vacías.
- Al retirar la actividad de recientes, el servicio conserva una cola activa o pausada y la persiste; solo se detiene si el usuario ya detuvo la reproducción o no existe cola.
- Se incrementó la versión a 1.5.

## Limitaciones conocidas

- La cola de reproducción todavía se deriva del orden de la biblioteca; no es una cola independiente con reordenación.
- La recuperación automática intenta saltar pistas que fallan, pero no sustituye las pruebas con archivos dañados, URLs inaccesibles, cambios de red e interrupciones de audio.
- Las carátulas remotas están limitadas por tamaño, dimensiones y tiempo de conexión; aún falta validar rendimiento en dispositivos de gama baja y con bibliotecas grandes.
- El almacenamiento de URI locales depende de que el proveedor de documentos permita mantener el permiso persistente. Los archivos eliminados o permisos revocados deben tratarse como pistas no disponibles.
- No se ha verificado el sistema de licencias comerciales de música ni se incluye una licencia propia de activación de Sonora.

## Pruebas funcionales requeridas en dispositivo

1. Importar uno y varios archivos locales, cerrar y abrir la app y comprobar que la biblioteca se conserva.
2. Reproducir, pausar, reanudar, avanzar, retroceder y detener desde la app y desde la notificación.
3. Bloquear la pantalla durante al menos 15 minutos y comprobar continuidad; probar auriculares Bluetooth, desconexión de auriculares y una interrupción de audio.
4. Retirar la app de recientes mientras reproduce y mientras está pausada; verificar el comportamiento esperado de la notificación.
5. Probar una pista inexistente, URI con permiso revocado, URL HTTPS inválida, red desconectada durante reproducción y emisora que deja de responder.
6. Probar carátulas correctas, URL que devuelve 404, redirección, archivo de imagen corrupto, imagen superior a 2 MB y cambio rápido entre pistas.
7. Denegar el permiso de notificaciones en Android 13+ y comprobar que la app informa correctamente del comportamiento sin cerrarse.
8. Probar biblioteca vacía, una sola pista y múltiples pistas; verificar que los saltos no crean un ciclo infinito tras fallos.
9. Ejecutar compilación debug y lint en CI; revisar el resultado y corregir todos los errores antes de distribuir.
10. Crear la clave de firma de producción de forma segura, configurar secretos de CI y verificar la firma del APK de release con `apksigner verify --verbose`.

## Estado de verificación

La fuente se ha revisado y actualizado, pero en el entorno de trabajo no está instalado Gradle ni hay un Android SDK disponible. Por tanto, esta revisión **no se considera compilada, instalada ni probada en dispositivo** hasta que el workflow de CI y las pruebas manuales anteriores terminen correctamente.
