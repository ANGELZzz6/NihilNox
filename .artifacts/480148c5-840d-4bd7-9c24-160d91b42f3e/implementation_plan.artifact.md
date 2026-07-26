# Plan de Implementación: Balance Inteligente de Géneros en el Pool

Este plan detalla cómo optimizar la recarga del Gacha para que siempre haya un stock saludable de personajes masculinos y femeninos, evitando que las tiradas específicas (solo mujeres o solo hombres) se vuelvan lentas por falta de reserva local.

## Problema Actual
Si un usuario solo tira personajes femeninos, agota el stock de ese género en el Pool Local. La recarga actual es genérica, por lo que el Pool podría llenarse mayoritariamente de hombres, forzando cargas lentas de red cuando se pidan mujeres de nuevo.

## Cambios Propuestos

### 1. Mejoras en la Persistencia

#### [MODIFY] [PersonajePoolDao.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/data/local/PersonajePoolDao.kt)
- Añadir métodos para contar específicamente cuántos personajes hay de cada género:
    - `getMaleCount(): Int`
    - `getFemaleCount(): Int`

### 2. Lógica de Recarga Equilibrada

#### [MODIFY] [GachaPoolRepository.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/data/local/repository/GachaPoolRepository.kt)
- Modificar `recargarPoolSiEsNecesario()` para:
    1. Obtener los conteos de hombres y mujeres.
    2. Identificar si algún género está por debajo de un umbral crítico (ej. menos de 30 personajes).
    3. Si un género está bajo, disparar una recarga **específica** para ese género en segundo plano.
    4. Mantener el balance ideal (ej. 50% hombres / 50% mujeres dentro del límite de 150).

### 3. Optimización de Segundo Plano
- Asegurar que el sistema no "sobrecargue" las APIs haciendo demasiadas peticiones seguidas, sino que rellene lo justo para mantener la reserva operativa.

## Verification Plan

### Verificación Manual
1. **Prueba de Agotamiento**: Vaciar manualmente los personajes femeninos del pool local.
2. **Revisión de Logs**: Observar cómo el sistema detecta el bajo stock de mujeres y dispara una recarga prioritaria de ese género.
3. **Prueba de Tirada**: Realizar una tirada de "Solo Femenino" y verificar que es instantánea a pesar del vaciado previo.
4. **Persistencia**: Confirmar que tras la recarga, el conteo en la base de datos vuelve a ser equilibrado (aprox. mitad y mitad).

## Open Questions
- ¿Consideras que una proporción de 50/50 es la ideal o prefieres priorizar más algún género en la reserva por defecto?
