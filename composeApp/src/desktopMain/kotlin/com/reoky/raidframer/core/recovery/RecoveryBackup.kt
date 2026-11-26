package com.reoky.raidframer.core.recovery

import com.reoky.raidframer.core.database.SCHEMA_VERSION
import com.reoky.raidframer.core.helpers.getRaidFramerDirectory
import com.reoky.raidframer.core.interactor.LoggingInteractor
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.WinBase
import com.sun.jna.platform.win32.WinNT
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/*
 * The "not destructive repartition": the database and its journals are moved into a
 * backup folder instead of being deleted, so the app can start fresh while nothing
 * is lost. If a future update makes the old database readable again, the data can be
 * salvaged on the user's behalf from this backup.
 */
object RecoveryBackup {

  private const val TAG = "RecoveryBackup"
  private const val BACKUP_ROOT_DIR = "migration_failure"

  /** The database file name, shared with recovery mode for its lock checks. */
  const val DATABASE_FILE = "raidframer.db"

  /** How long reset waits for the database file to become free before moving it. */
  private const val LOCK_RELEASE_GRACE_MILLIS = 15_000L
  private const val JOURNALS_DIR = "journals"
  private const val DEBUG_LOG_FILE = "debug.log"
  private const val BACKUP_LOG_FILE = "migration_failure_debug.log"

  // Transient lock and journal side files that only have meaning while the database
  // is open. They are removed outright; the database itself is never deleted here.
  private val REMOVABLE_SIDE_FILES = listOf(
    "$DATABASE_FILE.lck",
    "$DATABASE_FILE-shm",
    "$DATABASE_FILE-wal",
    "$DATABASE_FILE-journal"
  )

  data class Result(
    val backupDirectory: Path,
    val databaseMoved: Boolean,
    val journalsMoved: Boolean,
    val logCopied: Boolean
  )

  /**
   * Moves the current database and journals into
   * `.RaidFramer/migration_failure/backup_<schema version>` and copies the debug log
   * alongside them. Never deletes the database itself, never follows symlinks, and
   * never touches paths outside the app directory.
   */
  fun reset(): Result {
    // Flush pending entries so the copied log captures everything up to now.
    LoggingInteractor.flush()

    val root = getRaidFramerDirectory()
      ?: error("Unable to resolve the Raid Framer application directory")
    Files.createDirectories(root)

    val databaseFile = requireChild(root, DATABASE_FILE)
    val target = createBackupDirectory(root, databaseFile)

    // Move the database file itself. Wait for the file to be free first (a connection
    // that close missed, an antivirus scan), then retry the move for a while in case
    // something else grabs a transient lock.
    val databaseMoved = if (existsAsRealFile(databaseFile)) {
      awaitExclusiveAccess(databaseFile, LOCK_RELEASE_GRACE_MILLIS)
      moveToBackup(databaseFile, target.resolve(DATABASE_FILE))
      true
    } else {
      false
    }

    // Transient side files are safe to remove outright.
    REMOVABLE_SIDE_FILES.forEach { name -> removeQuietly(requireChild(root, name)) }

    // Pocket journals are persisted next to the database and are part of the user's
    // data, so they travel with the backup.
    val journals = requireChild(root, JOURNALS_DIR)
    val journalsMoved = if (existsAsRealDirectory(journals)) {
      moveToBackup(journals, target.resolve(JOURNALS_DIR))
      true
    } else {
      false
    }

    // Keep a copy of the log next to the failed database for future salvage attempts.
    val logCopied = copyDebugLog(root, target)

    LoggingInteractor.info(TAG, "Backup complete: ${target.toAbsolutePath()}")
    return Result(target, databaseMoved, journalsMoved, logCopied)
  }

  /** Resolves [name] directly under [root], refusing anything that escapes the app directory. */
  private fun requireChild(root: Path, name: String): Path {
    val child = root.resolve(name).normalize()
    check(child.startsWith(root.normalize())) { "Refusing to touch paths outside the app directory" }
    return child
  }

