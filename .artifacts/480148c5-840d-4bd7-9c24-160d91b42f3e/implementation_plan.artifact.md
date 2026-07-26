# Plan de Implementación: Gacha Ultra-Robusto y Lluvia de Personajes

Este plan describe la fase final de optimización del sistema de Gacha, asegurando que la carga sea fluida, en segundo plano y resistente a fallos de las APIs externas.

## User Review Required

> [!IMPORTANT]
> Se implementará un **Sistema de Emergencia Local**. Si todas las APIs fallan y el pool está vacío, la app usará un conjunto de 20 personajes "clásicos" guardados internamente para que el usuario nunca vea una pantalla de error.

> [!TIP]
> La recarga del pool se optimizará para ser totalmente asíncrona, evitando cualquier micro-tirón en la UI incluso en dispositivos de gama baja.

## Proposed Changes

### 1. Robustez de APIs y Fallbacks

#### [MODIFY] [GachaPoolRepository.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/data/local/repository/GachaPoolRepository.kt)
- **Balanceo de Cuotas**: Si AniList falla, el sistema intentará compensar pidiendo más personajes a Superhéroes e IGDB automáticamente.
- **Manejo de Errores Silencioso**: Las excepciones de red durante la recarga no afectarán al usuario; simplemente se reintentará en la próxima oportunidad.
- **Local Fallback**: Integrar una lista estática de personajes como último recurso si el pool está vacío y no hay red.

### 2. Optimización de Carga en Segundo Plano

#### [MODIFY] [AnimeViewModel.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/ui/gacha/metas/viewmodels/AnimeViewModel.kt)
- Asegurar que la recarga inicial y post-tirada se ejecute en un contexto de corrutina de baja prioridad (`Dispatchers.IO`) y no bloquee el flujo principal.

### 3. Mejora de Variedad (Diversidad Garantizada)

#### [MODIFY] [GachaPoolRepository.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/data/local/repository/GachaPoolRepository.kt)
- Refinar el algoritmo de selección para que, en caso de fallos parciales de APIs, siga priorizando la mezcla de categorías (Anime, Superhéroe, Videojuego).

## Verification Plan

### Manual Verification
1. **Prueba de Modo Avión**: Vaciar el pool manualmente (o mediante código temporal) y entrar en modo avión. Verificar que el Gacha muestra los personajes del "Local Fallback".
2. **Prueba de Fallo de API**: Simular un error en la API de AniList (cambiando la URL temporalmente) y verificar que la tirada se completa usando solo Superhéroes y Videojuegos sin que el usuario note el error.
3. **Fluidez UI**: Verificar que mientras el pool se recarga en segundo plano, las animaciones del Dashboard y el Gacha siguen siendo suaves a 60fps.

### Automated Tests
- Test de estrés: Ejecutar 50 tiradas seguidas y verificar que el Pool se mantiene estable y la diversidad de series es alta.
