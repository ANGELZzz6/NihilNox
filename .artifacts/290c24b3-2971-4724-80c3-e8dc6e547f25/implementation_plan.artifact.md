# Plan de Implementación: Mejora de Interacción y Memoria en Auto-Control AI

Este plan aborda el error de bucle en el cuestionario inicial y mejora la capacidad de la IA para recordar el historial del usuario y responder preguntas contextuales.

## User Review Required

> [!IMPORTANT]
> **Cambio en la Interfaz**: La pantalla de "Focus" ahora incluirá un campo de texto para que puedas hacerle preguntas libres a la IA (ej. sobre el gym o planes futuros).
>
> **Memoria de la IA**: Para que la IA "recuerde", le enviaremos un resumen de tus últimas 5 sesiones. Esto consume más tokens pero mejora mucho la precisión.

## Proposed Changes

### 1. Corrección del Bucle de Onboarding

#### [MODIFY] [AutoControlViewModel](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/ui/gacha/AutoControlViewModel.kt)
Introduciremos un estado explícito para la carga inicial. Esto evitará que la app asuma que no hay perfil mientras la base de datos está cargando.

### 2. Memoria y Consultas Contextuales

#### [MODIFY] [AutoControlRepository](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/data/local/repository/AutoControlRepository.kt)
*   **Historial**: Se incluirán las últimas sesiones en los prompts de la IA.
*   **Nueva función `preguntarIA`**: Permitirá enviar preguntas abiertas (como la del gimnasio) junto con el perfil y el historial.

### 3. Interfaz de Usuario (UI)

#### [MODIFY] [AutoControlActivity](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/ui/gacha/AutoControlActivity.kt)
*   Añadir un campo de texto y botón para "Preguntar a la IA".
*   Mostrar un contador de "Sesiones Totales" y "Última sesión exitosa".
*   Mejorar la visualización del plan actual.

#### [MODIFY] [activity_auto_control.xml](file:///C:/Users/elang/Documents/NihilNox/app/src/main/res/layout/activity_auto_control.xml)
Añadir los elementos visuales para la interacción de chat/pregunta.

## Verification Plan

### Automated Tests
*   Verificar que el prompt generado para `preguntarIA` contiene el historial de sesiones.
*   Probar que el ViewModel no emite "sin perfil" durante la carga inicial.

### Manual Verification
1.  **Bucle**: Abrir la sección de Focus teniendo un perfil creado y confirmar que NO salta al onboarding.
2.  **Memoria**: Realizar una sesión exitosa y luego preguntar a la IA "¿Cuándo fue mi última sesión?" (debería saberlo).
3.  **Contexto**: Preguntar algo específico como: "Tengo un examen mañana temprano, ¿es recomendable hacerlo hoy?" y verificar que la IA da un consejo basado en el rendimiento académico.