  /** True only for real (non-symlink) files; symlinks are never followed. */
  private fun existsAsRealFile(path: Path): Boolean =
    !Files.isSymbolicLink(path) && Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)

  /** True only for real (non-symlink) directories; symlinks are never followed. */
  private fun existsAsRealDirectory(path: Path): Boolean =
    !Files.isSymbolicLink(path) && Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)

  /**
   * Creates `.RaidFramer/migration_failure/backup_<version>`. The version is read from
   * the database header so the folder records the schema the database was actually at
   * (falling back to the app's current schema version). A timestamp suffix avoids
   * clobbering an existing backup.
   */
  private fun createBackupDirectory(root: Path, databaseFile: Path): Path {
    val base = requireChild(root, BACKUP_ROOT_DIR)
    Files.createDirectories(base)
    val version = readDatabaseUserVersion(databaseFile) ?: SCHEMA_VERSION
    val preferred = requireChild(base, "backup_$version")
    val target = if (Files.exists(preferred)) {
      val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
      requireChild(base, "backup_${version}_$stamp")
    } else {
      preferred
    }
    Files.createDirectories(target)
    return target
  }

  /**
   * Reads Room's schema version from the database file header. It is stored as the
   * SQLite `user_version` pragma: a big-endian 32-bit integer at header offset 60.
   * Returns null when the header is unreadable (a corrupted database, for example).
   */
  private fun readDatabaseUserVersion(databaseFile: Path): Int? = try {
    RandomAccessFile(databaseFile.toFile(), "r").use { file ->
      if (file.length() >= 100) {
        file.seek(60)
        val version = file.readInt()
        if (version in 1..SCHEMA_VERSION) version else null
      } else {
        null
      }
    }
  } catch (_: Exception) {
    null
  }

  /**
   * Waits until [path] can be opened exclusively, i.e. until no handle — here or in
   * another process — would block moving or deleting the file. Windows refuses a move
   * with a sharing violation while any such handle exists, so probing it up front turns
   * blind move retries into a real wait for the lock to disappear. Returns true when the
   * file is free (or no longer exists), false when it stayed locked for [timeoutMillis].
   */
  fun awaitExclusiveAccess(path: Path, timeoutMillis: Long): Boolean {
    val deadline = System.currentTimeMillis() + timeoutMillis
    var attempts = 0
    while (true) {
      attempts++
      if (!existsAsRealFile(path)) return true
      if (canOpenExclusively(path)) {
        if (attempts > 1) LoggingInteractor.info(TAG, "${path.fileName} became free after $attempts probes")
        return true
      }
      if (System.currentTimeMillis() >= deadline) {
        LoggingInteractor.warn(TAG, "${path.fileName} is still locked after $attempts probes over ${timeoutMillis}ms")
        return false
      }
      // The bundled driver's native connections have no finalizer, so garbage
      // collection cannot release them (recovery mode closes those explicitly via
      // LeakGuardSQLiteDriver). The nudge is for Java-side handles, whose cleaners do
      // respond to collection.
      System.gc()
      Thread.sleep(100L)
    }
  }

  /**
   * True when the file could be opened with no sharing at all, which is only possible
   * when every existing handle allows delete (the exact condition Files.move needs).
   * A read-only probe would not do: Java opens files with read/write sharing, so those
   * succeed even while the file cannot be moved.
   */
  private fun canOpenExclusively(path: Path): Boolean = try {
    val handle = Kernel32.INSTANCE.CreateFile(
      path.toAbsolutePath().toString(),
      WinNT.GENERIC_READ or WinNT.GENERIC_WRITE,
      0, // no sharing: the open only succeeds when nothing else holds the file open
      null,
      WinNT.OPEN_EXISTING,
      WinNT.FILE_ATTRIBUTE_NORMAL,
      null
    )
    val opened = handle != null && !WinBase.INVALID_HANDLE_VALUE.equals(handle)
    if (opened) Kernel32.INSTANCE.CloseHandle(handle)
    opened
  } catch (_: Throwable) {
    true // the probe is unavailable on this platform; let the move itself decide
  }

  /** Moves [source] to [target], retrying for a while in case something still holds a lock. */
  private fun moveToBackup(source: Path, target: Path) {
    var lastError: Exception? = null
    var attempts = 0
    // External locks (an antivirus scan, a lingering handle) are short-lived, so keep
    // retrying for a while before giving up. In-process driver connections are reaped
    // by recovery mode up front; the collector nudge covers Java-side handles whose
    // cleaners respond to collection.
    val deadline = System.currentTimeMillis() + LOCK_RELEASE_GRACE_MILLIS
    while (true) {
      attempts++
      try {
        Files.move(source, target)
        return
      } catch (e: Exception) {
        lastError = e
        LoggingInteractor.warn(TAG, "Move attempt $attempts failed for ${source.fileName}: ${e.message}")
        if (System.currentTimeMillis() >= deadline) break
        System.gc()
        Thread.sleep(minOf(250L * attempts, 2_000L))
      }
    }
    error("Failed to move ${source.fileName} after $attempts attempts: ${lastError.message}")
  }

  /** Deletes a transient side file without following symlinks; failures are logged, not fatal. */
  private fun removeQuietly(path: Path) {
    try {
      if (Files.isSymbolicLink(path) || !existsAsRealFile(path)) return
      Files.delete(path)
    } catch (e: Exception) {
      LoggingInteractor.warn(TAG, "Could not remove ${path.fileName}: ${e.message}")
    }
  }

  /** Copies the debug log into the backup as migration_failure_debug.log. */
  private fun copyDebugLog(root: Path, target: Path): Boolean {
    val log = requireChild(root, DEBUG_LOG_FILE)
    return try {
      if (!existsAsRealFile(log)) return false
      Files.copy(log, target.resolve(BACKUP_LOG_FILE), StandardCopyOption.REPLACE_EXISTING)
      true
    } catch (e: Exception) {
      LoggingInteractor.warn(TAG, "Could not copy the debug log: ${e.message}")
      false
    }
  }
}
