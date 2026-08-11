# Walkthrough - Administrador de Personajes Genshin Impact

Se ha implementado una nueva sección completa para gestionar personajes de Genshin Impact, integrada en la pantalla de "Mi Perfil".

## Cambios Realizados

### Persistencia de Datos
- **[GenshinCharacter.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/domain/model/GenshinCharacter.kt)**: Nueva entidad con atributos como Elemento, Rareza, Nivel y Constelaciones.
- **[GenshinDao.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/data/local/GenshinDao.kt)**: Operaciones CRUD para la base de datos.
- **[AppDatabase.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/data/local/AppDatabase.kt)**: Actualizada a la versión 46 con migración automática para incluir la tabla de personajes.

### Interfaz de Usuario
- **Acceso desde Perfil**: Se añadió el botón "✨ Genshin Impact" en `PerfilActivity.kt` justo debajo de Nutrición, usando el degradado dorado premium.
- **Lista de Personajes**: Una pantalla con `RecyclerView` que muestra tarjetas elegantes con el elemento (emoji), nombre, nivel y rareza (estrellas doradas).
- **Editor de Personajes**: Un formulario completo para añadir o editar personajes, permitiendo seleccionar Elemento y Rareza mediante selectores.

### Lógica
- **GenshinViewModel**: Gestiona la carga y persistencia de datos de forma reactiva usando Flow.
- **GenshinListActivity**: Muestra la lista y maneja la navegación.
- **GenshinEditActivity**: Maneja la creación, edición y eliminación de registros.

## Cómo Probarlo

1.  Ve a la pantalla de **Mi Perfil**.
2.  Pulsa el nuevo botón **✨ Genshin Impact**.
3.  Usa el botón flotante **(+)** para añadir un nuevo personaje.
4.  Rellena los datos (ej: *Raiden Shogun*, *Electro*, *5 estrellas*) y pulsa **GUARDAR**.
5.  Toca un personaje de la lista para editar sus detalles o eliminarlo.

> [!TIP]
> La lista se ordena automáticamente por rareza (5 estrellas primero) y luego por nivel, manteniendo tus personajes más poderosos al principio.
