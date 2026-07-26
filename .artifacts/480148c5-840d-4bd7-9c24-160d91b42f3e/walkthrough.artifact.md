# Walkthrough: Balance Inteligente de Géneros en el Gacha

He implementado un sistema de gestión de inventario inteligente para el Pool Local de personajes, garantizando que siempre haya disponibilidad de ambos géneros (femenino y masculino).

## Cambios Clave

### 1. Inteligencia de Inventario (DAO)
- Se han añadido métodos a la base de datos para monitorear el stock en tiempo real: `getMaleCount()` y `getFemaleCount()`. Esto permite a la app saber exactamente cuánta "munición" queda de cada tipo.

### 2. Recarga Proactiva y Balanceada
- **Detección de Desequilibrio**: El sistema ahora no solo mira el total de personajes, sino que vigila si un género cae por debajo de un umbral crítico (30 personajes).
- **Reponimiento Específico**: Si un usuario realiza muchas tiradas de un solo género (ej. solo femenino), la app detectará el agotamiento de stock y disparará una recarga prioritaria **específicamente de ese género** en segundo plano.
- **Mantenimiento del 50/50**: El objetivo del sistema es mantener siempre una proporción equilibrada (aprox. 75 personajes de cada género) dentro del pool de 150.

### 3. Optimización de la Experiencia de Usuario
- Gracias a este balanceo dinámico, las tiradas filtradas (Femenino/Masculino) serán **instantáneas casi el 100% de las veces**, eliminando la necesidad de recurrir a la carga de red lenta durante la tirada.

## Verificación

> [!NOTE]
> El sistema de balanceo se activa automáticamente cada vez que se abre la app o después de cualquier invocación del Gacha, asegurando que el pool se mantenga saludable sin intervención del usuario.

> [!TIP]
> Puedes observar en los logs (bajo la etiqueta `GachaPool`) cómo la app informa del estado actual del inventario: `Estado Pool - Total: 150, M: 75, F: 75`.
