# Tareas: Corrección de Errores y Robustez de Gacha

- [x] **1. Persistencia (Room)**
    - [x] Modificar `PersonajePoolDao.kt` para búsqueda de género insensible a mayúsculas (`COLLATE NOCASE`).
- [x] **2. Repositorio de Pool**
    - [x] Normalizar strings de género en `GachaPoolRepository.kt` ("Male", "Female", "Unknown").
    - [x] Mejorar el manejo de errores y logging en las descargas.
- [x] **3. Repositorios Externos**
    - [x] Silenciar errores 404 en `SuperheroRepository.kt`.
    - [x] Ajustar mapeo de géneros en `IGDBRepository.kt`.
- [ ] **4. Verificación y Sincronización**
    - [ ] Instruir al usuario para realizar Clean & Rebuild.
    - [ ] Verificar funcionamiento de botones de género.
