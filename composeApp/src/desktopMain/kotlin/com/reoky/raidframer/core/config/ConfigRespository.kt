package com.reoky.raidframer.core.config

import com.reoky.raidframer.core.database.ConfigDao
import com.reoky.raidframer.core.database.ConfigEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicBoolean

class ConfigRepository(
  private val dao: ConfigDao,
  private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
  private val _state = MutableStateFlow(ConfigEntity()) // default until loaded
  val state: StateFlow<ConfigEntity> = _state.asStateFlow()

  private val loadDone = CompletableDeferred<Unit>()
  private val writeMutex = Mutex()

  // The synchronous bootstrap owns the database's first query (see [initSync]). These
  // let the async fallback below wait for that attempt instead of racing it.
  private val syncInitAttempted = CompletableDeferred<Unit>()
  private val syncInitSucceeded = AtomicBoolean(false)
  private val isShutdown = AtomicBoolean(false)

  /**
   * Synchronously loads the config from the database on the calling thread.
   * Must be called once before the first async access to [state] (e.g. during
   * app startup before Compose composition) — it is the only code path that queries
   * the database here. After this call the [state] flow is immediately usable and
   * [loadDone] is already completed.
   *
   * The background coroutine launched in [init] never queries on its own; it waits
   * for this attempt to finish and stands down when it succeeded (the state and
   * [loadDone] are already set) or when the repository was shut down after a failure,
   * so it can never hold a database connection open against recovery mode.
   */
  fun initSync() {
    val outcome = runCatching {
      runBlocking {
        dao.getConfig() ?: ConfigEntity().also { dao.insert(it) }
      }
    }
    outcome
      .onSuccess { loaded ->
        _state.value = loaded
        loadDone.complete(Unit)
        syncInitSucceeded.set(true)
      }
      .onFailure {
        // The database could not be opened (most likely a migration failure). Stand the
        // async fallback down BEFORE releasing it, so it can never touch the database
        // that recovery mode is about to close — a query in flight there would keep a
        // connection, and with it the file lock, open indefinitely.
        isShutdown.set(true)
        scope.cancel()
        loadDone.completeExceptionally(
          IllegalStateException("ConfigRepository could not load the config", it)
        )
      }
    // Releases the async fallback below (or tells it to stand down, per above).
    syncInitAttempted.complete(Unit)
    outcome.getOrThrow()
  }

  init {
    scope.launch {
      // initSync owns the database's first query — it is what opens the database and
      // runs migrations. Querying the database here as well would race that, and when
      // the bootstrap fails (a migration failure) the racing query keeps a connection
      // open against recovery mode closing the database, so the files can never be
      // moved. This coroutine therefore only runs as a fallback: it waits for initSync
      // to finish, and stands down entirely when initSync succeeded or when recovery
      // mode is shutting the repository down.
      syncInitAttempted.await()
      if (syncInitSucceeded.get() || isShutdown.get()) return@launch
      try {
        val loaded = dao.getConfig() ?: ConfigEntity().also { dao.insert(it) }
        _state.value = loaded
        loadDone.complete(Unit)
      } catch (e: CancellationException) {
        throw e
      } catch (_: Exception) {
        // The config simply stays unloaded; recovery mode owns the cleanup on a
        // bootstrap failure.
      }
    }
  }

  suspend fun awaitLoaded() = loadDone.await()

  /**
   * Stops the repository for good: cancels every coroutine (so nothing can open the
   * database again) and fails any pending load waiters instead of leaving them parked.
   * Called when the database bootstrap fails so recovery mode can actually release the
   * database's file lock.
   */
  fun shutdown() {
    isShutdown.set(true)
    scope.cancel()
    loadDone.completeExceptionally(IllegalStateException("ConfigRepository was shut down before it could load"))
  }

  /**
   * Apply a transform to the current config, persist it, and update the state flow.
   * This launches a background coroutine (safe for UI callers).
   */
  fun update(transform: (ConfigEntity) -> ConfigEntity) {
    scope.launch {
      writeMutex.withLock {
        awaitLoaded()
        val new = transform(_state.value)
        dao.insert(new)
        _state.value = new
      }
    }
  }

  /**
   * Replace the whole config (suspend API).
   */
  suspend fun set(config: ConfigEntity) {
    writeMutex.withLock {
      awaitLoaded()
      dao.insert(config)
      _state.value = config
    }
  }
}
