# Sonora — auditoría técnica v1.4

## Estado honesto
**No certificada para distribución.** La revisión de código no sustituye una compilación, instalación y pruebas en dispositivos reales. En este entorno no se ha ejecutado Gradle/Android SDK; la compilación de CI sigue siendo necesaria.

## Mejoras de código en v1.4
- Servicio de reproducción foreground `mediaPlayback`, sesión multimedia y controles en notificación/sistema.
- La notificación tiene anterior, reproducir/pausar, siguiente y acción de detener/cerrar.
- Cola persistida en preferencias privadas junto con el índice de la pista; la cola de la interfaz es el orden actual de la biblioteca.
- En caso de error de una pista, el servicio intenta pasar a la siguiente hasta recorrer la cola sin entrar en un bucle infinito.
- Valida las fuentes remotas como HTTPS y limita metadatos de texto.
- Intenta cargar la carátula HTTPS para la notificación; límites de tamaño, dimensiones y decodificación reducida. La pantalla mantiene una caché LRU en memoria para carátulas.
- El servicio no está exportado; HTTP sin cifrar está desactivado; las credenciales de firma no se guardan en el repositorio.
- Configuración de CI para debug y un trabajo de release firmado, activable tras configurar secretos y variable de repositorio.

## Configuración de firma de producción
1. Crear y custodiar un keystore de release en un dispositivo/entorno confiable. Guardar una copia de seguridad segura; no regenerarlo para futuras actualizaciones.
2. En GitHub → Settings → Secrets and variables → Actions, crear secretos `SONORA_KEYSTORE_BASE64`, `SONORA_KEYSTORE_PASSWORD`, `SONORA_KEY_ALIAS`, `SONORA_KEY_PASSWORD`.
3. `SONORA_KEYSTORE_BASE64` es el contenido del keystore codificado en Base64. Nunca subir el keystore, contraseñas ni claves privadas al repositorio o a un chat.
4. Crear variable de repositorio `SONORA_RELEASE_SIGNING_ENABLED=true` para habilitar el trabajo release.
5. Ejecutar Actions y comprobar que `apksigner verify --verbose` finaliza correctamente. Guardar la clave de firma con seguridad; una actualización debe usar la misma clave.

El artefacto debug es solo para pruebas y no es una entrega de producción. Si se publica en Google Play, revisar si corresponde Android App Bundle y Play App Signing.

## Pruebas funcionales obligatorias
- Android 8/10/13/14/15: reproducción, pausa, anterior/siguiente desde notificación y controles Bluetooth/auriculares.
- Pantalla apagada durante 30 minutos, modo ahorro, cambio Wi-Fi/datos, pérdida de red y reanudación.
- Fin natural de pista, archivos inexistentes, permiso URI revocado, stream 404, red lenta, formato inválido y errores consecutivos en cola.
- Carátula válida, 404, redirección, imagen corrupta, imagen sobredimensionada y URL HTTP; verificar fallback y que la UI no se bloquee.
- Matar proceso/recrear Activity, volver a abrir app, borrar cola y detener explícitamente la sesión.
- Verificar que notificación y sesión multimedia muestran siempre pista/estado correctos.
- Verificar persistencia de biblioteca, favoritos y cola tras reinicio del proceso y tras actualizar versión.
- Comprobar firma con `apksigner`, instalación/actualización y hash del artefacto final.

## Riesgos pendientes antes de distribuir
1. Jamendo: mostrar atribución y enlace a página de la pista/artista y confirmar términos/uso permitido antes de habilitar el catálogo en una distribución pública.
2. Radio: HTTPS no demuestra autorización para redistribuir el contenido ni garantiza que el proveedor no redirija a HTTP.
3. Licencia de Sonora: no hay sistema propio de activación/licencia implementado.
4. Revisar permisos y política de privacidad, requisitos actuales de Google Play, metadatos/atribución y política de eliminación de datos.
5. La cola se deriva de la biblioteca completa, no de una lista de reproducción editable/reordenable; no se persiste posición exacta en la pista ni se recupera automáticamente tras todos los tipos de muerte del proceso.
6. Las carátulas se almacenan en caché solo en memoria; no hay caché de disco ni pruebas automatizadas de instrumentación.
7. Las URL remotas se filtran por HTTPS antes de reproducir; validar redirecciones del reproductor y URLs finales en pruebas reales.
