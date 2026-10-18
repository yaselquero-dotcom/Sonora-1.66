# Sonora Music 1.5.1 — diseño y listas locales

## Objetivo
Alinear la interfaz con la referencia aportada: fondo azul casi negro, paneles redondeados, contornos cian/azules y acentos violeta/menta. Mantener la implementación Java actual y las funciones existentes.

## Cambios de interfaz
- Cabecera SONORA con subtítulo y símbolo musical.
- Tarjeta de bienvenida en degradado azul/violeta con accesos directos a Música local y Listas.
- Navegación horizontal desplazable para Inicio, Local, Listas, Radio y Explorar.
- Mini reproductor destacado con título, artista/fuente y controles anterior, reproducir/pausar y siguiente.
- Filas de canciones y tarjetas con fondos oscuros, bordes suaves y mejor contraste.
- Estados vacíos que indican qué hacer cuando aún no hay canciones o listas.

## Listas locales
- Crear una lista con nombre.
- Renombrar o eliminar listas.
- Añadir y quitar varias canciones locales desde un selector de selección múltiple.
- Reproducir una lista o iniciar desde una canción concreta.
- Persistencia privada en SharedPreferences mediante JSON.
- Las listas guardan referencias a las pistas de la biblioteca; no duplican ni eliminan archivos originales.
- Si una pista deja de estar accesible o se elimina de la biblioteca, se omite al mostrar/reproducir la lista.

## Importación de música
Se intenta obtener el nombre visible del documento y, cuando el proveedor lo permite, los metadatos de título y artista con `MediaMetadataRetriever`. Si no hay metadatos accesibles se utilizan valores de reserva.

## Compatibilidad y preservación
Se conservan la radio Radio Browser, el catálogo Jamendo, favoritos, la biblioteca local, la cola/servicio multimedia y las notificaciones. El catálogo Jamendo sigue necesitando un client ID válido.

## Pruebas pendientes
- Compilar `assembleDebug` y ejecutar `lintDebug` con Android SDK 35 y Gradle 8.9.
- Instalar en Android y comprobar el selector de archivos.
- Crear, renombrar, eliminar y restaurar listas tras cerrar y reabrir.
- Añadir pistas, reproducir desde el inicio y desde una pista intermedia, comprobar anterior/siguiente y la reproducción con pantalla apagada.
- Verificar que las canciones no disponibles se omiten sin bloquear la pantalla.
- Probar en diferentes tamaños de pantalla y con nombres de canciones largos.

**No se declara compilación ni funcionamiento verificado en un dispositivo hasta ejecutar estas pruebas.**
