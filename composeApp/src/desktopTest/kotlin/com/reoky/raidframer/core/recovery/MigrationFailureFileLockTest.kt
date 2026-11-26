package com.reoky.raidframer.core.recovery

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.reoky.raidframer.core.database.AppDatabase
import com.reoky.raidframer.core.database.LeakGuardSQLiteDriver
import com.reoky.raidframer.core.database.SCHEMA_VERSION
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.nio.file.FileSystemException
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/*
 * Reproduces the recovery mode file lock at the library level: Room opens a connection,
 * runs the migration inside its open path, and on failure abandons the connection — it
 * is neither closed nor handed to the connection pool, so the pool's close() cannot
 * release it. The bundled driver's connection is a raw native pointer without a
 * finalizer, so the abandoned connection keeps the database file locked until the
 * process exits.
 */
class MigrationFailureFileLockTest {

  @Test
  fun `a failed first load keeps the database file locked until the abandoned connection is closed`() {
    val directory = Files.createTempDirectory("raidframer-lock-test")
    val databaseFile = directory.resolve("raidframer.db")

    // A database one schema version behind, with no migration path registered, so the
    // first load fails exactly like a real migration failure.
    BundledSQLiteDriver().open(databaseFile.toString()).use { connection ->
      connection.execSQL("PRAGMA user_version = ${SCHEMA_VERSION - 1}")
    }

    val driver = LeakGuardSQLiteDriver()
    val database = Room.databaseBuilder<AppDatabase>(name = databaseFile.toString())
      .setDriver(driver)
      .setQueryCoroutineContext(Dispatchers.IO)
      .build()

    val outcome = runCatching { runBlocking { database.getConfigDao().getConfig() } }
    assertTrue(outcome.isFailure, "The first load was expected to fail with a migration failure")
    database.close()

    // The abandoned connection from the failed open holds the file, so the move that
    // recovery mode's backup performs fails with a sharing violation. If this ever
    // stops failing, androidx fixed the abandoned open and the reap below becomes a
    // no-op worth keeping anyway.
    assertFailsWith<FileSystemException> {
      Files.move(databaseFile, databaseFile.resolveSibling("raidframer.db.moved"))
    }

    // Recovery mode's cleanup: close what Room abandoned, then the file is movable.
    val reaped = driver.closeUnreleasedConnections()
    assertTrue(reaped >= 1, "Expected the failed open to have abandoned at least one connection, reaped $reaped")

    Files.move(databaseFile, databaseFile.resolveSibling("raidframer.db.moved"))
    assertFalse(Files.exists(databaseFile))
  }
}
