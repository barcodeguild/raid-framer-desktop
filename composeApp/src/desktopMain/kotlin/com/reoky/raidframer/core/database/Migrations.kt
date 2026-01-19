package com.reoky.raidframer.core.database

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection

val MIGRATION_2_3 = object : Migration(2, 3) {
  override fun migrate(connection: SQLiteConnection) {
    connection.prepare("ALTER TABLE config ADD COLUMN killCounterMode TEXT NOT NULL DEFAULT 'MOST_DAMAGE'").use {
      it.step()
    }
  }
}