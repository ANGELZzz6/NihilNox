# Plan de Implementación: Nueva Fuente de Imágenes (Safebooru)

Debido a la inestabilidad de Jikan (MyAnimeList) y la falta de una API pública potente en AniList para galerías de personajes, implementaremos **Safebooru** como una fuente secundaria robusta y gratuita.

## ¿Qué es Safebooru?
Es una de las bases de datos de arte de anime más grandes y seguras (solo contenido apto para todos los públicos). No requiere API Key y es extremadamente rápida.

## Proposed Changes

### 1. Nuevo Repositorio de Imágenes

#### [NEW] [SafebooruRepository.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/data/local/repository/SafebooruRepository.kt)
- **Función `getImagenes(nombre: String)`**:
    1. Formateará el nombre del personaje al formato de etiquetas de Safebooru (ej: "Saeko Busujima" -> `saeko_busujima`).
    2. Realizará una petición a `https://safebooru.org/index.php?page=dapi&s=post&q=index&json=1&tags=NOMBRE_PERSONAJE`.
    3. Extraerá las URLs de las imágenes de alta calidad (`file_url`).
    4. Manejará errores de red y de cuotas de forma silenciosa.

### 2. Integración en la Colección

#### [MODIFY] [ColeccionPersonajesAdapter.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/ui/gacha/ColeccionPersonajesAdapter.kt)
- **Sistema de Multifuente**: Cuando el usuario pulse "Cargar más imágenes":
    1. Primero intentará con **Safebooru** (por ser más rápida y variada).
    2. Si Safebooru no devuelve nada, usará **Jikan (MAL)** como respaldo.
    3. Si ambos fallan, mostrará el mensaje de error.

### 3. Mejora de Formateo de Nombres
- Implementar un limpiador de nombres más agresivo para asegurar que las etiquetas de búsqueda sean precisas (ej: eliminar "Young", "Adult", "Version", etc.).

## Verification Plan

### Verificación Manual
1. Abrir la colección.
2. Seleccionar un personaje de anime.
3. Pulsar "Cargar más imágenes".
4. Verificar que aparecen imágenes de fanart y oficiales de alta calidad provenientes de Safebooru.
5. Comprobar la velocidad: Safebooru suele responder en menos de 1 segundo.

## Ventajas
- **Sin API Key**: No dependes de tokens que caducan.
- **Variedad**: Safebooru tiene miles de imágenes por personaje, no solo las 3-4 oficiales.
- **Gratis e Ilimitado**: Ideal para el uso que le damos en la app.
