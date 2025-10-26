package com.reoky.raidframer.core.database

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

@Database(
  entities = [WindowStateEntity::class, ConfigEntity::class],
  version = 1 // be sure to increment this friends!
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun getWindowStateDao(): WindowStateDao
  abstract fun getConfigDao(): ConfigDao
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

//@Entity(tableName = "player_cache")
//data class PlayerCacheEntity(
//  @PrimaryKey val playerName: String,
//  val lastSeen: Long = System.currentTimeMillis(),
//  val lastKnownSpec: String = "",
//  val lastKnownLevel: Int = 0,
//  val lastKnownGuild: String = "",
//  val lastKnownFaction: String = "",
//  val lastKnownRegion: String = "",
//  val lifetimeTotalDamage: Long = 0L,
//  val lifetimeTotalHealing: Long = 0L,
//  val lifetimeTotalDeaths: Long = 0L,
//  val lifetimeTotalDamageTaken: Long = 0L,
//  val lifetimeTotalCCDelivered: Long = 0L,
//)
@Dao interface PlayerCacheDao {
  @Query("SELECT * FROM player_cache WHERE playerName = :name")
  suspend fun getPlayerCache(name: String): PlayerCacheEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(cache: PlayerCacheEntity)
}
