# Plan de Acción: Reparación de Carga de Imágenes de Personajes

Se ha detectado un fallo en la funcionalidad "Cargar más imágenes" dentro de la colección de personajes, causado por una gestión deficiente de los errores de red (HTTP 404/429) en la API de Jikan (MyAnimeList).

## Problema Identificado

La aplicación utiliza `URL(searchUrl).readText()`, lo que provoca una `FileNotFoundException` cuando la API de Jikan responde con cualquier código de error (como un 404 si no encuentra el personaje o un 429 por límite de velocidad). Además, la falta de un `User-Agent` puede estar causando bloqueos por parte del servidor.

## Cambios Propuestos

### 1. Robustez en el Repositorio de Jikan

#### [MODIFY] [JikanRepository.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/data/local/repository/JikanRepository.kt)
- Reemplazar `URL.readText()` por una implementación manual de `HttpURLConnection`.
- Añadir un encabezado `User-Agent` para identificar la aplicación.
- Implementar verificación del código de respuesta (`responseCode`).
- Si se recibe un error, leer el `errorStream` para logging y evitar la excepción `FileNotFoundException`.
- Añadir un pequeño reintento automático en caso de error 429 (Rate Limit).

### 2. Mejora en la Lógica de Búsqueda

#### [MODIFY] [ColeccionPersonajesAdapter.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/ui/gacha/ColeccionPersonajesAdapter.kt)
- Limpiar el nombre del personaje antes de enviarlo a la búsqueda (quitar paréntesis o sufijos raros).
- Mejorar el feedback al usuario (Toasts más descriptivos si la API falla por red o por no encontrar resultados).

### 3. Soporte para Superhéroes (Opcional pero recomendado)

- Actualmente, los superhéroes intentan buscarse en Jikan (que es solo de Anime). Si falla, se podría intentar una búsqueda alternativa o simplemente ocultar el botón para esa categoría si no hay fuente de imágenes extra.

## Plan de Verificación

### Verificación Manual
1. Abrir la colección y seleccionar un personaje de Anime (ej. Saeko Busujima).
2. Pulsar "Cargar más imágenes".
3. Verificar que las imágenes aparecen en el carrusel (ViewPager).
4. Probar con un personaje inexistente o con red inestable para confirmar que la app no crashea y muestra un mensaje adecuado.

### Logs
- Monitorear `GachaPool` o `JikanRepo` en Logcat para ver los códigos de respuesta reales de la API.
