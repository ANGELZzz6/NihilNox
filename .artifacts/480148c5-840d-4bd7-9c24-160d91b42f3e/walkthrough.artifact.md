# Walkthrough: Gestión de Galería y Precisión Safebooru

He implementado un conjunto de mejoras avanzadas para la colección de personajes, enfocándome en la precisión de las búsquedas y el control total del usuario sobre su galería.

## Mejoras Realizadas

### 1. Búsqueda por Serie (Máxima Precisión)
- **[SafebooruRepository.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/data/local/repository/SafebooruRepository.kt)**: Ahora el sistema combina el nombre del personaje con el título de su anime.
- **Lógica Inteligente**: Si buscas a "Miku", el sistema ahora le pregunta a Safebooru por "Miku + Serie", garantizando que las imágenes correspondan a la franquicia correcta y no a otros personajes con el mismo nombre.

### 2. Selector de Cantidad y Botón Permanente
- **Selector**: Al pulsar "Cargar más imágenes", ahora aparecerá un diálogo para elegir cuántas fotos quieres traer (5, 10, 15 o 20).
- **Recursividad**: El botón ya no se oculta. Puedes usarlo todas las veces que quieras para seguir ampliando la colección de un personaje específico.

### 3. Gestión y Borrado de Galería
- **Borrado Individual**: Si mantienes pulsada cualquier imagen en el carrusel de detalles del personaje, aparecerá una opción para eliminarla.
- **Gestión por Lotes**: He añadido un nuevo botón **"Gestionar"** al lado de "Fotos".
    - Al pulsarlo, se abre una cuadrícula con todas las imágenes extra.
    - Puedes seleccionar múltiples imágenes mediante un toque y borrarlas todas de una vez con el botón "Borrar Seleccionadas".
- **Protección de Datos**: El sistema impide borrar la imagen principal del personaje para evitar errores de visualización en la lista general.

## Verificación Realizada

> [!NOTE]
> Se han añadido métodos a `ImagenPersonajeDao` para soportar borrados masivos (`deleteBatch`), optimizando el rendimiento de la base de datos al limpiar galerías grandes.

> [!TIP]
> ¡Pruébalo! Abre un personaje, carga 20 imágenes y usa el nuevo gestor para borrar las que no te gusten o estén repetidas. Notarás que el carrusel se actualiza al instante.
