# Plan de Corrección del Widget de Calendario

El objetivo es solucionar el problema donde las tareas marcadas como completadas en el widget permanecen marcadas al día siguiente. Esto ocurre porque el widget utiliza el campo persistente `completada` de la entidad `Tarea`, el cual no se reinicia automáticamente para tareas recurrentes.

## Análisis del Problema
1.  **Estado Persistente:** Actualmente, al marcar una tarea en el widget, se actualiza `Tarea.completada` en la base de datos. Para tareas diarias o semanales, este campo sigue siendo `true` al día siguiente.
2.  **Falta de Validación por Fecha:** La lógica de visualización del widget no consulta la tabla `registros_tarea` para determinar si la tarea fue completada *específicamente hoy*.
3.  **Refresco Diario:** El widget se actualiza periódicamente (cada 30 min), pero no garantiza un refresco inmediato al cambiar el día (medianoche).

## Cambios Propuestos

### Componente: Datos y Persistencia
*   **TareaViewModel**: Actualizar la lógica de `marcarCompletada` para que sea consciente del registro diario.

### Componente: Widget de Calendario
#### [MODIFY] [WidgetCalendario.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/ui/gacha/WidgetCalendario.kt)
*   Modificar `marcarCompletada` para que determine el nuevo estado consultando si existe un registro en `registros_tarea` para el día actual.
*   Implementar un programador (usando `AlarmManager`) para forzar la actualización del widget exactamente a la medianoche.

#### [MODIFY] [CalendarioRemoteViewsService.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/ui/gacha/CalendarioRemoteViewsService.kt)
*   En `onDataSetChanged`, cruzar la lista de tareas con los registros de `registros_tarea` del día de hoy.
*   Actualizar el estado `completada` en memoria de los objetos `Tarea` antes de mostrarlos, asegurando que reflejen la realidad del día actual.

## Plan de Verificación

### Pruebas Manuales
1.  **Marcar Tarea:** Abrir el widget, marcar una tarea recurrente. Verificar que se muestra el check.
2.  **Persistencia:** Reiniciar el widget o esperar la actualización periódica. Verificar que sigue marcada.
3.  **Cambio de Día (Simulado):** Cambiar la fecha del dispositivo al día siguiente. Verificar que la tarea aparece desmarcada automáticamente en el widget.
4.  **Desmarcar:** Marcar y luego desmarcar una tarea. Verificar que el registro desaparece de la base de datos y el widget se actualiza.

## Preguntas Abiertas
*   ¿Deseas que las tareas de "una vez" (recurrencia = UNA_VEZ) también se desmarquen si no se completaron, o esas sí deben mantener su estado hasta que se marquen? (El plan actual asume que todas se rigen por el registro diario para consistencia en el calendario).
