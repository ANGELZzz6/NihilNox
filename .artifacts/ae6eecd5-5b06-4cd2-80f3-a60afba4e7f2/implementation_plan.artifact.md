# Plan: Persistencia de Sesión, Temporizador y Pulido de UI

Este plan aborda la recuperación de sesiones interrumpidas, la implementación de un temporizador de descanso y el refinamiento visual final antes de la fase de pruebas.

## User Review Required

> [!IMPORTANT]
> **Nueva Migración (v50):** Se creará una tabla `sesion_borrador` para guardar el estado de las series, pesos y reps en tiempo real. Esto evitará que pierdas tus datos si sales de la pantalla accidentalmente.
>
> **Temporizador de Descanso:** Se activará al tocar el tiempo de descanso en la tarjeta de detalles técnicos. Mostrará un cronómetro descendente con una alerta visual al terminar.

## Proposed Changes

### Data Layer

#### [NEW] [SesionDraftEntity.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/domain/model/ProgresionEntities.kt)
*   Añadir `SesionBorradorEntity` que guarde: `ejercicioId`, `jsonSeries`, `molestia`, `notas`.

#### [MODIFY] [AppDatabase.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/data/local/AppDatabase.kt)
*   Incrementar versión a 50.
*   Añadir `MIGRATION_49_50`.

---

### Logic Layer

#### [MODIFY] [ProgresionViewModel.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/ui/gacha/ProgresionViewModel.kt)
*   Implementar `guardarBorrador()` y `cargarBorrador()`.
*   Llamar a `guardarBorrador` cada vez que cambie un slider o se añada una serie.
*   Limpiar el borrador al pulsar "Guardar Sesión".

#### [MODIFY] [ProgresionActivity.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/ui/gacha/ProgresionActivity.kt)
*   **Temporizador:** Implementar `CountDownTimer` que se dispare desde la `cardDetalles`.
*   **Buscador:** Asegurar que el filtro funcione correctamente mientras escribes.

---

### UI Layer

#### [MODIFY] [activity_progresion.xml](file:///C:/Users/elang/Documents/NihilNox/app/src/main/res/layout/activity_progresion.xml)
*   Añadir un `TextView` de **Temporizador Gigante** (flotante o en la cabecera) que aparezca solo cuando esté activo.
*   Ajustar márgenes del título "PROGRESIÓN" para evitar solapamiento total.

#### [MODIFY] [item_calendario_dia.xml](file:///C:/Users/elang/Documents/NihilNox/app/src/main/res/layout/item_calendario_dia.xml)
*   Asegurar que el estado `viewSeleccion` sea persistente y visible al cambiar de día.

## Verification Plan

### Manual Verification
1.  **Draft:** Empezar a anotar un ejercicio, salir al Dashboard, volver a Progresión y verificar que las series y pesos siguen ahí.
2.  **Timer:** Tocar "150s" en la tarjeta, verificar que inicia la cuenta atrás y suena/vibra al llegar a cero.
3.  **UI:** Verificar que el título no choca con los botones en un móvil pequeño.
4.  **Buscador:** Escribir una sola letra y ver cómo la lista de sugerencias se filtra al instante.
