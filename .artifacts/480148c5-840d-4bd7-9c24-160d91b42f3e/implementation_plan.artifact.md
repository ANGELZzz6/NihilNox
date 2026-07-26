# Plan de Implementación: Gestión Avanzada de Imágenes y Precisión

Este plan detalla las mejoras para la búsqueda de imágenes (precisión por serie y selector de cantidad) y la nueva funcionalidad de gestión de galería (eliminación individual y por lotes).

## Proposed Changes

### 1. Motor de Búsqueda Safebooru (Precisión y Cantidad)

#### [MODIFY] [SafebooruRepository.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/data/local/repository/SafebooruRepository.kt)
- **Actualizar `getImagenes`**: Añadir parámetros `serie: String?` y `limit: Int`.
- **Búsqueda Combinada**: Implementar lógica para buscar `tag_personaje` + `tag_serie` como primera opción para evitar personajes de otras franquicias.

### 2. Interfaz de Colección y Selector

#### [MODIFY] [ColeccionPersonajesAdapter.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/ui/gacha/ColeccionPersonajesAdapter.kt)
- **Selector de Cantidad**: Al pulsar "Cargar más", mostrar un diálogo con opciones (5, 10, 15, 20).
- **Mantener Botón**: El botón de carga seguirá visible tras descargar imágenes.

### 3. Gestión de Galería (Eliminación)

#### [MODIFY] [ImagenPagerAdapter.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/ui/gacha/ImagenPagerAdapter.kt)
- Añadir callback `onLongClick` para detectar pulsaciones largas en las imágenes del carrusel.

#### [MODIFY] [ColeccionPersonajesAdapter.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/ui/gacha/ColeccionPersonajesAdapter.kt)
- **Eliminación Individual**: Al mantener pulsada una imagen en el carrusel, mostrar un diálogo de confirmación para borrarla.
- **Eliminación por Lotes**:
    - Añadir un nuevo botón "Gestionar Galería" en el diálogo del personaje.
    - Este botón abrirá una nueva vista (cuadrícula) con todas las imágenes extra del personaje.
    - Permitir seleccionar varias imágenes mediante checkboxes y eliminarlas de una sola vez de la base de datos.
- **Protección**: La imagen principal del personaje no podrá ser eliminada.

## Verification Plan

### Verificación Manual
1.  **Precisión**: Buscar imágenes para un personaje con nombre común y verificar que el filtro de serie funciona.
2.  **Selector**: Pedir exactamente 5 imágenes y verificar el límite.
3.  **Borrado Individual**: Mantener pulsada una imagen del carrusel y confirmar su eliminación.
4.  **Borrado por Lotes**: Abrir el gestor, marcar 3 imágenes repetidas y borrarlas. Verificar que desaparecen tanto del gestor como del carrusel.

## Ventajas
- **Galería Limpia**: El usuario puede depurar fácilmente imágenes repetidas o erróneas.
- **Experiencia de Usuario**: Mayor control sobre el contenido y la precisión de la app.
