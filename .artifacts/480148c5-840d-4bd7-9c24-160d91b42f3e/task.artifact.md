# Tareas: Balance Inteligente de Géneros en el Pool

- [x] **1. Persistencia (Room)**
    - [x] Añadir `getMaleCount()` y `getFemaleCount()` en `PersonajePoolDao.kt`.
- [x] **2. Repositorio de Pool**
    - [x] Implementar lógica de detección de desequilibrio en `recargarPoolSiEsNecesario`.
    - [x] Ajustar la recarga para priorizar géneros con bajo stock.
- [x] **3. Estabilidad y Errores (Fix 400/401)**
    - [x] Reducir rango de páginas en AniList a 200 (evita error 400).
    - [x] Añadir log específico para token caducado en IGDB (error 401).
- [x] **4. Verificación**
    - [ ] Confirmar que el pool mantiene una proporción equilibrada (aprox. 50/50).
    - [ ] Verificar que las tiradas específicas siguen siendo instantáneas tras varios usos.
