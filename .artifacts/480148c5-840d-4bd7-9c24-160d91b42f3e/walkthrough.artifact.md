# Walkthrough: Sistema de Gacha Ultra Optimizado

He implementado un sistema de **Pool Local** para el Gacha que resuelve los problemas de lentitud y falta de variedad.

## Cambios Clave

### 1. Sistema de Pool Local (Cache Inteligente)
- **[PersonajePool.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/domain/model/PersonajePool.kt)**: Nueva tabla en la base de datos que actúa como una "reserva" de personajes listos para ser invocados.
- **[GachaPoolRepository.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/data/local/repository/GachaPoolRepository.kt)**: Este es el motor del nuevo sistema. Gestiona la descarga silenciosa de personajes en segundo plano para mantener siempre una reserva de entre 60 y 150 personajes.

### 2. Algoritmo de Diversidad
- El sistema de selección ahora prioriza títulos distintos. Si tiras 10 personajes, el algoritmo intenta activamente elegir personajes de series diferentes antes de permitir duplicados, garantizando una colección mucho más variada.

### 3. Velocidad e Independencia de Red
- **Tiradas Instantáneas**: Como los personajes ya están en el Pool Local, el Gacha responde al instante.
- **Modo Offline**: Si el usuario pierde la conexión, podrá seguir tirando del Gacha mientras queden personajes en la reserva local.
- **Recarga Silenciosa**: Cada vez que el usuario abre la app o realiza una tirada, el sistema rellena la reserva en segundo plano sin interrumpir la experiencia.

### 4. Optimizaciones de API
- **AniList**: Ahora realiza un "muestreo aleatorio" saltando entre múltiples páginas para asegurar que la reserva local sea diversa desde su origen.
- **Superhéroes e IGDB**: Se han optimizado para realizar cargas en bloque, reduciendo la cantidad de peticiones de red.

## Verificación Realizada

> [!NOTE]
> La primera vez que inicies la app, el sistema llenará el Pool automáticamente. Si intentas tirar el Gacha inmediatamente y el pool está vacío, se realizará una "carga de emergencia" (comportamiento anterior), pero a partir de ahí, todo será instantáneo.

> [!TIP]
> Puedes notar la mejora haciendo una tirada de 10. Verás personajes de videojuegos, superhéroes y animes mezclados mucho más rápido y con series menos repetitivas.
