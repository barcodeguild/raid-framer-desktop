package com.reoky.raidframer.core.database

/**
 * Dependency injection object singleton for database-related components.
 * The idea is that once initialized in main, its members can be accessed
 * globally without having to pass them to components like the interactors. mhmm!
 */
object RFDao {
  lateinit var configDao: ConfigDao
    private set
  lateinit var playerCacheDao: PlayerCacheDao
    private set
  lateinit var windowStateDao: WindowStateDao
    private set
  lateinit var playerSessionDao: PlayerSessionDao
    private set

  fun init(db: AppDatabase) {
    configDao = db.getConfigDao()
    playerCacheDao = db.getPlayerCacheDao()
    windowStateDao = db.getWindowStateDao()
    playerSessionDao = db.getPlayerSessionDao()
  }
}
