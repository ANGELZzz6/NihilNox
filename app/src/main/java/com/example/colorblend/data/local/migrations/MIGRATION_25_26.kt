package com.example.colorblend.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_25_26 = object : Migration(25, 26) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS canciones (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                uri_local TEXT NOT NULL,
                titulo TEXT NOT NULL,
                artista TEXT NOT NULL DEFAULT '',
                playlist_id TEXT NOT NULL,
                uri_spotify TEXT NOT NULL DEFAULT '',
                fecha_agregada INTEGER NOT NULL DEFAULT 1781733156835
            )
            """.trimIndent()
        )
        // Crear índice para búsquedas rápidas por Spotify
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_uri_spotify ON canciones(uri_spotify)")
    }
}
