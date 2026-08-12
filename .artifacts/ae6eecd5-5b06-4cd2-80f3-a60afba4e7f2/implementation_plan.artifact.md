# Plan: Botón de Temporizador y Refuerzo de Borradores

Este plan añade un botón explícito para iniciar el descanso y asegura que la persistencia de borradores sea infalible ante navegaciones accidentales.

## User Review Required

> [!IMPORTANT]
> **Botón "Iniciar Descanso":** Se añadirá un botón destacado debajo de la información de descanso en la tarjeta técnica. Al pulsarlo, el temporizador comenzará automáticamente con el tiempo configurado para ese ejercicio.
>
> **Persistencia de Estado:** Se verificará que al cambiar de ejercicio o salir de la pantalla, el estado de los sliders se mantenga intacto mediante el sistema de borradores ya implementado, asegurando que se carguen al volver a seleccionar el ejercicio.

## Proposed Changes

### UI Layer

#### [MODIFY] [activity_progresion.xml](file:///C:/Users/elang/Documents/NihilNox/app/src/main/res/layout/activity_progresion.xml)
*   Añadir `MaterialButton` llamado `btnIniciarDescanso` dentro de `cardDetallesTecnicos`, debajo de la fila de Descanso/Tempo.
*   Estilo: Botón pequeño con ícono de "Play" o reloj, color cian/primario.

### Logic Layer

#### [MODIFY] [ProgresionActivity.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/ui/gacha/ProgresionActivity.kt)
*   Conectar `btnIniciarDescanso` para disparar `iniciarTimer(segundos)`.
*   Asegurar que el temporizador sea visible y legible.
*   **Refuerzo de Borradores:** Asegurar que `autoGuardarBorrador()` cubra todos los cambios posibles en la UI.

## Verification Plan

### Manual Verification
1.  **Botón Timer:** Seleccionar un ejercicio con descanso (ej. 120s), pulsar el nuevo botón "INICIAR DESCANSO" y verificar que el contador empieza.
2.  **Prueba de Fuego (Borrador):**
    *   Seleccionar ejercicio.
    *   Cambiar peso de serie 1 a 85kg.
    *   Salir al Dashboard.
    *   Volver a Progresión.
    *   Seleccionar el mismo ejercicio.
    *   Verificar que la serie 1 sigue marcando 85kg.
