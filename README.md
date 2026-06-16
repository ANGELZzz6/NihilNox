# 🌌 NihilNox (ColorBlend)

¡Bienvenido a **NihilNox (ColorBlend)**! 🚀 

**NihilNox** es una revolucionaria aplicación de **gamificación de hábitos y bienestar** para Android. Combina el seguimiento diario de tus metas de vida y nutrición con un sistema **Gacha** inmersivo. El concepto es simple pero altamente adictivo: **cuida de ti mismo en el mundo real para desbloquear e interactuar con tus personajes favoritos del multiverso en el mundo virtual.**

---

## 🔄 El Ciclo de Juego Principal (Core Loop)

```
┌─────────────────────────────────┐
│       VIDA REAL / HÁBITOS       │
│  • Cumplir Metas de Vida        │
│  • Registrar Nutrición Diaria   │
└────────────────┬────────────────┘
                 │
                 ▼ ¡Gana Monedas! (🪙)
┌─────────────────────────────────┐
│          SISTEMA GACHA          │
│  • Tiradas Individuales o x10   │
│  • Banners: General / ♀ / ♂     │
└────────────────┬────────────────┘
                 │
                 ▼ ¡Desbloquea Personajes!
┌─────────────────────────────────┐
│         CHATS CON IA & MAS       │
│  • Chatea con tus Personajes    │
│  • Escucha música, notas, etc. │
└─────────────────────────────────┘
```

1. **Completa tus metas y come sano:** Registra tu comida diaria y valida tus metas para obtener **Monedas (🪙)**.
2. **Invoca en el Gacha:** Gasta tus monedas acumuladas en banners dinámicos para invocar personajes icónicos de anime, videojuegos y cómics.
3. **Colecciona y Conecta:** Agrega personajes a tu colección, consulta sus estadísticas e **inicia chats interactivos de IA** con ellos basados en sus respectivas personalidades.

---

## 🌟 Funcionalidades Principales

### 🎰 1. Sistema Gacha Multiverso (`MainActivity`)
* **Múltiples Banners:** Realiza invocaciones individuales o de 10 tiradas (`Multi-Roll`) usando filtros por género: General, Femenino (`Fem`) y Masculino (`Masc`).
* **Integración con Múltiples APIs:**
  * **AniList** (a través de Apollo Client & GraphQL) para personajes de anime modernos.
  * **Jikan API** (MyAnimeList) para anime y manga clásico/popular.
  * **IGDB / Giant Bomb API** para personajes memorables de videojuegos.
  * **Superhero API** para héroes y villanos del cómic estadounidense (Marvel, DC, etc.).
* **Transiciones y Efectos:** Efectos visuales de carga interactivos con mensajes dinámicos durante el proceso de invocación.

### 💬 2. Chat Inteligente con Personajes (`ChatPersonajeActivity`)
* **Conversación con IA en tiempo real:** Utiliza modelos de lenguaje avanzados (**Venice AI / OpenAI**) para dar vida a los personajes que has invocado.
* **Personalidades Únicas:** El backend de la IA adapta sus respuestas de acuerdo al trasfondo del personaje seleccionado para una inmersión total.
* **Persistencia de Chat:** Cada conversación se almacena localmente en la base de datos para que nunca pierdas el hilo de tus diálogos.

### 🍎 3. Monitor de Nutrición Inteligente (`NutricionActivity`)
* **Onboarding Personalizado:** Configura tus datos biométricos y objetivos para calcular tus necesidades exactas de calorías, proteínas, carbohidratos y grasas.
* **Diario de Alimentos:** Registra fácilmente los alimentos consumidos y sus macronutrientes para llevar un control estricto de tu dieta.
* **Historial Completo:** Gráficas e historiales detallados para visualizar la constancia de tus hábitos alimenticios en el tiempo.

### 🎯 4. Gestor de Metas y Hábitos (`MetasActivity`)
* **Definición de Metas:** Crea metas diarias o de largo plazo con categorías específicas.
* **Recordatorios Inteligentes:** Alarmas personalizadas implementadas con `AlarmManager` para mantenerte enfocado en tus deberes cotidianos.
* **Validación de Logros:** Un sistema justo para validar tus metas del día, recompensándote directamente con monedas para tu cuenta gacha.

### 🎵 5. Reproductor de Música y Descargador (`ReproductorActivity`)
* **Servicio en Segundo Plano:** `MusicaService` permite continuar escuchando tus canciones favoritas incluso fuera de la aplicación.
* **Organización Inteligente:** Agrupa tus canciones en secciones y listas colapsables mediante una interfaz táctil de arrastre fluida.
* **Descarga e Integración:** Soporte para la extracción y descarga de audio directo desde enlaces multimedia (`YoutubeExtractor` / `DescargaService`).
* **Ecualizador Integrado:** Ajusta las frecuencias de sonido directamente desde la aplicación para una experiencia de audio a tu gusto.

