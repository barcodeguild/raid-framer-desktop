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

class ConfigRepository(
  private val dao: ConfigDao,
  private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
  private val _state = MutableStateFlow(ConfigEntity()) // default until loaded
  val state: StateFlow<ConfigEntity> = _state.asStateFlow()

  private val loadDone = CompletableDeferred<Unit>()
  private val writeMutex = Mutex()

  init {
    scope.launch {
      val loaded = dao.getConfig() ?: ConfigEntity().also { dao.insert(it) }
      _state.value = loaded
      loadDone.complete(Unit)
    }
  }

  suspend fun awaitLoaded() = loadDone.await()

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
