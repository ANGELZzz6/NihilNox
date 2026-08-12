# Tareas: Auto-guardado y Temporizador de Descanso

- [x] **Data Layer**
    - [x] [MODIFY] `ProgresionEntities.kt`: Añadir `SesionBorradorEntity`.
    - [x] [MODIFY] `ProgresionDao.kt`: Métodos para insertar/obtener borradores.
    - [x] [MODIFY] `AppDatabase.kt`: Incrementar a v50 y añadir `MIGRATION_49_50`.
- [x] **Logic Layer**
    - [x] [MODIFY] `ProgresionRepository.kt`: Fachada para borradores.
    - [x] [x] `ProgresionViewModel.kt`: Lógica de auto-guardado reactivo.
- [x] **UI Layer**
    - [x] [MODIFY] `activity_progresion.xml`: Añadir TextView de Temporizador.
    - [x] [x] `ProgresionActivity.kt`: Implementar CountDownTimer y recarga de borrador.
- [ ] **VCS**
    - [ ] Commit y Push final.
