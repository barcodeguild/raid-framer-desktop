package com.reoky.raidframer.core.config

import com.reoky.raidframer.core.database.ConfigDao
import com.reoky.raidframer.core.database.ConfigEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow

object RFConfig {
  private lateinit var repo: ConfigRepository

  private const val ERMMAGEHRD = "GlobalConfig not initialized. Call RFConfig.init(dao) first."

  /**
   * Initialize once at app start (call from platform main). Safe to call multiple times.
   */
  fun init(dao: ConfigDao, scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())) {
    if (::repo.isInitialized) return
    repo = ConfigRepository(dao, scope)
  }

  /**
   * Current config state as a StateFlow.
   * Example: val config by RFConfig.state.collectAsState()
   * Example Two: RFConfig.state.collect { config -> ... }
   * Use config.* to access individual properties. Automatic recomposition when new value is != while collecting state.
   */
  val state: StateFlow<ConfigEntity>
    get() {
      check(::repo.isInitialized) { ERMMAGEHRD }
      return repo.state
    }

  /**
   * Apply a transform to the current config that updates the state flow...
   * Friend's example: Button(onClick = { RFConfig.update { it.copy(key = "value") } })
   */
  fun update(transform: (ConfigEntity) -> ConfigEntity) {
    check(::repo.isInitialized) { ERMMAGEHRD }
    repo.update(transform)
  }

  /**
   * Replace the whole config (suspend API).
   */
  suspend fun set(config: ConfigEntity) {
    check(::repo.isInitialized) { ERMMAGEHRD }
    repo.set(config)
  }

  suspend fun awaitLoaded() {
    check(::repo.isInitialized) { ERMMAGEHRD }
    repo.awaitLoaded()
  }
}
