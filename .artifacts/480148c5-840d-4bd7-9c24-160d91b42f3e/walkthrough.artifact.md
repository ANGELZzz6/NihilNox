# Walkthrough: Nueva Fuente de Imágenes (Safebooru)

He implementado una nueva fuente de imágenes de alta calidad para los personajes de anime, utilizando **Safebooru** como proveedor principal.

## Cambios Realizados

### 1. Motor de Búsqueda Safebooru
- **[SafebooruRepository.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/data/local/repository/SafebooruRepository.kt)**: Nuevo repositorio encargado de conectar con el API de Safebooru.
    - **Smart Search**: El sistema ahora es más inteligente. Si no encuentra al personaje por su nombre normal, intenta invertirlo (Apellido_Nombre) o buscar solo por el primer nombre. Esto aumenta drásticamente las posibilidades de encontrar imágenes.
    - **Manejo de Errores Silencioso**: Se ha corregido el error `End of input` gestionando correctamente las respuestas vacías del servidor.

### 2. Sistema Multifuente Inteligente
- **[ColeccionPersonajesAdapter.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/ui/gacha/ColeccionPersonajesAdapter.kt)**: Se ha actualizado la lógica del botón "Cargar más imágenes".
    - **Primaria**: Intenta obtener imágenes desde **Safebooru** primero, ya que es más rápido y ofrece mayor variedad artística.
    - **Secundaria (Fallback)**: Si Safebooru no devuelve resultados, el sistema recurre automáticamente a **Jikan (MyAnimeList)**.
    - Esto garantiza que casi siempre encuentres imágenes nuevas, incluso si una de las fuentes falla.

## Verificación

> [!NOTE]
> Las imágenes obtenidas de Safebooru suelen ser Fanarts y artes oficiales de alta resolución. El sistema filtra automáticamente cualquier contenido no apto, asegurando una galería segura y visualmente rica.

> [!TIP]
> Prueba a cargar imágenes con personajes de anime populares. Notarás que ahora aparecen muchas más opciones y de forma mucho más rápida que antes.
