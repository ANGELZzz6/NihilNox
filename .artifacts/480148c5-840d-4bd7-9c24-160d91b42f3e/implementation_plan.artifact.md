# Restaurar GIFs Reales (Nekobot) con Mejor Manejo de Errores

El objetivo es restaurar la funcionalidad de **Gifs Real** utilizando la API de Nekobot, pero implementando un sistema de filtrado más robusto para ignorar aquellos GIFs cuyos servidores estén caídos (error 521), evitando que aparezcan cuadros vacíos en la app.

## User Review Required

> [!NOTE]
> Nekobot utiliza servidores externos (`cdn.nekobot.xyz`) que a veces fallan. Restauraremos la fuente pero mejoraremos la lógica para que la app solo muestre los que realmente cargan.

## Proposed Changes

### [Component] Data & Models

#### [MODIFY] [DoujinModels.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/data/network/models/DoujinModels.kt)
- Re-introducir `NekobotResponse` para parsear la respuesta de la API.

### [Component] API Service

#### [MODIFY] [DoujinApiService.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/network/DoujinApiService.kt)
- Re-introducir la interfaz `NekobotApi`.

### [Component] Repository & ViewModel

#### [MODIFY] [DoujinRepository.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/data/local/repository/DoujinRepository.kt)
- Restaurar `getRandomRealGifs`.
- Mejorar la lógica: Se realizarán peticiones y se verificará brevemente si la URL es accesible antes de añadirla a la lista de resultados.

#### [MODIFY] [DoujinViewModel.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/ui/gacha/DoujinViewModel.kt)
- Restaurar la inicialización de `NekobotApi` y los métodos de búsqueda para "Gifs Real".

### [Component] UI

#### [MODIFY] [activity_doujin.xml](file:///C:/Users/elang/Documents/NihilNox/app/src/main/res/layout/activity_doujin.xml)
- Re-agregar el `RadioButton` para "Gifs Real".

#### [MODIFY] [DoujinActivity.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/ui/gacha/DoujinActivity.kt)
- Restaurar el manejo de la fuente en el selector.

### [Component] Downloads & Utils

#### [MODIFY] [DoujinDownloadWorker.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/workers/DoujinDownloadWorker.kt)
- Restaurar el soporte de descarga para Nekobot.

#### [MODIFY] [DoujinUtils.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/utils/DoujinUtils.kt)
- Re-agregar el Referer para `nekobot.xyz`.

## Verification Plan
1.  Seleccionar "Gifs Real".
2.  Verificar que se carguen GIFs.
3.  Observar si los que dan error 521 son filtrados o si la app intenta cargar otros en su lugar.
