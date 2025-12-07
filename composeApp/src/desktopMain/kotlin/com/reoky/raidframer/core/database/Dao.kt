package com.reoky.raidframer.core.database

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlin.text.orEmpty

@Database(
  entities = [WindowStateEntity::class, ConfigEntity::class, PlayerCacheEntity::class],
  version = SCHEMA_VERSION
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun getWindowStateDao(): WindowStateDao
  abstract fun getConfigDao(): ConfigDao
  abstract fun getPlayerCacheDao(): PlayerCacheDao
}

@Dao
interface WindowStateDao {
  @Query("SELECT * FROM window_states")
  suspend fun getAll(): List<WindowStateEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(state: WindowStateEntity)

  @Query("DELETE FROM window_states")
  suspend fun deleteAll()
}

@Dao interface ConfigDao {
  @Query("SELECT * FROM config WHERE id = 0")
  suspend fun getConfig(): ConfigEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(config: ConfigEntity)
}

@Dao
interface PlayerCacheDao {
  @Query("SELECT * FROM player_cache WHERE playerName = :name")
  suspend fun getPlayerCacheFor(name: String): PlayerCacheEntity?

  @Query("SELECT * FROM player_cache ORDER BY lastSeen DESC LIMIT 10000")
  suspend fun getRecentPlayerCacheMetadata(): List<PlayerCacheEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(cache: PlayerCacheEntity)

  @Query("SELECT COUNT(*) FROM player_cache")
  suspend fun getPlayerCount(): Int
}
