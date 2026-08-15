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

const val SCHEMA_VERSION = 39

const val MAX_EXPORT_BACKGROUND_DIMNESS = 0.70f

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
  val windowOpacity: Float = 0.43f,
  val windowColor: Int = 0xFF000000.toInt(),
  val dragonBreathOverlayEnabled: Boolean = false,
  val killCounterMode: String = KillCounterMode.MOST_DAMAGE.name,
  val firstLaunch: Boolean = true,
  val installationFinalized: Boolean = false, // user confirmed their game directory
  val playerName: String = "", // player's own name
  val playerFaction: String = "", // player's own faction

  // companion features
  val companionEnabled: Boolean = false,
  val companionShowRaidStatus: Boolean = true,
  val companionShowCharmedInChat: Boolean = true,
  val companionShowSilencedInChat: Boolean = true,
  val companionShowDistressedInChat: Boolean = true,
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
  val companionMarkDistressedTargets: Boolean = true,

  // Companion debug and info display toggles 7/11/26
  val companionShowDebugInfo: Boolean = false,
  val companionShowDeathsPerMinute: Boolean = false,

  // Combat overlay column visibility toggles
  val combatShowDamageColumn: Boolean = true,
  val combatShowHealsColumn: Boolean = true,
  val combatShowCCColumn: Boolean = true,

  // Combat overlay custom category columns (up to 3)
  val combatCustomCategory1: String = "",
  val combatCustomCategory2: String = "",
  val combatCustomCategory3: String = "",

  // Combat overlay UX
  val combatControlsFadeEnabled: Boolean = true, // fade icon controls out when cursor leaves the overlay

  // New session recording
  val lastSessionTitle: String = "",
  val lastSessionStart: Long = 0L,
  val lastSessionType: String = "",
  val lastSessionDurationMs: Long = 0L,
  val lastSessionExportDir: String = "",
  val exportIncludeRawJsonLogs: Boolean = false,
  val previousSessionStart: Long = 0L,

  // Seed table config
  val seedTableLastAppliedTimestamp: Long = 0L,
  val seedTableFileName: String = "",

  // Language preference (empty = use system locale)
  val preferredLanguage: String = "",

  // Auto-update settings
  val autoUpdateEnabled: Boolean = true,

  // Ode to Recovery heal filtering
  val allowOdeToRecoveryCountAsHeals: Boolean = false,

  // PNG export background settings
  val exportPngEnabled: Boolean = true,
  // Comma-separated AppLocale codes for additional PNG exports. The current app language is always added.
  val exportPngLanguages: String = "",
  val exportBackgroundSelection: String = "REOKY",
  val exportCustomBackgroundPath: String = "",
  val exportBackgroundColor: Int = 0xFF000000.toInt(),
  val exportBackgroundDimness: Float = 0.20f,

  // Item Use Overlay toggle
  val itemUseOverlayEnabled: Boolean = false,

  // Combat overlay spec icons toggle
  val combatShowSpecIcons: Boolean = true
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
  val lastRavenspineWings: Long = 0L,
  val lastTWTGlider: Long = 0L,
  val lastMoonshadowGlider: Long = 0L,

  // utility item usages
  val lastKrakenScepter: Long = 0L,
  val lastKrakenSpear: Long = 0L,
  val lastKrakenShield: Long = 0L,
  val lastLibShieldPull: Long = 0L,
  val lastGreatclub: Long = 0L,
  val lastHalcyNecklace: Long = 0L,
  val lastSoulNecklace: Long = 0L,
  val lastHonorNodachi: Long = 0L,
  val lastJolaShield: Long = 0L,

  // snake weapons and shield usages
  val lastSnakeGreatsword: Long = 0L,
  val lastSnakeShield: Long = 0L,
  val lastSnakeSword: Long = 0L,
  val lastSnakeAxe: Long = 0L,
  val lastSnakeScepter: Long = 0L,
  val lastSnakeGun: Long = 0L,

  // black dragon items usages
  val lastBdShield: Long = 0L,
  val lastBdClub: Long = 0L,
  val lastBdBow: Long = 0L,
  val lastBdRifle: Long = 0L,
  val lastBdStaff: Long = 0L,
  val lastBdSword: Long = 0L,
  val lastBd2hSword: Long = 0L,

  // anthalon items usages
  val lastAnthSetPull: Long = 0L,
  val lastGardenAnthSetPull: Long = 0L,

  // library dungeon items usages
  val lastLibBow: Long = 0L,
  val lastLibDagger: Long = 0L,
  val lastLibShortspear: Long = 0L,
  val lastLibStaff: Long = 0L,

  // serpentis dungeon items usages
  val lastSerpStaff: Long = 0L,
  val lastSerpShield: Long = 0L,

  // mistsong dungeon items usages
  val lastMistNodachi: Long = 0L,
  val lastMistDagger: Long = 0L,
  val lastMistShield: Long = 0L,

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
  val lifetimeTotalBuffsApplied: Long = 0L,
  val lifetimeTotalDebuffsApplied: Long = 0L,
  val lifetimeTotalCharms: Long = 0L,
  val lifetimeTotalSongs: Long = 0L,
  val lifetimeTotalDistresses: Long = 0L,
  val lifetimeTotalSilences: Long = 0L,
  val lifetimeTotalGliderUses: Long = 0L,
  val lifetimeTotalItemSkillsUsed: Long = 0L,
  val lifetimeTotalPotionUsages: Long = 0L,
  val lifetimeTotalKills: Long = 0L,
  val lifetimeTotalKillsKB: Long = 0L,
  val lifetimeTotalDeaths: Long = 0L,
  val lifetimeTotalDamageTaken: Long = 0L,
  val lifetimeTotalHealsReceived: Long = 0L,
  val lifetimeTotalTigerStrikes: Long = 0L,
  val lifetimeTotalFreezes: Long = 0L,
  val lifetimeTotalTrips: Long = 0L,
  val lifetimeTotalBubbles: Long = 0L,
  val lifetimeTotalBracings: Long = 0L,
  val lifetimeTotalShieldStrip: Long = 0L,
  val lifetimeTotalWeaponDisables: Long = 0L,
  val lifetimeTotalPotionDisables: Long = 0L,
  val lifetimeTotalBdGlider: Long = 0L,
  val lifetimeTotalCrystalWings: Long = 0L,
  val lifetimeTotalGliderDisables: Long = 0L,
  val lifetimeTotalProvoked: Long = 0L,
  val lifetimeTotalDefiance: Long = 0L,
  val lifetimeTotalGardenDefiance: Long = 0L,
  val lifetimeTotalPurges: Long = 0L,
  val lifetimeTotalSacDances: Long = 0L,
  val lifetimeTotalDeepTranquility: Long = 0L,
  val lifetimeTotalDeependDebuff: Long = 0L,
  val lifetimeTotalThrowDagger: Long = 0L,
  val lifetimeTotalStuns: Long = 0L,
  val lifetimeTotalStaggers: Long = 0L,
  val lifetimeTotalPetrification: Long = 0L,
  val lifetimeTotalAbsorbLifeforce: Long = 0L,
  val lifetimeTotalCorrosiveBarrage: Long = 0L,
  val lifetimeTotalBlindedByCrows: Long = 0L,
  val lifetimeTotalMistSunder: Long = 0L,
  val lifetimeTotalRegularSunder: Long = 0L,
  val lifetimeTotalImpaleImmunity: Long = 0L,
  val lifetimeTotalProtectiveWings: Long = 0L,
  val lifetimeTotalCourageousAction: Long = 0L,
  val lifetimeTotalManaBarrier: Long = 0L,
  val lifetimeTotalRevive: Long = 0L,
)