### 🔒 6. Bloc de Notas Seguro (`BlockNotasActivity`)
* **Seguridad Biométrica:** Protege tus notas personales con autenticación nativa por huella dactilar (`BiometricPrompt`) o contraseña cifrada de respaldo.
* **Almacenamiento Local:** Notas guardadas de manera 100% segura en tu dispositivo.

### 🔑 7. Gestor de API Keys (`ApiKeysActivity`)
* Un panel seguro y centralizado para que configures dinámicamente tus credenciales personales para las APIs de IGDB, Jikan, Venice AI, y más.

---

## 🛠️ Arquitectura y Tecnologías Utilizadas

Este proyecto se ha desarrollado siguiendo los principios de **Clean Architecture** y las mejores prácticas de la comunidad Android:

* **Lenguaje:** [Kotlin](https://kotlinlang.org/) (100%).
* **Patrón de Arquitectura:** MVVM (Model-View-ViewModel) para una separación de responsabilidades limpia.
* **Persistencia:** [Room Database](https://developer.android.com/training/data-storage/room) para el almacenamiento local robusto de personajes, mensajes de chat, alimentos y metas.
* **Consumo de APIs & Red:**
  * [Apollo GraphQL Client](https://www.apollographql.com/docs/kotlin/) para las consultas complejas a AniList.
  * [Retrofit2](https://square.github.io/retrofit/) para el consumo de endpoints REST.
* **Concurrencia:** [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) y asincronía reactiva con `StateFlow` / `SharedFlow`.
* **Carga de Imágenes:** [Glide](https://github.com/bumptech/glide) para el renderizado eficiente e inteligente de imágenes de personajes en listas y chats.
* **Seguridad:** Jetpack Biometric para la autenticación biométrica de notas.
* **Servicios de Sistema:** `Service` persistentes y `BroadcastReceiver` con `AlarmManager` para la gestión de música, descargas y alertas en segundo plano.

---

## 🚀 Guía de Instalación y Configuración

### Prerrequisitos
* **Android Studio** (Koala o superior recomendado).
* **JDK 17** o superior configurado en el sistema.
* Dispositivo físico Android o Emulador con API Level 26 (Android 8.0) o superior.

### Pasos de Configuración

1. **Clonar el Repositorio:**
   ```bash
   git clone https://github.com/tu-usuario/NihilNox.git
   cd NihilNox
   ```

2. **Abrir en Android Studio:**
   * Inicia Android Studio.
   * Selecciona **File > Open** y elige el directorio del proyecto clonado `NihilNox`.
   * Deja que Gradle sincronice todas las dependencias requeridas.

3. **Configurar API Keys:**
   * Al iniciar la aplicación en tu dispositivo por primera vez, se te redirigirá automáticamente a la pantalla de **API Keys**.
   * Deberás introducir tus credenciales para los distintos servicios para poder utilizar todas las funciones (por ejemplo, el chat de IA de Venice o el buscador de videojuegos).

4. **Compilar y Ejecutar:**
   * Conecta tu dispositivo Android con la Depuración USB habilitada.
   * Presiona el botón verde de **Run (Ejecutar)** en Android Studio.

---

## 📁 Estructura del Proyecto

```
app/src/main/java/com/example/colorblend/
│
├── adapters/                # Adaptadores de RecyclerView para listas (Personajes, etc.)
├── data/
│   ├── local/               # Base de datos Room (AppDatabase, DAOs, Converters)
│   └── repository/          # Implementación de los repositorios de datos (APIs & DB)
│
├── domain/
│   └── model/               # Modelos de dominio y entidades (Meta, Personaje, RegistroAlimento)
│
├── network/                 # Proveedores de clientes de red (ApolloClientProvider)
│
└── ui/
    └── gacha/               # Actividades, ViewModels y componentes de la UI
        ├── metas/           # Modulo de metas cotidianas y recordatorios
        └── viewmodels/      # ViewModels compartidos para lógica de negocio de UI
```

---

## 🤝 Contribuciones

¿Quieres ayudar a hacer de NihilNox un proyecto aún más increíble? ¡Las contribuciones son bienvenidas!
1. Haz un **Fork** de este repositorio.
2. Crea una rama para tu nueva funcionalidad o corrección (`git checkout -b feature/NuevaFuncionalidad`).
3. Realiza tus cambios y haz un commit descriptivo (`git commit -m 'Añade nueva funcionalidad increíble'`).
4. Sube los cambios a tu rama (`git push origin feature/NuevaFuncionalidad`).
5. Abre un **Pull Request** para revisión.

---

## 📄 Licencia

Este proyecto se encuentra bajo la licencia **MIT**. Puedes consultar el archivo `LICENSE` (en caso de existir) para más detalles.

---

**NihilNox** - *Convierte tu rutina en una aventura de colección legendaria.* 🌟
