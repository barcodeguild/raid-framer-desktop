package com.reoky.raidframer.core.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.reoky.raidframer.AppGlobals
import com.reoky.raidframer.core.interactor.LoggingInteractor
import kotlinx.coroutines.Dispatchers
import java.io.File


/*
 * Place to specify database options and settings and where the builder for the
 * database is created.
 */
fun initialize(): AppDatabase {

  val TAG = "Core/Database.initialize()"

  // We don't have permissions to write to the program files directory, so we'll use the user's home directory
  val userHomeDirectory = System.getProperty("user.home")
  val appDirectory = "$userHomeDirectory/.RaidFramer"
  val databaseFilePath = "$appDirectory/raidframer.db"

  // Create the directory if it doesn't exist
  val directory = File(appDirectory)
  if (!directory.exists()) {
    directory.mkdirs()
  }

  // testing delete database between launches
  File(databaseFilePath).let {
    if (!AppGlobals.DEBUG_WIPE_DB_AND_CACHE_ON_LAUNCH) return@let
    if (it.exists()) it.delete()
    LoggingInteractor.debug(TAG, "The app database has been wiped as this is a debugging instance.")
  }

  return Room.databaseBuilder<AppDatabase>(
    name = File(databaseFilePath).absolutePath,
  )
  .setDriver(BundledSQLiteDriver())
  .fallbackToDestructiveMigrationFrom(true, 1) // still developing, wipe from v1
  .addMigrations(MIGRATION_2_3) // added kill methods and basic migration for testing migrations to (2 -> 3 : 01/18/26)
  .addMigrations(MIGRATION_3_4) // added charm sounds conf entry (3 -> 4 : 01/19/26)
  .addMigrations(MIGRATION_4_5) // added leaderships to cache (4 -> 5 : 01/21/26)
  .addMigrations(MIGRATION_5_6) // 01/22/26 added PVE damage flag
  .addMigrations(MIGRATION_6_7) // 01/23/26 added lifetimeTotalKillsKB to cache
  .addMigrations(MIGRATION_7_8) // 01/26/26 added lifetimeTotalSongs to cache
  .addMigrations(MIGRATION_8_9) // 01/25/26 added lifetimeTotalBuffsApplied to cache
  .addMigrations(MIGRATION_9_10) // 02/22/26 added lifetimeTotalHealsReceived to cache
  .addMigrations(MIGRATION_10_11) // 02/22/26 added lifetimeTotalPotionUsages to cache
  .addMigrations(MIGRATION_11_12) // 02/22/26 added val lastKrakenShield: Long = 0L, to cache
  .addMigrations(MIGRATION_12_13) // 05/10/26 added val companionShowDistressedInChat: Boolean = true, to config
  .addMigrations(MIGRATION_13_14)
  .addMigrations(MIGRATION_14_15) // 06/07/26 added three checkboxes in settings for combat overlay columns
  .addMigrations(MIGRATION_15_16) // 06/07/26 added combatControlsFadeEnabled to config
  .addMigrations(MIGRATION_16_17) // 06/13/26 added val windowOpacity: Float = 0.43f, to config
  .addMigrations(MIGRATION_17_18) // 06/13/26 added windowColor to config
  .addMigrations(MIGRATION_18_19) // 06/16/26 added session recording fields to config
  .addMigrations(MIGRATION_19_20) // 06/19/26 added lastSessionDurationMs and lastSessionExportDir to config
  .addMigrations(MIGRATION_20_21) // 06/20/26 added exportIncludeRawJsonLogs to config
  .addMigrations(MIGRATION_21_22) // 06/25/26 added seed table config fields
  .addMigrations(MIGRATION_22_23) // 06/25/26 added preferredLanguage to config eek!
  .addMigrations(MIGRATION_23_24) // 07/07/26 added combat custom category columns to config
  .addMigrations(MIGRATION_24_25) // 07/11/26 added player_session_totals table for historical session views
  .addMigrations(MIGRATION_25_26) // 07/11/26 added companionShowDebugInfo and companionShowDeathsPerMinute to config
  .addMigrations(MIGRATION_26_27) // 07/12/26 added autoUpdateEnabled to config
  .addMigrations(MIGRATION_27_28) // 07/14/26 added allowOdeToRecoveryCountAsHeals to config for filtering Ode heals in rankings
  .fallbackToDestructiveMigration(true) // Wipes DB if no migration found
    //.setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING) // WAL for better concurrency
  .fallbackToDestructiveMigrationOnDowngrade(true)
  .setQueryCoroutineContext(Dispatchers.IO)
  .build()
}
