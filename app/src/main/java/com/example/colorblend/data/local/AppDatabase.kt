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
        Identidad::class
    ],
    version = 33
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

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "colorblend_db"
                )
                    .addMigrations(MIGRATION_24_25, MIGRATION_25_26, MIGRATION_26_27, MIGRATION_27_28, MIGRATION_28_29, MIGRATION_29_30, MIGRATION_30_31, MIGRATION_31_32, MIGRATION_32_33)
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
