# Renovación Visual: Dashboard y Burbuja de Hábitos

Se ha unificado la estética del dashboard y mejorado la interactividad de la burbuja flotante de hábitos.

## Cambios Realizados

### 1. Dashboard Unificado
- **Iconografía Coherente**: Se han creado e implementado nuevos iconos blancos para los botones de **Life Stream** e **HÁBITOS DIARIOS**.
    - [ic_habitos_white.xml](file:///C:/Users/elang/Documents/NihilNox/app/src/main/res/drawable/ic_habitos_white.xml)
    - [ic_lifestream_white.xml](file:///C:/Users/elang/Documents/NihilNox/app/src/main/res/drawable/ic_lifestream_white.xml)
- **Mejor Legibilidad**: Al usar el mismo estilo blanco que el resto de botones, la interfaz se siente más profesional y menos fragmentada.

### 2. Burbuja de Hábitos "Premium"
- **Capas de Diseño**: Se ha reconstruido el [layout_bubble.xml](file:///C:/Users/elang/Documents/NihilNox/app/src/main/res/layout/layout_bubble.xml) para incluir:
    - **Sombra Suave**: Un fondo oscuro difuminado para dar profundidad.
    - **Borde Blanco**: Un anillo semi-transparente que ayuda a la burbuja a destacar en cualquier fondo de pantalla.
- **Vida y Animación**: En [BurbujaHabitoService.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/ui/gacha/BurbujaHabitoService.kt):
    - **Efecto Pulso**: La burbuja ahora tiene una animación de latido constante y suave.
    - **Feedback al Completar**: Se ha añadido una animación de escala (pop) y una transición suave de opacidad para el checkmark verde al completar un hábito.

## Verificación

### Dashboard
- [x] Los iconos de Life Stream y Hábitos ahora son blancos y alineados con los demás.
- [x] El botón Gacha mantiene su color dorado único como elemento destacado.

### Burbuja Interactiva
- [x] Se observa el "latido" visual de la burbuja.
- [x] El borde blanco ayuda a ver la burbuja incluso sobre fondos del mismo color que el hábito.
- [x] La animación de "completado" es fluida y satisfactoria.
