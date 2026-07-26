package com.example.colorblend.data.local

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.colorblend.domain.model.*
import com.example.colorblend.data.local.migrations.MIGRATION_25_26
import com.example.colorblend.data.local.migrations.MIGRATION_26_27

@Database(
    entities = [
        Meta::class,
        MetaImagenDia::class,
        UserStats::class,
        PersonajeObtenido::class,
        ImagenPersonaje::class,
        MensajeChat::class,
        PersonajeChat::class,
        CarpetaImagenes::class,
        ImagenGenerada::class,
        PerfilNutricion::class,
        RegistroAlimento::class,
        AlimentoGuardado::class,
        AnalisisDia::class,
        FallVideo::class,
        Cancion::class,
        LearnTopic::class,
        LearnCard::class,
        LearnQuizQuestion::class,
        Habito::class,
        RegistroHabito::class,
        Identidad::class,
        Tarea::class,
        RegistroTarea::class,
        FraseZen::class,
        PersonajePool::class
    ],
    version = 39
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun carpetaImagenesDao(): CarpetaImagenesDao
    abstract fun imagenGeneradaDao(): ImagenGeneradaDao
    abstract fun personajeChatDao(): PersonajeChatDao
    abstract fun mensajeChatDao(): MensajeChatDao
    abstract fun imagenPersonajeDao(): ImagenPersonajeDao
    abstract fun metaDao(): MetaDao
    abstract fun metaImagenDao(): MetaImagenDao
    abstract fun userStatsDao(): UserStatsDao
    abstract fun personajeDao(): PersonajeDao
    abstract fun nutricionDao(): NutricionDao
    abstract fun fallVideoDao(): FallVideoDao
    abstract fun cancionDao(): CancionDao
    abstract fun learnDao(): LearnDao
    abstract fun habitoDao(): HabitoDao
    abstract fun registroHabitoDao(): RegistroHabitoDao
    abstract fun identidadDao(): IdentidadDao
    abstract fun tareaDao(): TareaDao
    abstract fun registroTareaDao(): RegistroTareaDao
    abstract fun fraseZenDao(): FraseZenDao
    abstract fun personajePoolDao(): PersonajePoolDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "colorblend_db"
                )
                    .addMigrations(MIGRATION_24_25, MIGRATION_25_26, MIGRATION_26_27, MIGRATION_27_28, MIGRATION_28_29, MIGRATION_29_30, MIGRATION_30_31, MIGRATION_31_32, MIGRATION_32_33, MIGRATION_33_34, MIGRATION_34_35, MIGRATION_35_36, MIGRATION_36_37, MIGRATION_37_38, MIGRATION_38_39)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

val MIGRATION_24_25 = object : Migration(24, 25) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `fall_videos` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `file_path` TEXT NOT NULL, 
                `file_name` TEXT NOT NULL, 
                `category` TEXT NOT NULL DEFAULT 'NEWS', 
                `date_added` INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

val MIGRATION_27_28 = object : Migration(27, 28) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS learn_topics (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                titulo TEXT NOT NULL,
                descripcion TEXT NOT NULL,
                categoria TEXT NOT NULL,
                materialUsuario TEXT,
                fechaCreacion INTEGER NOT NULL DEFAULT 0,
                ultimaRepaso INTEGER NOT NULL DEFAULT 0,
                rachaEstudio INTEGER NOT NULL DEFAULT 0,
                dominioTotal REAL NOT NULL DEFAULT 0,
                totalSesiones INTEGER NOT NULL DEFAULT 0,
                activo INTEGER NOT NULL DEFAULT 1
            )
        """)
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS learn_cards (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                topicId INTEGER NOT NULL,
                frente TEXT NOT NULL,
                reverso TEXT NOT NULL,
                ejemplo TEXT,
                intervalo INTEGER NOT NULL DEFAULT 1,
                facilidad REAL NOT NULL DEFAULT 2.5,
                repeticiones INTEGER NOT NULL DEFAULT 0,
                proximoRepaso INTEGER NOT NULL DEFAULT 0,
                ultimaCalificacion INTEGER NOT NULL DEFAULT 0
            )
        """)
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS learn_quiz_questions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                topicId INTEGER NOT NULL,
                pregunta TEXT NOT NULL,
                opcionA TEXT NOT NULL,
                opcionB TEXT NOT NULL,
                opcionC TEXT NOT NULL,
                opcionD TEXT NOT NULL,
                respuestaCorrecta TEXT NOT NULL,
                explicacion TEXT NOT NULL
            )
        """)
    }
}

val MIGRATION_28_29 = object : Migration(28, 29) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS habitos (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                nombre TEXT NOT NULL,
                descripcion TEXT NOT NULL DEFAULT '',
                fechaCreacion INTEGER NOT NULL,
                rachaActual INTEGER NOT NULL DEFAULT 0,
                ultimaFechaCompletado INTEGER,
                completadoHoy INTEGER NOT NULL DEFAULT 0
            )
        """)
    }
}

