package com.reoky.raidframer.core.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import java.io.File


/*
 * Place to specify database options and settings and where the builder for the
 * database is created.
 */
fun initialize(): AppDatabase {

  // We don't have permissions to write to the program files directory, so we'll use the user's home directory
  val userHomeDirectory = System.getProperty("user.home")
  val appDirectory = "$userHomeDirectory/.RaidFramer"
  val databaseFilePath = "$appDirectory/raidframer.db"

  // Create the directory if it doesn't exist
  val directory = File(appDirectory)
  if (!directory.exists()) {
    directory.mkdirs()
  }

//  val dbFile = File(databaseFilePath)
//  if (dbFile.exists()) {
//    dbFile.delete()
//  }

  return Room.databaseBuilder<AppDatabase>(
    name = File(databaseFilePath).absolutePath,
  )
  .setDriver(BundledSQLiteDriver())
  .fallbackToDestructiveMigration(true) // Wipes DB if no migration found
  //.setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING) // WAL for better concurrency
  //.addMigrations(MIGRATION_1_2) // Add your migration objects here
  .fallbackToDestructiveMigrationOnDowngrade(true)
  .setQueryCoroutineContext(Dispatchers.IO)
  .build()
}
