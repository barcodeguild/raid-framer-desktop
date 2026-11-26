package com.reoky.raidframer.core.database

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.util.concurrent.ConcurrentHashMap

/*
 * A BundledSQLiteDriver wrapper that never loses track of an opened connection.
 *
 * When the database's first load fails (most notably a migration failure), Room opens a
 * connection, runs the migration inside its open path, and on error abandons the
 * connection: it is neither closed nor handed to the connection pool, so the pool's
 * close() cannot release it either. A bundled-driver connection is a raw native pointer
 * without a finalizer, so the abandoned connection keeps the database file itself locked
 * until the process exits — no retry, garbage collection or db.close() changes that.
 *
 * This wrapper records every connection it hands out and can close the ones nobody else
 * closed (see [closeUnreleasedConnections]), which recovery mode uses after closing the
 * failed database to make sure the file lock is really gone.
 */
class LeakGuardSQLiteDriver(
  private val delegate: SQLiteDriver = BundledSQLiteDriver()
) : SQLiteDriver by delegate {

  private val openedConnections = ConcurrentHashMap.newKeySet<TrackedConnection>()

  override fun open(fileName: String): SQLiteConnection =
    TrackedConnection(delegate.open(fileName)).also { openedConnections.add(it) }

  /**
   * Closes every connection opened through this driver that its owner never closed —
   * Room's abandoned failed-open connection, for example. Returns how many connections
   * were closed. Only call this once nothing is legitimately using the database anymore
   * (recovery mode, after the failed database has been closed).
   */
  fun closeUnreleasedConnections(): Int {
    var closed = 0
    openedConnections.forEach { connection ->
      if (connection.closeOnce()) closed++
    }
    return closed
  }

  private inner class TrackedConnection(
    private val delegate: SQLiteConnection
  ) : SQLiteConnection by delegate {

    override fun close() {
      openedConnections.remove(this)
      delegate.close()
    }

    /** Closes the underlying connection unless some other close beat this call to it. */
    fun closeOnce(): Boolean {
      // remove() wins the race: exactly one caller gets to close each connection.
      if (!openedConnections.remove(this)) return false
      try {
        delegate.close()
      } catch (_: Exception) {
        // Already closed by the driver side; the handle is no longer ours either way.
      }
      return true
    }
  }
}
