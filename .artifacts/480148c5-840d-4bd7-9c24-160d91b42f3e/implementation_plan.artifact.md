# Plan de Acción: Corrección de Errores y Robustez de Género

Tras analizar los logs, he identificado que el sistema aún estaba intentando ejecutar código antiguo y que hay inconsistencias en el filtrado por género y manejo de errores de APIs.

## Problemas Identificados

1.  **Código Desactualizado**: Los logs muestran llamadas a `fetchPersonajeAleatorio`, método que fue eliminado en la última actualización. Esto indica que el proyecto necesita una limpieza/sincronización.
2.  **Filtro de Género Sensible**: La búsqueda en la base de datos local para el modo Femenino/Masculino podría estar fallando si los strings no coinciden exactamente (ej. "Female" vs "female").
3.  **Manejo de Errores de Red**: Aunque se añadieron `try-catch`, algunos errores de "Archivo no encontrado" (404) en la API de superhéroes están ensuciando el log.
4.  **Autenticación IGDB (401)**: El token de IGDB parece haber expirado o es inválido.

## Cambios Propuestos

### 1. Mejora en la Base de Datos Local

#### [MODIFY] [PersonajePoolDao.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/data/local/PersonajePoolDao.kt)
- Cambiar la consulta de género a `COLLATE NOCASE` para que no importe si es mayúscula o minúscula.

### 2. Refuerzo de GachaPoolRepository

#### [MODIFY] [GachaPoolRepository.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/data/local/repository/GachaPoolRepository.kt)
- Normalizar los strings de género a un formato estándar ("Male", "Female") antes de guardarlos en el Pool.
- Mejorar el logging para que los errores de API sean más descriptivos pero no rompan la ejecución.

### 3. Limpieza de Repositorios

#### [MODIFY] [SuperheroRepository.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/data/local/repository/SuperheroRepository.kt)
- Asegurar que el `try-catch` sea lo más específico posible para evitar que los 404 de IDs inexistentes causen ruidos innecesarios.

## Plan de Verificación

### Sincronización Forzada
- Solicitar al usuario que realice un **"Clean Project"** y **"Rebuild Project"** en Android Studio para asegurar que el código antiguo desaparezca.

### Pruebas de Género
- Tirar Gacha Femenino y Masculino y verificar en los logs que el Pool devuelve personajes correctamente filtrados sin importar el origen (AniList, IGDB o Superheros).
