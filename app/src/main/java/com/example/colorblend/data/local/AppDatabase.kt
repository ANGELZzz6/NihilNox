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
        PersonajePool::class,
        DoujinEntity::class,
        AutoControlProfile::class,
        AutoControlSession::class,
        Genero::class,
        GenshinCharacter::class,
        EjercicioEntity::class,
        SesionEntity::class,
        SerieEntity::class,
        RegistroDiarioProgresionEntity::class
    ],
    version = 49
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun autoControlDao(): AutoControlDao
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
    abstract fun doujinDao(): DoujinDao
    abstract fun generoDao(): GeneroDao
    abstract fun genshinDao(): GenshinDao
    abstract fun progresionDao(): ProgresionDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "colorblend_db"
                )
                    .addMigrations(
                        MIGRATION_24_25, MIGRATION_25_26, MIGRATION_26_27, MIGRATION_27_28, 
                        MIGRATION_28_29, MIGRATION_29_30, MIGRATION_30_31, MIGRATION_31_32, 
                        MIGRATION_32_33, MIGRATION_33_34, MIGRATION_34_35, MIGRATION_35_36, 
                        MIGRATION_36_37, MIGRATION_37_38, MIGRATION_38_39, MIGRATION_39_40, 
                        MIGRATION_40_41, MIGRATION_41_42, MIGRATION_42_43, MIGRATION_43_44,
                        MIGRATION_44_45, MIGRATION_45_46, MIGRATION_46_47, MIGRATION_47_48, MIGRATION_48_49
                    )
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

val MIGRATION_48_49 = object : Migration(48, 49) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `ejercicios` ADD COLUMN `descansoSegundos` INTEGER")
        database.execSQL("ALTER TABLE `ejercicios` ADD COLUMN `tempo` TEXT")
        database.execSQL("ALTER TABLE `ejercicios` ADD COLUMN `requiereCalentamientoEspecifico` INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE `ejercicios` ADD COLUMN `protocoloCalentamiento` TEXT")
        database.execSQL("ALTER TABLE `ejercicios` ADD COLUMN `notasTendon` TEXT")
        database.execSQL("ALTER TABLE `ejercicios` ADD COLUMN `seriesPredeterminadas` INTEGER NOT NULL DEFAULT 3")
    }
}

val MIGRATION_47_48 = object : Migration(47, 48) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `ejercicios` ADD COLUMN `esIsometrico` INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_46_47 = object : Migration(46, 47) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `ejercicios` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `nombre` TEXT NOT NULL, 
                `esEjercicioPrincipal` INTEGER NOT NULL, 
                `rangoRepsMin` INTEGER NOT NULL, 
                `rangoRepsMax` INTEGER NOT NULL, 
                `pesoActualKg` REAL NOT NULL, 
                `orden` INTEGER NOT NULL, 
                `activo` INTEGER NOT NULL
            )
        """)
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `sesiones` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `ejercicioId` INTEGER NOT NULL, 
                `fecha` INTEGER NOT NULL, 
                `esDescarga` INTEGER NOT NULL
            )
        """)
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `series` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `sesionId` INTEGER NOT NULL, 
                `numeroSerie` INTEGER NOT NULL, 
                `pesoKg` REAL NOT NULL, 
                `reps` INTEGER NOT NULL, 
                `rir` INTEGER
            )
        """)
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `registro_diario_progresion` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `sesionId` INTEGER NOT NULL, 
                `molestiaArticular` INTEGER NOT NULL, 
                `notas` TEXT NOT NULL
            )
        """)
    }
}

val MIGRATION_44_45 = object : Migration(44, 45) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `canciones` RENAME COLUMN `genero` TO `generos` ")
    }
}

val MIGRATION_45_46 = object : Migration(45, 46) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `genshin_characters` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `nombre` TEXT NOT NULL, 
                `rareza` INTEGER NOT NULL, 
                `elemento` TEXT NOT NULL, 
                `armaTipo` TEXT NOT NULL, 
                `nivel` INTEGER NOT NULL, 
                `nivelAscension` INTEGER NOT NULL, 
                `nivelAmistad` INTEGER NOT NULL, 
                `constelacion` INTEGER NOT NULL, 
                `talentoAtaque` INTEGER NOT NULL, 
                `talentoElemental` INTEGER NOT NULL, 
                `talentoDefinitiva` INTEGER NOT NULL, 
                `notas` TEXT NOT NULL, 
                `fechaAgregado` INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

val MIGRATION_43_44 = object : Migration(43, 44) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS `generos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `nombre` TEXT NOT NULL)")
        database.execSQL("ALTER TABLE `canciones` ADD COLUMN `genero` TEXT")
    }
}

val MIGRATION_42_43 = object : Migration(42, 43) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE autocontrol_profile ADD COLUMN ultimaVez INTEGER")
    }
}

val MIGRATION_41_42 = object : Migration(41, 42) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `autocontrol_profile` (
                `id` INTEGER NOT NULL, 
                `frecuenciaActual` TEXT NOT NULL, 
                `objetivoPrincipal` TEXT NOT NULL, 
                `triggers` TEXT NOT NULL, 
                `planIA` TEXT NOT NULL, 
                `fechaCreacion` INTEGER NOT NULL, 
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `autocontrol_sessions` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `fecha` INTEGER NOT NULL, 
                `horaConsulta` TEXT NOT NULL, 
                `duracionSolicitada` INTEGER NOT NULL, 
                `respuestaIA` TEXT NOT NULL, 
                `aprobado` INTEGER NOT NULL, 
                `motivoIA` TEXT NOT NULL
            )
        """.trimIndent())
    }
}

val MIGRATION_39_40 = object : Migration(39, 40) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `doujins_guardados` (
                `id` TEXT PRIMARY KEY NOT NULL, 
                `title` TEXT NOT NULL, 
                `coverUrl` TEXT NOT NULL, 
                `source` TEXT NOT NULL, 
                `fechaGuardado` INTEGER NOT NULL, 
                `artist` TEXT NOT NULL DEFAULT '', 
                `totalPages` INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())
    }
}

val MIGRATION_40_41 = object : Migration(40, 41) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `doujins_guardados` ADD COLUMN `isDownloaded` INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE `doujins_guardados` ADD COLUMN `localPath` TEXT")
        database.execSQL("ALTER TABLE `doujins_guardados` ADD COLUMN `downloadProgress` INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE `doujins_guardados` ADD COLUMN `downloadStatus` TEXT NOT NULL DEFAULT 'IDLE'")
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
