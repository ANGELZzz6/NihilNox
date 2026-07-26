# Walkthrough: Pantalla Zen "RECALL"

He implementado la nueva pantalla "Zen" para recordar frases y palabras, integrándola en el Dashboard principal.

## Cambios Realizados

### 1. Persistencia (Room)
- **[FraseZen.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/domain/model/FraseZen.kt)**: Nueva entidad para almacenar las frases.
- **[FraseZenDao.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/data/local/FraseZenDao.kt)**: Operaciones CRUD (Insertar, Eliminar, Obtener Aleatoria).
- **[AppDatabase.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/data/local/AppDatabase.kt)**: Actualizada a la versión 38 con una migración automática para la nueva tabla.

### 2. Interfaz de Usuario
- **[activity_zen_recordar.xml](file:///C:/Users/elang/Documents/NihilNox/app/src/main/res/layout/activity_zen_recordar.xml)**:
    - Fondo negro absoluto (`#000000`).
    - Texto blanco central con estilo minimalista.
    - Botón de agregar sutil en la esquina inferior derecha.
- **[Dashboard Activity](file:///C:/Users/elang/Documents/NihilNox/app/src/main/res/layout/activity_dashboard.xml)**: Añadido el botón "RECALL" justo debajo de "FALL", manteniendo la estética de los otros botones (icono dorado `ic_sparkles`).

### 3. Lógica y Animaciones
- **[ZenRecordarActivity.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/ui/gacha/ZenRecordarActivity.kt)**:
    - **Inicio Silencioso**: Pantalla totalmente negra al abrir.
    - **Efecto de Gravedad**: Ahora las frases no se quedan estáticas. Después de 3 segundos, la frase "cae" hacia la parte inferior de la pantalla mientras se desvanece, regresando la pantalla al negro absoluto.
    - **Modo Lluvia Zen (Combo 30x)**: Se ha implementado un modo especial. Si el usuario realiza **30 clics rápidos** (menos de 600ms entre ellos), la pantalla entra en "Modo Lluvia".
    - **Comportamiento Multitarea**: En el modo lluvia, cada clic genera una nueva frase en una posición aleatoria de la pantalla. Las frases no se reemplazan entre sí, permitiendo tener múltiples pensamientos cayendo al mismo tiempo.
    - **Gestión Inteligente de Memoria**: Cada frase generada dinámicamente se elimina automáticamente del sistema una vez que termina su animación de caída.
    - **Retorno a la Calma**: Si el usuario deja de interactuar por 5 segundos, el contador se reinicia y la app vuelve al modo de frase única central.

## Verificación

> [!IMPORTANT]
> El modo lluvia respeta la configuración de velocidad de caída elegida por el usuario. Si aumentas la velocidad en los ajustes, la "lluvia" caerá más rápido también.

> [!TIP]
> ¡Intenta llegar al combo de 30! Verás cómo tu pantalla se llena de pensamientos flotantes que caen al abismo.

> [!TIP]
> Observa cómo la frase parece "pesar" y caer al vacío después de que terminas de leerla. Esto refuerza el concepto de dejar ir los pensamientos.
