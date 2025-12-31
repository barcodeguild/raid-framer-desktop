package com.reoky.raidframer.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.reoky.raidframer.ui.OverlayWindowType

const val SCHEMA_VERSION = 1 // increment this when making schema changes

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
  val defaultArcheRageDirectory: String = "", // automatic path detection if empty (location of addons, logs, configs for AR)
  val tabbedDetectionEnabled: Boolean = false,
  val overlayResizingEnabled: Boolean = true,
  val firstLaunch: Boolean = true,
  val playerName: String = "", // player's own name
  val playerFaction: String = "", // player's own faction

  // companion features
  val companionEnabled: Boolean = false,
  val companionShowRaidStatus: Boolean = true,
  val companionShowCharmedInChat: Boolean = true,
  val companionShowSilencedInChat: Boolean = true,
  val companionMarkHVTHealers: Boolean = false,
  val companionMarkHVTDPS: Boolean = false,
  val companionMarkHVTCrowdControl: Boolean = false,
  val companionMarkSacDancers: Boolean = true,
  val companionMarkCharmedTargets: Boolean = true,
  val companionMarkSilencedTargets: Boolean = true,
  val companionMarkDistressedTargets: Boolean = true
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

  // future proofing
  val lastKnownGearScore: Int = 0,
  val lastKnownGuild: String = "",
  val lastKnownFaction: String = "",
  val lastKnownRegion: String = "",

  // glider usages
  val lastBDGlider: Long = 0L,
  val lastSkyEmpGlider: Long = 0L,
  val lastKrakenGlider: Long = 0L,
  val lastCrystalWings: Long = 0L,
  val lastRocketGlider: Long = 0L,

  // utility item usages
  val lastKrakenScepter: Long = 0L,
  val lastKrakenSpear: Long = 0L,
  val lastLibShieldPull: Long = 0L,
  val lastGreatclub: Long = 0L,
  val lastHalcyNecklace: Long = 0L,
  val lastSoulNecklace: Long = 0L,
  val lastHonorNodachi: Long = 0L,
  val lastJolaShield: Long = 0L,

  // potion/buff item usages
  val lastMinorHealingPotion: Long = 0L,
  val lastMajorHealingPotion: Long = 0L,
  val lastMinorManaPotion: Long = 0L,
  val lastMajorManaPotion: Long = 0L,
  val lastWildGinseng: Long = 0L,
  val lastJinhuiWish: Long = 0L,
  val lastBlueGoblet: Long = 0L, // melee attack
  val lastYellowGoblet: Long = 0L, // ranged
  val lastPurpleGoblet: Long = 0L, // magic
  val lastPinkGoblet: Long = 0L, // heals
  val lastGrayGoblet: Long = 0L, // focus
  val lastOrangeGoblet: Long = 0L, // received damage
  val lastAncientsPotion: Long = 0L,
  val lastDahutasBubble: Long = 0L,
  val lastWhisperPotion: Long = 0L,
  val lastRedBerryFruit: Long = 0L,
  val lastBlueBerryFruit: Long = 0L,
  val lastSecretGift: Long = 0L,
  val lastHonorElixir: Long = 0L,
  val lastWonderlandPVEBook: Long = 0L,

  // lifetime stats
  val lifetimeTotalDamage: Long = 0L,
  val lifetimeTotalHealing: Long = 0L,
  val lifetimeTotalDeaths: Long = 0L,
  val lifetimeTotalDamageTaken: Long = 0L,
  val lifetimeTotalCCDelivered: Long = 0L,
  val lifetimeTotalGliderUses: Long = 0L,
)

