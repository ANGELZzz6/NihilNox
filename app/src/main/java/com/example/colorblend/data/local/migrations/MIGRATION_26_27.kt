package com.example.colorblend.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_26_27 = object : Migration(26, 27) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE user_stats ADD COLUMN xp INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE user_stats ADD COLUMN nivel INTEGER NOT NULL DEFAULT 1")
    }
}
