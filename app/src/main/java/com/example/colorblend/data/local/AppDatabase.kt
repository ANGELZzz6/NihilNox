package com.example.colorblend.data.local

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.colorblend.domain.model.*

// ══════════════════════════════════════════════════════════════════════════════
//  REGLA DE ORO — lee esto antes de cada update que toque la base de datos
// ══════════════════════════════════════════════════════════════════════════════
//
//  Cada vez que cambies CUALQUIER cosa del esquema (nueva tabla, nueva columna,
//  borrar columna, cambiar tipo) debes hacer DOS cosas obligatoriamente:
//
//  1. Subir `version` en @Database  (ej: 24 → 25)
//
//  2. Escribir una Migration nueva aquí abajo con el SQL exacto del cambio
//     y registrarla en el builder con .addMigrations(MIGRATION_24_25)
//
//  Si subes la versión sin migración → la app crashea en el teléfono del usuario
//  Si escribes la migración mal      → la app crashea en el teléfono del usuario
//  Si haces ambas bien               → los datos del usuario se conservan ✓
//
// ══════════════════════════════════════════════════════════════════════════════
//  REFERENCIA RÁPIDA DE SQL PARA MIGRACIONES
// ══════════════════════════════════════════════════════════════════════════════
//
//  Agregar tabla nueva:
//    database.execSQL("CREATE TABLE IF NOT EXISTS `nombre_tabla` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `campo` TEXT NOT NULL)")
//
//  Agregar columna nueva (Room no permite borrar columnas directamente):
//    database.execSQL("ALTER TABLE `nombre_tabla` ADD COLUMN `nueva_columna` TEXT NOT NULL DEFAULT ''")
//    database.execSQL("ALTER TABLE `nombre_tabla` ADD COLUMN `numero` INTEGER NOT NULL DEFAULT 0")
//    database.execSQL("ALTER TABLE `nombre_tabla` ADD COLUMN `decimal` REAL NOT NULL DEFAULT 0.0")
//    database.execSQL("ALTER TABLE `nombre_tabla` ADD COLUMN `booleano` INTEGER NOT NULL DEFAULT 0")  // 0=false 1=true
//    database.execSQL("ALTER TABLE `nombre_tabla` ADD COLUMN `nullable` TEXT")  // sin DEFAULT = nullable
//
//  Renombrar tabla:
//    database.execSQL("ALTER TABLE `viejo_nombre` RENAME TO `nuevo_nombre`")
//
//  Borrar tabla:
//    database.execSQL("DROP TABLE IF EXISTS `nombre_tabla`")
//
//  Borrar columna (SQLite no soporta DROP COLUMN directo — hay que recrear):
//    database.execSQL("CREATE TABLE `tabla_nueva` (...columnas que quieres conservar...)")
//    database.execSQL("INSERT INTO `tabla_nueva` SELECT col1, col2 FROM `tabla_vieja`")
//    database.execSQL("DROP TABLE `tabla_vieja`")
//    database.execSQL("ALTER TABLE `tabla_nueva` RENAME TO `tabla_vieja`")
//
// ══════════════════════════════════════════════════════════════════════════════

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
        FallVideo::class
    ],
    version = 25   // ← sube este número cada vez que cambies el esquema
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

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "colorblend_db"
                )
                    // ── Registra aquí cada migración nueva que escribas ──────
                    .addMigrations(MIGRATION_24_25)
                    // .addMigrations(MIGRATION_25_26)
                    // ── NO agregues más fallbackToDestructiveMigration() ─────
                    // Si la app crashea por migración incorrecta es preferible
                    // al borrado silencioso de todos los datos del usuario.
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  MIGRACIONES — escribe cada una abajo y regístrala en el builder arriba
// ══════════════════════════════════════════════════════════════════════════════

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
