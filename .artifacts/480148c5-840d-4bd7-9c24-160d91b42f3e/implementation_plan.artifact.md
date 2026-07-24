# Dashboard Premium: Animaciones "Bubble" y Refinamiento Visual

Este plan eleva la calidad del Dashboard implementando micro-interacciones avanzadas y un sistema de animaciones orgánicas ("respiración") que hacen que el panel se sienta vivo y fluido.

## User Review Required

> [!IMPORTANT]
> Implementaremos un **efecto de respiración (breathing)** individual para cada tarjeta. Esto significa que cada elemento del dashboard se expandirá y contraerá sutilmente de forma asíncrona, simulando el comportamiento de burbujas flotantes.

> [!TIP]
> Mejoraremos el estilo de las tarjetas añadiendo un **borde de cristal (glassmorphism)** más refinado y sombras internas que darán una mayor sensación de profundidad.

## Proposed Changes

### [Visual Styling]

#### [MODIFY] [bg_card_bento.xml](file:///C:/Users/elang/Documents/NihilNox/app/src/main/res/drawable/bg_card_bento.xml)
- Añadir un sutil gradiente radial o lineal para simular profundidad.
- Refinar el borde para que sea más "metálico/premium".

#### [NEW] [bg_card_glass_light.xml](file:///C:/Users/elang/Documents/NihilNox/app/src/main/res/drawable/bg_card_glass_light.xml)
- Variante más clara para tarjetas secundarias (Life Stream, Games).

### [Animations & Logic]

#### [MODIFY] [DashboardActivity.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/ui/gacha/DashboardActivity.kt)
- **Sistema de Respiración**:
    - Crear una función `animarRespiracionBento(view: View, delay: Long)` que aplique una escala de 1.0 a 1.02 de forma infinita con `AccelerateDecelerateInterpolator`.
    - Aplicar esta animación a cada tarjeta del grid y de la lista con un desfase temporal para evitar sincronía perfecta.
- **Pop-In Entrance**:
    - Actualizar `iniciarAnimacionesEntrada` para usar `OvershootInterpolator`, haciendo que los elementos aparezcan "inflandose" como burbujas.
- **Feedback Táctil**: Refinar `animarBoton` para que el escalado sea más elástico.

### [Layout UI]

#### [MODIFY] [activity_dashboard.xml](file:///C:/Users/elang/Documents/NihilNox/app/src/main/res/layout/activity_dashboard.xml)
- Ajustar márgenes y paddings para dar más "aire" entre los elementos, reforzando la estética Bento.

## Verification Plan

### Manual Verification
1. **Sensación de Vida**: Observar el dashboard durante 10 segundos y confirmar que todas las tarjetas tienen un movimiento sutil e independiente.
2. **Entrada**: Reiniciar la actividad y verificar que los elementos aparecen con un rebote suave (efecto bubble).
3. **Consistencia**: Comprobar que no hay saltos visuales al navegar entre el dashboard y otras pantallas.
