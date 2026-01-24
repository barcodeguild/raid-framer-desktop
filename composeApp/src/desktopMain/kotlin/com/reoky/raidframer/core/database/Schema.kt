package com.reoky.raidframer.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.reoky.raidframer.core.model.Faction
import com.reoky.raidframer.core.model.FactionStatus
import com.reoky.raidframer.ui.OverlayWindowType
import org.jetbrains.compose.resources.StringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.leadership_faction_hero
import raid_framer_desktop.composeapp.generated.resources.leadership_gm
import raid_framer_desktop.composeapp.generated.resources.leadership_guild_lead
import raid_framer_desktop.composeapp.generated.resources.leadership_none
import raid_framer_desktop.composeapp.generated.resources.leadership_raid_lead
import raid_framer_desktop.composeapp.generated.resources.leadership_shot_caller

const val SCHEMA_VERSION = 7 // increment this when making schema changes

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
  val miniGraphEnabled: Boolean = false,
  val splitChatEnabled: Boolean = false,
  val allowPVEDamage: Boolean = false,
  val gameScheduleHotkeyEnabled: Boolean = false,
  val useSadlyDotEyeOhhh: Boolean = false,
  val dragonBreathOverlayEnabled: Boolean = false,
  val killCounterMode: String = KillCounterMode.MOST_DAMAGE.name,
  val firstLaunch: Boolean = true,
  val playerName: String = "", // player's own name
  val playerFaction: String = "", // player's own faction

  // companion features
  val companionEnabled: Boolean = false,
  val companionShowRaidStatus: Boolean = true,
  val companionShowCharmedInChat: Boolean = true,
  val companionShowSilencedInChat: Boolean = true,
  val companionPlayCharmSound: Boolean = true,

  // future-proofing for more companion marks
  // currently in lua we can only track the location / mark one target at a time
  // so these are just toggles for potential future api whitelists where maybe we could do that?
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
  val leaderships: Int = LeadershipRole.NONE.value,

  // future proofing
  val lastKnownGearScore: Int = 0,
  val lastKnownFaction: String = Faction.UNKNOWN.value,
  val lastKnownFactionStatus: String = FactionStatus.UNKNOWN.value,
  val lastKnownGuild: String = "",
  val lastKnownRegion: String = "", // unused for now

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

  // lifetime stats (could be fun to track over time)
  val lifetimeTotalDamage: Long = 0L,
  val lifetimeTotalHealing: Long = 0L,
  val lifetimeTotalCCDelivered: Long = 0L,
  val lifetimeTotalDebuffsApplied: Long = 0L,
  val lifetimeTotalCharms: Long = 0L,
  val lifetimeTotalDistresses: Long = 0L,
  val lifetimeTotalSilences: Long = 0L,
  val lifetimeTotalGliderUses: Long = 0L,
  val lifetimeTotalItemSkillsUsed: Long = 0L,
  val lifetimeTotalKills: Long = 0L,
  val lifetimeTotalKillsKB: Long = 0L,
  val lifetimeTotalDeaths: Long = 0L,
  val lifetimeTotalDamageTaken: Long = 0L,
)

// global enums below for consolidation
enum class KillCounterMode {
  KILLING_BLOW,
  MOST_DAMAGE;

  companion object {
    fun fromString(value: String): KillCounterMode {
      return entries.find { it.name == value } ?: KILLING_BLOW
    }
  }
}

// Leadership roles enum
enum class LeadershipRole(val value: Int) {
  NONE(0),
  RAID_LEAD(1),
  GUILD_LEAD(2),
  FACTION_HERO(3),
  SHOT_CALLER(4),
  GM(5);

  val friendlyNameRes: StringResource
    get() = when (this) {
      NONE -> Res.string.leadership_none
      RAID_LEAD -> Res.string.leadership_raid_lead
      GUILD_LEAD -> Res.string.leadership_guild_lead
      FACTION_HERO -> Res.string.leadership_faction_hero
      SHOT_CALLER -> Res.string.leadership_shot_caller
      GM -> Res.string.leadership_gm
    }

  companion object {
    fun fromInt(value: Int): LeadershipRole {
      return entries.firstOrNull { it.value == value } ?: NONE
    }
  }
}
