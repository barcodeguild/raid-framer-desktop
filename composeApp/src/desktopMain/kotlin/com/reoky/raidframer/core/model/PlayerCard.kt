package com.reoky.raidframer.core.model

import com.reoky.raidframer.core.database.PlayerCacheEntity
import org.jetbrains.compose.resources.StringResource

/**
 * Represents a real player character with associated cached data and recent events.
 *
 * See: [PlayerCacheEntity] for database storage, and also PlayerCardExtensions for helper methods. eek
 */
data class PlayerCard (
  val name: String, // player character name
  val recentCids: List<String>, // these change so keeping a list
  val lastEvent: Long, // timestamp of the last event seen for this player
  val lastKnownFaction: String = Faction.UNKNOWN.value, // see Faction definitions
  val lastKnownFactionStatus: String = FactionStatus.UNKNOWN.value, // friendly / hostile / neutral
  val lastKnownGuild: String = "", // string (not used for anything yet, thinking maybe dominion / duels support for future release)
  val lastKnownGearScore: Int = 0, // cached gear score for quick access
  val lastGliderUse: Long = 0L, // this is because of a game bug where the glider debuff is applied twice for every wing closing event (we simply don't allow more than one open in the same second as a safeguard)
  val lastItemUse: Long = 0L, // cooldown to help prevent double counting of item uses
  val currentBuild: String,
  val currentRole: Int = -1, // current spec / role int value
  val leaderships: Int = 0, // 0 = none, 1 = raid lead, 2 = guild lead, 3 = faction hero, 4 = shot-caller, 5 = gm (reserved for admins use if they use this)

  // cache management for player data
  val isLoaded: Boolean = false,
  val isRealPlayer: Boolean = false, // determined after some analysis
  val isShotcaller: Boolean = false, // is the player a shotcaller (frequently issues commands in voice chat, determined externally)
  val cache: PlayerCacheEntity?, // saved to the database

  // important buffs/debuffs currently active on the player
  val isBuildingAggression: Boolean = false,
  val isCharmed: Boolean = false,
  val isDistressed: Boolean = false,

  // held in memory only for each player, this prevents having singular exhaustive lists of events like before
  // are cleared between sessions and not persisted to the database
  val recentCastSuccessfulCastEvents: List<SuccessfulCastEvent> = listOf(),
  val recentCastEvents: List<CastingEvent> = listOf(),
  val recentDamageEvents: List<DamageEvent> = listOf(),
  val recentHealEvents: List<HealEvent> = listOf(),
  val recentBuffGainedEvents: List<BuffGainedEvent> = listOf(),
  val recentBuffEndedEvents: List<BuffEndedEvent> = listOf(),
  val recentDebuffGainedEvents: List<DebuffGainedEvent> = listOf(),
  val recentDebuffEndedEvents: List<DebuffEndedEvent> = listOf(),
  val recentKills: Map<Long, String> = mapOf(), // Timestamp -> Victim Name
  val recentKilledBys: Map<Long, String> = mapOf(), // Timestamp -> Killer Name (stored in reverse for ez lookup)
  val recentKillsKB: Map<Long, String> = mapOf(), // Timestamp -> Victim Name (Killing Blow Method)
  val recentKilledByKB: Map<Long, String> = mapOf(), // Timestamp -> Killer Name (Killing Blow Method)
  val recentSkillItemUsages: List<Triple<Long, StringResource, String>> = listOf(), // List of Timestamp, Item Name Resource (because of i18n), and Target Name

  // cost more memory but useful for quick access and uses less CPU than scanning all events on all cards
  // this solution is similar to what facebook did before switching to graph databases; effectively storing some data twice by stashing it all in memcached
  // don't look at me aaaaaaaaaa
  val recentBuffAppliedEvents: List<BuffAppliedEvent> = listOf(),
  val recentDebuffAppliedEvents: List<DebuffAppliedEvent> = listOf(),

  // session edge weights for battle graph: per-player adjacency lists (graph-database style)
  val sessionDamageToPlayer: Map<String, Long> = mapOf(),   // target name -> damage dealt to them
  val sessionDamageFromPlayer: Map<String, Long> = mapOf(), // source name -> damage received from them
  val sessionHealToPlayer: Map<String, Long> = mapOf(),     // target name -> heals done to them
  val sessionHealFromPlayer: Map<String, Long> = mapOf(),   // source name -> heals received from them
  val sessionCCToPlayer: Map<String, Int> = mapOf(),        // target name -> CC applied to them
  val sessionCCFromPlayer: Map<String, Int> = mapOf(),      // source name -> CC received from them

  // per-target spell breakdowns for battle graph edge hover details
  val sessionDamageToPlayerBySpell: Map<String, Map<String, Long>> = mapOf(),  // target -> spell -> damage
  val sessionHealToPlayerBySpell: Map<String, Map<String, Long>> = mapOf(),    // target -> spell -> heal
  val sessionCCToPlayerBySpell: Map<String, Map<String, Int>> = mapOf(),       // target -> debuff -> count

  // Kill adjacency for battle graph
  val sessionKillsToPlayer: Map<String, Int> = mapOf(),                         // victim -> kill count (most-damage method)
  val sessionKillsToPlayerBySpell: Map<String, Map<String, Long>> = mapOf(),    // victim -> spell -> cumulative damage in pre-death windows

  // Debuff adjacency for battle graph (ALL debuffs)
  val sessionDebuffToPlayer: Map<String, Int> = mapOf(),                         // target -> debuff count
  val sessionDebuffToPlayerBySpell: Map<String, Map<String, Int>> = mapOf(),    // target -> debuff name -> count

  // Buff adjacency for battle graph
  val sessionBuffToPlayer: Map<String, Int> = mapOf(),                           // target -> buff count
  val sessionBuffToPlayerBySpell: Map<String, Map<String, Int>> = mapOf(),      // target -> buff name -> count

  // Charm adjacency for battle graph
  val sessionCharmToPlayer: Map<String, Int> = mapOf(),                          // target -> charm count
  val sessionCharmToPlayerBySpell: Map<String, Map<String, Int>> = mapOf(),     // target -> debuff name -> count

  // Distress adjacency for battle graph
  val sessionDistressToPlayer: Map<String, Int> = mapOf(),                       // target -> distress count
  val sessionDistressToPlayerBySpell: Map<String, Map<String, Int>> = mapOf(),  // target -> debuff name -> count

  // Silence adjacency for battle graph
  val sessionSilenceToPlayer: Map<String, Int> = mapOf(),                        // target -> silence count
  val sessionSilenceToPlayerBySpell: Map<String, Map<String, Int>> = mapOf(),   // target -> debuff name -> count

  // Per-spell aggregate maps for dropdown filtering
  val sessionSpellBuffMap: Map<String, Int> = mapOf(),     // buff name -> total count across all targets
  val sessionSpellDebuffMap: Map<String, Int> = mapOf(),   // debuff name -> total count across all targets

  // session counter totals : when a new session starts, these reset to 0 and the totals are written to the database cache
  val sessionSpellDamageMap: Map<String, Long> = mapOf(), // spell name -> total damage dealt this session (never capped, used for accurate spell breakdown)
  val sessionSpellHealMap: Map<String, Long> = mapOf(), // spell name -> total healing done this session
  val sessionSpellCCMap: Map<String, Int> = mapOf(), // debuff name -> count of CC applied this session
  val sessionDamageTotal: Long = 0L,
  val sessionHealTotal: Long = 0L,
  val sessionCCTotal: Int = 0, // only crowd control effects (Snare, Stun, Silence, Trip, etc)
  val sessionBuffTotal: Int = 0, // all buffs (including sac applications of encouragement)
  val sessionDebuffTotal: Int = 0, // all debuffs
  val sessionCharmTotal: Int = 0,
  val sessionSongsTotal: Int = 0,
  val sessionDistressTotal: Int = 0,
  val sessionSilenceTotal: Int = 0,
  val sessionGliderTotal: Int = 0,
  val sessionItemSkillTotal: Int = 0,
  val sessionPotionTotal: Int = 0,
  val sessionKillTotal: Int = 0,
  val sessionKillTotalKB: Int = 0,
  val sessionDeathTotal: Int = 0,
  val sessionDamageTakenTotal: Int = 0,
  val sessionHealsReceivedTotal: Int = 0,
  val sessionOdeHealsTotal: Long = 0,
)

