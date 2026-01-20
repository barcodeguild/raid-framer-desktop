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

// added boolean  val companionPlayCharmSound: Boolean = false, to ConfigEntity
val MIGRATION_3_4 = object : Migration(3, 4) {
  override fun migrate(connection: SQLiteConnection) {
    connection.prepare("ALTER TABLE config ADD COLUMN companionPlayCharmSound INTEGER NOT NULL DEFAULT 1").use {
      it.step()
    }
  }
}