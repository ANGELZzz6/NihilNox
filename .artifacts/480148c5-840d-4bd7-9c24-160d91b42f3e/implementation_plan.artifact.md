# Sincronización del Widget LifeStream

Este plan corrige el estado estático del widget LifeStream asegurando que se actualice en tiempo real cada vez que un hábito o tarea sea modificado, ya sea desde la app o desde la burbuja flotante.

## User Review Required

> [!IMPORTANT]
> Se habilitará la actualización cruzada: completar un hábito en la **burbuja** ahora refrescará automáticamente el gráfico de ondas en el **widget** de la pantalla de inicio.

## Proposed Changes

### [Core Synchronization]

#### [MODIFY] [BurbujaHabitoService.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/ui/gacha/BurbujaHabitoService.kt)
- Añadir `WidgetLifeStream.forzarActualizacion(applicationContext)` al método `confirmarYCerrar`.

#### [MODIFY] [HabitosViewModel.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/ui/gacha/HabitosViewModel.kt)
- Invocar `WidgetLifeStream.forzarActualizacion(getApplication())` en las funciones `marcarCompletado` y `eliminar`.

### [Widget Data Logic]

#### [MODIFY] [LifeStreamRemoteViewsService.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/ui/gacha/LifeStreamRemoteViewsService.kt)
- Actualizar `onDataSetChanged` para obtener el inicio del día actual correctamente.
- Usar el timestamp de hoy al llamar a `db.tareaDao().getTareasDelDia(hoy)` para obtener datos vigentes.

## Verification Plan

### Manual Verification
1. **Flujo de Burbuja**:
    - Lanzar burbuja de un hábito real.
    - Marcar como completado.
    - Ir a la pantalla de inicio y verificar que la onda del widget LifeStream ha crecido/cambiado.
2. **Eliminación**:
    - Eliminar un hábito desde la app.
    - Confirmar que desaparece inmediatamente de la lista del widget.
