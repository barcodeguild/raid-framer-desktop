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
  .fallbackToDestructiveMigration(true) // Wipes DB if no migration found
    //.setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING) // WAL for better concurrency
  .fallbackToDestructiveMigrationOnDowngrade(true)
  .setQueryCoroutineContext(Dispatchers.IO)
  .build()
}
