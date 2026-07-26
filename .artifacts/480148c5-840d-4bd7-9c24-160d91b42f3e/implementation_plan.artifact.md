# Plan de Implementación: Modo "Lluvia Zen" (Easter Egg)

Este plan describe la implementación de un modo especial que se activa tras realizar 30 clics rápidos, permitiendo que múltiples frases caigan simultáneamente en posiciones aleatorias.

## Nueva Lógica de Disparo

### 1. Detección de Clics Rápidos
- Se añadirán dos variables en `ZenRecordarActivity`: `contadorClicsRápidos` y `ultimoTiempoClic`.
- Si el tiempo entre clics es inferior a 500ms, el contador aumenta.
- Si pasa más de 1 segundo sin clics, el contador se reinicia.
- Al llegar a **30 clics**, se activa el **Modo Lluvia**.

## Cambios en la Interfaz y Comportamiento

### 2. Comportamiento Dual

#### Modo Normal ( < 30 clics )
- Mantiene el comportamiento actual: una sola frase en el centro que se limpia antes de mostrar la siguiente.

#### Modo Lluvia ( >= 30 clics )
- Cada clic genera un **nuevo** `TextView` en una posición `(X, Y)` aleatoria de la pantalla.
- **Posicionamiento Aleatorio**: Se calculará dinámicamente basándose en el ancho y alto del `rootZen`.
- **Independencia**: Cada frase tendrá su propio ciclo:
    1. `pop` (aparecer).
    2. `delay` (esperar el tiempo configurado).
    3. `fall` (caer y desvanecerse).
    4. `remove` (eliminarse del contenedor para liberar memoria).

### 3. Finalización del Modo
- Tras 5 segundos de inactividad (sin clics), el contador se reinicia y la siguiente frase volverá a aparecer en el centro (Modo Normal).

## Proposed Changes

#### [MODIFY] [ZenRecordarActivity.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/ui/gacha/ZenRecordarActivity.kt)
- Implementar la lógica del contador y el temporizador.
- Crear una función `spawnFraseAleatoria(texto: String)` que genere los `TextView` dinámicamente.
- Ajustar `mostrarSiguienteFrase()` para decidir qué modo usar.

## User Review Required

> [!IMPORTANT]
> ¿Quieres algún efecto visual extra (como un pequeño destello o cambio de color de fondo momentáneo) cuando se activen los 30 clics para que el usuario sepa que ha desbloqueado la "lluvia"?

## Plan de Verificación

### Verificación Manual
1. Abrir la pantalla Zen.
2. Hacer clics pausados: Confirmar que solo aparece una frase en el centro.
3. Hacer 30 clics muy rápidos: Confirmar que las frases empiezan a "brotar" por toda la pantalla.
4. Dejar de tocar por 5 segundos: Confirmar que el siguiente toque vuelve a poner la frase en el centro.
