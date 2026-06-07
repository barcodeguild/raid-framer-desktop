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

// added int   val leaderships: Int = 0, to PlayerCacheEntity
val MIGRATION_4_5 = object : Migration(4, 5) {
  override fun migrate(connection: SQLiteConnection) {
    connection.prepare("ALTER TABLE player_cache ADD COLUMN leaderships INTEGER NOT NULL DEFAULT 0").use {
      it.step()
    }
  }
}

// added   val allowPVEDamage: Boolean = false, flag to ConfigEntity
val MIGRATION_5_6 = object : Migration(5, 6) {
  override fun migrate(connection: SQLiteConnection) {
    connection.prepare("ALTER TABLE config ADD COLUMN allowPVEDamage INTEGER NOT NULL DEFAULT 0").use {
      it.step()
    }
  }
}

// added   val lifetimeTotalKillsKB: Long = 0L, to PlayerCacheEntity to support computing both kill methods in parallel
val MIGRATION_6_7 = object : Migration(6, 7) {
  override fun migrate(connection: SQLiteConnection) {
    connection.prepare("ALTER TABLE player_cache ADD COLUMN lifetimeTotalKillsKB INTEGER NOT NULL DEFAULT 0").use {
      it.step()
    }
  }
}

// added songs to player cache for tracking song applications (ballad, chanty etc)
val MIGRATION_7_8 = object : Migration(7, 8) {
  override fun migrate(connection: SQLiteConnection) {
    connection.prepare("ALTER TABLE player_cache ADD COLUMN lifetimeTotalSongs INTEGER NOT NULL DEFAULT 0").use {
      it.step()
    }
  }
}


// added buffs applied to player cache for tracking buff applications (all buffs)
val MIGRATION_8_9 = object : Migration(8, 9) {
  override fun migrate(connection: SQLiteConnection) {
    connection.prepare("ALTER TABLE player_cache ADD COLUMN lifetimeTotalBuffsApplied INTEGER NOT NULL DEFAULT 0").use {
      it.step()
    }
  }
}

// migration to add   val lifetimeTotalHealsReceived: Long = 0L, to PlayerCacheEntity for tracking heals received over time
val MIGRATION_9_10 = object : Migration(9, 10) {
  override fun migrate(connection: SQLiteConnection) {
    connection.prepare("ALTER TABLE player_cache ADD COLUMN lifetimeTotalHealsReceived INTEGER NOT NULL DEFAULT 0").use {
      it.step()
    }
  }
}

// add val lifetimeTotalPotionUsages: Long = 0L,
val MIGRATION_10_11 = object : Migration(10, 11) {
  override fun migrate(connection: SQLiteConnection) {
    connection.prepare("ALTER TABLE player_cache ADD COLUMN lifetimeTotalPotionUsages INTEGER NOT NULL DEFAULT 0").use {
      it.step()
    }
  }
}

// added val lastKrakenShield: Long = 0L,
val MIGRATION_11_12 = object : Migration(11, 12) {
  override fun migrate(connection: SQLiteConnection) {
    connection.prepare("ALTER TABLE player_cache ADD COLUMN lastKrakenShield INTEGER NOT NULL DEFAULT 0").use {
      it.step()
    }
  }
}

// added val companionShowDistressedInChat: Boolean = true, to ConfigEntity
val MIGRATION_12_13 = object : Migration(12, 13) {
  override fun migrate(connection: SQLiteConnection) {
    connection.prepare("ALTER TABLE config ADD COLUMN companionShowDistressedInChat INTEGER NOT NULL DEFAULT 1").use {
      it.step()
    }
  }
}

// added val installationFinalized: Boolean = false,
val MIGRATION_13_14 = object : Migration(13, 14) {
  override fun migrate(connection: SQLiteConnection) {
    connection.prepare("ALTER TABLE config ADD COLUMN installationFinalized INTEGER NOT NULL DEFAULT 0").use {
      it.step()
    }
  }
}

// added combat overlay visibility flags to ConfigEntity
val MIGRATION_14_15 = object : Migration(14, 15) {
  override fun migrate(connection: SQLiteConnection) {
    connection.prepare("ALTER TABLE config ADD COLUMN combatShowDamageColumn INTEGER NOT NULL DEFAULT 1").use { it.step() }
    connection.prepare("ALTER TABLE config ADD COLUMN combatShowHealsColumn INTEGER NOT NULL DEFAULT 1").use { it.step() }
    connection.prepare("ALTER TABLE config ADD COLUMN combatShowCCColumn INTEGER NOT NULL DEFAULT 1").use { it.step() }
  }
}

// added combatControlsFadeEnabled to ConfigEntity (06/07/26)
val MIGRATION_15_16 = object : Migration(15, 16) {
  override fun migrate(connection: SQLiteConnection) {
    connection.prepare("ALTER TABLE config ADD COLUMN combatControlsFadeEnabled INTEGER NOT NULL DEFAULT 0").use { it.step() }
  }
}

