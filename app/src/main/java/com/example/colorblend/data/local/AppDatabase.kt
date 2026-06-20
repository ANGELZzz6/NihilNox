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
        LearnQuizQuestion::class
    ],
    version = 28
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

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "colorblend_db"
                )
                    .addMigrations(MIGRATION_24_25, MIGRATION_25_26, MIGRATION_26_27, MIGRATION_27_28)
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
