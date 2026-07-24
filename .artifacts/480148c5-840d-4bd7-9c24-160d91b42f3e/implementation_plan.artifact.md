# Rediseño Dashboard: Estilo Bento & Material Next

Este plan detalla la implementación del nuevo diseño del Dashboard basado en el prototipo visual (HTML/Tailwind), adaptándolo a componentes nativos de Android para una experiencia fluida y coherente.

## User Review Required

> [!IMPORTANT]
> El cambio es radical: pasamos de una lista simple de botones a una **Bento Grid** (cuadrícula dinámica). Esto mejora la jerarquía visual y permite ver más información de un vistazo.

> [!TIP]
> Implementaremos tarjetas con estados dinámicos (como la barra de progreso en Nutrición y contadores de notas) para que el Dashboard no solo sea un lanzador, sino un panel de información real.

## Proposed Changes

### [Resources & Styling]

#### [MODIFY] [colors.xml](file:///C:/Users/elang/Documents/NihilNox/app/src/main/res/values/colors.xml)
- Añadir la paleta de colores del prototipo (Surface, Primary Container, Tertiary, etc.).

#### [NEW] [drawables](file:///C:/Users/elang/Documents/NihilNox/app/src/main/res/drawable/)
- `bg_card_bento.xml`: Fondo para las tarjetas del grid con bordes sutiles.
- `bg_gacha_banner.xml`: Degradado dorado premium para la acción principal.
- `ic_chevron_right.xml`: Icono de navegación para las listas.

### [Layout UI]

#### [MODIFY] [activity_dashboard.xml](file:///C:/Users/elang/Documents/NihilNox/app/src/main/res/layout/activity_dashboard.xml)
- **Header**: Avatar circular a la izquierda, textos de bienvenida y botón de ajustes a la derecha.
- **Sección Gacha**: Banner horizontal destacado.
- **Bento Grid (ConstraintLayout)**:
    - Tarjeta Reproductor (Cuadrante superior izquierdo).
    - Tarjeta Notas (Cuadrante superior derecho).
    - Tarjeta Nutrición (Fila completa con barra de progreso).
- **Sección Listado**: Perfil, Fall y Academia en una lista compacta.
- **Grid Secundario**: Hábitos y Calendario.
- **Bottom Bar**: Barra de navegación fija con desenfoque (blur).

### [Logic & Integration]

#### [MODIFY] [DashboardActivity.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/ui/gacha/DashboardActivity.kt)
- Vincular los nuevos IDs de tarjetas y botones.
- **Datos Dinámicos**: Implementar la carga de calorías actuales y conteo de notas para mostrarlos en las tarjetas.
- **Animaciones**: Actualizar la entrada en "cascada" para que coincida con la nueva disposición modular.

## Verification Plan

### Manual Verification
1. **Navegación**: Verificar que todos los módulos abren su actividad correspondiente al tocar las tarjetas.
2. **Estética**: Validar que los colores y contrastes coinciden con el diseño "Dark Premium" propuesto.
3. **Scroll**: Asegurar que en pantallas pequeñas el contenido se puede desplazar sin que se corten los elementos críticos.
