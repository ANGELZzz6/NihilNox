# Mejoras de UX y Sincronización de Música

Este plan aborda la falta de feedback visual al iniciar una partida de solitario y el problema de sincronización de la lista de canciones tras una descarga.

## Proposed Changes

### [Solitaire UX]

#### [MODIFY] [activity_difficulty_selection.xml](file:///C:/Users/elang/Documents/NihilNox/app/src/main/res/layout/activity_difficulty_selection.xml)
- Envolver el contenido en un `FrameLayout` para permitir capas.
- Añadir un `loadingOverlay` con un `ProgressBar` y el texto "Iniciando partida..." que cubra la pantalla al seleccionar una dificultad.

#### [MODIFY] [DifficultySelectorActivity.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/ui/gacha/DifficultySelectorActivity.kt)
- Obtener la referencia de `loadingDifficulty`.
- Mostrar el overlay con una animación de fundido (fade-in) justo después de la animación de pulso de la tarjeta seleccionada.

### [Music Synchronization]

#### [MODIFY] [ReproductorActivity.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/ui/gacha/ReproductorActivity.kt)
- **Fix `onResume`**: Actualmente solo refresca la lista si el servicio está vacío. Se cambiará para que siempre llame a `cargarCancionesDescargadas()`, asegurando que cualquier archivo nuevo en la carpeta "Playlists" sea detectado al volver a la pantalla.
- Asegurar que `cargarCancionesDescargadas` notifique al adaptador incluso si el servicio ya tenía canciones, para mostrar las nuevas.

## Verification Plan

### Manual Verification
1. **Solitario**: Entrar en el selector de dificultad, pulsar "Normal". Verificar que aparece el mensaje "Iniciando partida..." antes de que cambie la pantalla a la mesa de juego.
2. **Reproductor**:
    - Abrir el Reproductor (ver lista actual).
    - Salir y descargar una canción nueva desde YouTube.
    - Volver al Reproductor. Verificar que la canción recién descargada aparece automáticamente en la sección correspondiente (ej. "Descargas YouTube") sin reiniciar la app.
