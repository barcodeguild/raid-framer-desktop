package com.reoky.raidframer.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.reoky.raidframer.core.definitions.SkillTreeDefinition
import com.reoky.raidframer.core.definitions.SpecType
import com.reoky.raidframer.ui.OverlayWindowType

/*
 * Used to remember window positions since friends tend to want to position their overlays
 * how they like. ~
 */
@Entity(tableName = "window_states")
data class WindowStateEntity(
  @PrimaryKey val overlayType: String,
  val windowType: OverlayWindowType,
  val lastPositionXDp: Float,
  val lastPositionYDp: Float,
  val lastWidthDp: Float,
  val lastHeightDp: Float,
  val isVisible: Boolean
)

/*
 * Holds the configuration for the app. This is a singleton entity, so it always has id 0.
 * Similarly, the config is held in memory as a singleton object also.
 */
@Entity(tableName = "config")
data class ConfigEntity(
  @PrimaryKey val id: Int = 0, // Singleton config, always id 0
  val defaultLogPath: String = "",
  val tabbedDetectionEnabled: Boolean = false,
  val overlayResizingEnabled: Boolean = true,
  val searchEverywhere: Boolean = false,
  val firstLaunch: Boolean = true,
  val playerName: String = ""
)

/*
 * Holds a list of players that have been seen out in the field. This cache is used to guess
 * player specs, and establish a collection of real players that have been seen in the game to
 * differentiate between real players and NPCs/mobs. When cleared, the app will have to discover
 * players again.
 */
@Entity(tableName = "player_cache")
data class PlayerCacheEntity(
  @PrimaryKey val playerName: String,
  val lastSeen: Long = System.currentTimeMillis(),
  val lastKnownSpec: String = "",
  val lastKnownLevel: Int = 0,
  val lastKnownGuild: String = "",
  val lastKnownFaction: String = "",
  val lastKnownRegion: String = "",
  val lifetimeTotalDamage: Long = 0L,
  val lifetimeTotalHealing: Long = 0L,
  val lifetimeTotalDeaths: Long = 0L,
  val lifetimeTotalDamageTaken: Long = 0L,
  val lifetimeTotalCCDelivered: Long = 0L,
)
