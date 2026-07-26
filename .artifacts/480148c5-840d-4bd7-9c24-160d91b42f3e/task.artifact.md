# Tareas: Optimización de Gacha y Pool Local

- [x] **1. Persistencia (Room)**
    - [x] Crear entidad `PersonajePool` en `domain/model/PersonajePool.kt`.
    - [x] Crear DAO `PersonajePoolDao` en `data/local/PersonajePoolDao.kt`.
    - [x] Actualizar `AppDatabase.kt` con la nueva entidad y DAO (Versión 39).
- [x] **2. Repositorio de Pool**
    - [x] Crear `GachaPoolRepository.kt` para gestionar la pre-carga y diversidad.
    - [x] Adaptar `SuperheroRepository` e `IGDBRepository` para cargas más eficientes.
- [x] **3. Lógica de Negocio (ViewModel)**
    - [x] Modificar `AnimeViewModel` para consumir desde el Pool Local.
    - [x] Implementar la recarga silenciosa del Pool al iniciar la app.
- [x] **4. Verificación**
    - [x] Probar tiradas instantáneas (Pool lleno).
    - [x] Verificar diversidad de animes en tiradas de 10.
    - [x] Probar modo offline con pool pre-cargado.
