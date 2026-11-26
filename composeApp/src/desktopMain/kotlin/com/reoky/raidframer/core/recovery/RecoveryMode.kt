package com.reoky.raidframer.core.recovery

import com.reoky.raidframer.core.database.AppDatabase
import com.reoky.raidframer.core.database.appDatabaseDriver
import com.reoky.raidframer.core.helpers.getRaidFramerDirectory
import com.reoky.raidframer.core.interactor.Log
import java.nio.file.Files
import java.nio.file.Path

/*
 * Recovery mode is a shim between app launch and database initialization. It exists
 * because a single failed database migration would otherwise make the app unopenable
 * and unupdatable. While recovery mode is active, none of the main app initializes:
 * only the barebones components needed to show a UI and manage the database files.
 */
object RecoveryMode {

  /** Why recovery mode is showing. */
  enum class Reason {
    /** A migration failed or the database could not be opened while booting. */
    MIGRATION_FAILURE,

    /** The user deliberately entered recovery mode from Settings. */
    REQUESTED_BY_USER
  }

  /** Command line argument that boots the app directly into recovery mode. */
  const val LAUNCH_ARG = "--recovery"

  private const val TAG = "RecoveryMode"
  private const val MARKER_FILE_NAME = "recovery_mode.flag"

  /** How long entering recovery waits (in the background) for the database lock to go. */
  private const val LOCK_RELEASE_GRACE_MILLIS = 5_000L

  @Volatile
  var reason: Reason = Reason.REQUESTED_BY_USER
    private set

  /** The failure that triggered automatic recovery, when there was one. */
  @Volatile
  var error: Throwable? = null
    private set

  private var failedDatabase: AppDatabase? = null
  private var active: Boolean = false

  fun isActive(): Boolean = active

  /** Keeps the half-opened database around so [enter] can release its file lock. */
  fun attachFailedDatabase(db: AppDatabase?) {
    failedDatabase = db
  }

  /**
   * Enters recovery mode. Any half-opened database is closed immediately so its files
   * can be moved by the backup without fighting a Windows file lock, and the file's
   * lock is verified in the background (the recovery window is never delayed by it;
   * the logged result is what the reset builds on).
   */
  fun enter(newReason: Reason, failure: Throwable? = null) {
    active = true
    reason = newReason
    error = failure
    failedDatabase?.let { db ->
      try {
        db.close()
      } catch (e: Exception) {
        println("$TAG: could not close the failed database: ${e.message}")
      }
      // When the open fails, Room abandons the connection it opened for the migration:
      // it is never closed and never returned to the pool, so it holds the database
      // file locked until the process exits. Close anything the pool did not take back
      // before verifying the lock is gone.
      val reaped = appDatabaseDriver.closeUnreleasedConnections()
      if (reaped > 0) {
        Log.info(TAG, "Closed $reaped database connection(s) abandoned by the failed open")
      }
      // The database files cannot be moved while any connection is still open, so wait
      // for the lock to be really gone and say so in the log either way.
      val databaseFile = getRaidFramerDirectory()?.resolve(RecoveryBackup.DATABASE_FILE)
      if (databaseFile != null) {
        Thread {
          if (RecoveryBackup.awaitExclusiveAccess(databaseFile, LOCK_RELEASE_GRACE_MILLIS)) {
            Log.info(TAG, "The database file lock was released after closing it")
          } else {
            Log.warn(TAG, "The database file is still locked by another process; the reset may need it released")
          }
        }.apply {
          isDaemon = true
          name = "$TAG-lock-watcher"
          start()
        }
      }
    }
    failedDatabase = null
  }

  /** Clears recovery state. Recovery mode never runs alongside the main app, so this is rarely used. */
  fun exit() {
    active = false
    reason = Reason.REQUESTED_BY_USER
    error = null
  }

  /** Path of the app's debug log, when the app directory is resolvable. */
  fun debugLogPath(): Path? = getRaidFramerDirectory()?.resolve("debug.log")

  private fun markerFilePath(): Path? = getRaidFramerDirectory()?.resolve(MARKER_FILE_NAME)

  /**
   * Writes the recovery marker file so the next launch opens in recovery mode even
   * if the relaunch process could not be spawned.
   */
  fun writeMarkerFile(): Boolean {
    val path = markerFilePath() ?: return false
    return try {
      path.parent?.let(Files::createDirectories)
      Files.write(path, "Recovery mode requested from Settings".toByteArray())
      true
    } catch (_: Exception) {
      false
    }
  }

  /** Checks for the recovery marker file, deleting it when present. Returns true when it was found. */
  fun consumeMarkerFile(): Boolean {
    val path = markerFilePath() ?: return false
    return try {
      val existed = Files.exists(path)
      if (existed) Files.deleteIfExists(path)
      existed
    } catch (_: Exception) {
      false
    }
  }
}