/*
 * Stores a per-player snapshot of session totals captured at the end of a recording session.
 * One row is written per (playerName, sessionStart) the moment a new session begins (and when
 * a session is stopped without a follow-up start), so the player card overlay can show historical
 * session totals ("Previous Session", "Last 2 Sessions", "All Sessions", etc.) in addition to
 * the in-memory current session. The composite primary key makes the archive step idempotent.
 */
@Entity(
  tableName = "player_session_totals",
  primaryKeys = ["playerName", "sessionStart"]
)
data class PlayerSessionTotalsEntity(
  val playerName: String,
  val sessionStart: Long, // UTC ms; from RFConfig.lastSessionStart when archive happens
  val sessionEnd: Long,   // UTC ms; System.currentTimeMillis() at archive time
  val sessionType: String = "", // e.g. "Kraken", "Halcy", "Custom", "manual_stop"
  val sessionTitle: String = "", // session file/title at the time of archive

  val totalDamage: Long = 0L,
  val totalHealing: Long = 0L,
  val totalCC: Int = 0,
  val totalBuffs: Int = 0,
  val totalDebuffs: Int = 0,
  val totalCharms: Int = 0,
  val totalSongs: Int = 0,
  val totalDistresses: Int = 0,
  val totalSilences: Int = 0,
  val totalGliderUses: Int = 0,
  val totalItemSkills: Int = 0,
  val totalPotions: Int = 0,
  val totalKills: Int = 0,
  val totalKillsKB: Int = 0,
  val totalDeaths: Int = 0,
  val totalDamageTaken: Int = 0,
  val totalHealsReceived: Int = 0,
  val totalOdeHeals: Long = 0L,
  val totalTigerStrikes: Int = 0,
  val totalFreezes: Int = 0,
  val totalTrips: Int = 0,
  val totalBubbles: Int = 0,
  val totalBracings: Int = 0,
  val totalShieldStrip: Int = 0,
  val totalWeaponDisables: Int = 0,
  val totalPotionDisables: Int = 0,
  val totalBdGlider: Int = 0,
  val totalCrystalWings: Int = 0,
  val totalGliderDisables: Int = 0,
  val totalProvoked: Int = 0,
  val totalDefiance: Int = 0,
  val totalGardenDefiance: Int = 0,
  val totalPurges: Int = 0,
  val totalSacDances: Int = 0,
  val totalDeepTranquility: Int = 0,
  val totalDeependDebuff: Int = 0,
  val totalThrowDagger: Int = 0,
  val totalStuns: Int = 0,
  val totalStaggers: Int = 0,
  val totalPetrification: Int = 0,
  val totalAbsorbLifeforce: Int = 0,
  val totalCorrosiveBarrage: Int = 0,
  val totalBlindedByCrows: Int = 0,
  val totalMistSunder: Int = 0,
  val totalRegularSunder: Int = 0,
  val totalImpaleImmunity: Int = 0,
  val totalProtectiveWings: Int = 0,
  val totalCourageousAction: Int = 0,
  val totalManaBarrier: Int = 0,
  val totalRevive: Int = 0,
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
