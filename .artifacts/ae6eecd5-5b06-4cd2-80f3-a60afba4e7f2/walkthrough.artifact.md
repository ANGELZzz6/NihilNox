# Walkthrough - Blindaje de Datos y Temporizador de Descanso

Esta actualización introduce el sistema de auto-guardado para evitar la pérdida de progresos durante el entrenamiento y añade una herramienta esencial de cronometrado.

## Cambios Realizados

### Blindaje de Datos (Auto-guardado)
- **Sistema de Borradores:** Se creó la tabla `sesion_borrador` (Migración v50). Cada cambio que realices (mover un slider, añadir una serie, escribir una nota) se guarda automáticamente en segundo plano.
- **Recuperación Inteligente:** Si sales de la app por accidente o navegas a otra sección, al volver a seleccionar el ejercicio, la app detectará el borrador y restaurará exactamente donde lo dejaste (series, pesos, repeticiones y notas).
- **Limpieza Automática:** El borrador se elimina solo cuando completas y guardas formalmente la sesión.

### Temporizador de Descanso
- **Activación por Toque:** En la tarjeta de detalles técnicos, toca el tiempo de descanso (ej. "120s").
- **Cronómetro Flotante:** Aparecerá un contador gigante en la esquina superior derecha indicando el tiempo restante.
- **Alerta de Finalización:** Al llegar a cero, el contador mostrará "¡TIEMPO!" y el teléfono vibrará para avisarte que es hora de la siguiente serie.

### Mejoras de UI y Navegación
- **Ajuste de Título:** Se reajustaron las restricciones del título "PROGRESIÓN" para asegurar que nunca se solape con los botones de acción, aplicando elipses si el nombre del ejercicio es muy largo.
- **Resaltado de Calendario:** Al tocar un día en el calendario, se ilumina con el fondo circular de selección de forma persistente.
- **Permisos:** Se añadió el permiso de vibración necesario para las alertas del temporizador.

## Verificación

> [!TIP]
> **Prueba de Auto-guardado:** Empieza a registrar un ejercicio, añade una serie y pon un peso. Sal al Dashboard de la app, vuelve a Progresión y selecciona el mismo ejercicio. Verás un mensaje de "Borrador recuperado".
>
> **Prueba de Temporizador:** Asegúrate de que el ejercicio tenga un tiempo de descanso definido. Tócalo y verifica que inicia la cuenta atrás. El temporizador desaparecerá solo unos segundos después de terminar.

render_diffs(file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/ui/gacha/ProgresionActivity.kt)
render_diffs(file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/ui/gacha/ProgresionViewModel.kt)
render_diffs(file:///C:/Users/elang/Documents/NihilNox/app/src/main/res/layout/activity_progresion.xml)
