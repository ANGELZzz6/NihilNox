# Plan de Sincronización entre Clasificación y Reproductor

Este plan detalla los ajustes necesarios para asegurar que el reproductor recupere su estado normal (lista completa de canciones) al regresar de la pantalla de clasificación, y verifica la integridad del flujo de datos tras los cambios de base de datos.

## User Review Required

> [!NOTE]
> **Comportamiento al regresar:** Al volver al reproductor, se restaurará la lista completa de todas las canciones disponibles en el servicio para que el usuario pueda navegar por toda su biblioteca de nuevo, sin interrumpir la canción que esté sonando en ese momento.

## Proposed Changes

### 1. Servicio de Música (`MusicaService.kt`)

#### [MODIFY] [MusicaService.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/ui/gacha/MusicaService.kt)
- Añadir método `restaurarListaCompleta()`:
    - Sincroniza la lista actual (`canciones`) con la lista maestra (`cancionesCompletas`).
    - Actualiza el `indiceActual` para que apunte a la misma canción que está sonando, evitando saltos.
    - No interrumpe la reproducción activa.

### 2. Pantalla de Reproductor (`ReproductorActivity.kt`)

#### [MODIFY] [ReproductorActivity.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/ui/gacha/ReproductorActivity.kt)
- Actualizar `onResume()`:
    - Llamar a `service.restaurarListaCompleta()`.
    - Refrescar la UI llamando a `actualizarLista(service.getCanciones())`.
    - Esto garantiza que, aunque vengamos de "Modo Clasificación" (donde solo había una canción o una lista filtrada), el reproductor vuelva a mostrar todo "normal".

### 3. Verificación de Integridad
- Validar que la migración de `genero` a `generos` no afecte la carga de canciones desde el disco. (Confirmado: el flujo de reproducción usa URIs de archivos y es independiente de los metadatos de la DB en la carga inicial).

---

## Plan de Verificación

### Pruebas Manuales
1. Abrir el **Reproductor** y ver la lista completa.
2. Entrar a **Clasificar Música** (donde se reproduce una sola canción al azar).
3. Clasificar una canción o simplemente presionar "Atrás".
4. Verificar que al volver al **Reproductor**, la lista vuelve a mostrar todas las canciones y la música sigue sonando si estaba activa.
5. Verificar que el botón "Siguiente" en el reproductor funciona correctamente tras volver (pasa a la siguiente de la lista completa).