val MIGRATION_29_30 = object : Migration(29, 30) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE habitos ADD COLUMN ancla TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE habitos ADD COLUMN rachaMaxima INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE habitos ADD COLUMN penultimaFechaCompletado INTEGER")
        database.execSQL("ALTER TABLE habitos ADD COLUMN totalCompletados INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_30_31 = object : Migration(30, 31) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE habitos ADD COLUMN notificacionHabilitada INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE habitos ADD COLUMN notificacionHora INTEGER NOT NULL DEFAULT 8")
        database.execSQL("ALTER TABLE habitos ADD COLUMN notificacionMinuto INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_31_32 = object : Migration(31, 32) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS registros_habito (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                habitoId INTEGER NOT NULL,
                fechaDia INTEGER NOT NULL,
                FOREIGN KEY(habitoId) REFERENCES habitos(id) ON DELETE CASCADE
            )
        """)
        database.execSQL("CREATE INDEX IF NOT EXISTS index_registros_habito_habitoId ON registros_habito(habitoId)")
    }
}

val MIGRATION_32_33 = object : Migration(32, 33) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS identidades (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                declaracion TEXT NOT NULL,
                fechaCreacion INTEGER NOT NULL,
                votosTotal INTEGER NOT NULL DEFAULT 0
            )
        """)
        database.execSQL("ALTER TABLE habitos ADD COLUMN identidadId INTEGER")
    }
}

val MIGRATION_33_34 = object : Migration(33, 34) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE habitos ADD COLUMN diasSemana TEXT NOT NULL DEFAULT '1,2,3,4,5,6,7'")
        database.execSQL("ALTER TABLE habitos ADD COLUMN tiempoAnticipacion INTEGER NOT NULL DEFAULT 15")
        database.execSQL("ALTER TABLE habitos ADD COLUMN enabledBurbuja INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_34_35 = object : Migration(34, 35) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE habitos ADD COLUMN burbujaTexto TEXT")
        database.execSQL("ALTER TABLE habitos ADD COLUMN burbujaColor TEXT NOT NULL DEFAULT '#FFD700'")
        database.execSQL("ALTER TABLE habitos ADD COLUMN burbujaImagenUri TEXT")
        database.execSQL("ALTER TABLE habitos ADD COLUMN burbujaUsarImagen INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_35_36 = object : Migration(35, 36) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `tareas` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `titulo` TEXT NOT NULL, 
                `descripcion` TEXT NOT NULL DEFAULT '', 
                `fecha` INTEGER NOT NULL, 
                `hora` INTEGER NOT NULL DEFAULT 0, 
                `minuto` INTEGER NOT NULL DEFAULT 0, 
                `notificacionHabilitada` INTEGER NOT NULL DEFAULT 0, 
                `recurrencia` TEXT NOT NULL DEFAULT 'UNA_VEZ', 
                `diasSemana` TEXT NOT NULL DEFAULT '', 
                `color` TEXT NOT NULL DEFAULT '#FFD700', 
                `completada` INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())
    }
}

val MIGRATION_36_37 = object : Migration(36, 37) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `registros_tarea` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `tareaId` INTEGER NOT NULL, 
                `fechaDia` INTEGER NOT NULL, 
                FOREIGN KEY(`tareaId`) REFERENCES `tareas`(`id`) ON DELETE CASCADE
            )
        """.trimIndent())
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_registros_tarea_tareaId` ON `registros_tarea`(`tareaId`)")
    }
}

val MIGRATION_37_38 = object : Migration(37, 38) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `frases_zen` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `texto` TEXT NOT NULL
            )
        """.trimIndent())
    }
}

val MIGRATION_38_39 = object : Migration(38, 39) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `personaje_pool` (
                `id` INTEGER PRIMARY KEY NOT NULL, 
                `nombre` TEXT NOT NULL, 
                `imagenUrl` TEXT NOT NULL, 
                `favoritos` INTEGER NOT NULL, 
                `rareza` TEXT NOT NULL, 
                `genero` TEXT NOT NULL, 
                `categoria` TEXT NOT NULL, 
                `animeId` INTEGER NOT NULL, 
                `animeTitulo` TEXT NOT NULL, 
                `animeCoverUrl` TEXT NOT NULL, 
                `fechaAgregado` INTEGER NOT NULL
            )
        """.trimIndent())
    }
}
