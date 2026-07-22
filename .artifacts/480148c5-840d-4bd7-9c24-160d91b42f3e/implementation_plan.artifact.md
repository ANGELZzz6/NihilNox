# Integración de Hábitos en el Calendario

Este plan describe cómo integrar los hábitos diarios en la sección de calendario, permitiendo visualizar tanto tareas como hábitos en una sola vista unificada.

## User Review Required

> [!IMPORTANT]
> Los hábitos se mostrarán en la lista diaria junto con las tareas. Para los días pasados, se mostrará si el hábito fue completado (consultando los registros históricos). Para el día de hoy, se podrá marcar como completado directamente desde el calendario.

> [!NOTE]
> En el grid del calendario, se añadirán indicadores visuales adicionales para los hábitos activos en cada día.

## Proposed Changes

### [Domain & Data]

#### [MODIFY] [TareaViewModel.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/ui/gacha/TareaViewModel.kt)
- Inyectar `HabitoDao` y `RegistroHabitoDao` (o el repositorio de hábitos).
- Añadir un `StateFlow` o similar que combine tareas y hábitos activos.
- Función para obtener hábitos activos en una fecha específica basándose en `diasSemana`.
- Función para marcar hábito como completado (usando la lógica existente en `HabitosRepository`).

### [UI Components]

#### [NEW] [CalendarItem.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/ui/gacha/CalendarItem.kt)
- Clase sellada (`sealed class`) para representar tanto una `Tarea` como un `Habito` en la lista.

#### [MODIFY] [TareaResumenAdapter.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/ui/gacha/TareaResumenAdapter.kt)
- Adaptar para recibir una lista de `CalendarItem`.
- Mostrar iconos diferentes para distinguir tareas de hábitos.
- Usar el color del hábito (`burbujaColor`) en el indicador lateral.

#### [MODIFY] [CalendarioActivity.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/ui/gacha/CalendarioActivity.kt)
- Observar los hábitos del usuario.
- Combinar tareas y hábitos en la función `actualizarTareasDelDia`.

#### [MODIFY] [CalendarioAdapter.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/ui/gacha/CalendarioAdapter.kt)
- Recibir la lista de hábitos.
- En `actualizarIndicadores`, incluir puntos para los hábitos que correspondan a ese día de la semana.

## Verification Plan

### Manual Verification
1. **Visualización en Grid**: Confirmar que los días que tienen hábitos programados (ej. todos los días) muestran indicadores en el calendario.
2. **Lista Diaria**: Seleccionar un día y verificar que aparecen tanto las tareas de ese día como los hábitos que tocan ese día de la semana.
3. **Interacción**: Marcar un hábito como completado desde la lista del calendario y verificar que se sincroniza con la sección de Hábitos principal.
4. **Historial**: Ir a un día anterior y verificar que los hábitos se muestran como "completados" o "pendientes" según lo que ocurrió ese día real.
