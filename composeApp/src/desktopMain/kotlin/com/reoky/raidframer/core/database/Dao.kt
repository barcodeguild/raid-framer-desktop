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
  entities = [WindowStateEntity::class, ConfigEntity::class, PlayerCacheEntity::class, PlayerSessionTotalsEntity::class],
  version = SCHEMA_VERSION
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun getWindowStateDao(): WindowStateDao
  abstract fun getConfigDao(): ConfigDao
  abstract fun getPlayerCacheDao(): PlayerCacheDao
  abstract fun getPlayerSessionDao(): PlayerSessionDao
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

@Dao
interface PlayerSessionDao {
  // Newest first; used by "Previous Session" / "Last N Sessions" / "All Sessions" UIs.
  @Query("SELECT * FROM player_session_totals WHERE playerName = :playerName ORDER BY sessionEnd DESC")
  suspend fun getSessionsForPlayer(playerName: String): List<PlayerSessionTotalsEntity>

  // Same ordering but bounded for "Last N" lookups so we never aggregate more than the user asked for.
  @Query("SELECT * FROM player_session_totals WHERE playerName = :playerName ORDER BY sessionEnd DESC LIMIT :limit")
  suspend fun getRecentSessionsForPlayer(playerName: String, limit: Int): List<PlayerSessionTotalsEntity>

  // Distinct recent session starts for the dropdown / scope label (e.g. "5 sessions logged").
  @Query("SELECT COUNT(*) FROM player_session_totals WHERE playerName = :playerName")
  suspend fun getSessionCountForPlayer(playerName: String): Int

  // Composite primary key (playerName, sessionStart) makes this an upsert: re-archiving a session
  // is a no-op rather than a duplicate row. Cheaper than a defensive DELETE-then-INSERT.
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(session: PlayerSessionTotalsEntity)
}
