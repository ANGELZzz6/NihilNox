# Plan: Pulido Final, Búsqueda y Navegación Visual

Este plan aplica los ajustes finales de UI, añade búsqueda de ejercicios y mejora la respuesta visual en el calendario antes de la fase de pruebas.

## User Review Required

> [!IMPORTANT]
> **Cambio de Selector:** El `Spinner` de ejercicios será reemplazado por un campo de búsqueda inteligente (`AutoCompleteTextView`). Esto permite escribir para filtrar rápidamente entre muchos ejercicios.
>
> **Reajuste de Barra Superior:** Se moverá el título "PROGRESIÓN" a la izquierda para evitar que se solape con los botones de acción (Calendario, Opciones, Añadir).

## Proposed Changes

### UI Layer

#### [MODIFY] [activity_progresion.xml](file:///C:/Users/elang/Documents/NihilNox/app/src/main/res/layout/activity_progresion.xml)
*   Cambiar la alineación del título "PROGRESIÓN" de `centerInParent` a `layout_toEndOf="@id/btnBackProgresion"`.
*   Reemplazar `Spinner` por un `TextInputLayout` con `MaterialAutoCompleteTextView` para habilitar la búsqueda por texto.

#### [MODIFY] [ProgresionActivity.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/ui/gacha/ProgresionActivity.kt)
*   Actualizar la lógica de `setupObservers` para llenar el `AutoCompleteTextView` y manejar la selección filtrada.
*   Asegurar que el teclado se oculte al seleccionar un ejercicio.

#### [MODIFY] [ProgresionCalendarioActivity.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/ui/gacha/ProgresionCalendarioActivity.kt)
*   Mejorar el `ProgresionCalendarioGridAdapter` para que refresque visualmente el "brillo" o resalte del día seleccionado al hacer clic.

---

### Verification & VCS

#### [ACTION] Build & Git
1.  Ejecutar `gradle_build` para asegurar integridad.
2.  `git add .`
3.  `git commit -m "feat: implementacion completa de progresion con busqueda y calendario"`
4.  `git push origin main` (o la rama activa).

## Verification Plan

### Manual Verification
1.  **Búsqueda:** Escribir "Press" en el nuevo buscador y verificar que solo aparecen ejercicios que contienen esa palabra.
2.  **Overlap:** Verificar en un teléfono con pantalla estrecha que el título no choca con los botones.
3.  **Calendario:** Tocar el día 15, luego el 16, y verificar que el círculo de selección se mueve correctamente.
