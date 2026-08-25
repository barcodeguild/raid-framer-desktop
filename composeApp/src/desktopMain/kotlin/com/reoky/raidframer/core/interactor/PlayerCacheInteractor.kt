package com.reoky.raidframer.core.interactor

import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshotFlow
import com.reoky.raidframer.AppState
import com.reoky.raidframer.core.calc.ArrangementMode
import com.reoky.raidframer.core.calc.MetricRawSample
import com.reoky.raidframer.core.calc.RaidOrganizer
import com.reoky.raidframer.core.calc.RealtimeComputer
import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.database.RFDao
import com.reoky.raidframer.core.database.PlayerCacheEntity
import com.reoky.raidframer.core.database.PlayerSessionTotalsEntity
import com.reoky.raidframer.core.definitions.SkillTreeType
import com.reoky.raidframer.core.definitions.SpecType
import com.reoky.raidframer.core.definitions.findSkillTreeForSpell
import com.reoky.raidframer.core.definitions.petSkillWhitelist
import com.reoky.raidframer.core.helpers.createCacheObject
import com.reoky.raidframer.core.seedtable.SeedTableInteractor
import com.reoky.raidframer.core.seedtable.SeedTableEntry
import com.reoky.raidframer.core.helpers.guessPlayerRole
import com.reoky.raidframer.core.helpers.resetSession
import com.reoky.raidframer.core.model.*
import com.reoky.raidframer.core.serialization.Party
import com.reoky.raidframer.core.serialization.RaidFramePayload
import com.reoky.raidframer.core.serialization.TargetUpdatedPayload
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.StringResource

/**
 * Keeps a cache of players and NPCs seen in the log. When a player is first detected their player card is loaded
 * from the database (if it exists) or created new (if it doesn't). Cards are held in memory, and continually written
 * back to the database for persistence. This allows the app to remember that a player is real vs an NPC across sessions,
 * instead of having to re-discover players every time the app is launched. (Which reduces accuracy of PvP vs PvE damage stats.)
 * We have to do this to accurately keep track of damage totals and raid status over time.
 */
object PlayerCacheInteractor : Interactor() {

  const val TAG = "PlayerCacheInteractor"

  // Rider spell IDs for breath/rocket counters
  private val DRAGON_BREATH_RIDER_SPELL_IDS = setOf(38418, 38699, 38701) // Red, Green, Black
  private const val DRAKE_BREATH_RIDER_SPELL_ID = 35787 // Thunderbreath (Rider)
  private const val GUIDED_MISSILES_RIDER_SPELL_ID = 46058
  // Damage spell IDs that result from dragon breath (used for damage attribution)
  private val DRAGON_BREATH_DAMAGE_SPELL_IDS = setOf(22608, 22609, 22618) // Clinging Flame x2, Clinging Flame Explosion
  // Damage spell IDs that result from drake breath
  private val DRAKE_BREATH_DAMAGE_SPELL_IDS = setOf(35786, 21015) // Thunderbreath, Thunderbreath Aftershock
  // Damage spell IDs that result from guided missiles
  private val GUIDED_MISSILES_DAMAGE_SPELL_IDS = setOf(46055) // Guided Missiles (non-rider damage)

  // Mapping of all the players (and NPCs) sorted in no particular order
  val realtimeComputer = RealtimeComputer(windowBuckets = 60, bucketMillis = 10_000L)
  private val raids = mutableStateMapOf<Int, List<Party>>()
  private val raidAttendance = mutableStateMapOf<Int, MutableSet<String>>()
  private val raidDepartures = mutableStateMapOf<Int, MutableSet<String>>()
  private val raidBuffHistory = mutableMapOf<String, RaidBuffSnapshot>()
  private const val RAID_BUFF_HISTORY_RETENTION_MS = 6L * 60L * 60L * 1000L
  // Distance (meters) beyond which a raid member is considered out of range and cannot be
  // reliably scanned. An empty buff scan from beyond this range is not evidence the player
  // lacks buffs — it just means the app couldn't see them.
  private const val OUT_OF_RANGE_DISTANCE = 115
  private var lastRaidBuffHistoryCleanupAt = 0L

  // --- Coherence tracking (time-in-range metric) ---
  // Distances (meters) for the coherence thresholds. Distances are delivered from the recorder's
  // (user's) perspective via X2Unit:UnitDistance, so these measure how tightly the raid clusters
  // around the recorder.
  private const val COHERENCE_RENDER_DISTANCE = 120
  private const val COHERENCE_RAID_DISTANCE = 60
  private const val COHERENCE_CLUMP_DISTANCE = 15
  // A real raid (for the recording-time denominator) is 5+ players regardless of co-raid.
  private const val COHERENCE_MIN_RAID_SIZE = 5
  // Guard against the IPC delivering batched/gapped frames: only count elapsed time slices that
  // look like live updates (1–60s). Larger gaps are treated as dead air, not recording time.
  private const val COHERENCE_MAX_FRAME_DELTA_MS = 60_000L
  // Raid-adjusted recording time (ms) — the global denominator for coherence percentages.
  private val coherenceRecordingMs = mutableLongStateOf(0L)
  // Time (ms) the recorder was within render range (120m) of at least half of the raid.
  private val coherenceRecorderMs = mutableLongStateOf(0L)
  private var lastCoherenceFrameAtMs = 0L

  val coherenceRecordingMsMs: Long get() = coherenceRecordingMs.longValue
  val coherenceRecorderMsMs: Long get() = coherenceRecorderMs.longValue
  private val _raidDeparturesFlow = MutableStateFlow<Map<Int, Set<String>>>(emptyMap())
  val raidDeparturesFlow: StateFlow<Map<Int, Set<String>>> = _raidDeparturesFlow.asStateFlow()
  private val cards = mutableStateMapOf<String, PlayerCard>()
  val trackedPlayerCount: Int get() = cards.size
  private val petCards = mutableStateMapOf<String, PetCard>()
  // Life Mend caster tracking: target → caster (updated on SPELL_AURA_APPLIED/REMOVED for buff 25875)
  private val lifeMendCasterMap = mutableMapOf<String, String>()
  // Leech steal detection: source → timestamp of last Leech cast (spellId 10104)
  // Used to filter out buffs stolen via Leech that would otherwise be misattributed as Life Mend casts.
  private const val LEECH_SPELL_ID = 10104
  private const val LEECH_WINDOW_MS = 2000L
  private val recentLeechCasts = mutableMapOf<String, Long>()
  private val mutex = Mutex() // to protect critical sections during player card updates from other threads
  private var archiveJob: Job? = null
  private var preSessionCacheSnapshot: MutableMap<String, PlayerCacheEntity?> = mutableMapOf()
  private var lastMemoryCensusAt = 0L

  // Keep one sampled card-list snapshot for all derived UI pumps. This preserves
  // eager availability while avoiding one Compose snapshot collector per pump.
  private val cardSnapshots = snapshotFlow { cards.values.toList() }
    .sample(250L)
    .shareIn(scope, SharingStarted.Eagerly, replay = 1)
    .sample(250L)
    .shareIn(scope, SharingStarted.Eagerly, replay = 1)

  init {
    scope.launch {
      refreshOwnSessionCount()
      while (true) {
        realtimeComputer.push(MetricRawSample(System.currentTimeMillis(), 5000.0))
        delay(1000)
      }
    }
  }

  // The main interactor event loop
  // Takes a snapshot of values to iterate. Iterating the map directly while updates happen
  // (even with ConcurrentHashMap/MutableStateMap) can be risky with heavy logic,
  // and we want to perform calculations without holding a lock. Ok friends!?
  override suspend fun interact() {
    val snapshot = cards.values.toList()
    val now = System.currentTimeMillis()
    if (snapshot.isNotEmpty() && now - lastMemoryCensusAt >= 300_000L) {
      val recentEvents = snapshot.sumOf {
        it.recentCastSuccessfulCastEvents.size + it.recentCastEvents.size +
          it.recentDamageEvents.size + it.recentHealEvents.size +
          it.recentBuffGainedEvents.size + it.recentBuffEndedEvents.size +
          it.recentDebuffGainedEvents.size + it.recentDebuffEndedEvents.size +
          it.recentBuffAppliedEvents.size + it.recentDebuffAppliedEvents.size
      }
      // Flat per-target adjacency maps + nested per-target/per-spell maps + dropdown spell maps.
      val adjacencyEntries = snapshot.sumOf {
        it.sessionDamageToPlayer.size + it.sessionDamageFromPlayer.size +
          it.sessionHealToPlayer.size + it.sessionHealFromPlayer.size +
          it.sessionCCToPlayer.size + it.sessionCCFromPlayer.size +
          it.sessionDebuffToPlayer.size + it.sessionBuffToPlayer.size +
          it.sessionCharmToPlayer.size + it.sessionDistressToPlayer.size +
          it.sessionSilenceToPlayer.size + it.sessionKillsToPlayer.size +
          it.sessionDamageToPlayerBySpell.values.sumOf { map -> map.size } +
          it.sessionHealToPlayerBySpell.values.sumOf { map -> map.size } +
          it.sessionCCToPlayerBySpell.values.sumOf { map -> map.size } +
          it.sessionDebuffToPlayerBySpell.values.sumOf { map -> map.size } +
          it.sessionBuffToPlayerBySpell.values.sumOf { map -> map.size } +
          it.sessionCharmToPlayerBySpell.values.sumOf { map -> map.size } +
          it.sessionDistressToPlayerBySpell.values.sumOf { map -> map.size } +
          it.sessionSilenceToPlayerBySpell.values.sumOf { map -> map.size } +
          it.sessionKillsToPlayerBySpell.values.sumOf { map -> map.size } +
          it.sessionSpellBuffMap.size + it.sessionSpellDebuffMap.size
      }
      // Per-player spell damage/heal/CC breakdown maps (never capped) + recent kill/item maps.
      val spellMapEntries = snapshot.sumOf {
        it.sessionSpellDamageMap.size + it.sessionSpellHealMap.size + it.sessionSpellCCMap.size
      }
      val killMapEntries = snapshot.sumOf {
        it.recentKills.size + it.recentKilledBys.size +
          it.recentKillsKB.size + it.recentKilledByKB.size + it.recentSkillItemUsages.size
      }
      // Pet cards also retain recent events; count them separately.
      val petEvents = petCards.values.sumOf {
        it.recentDamageEvents.size + it.recentDebuffAppliedEvents.size
      }
      // Raid buff history: per-player buff-id snapshot sets retained for the grace/retention window.
      val raidHistoryPlayers = raidBuffHistory.size
      val raidHistoryBuffIds = raidBuffHistory.values.sumOf { it.buffIds.size }
      Log.info(TAG, "Session retention status: cards=${snapshot.size} pets=${petCards.size} petEvents=$petEvents recentEvents=$recentEvents adjacencyEntries=$adjacencyEntries spellMaps=$spellMapEntries killMaps=$killMapEntries raidHistory=$raidHistoryPlayers raidHistoryBuffIds=$raidHistoryBuffIds graphPlayers=${GraphDataInteractor.getPlayerNames().size}")
      lastMemoryCensusAt = now
    }
    snapshot.forEach { card ->
      val name = card.name
      if (!card.isRealPlayer && card.shouldUpgradeToPlayer()) {
        scope.launch {
          mutex.withLock {
            // Re-fetch inside lock to ensure we don't overwrite concurrent changes
            cards[name]?.let { current ->
              // Verify condition still holds
              if (!current.isRealPlayer && current.shouldUpgradeToPlayer()) {
                val upgradedCard = current.copy(
                  isRealPlayer = true,
                  cache = card.createCacheObject()
                )
                cards[name] = upgradedCard

                // Persist the fact this is a player immediately
                upgradedCard.cache?.let { cacheEntity ->
                  RFDao.playerCacheDao.insert(cacheEntity)
                  Log.debug(TAG, "Persisted player cache on auto-upgrade for ${cacheEntity.playerName}")
                }
              }
            }
          }
        }
      }

      // Early exit if still not a real player (calculations below are for specs)
      if (!card.isRealPlayer) return@forEach

      // Heavy Calculation: Spec Determination
      // Performed on the snapshot data, outside the lock.
      val threeMostRecentTrees = sequenceOf(
        card.recentCastEvents.take(128).map { it.timestamp to it.spell },
        card.recentDamageEvents.take(128).map { it.timestamp to it.spell }
      )
        .flatten()
        .mapNotNull { (ts, spell) -> findSkillTreeForSpell(spell)?.let { tree -> tree to ts } }
        .sortedByDescending { (_, ts) -> ts }
        .distinctBy { (tree, _) -> tree }
        .take(3)
        .map { (tree, _) -> tree }
        .toSet()

      if (threeMostRecentTrees.count() < 3) return@forEach

      val determinedSpec = SpecType.fromTrees(threeMostRecentTrees)

      // Update the card with the calculated spec
      scope.launch {
        mutex.withLock {
          cards[name]?.let { currentCard ->
            val updatedCard = currentCard.copy(
              currentBuild = determinedSpec.name,
              currentRole = (SpecType.fromName(determinedSpec.name)?.guessPlayerRole()?.value ?: PlayerRole.BLUE.value),
              cache = currentCard.createCacheObject(specOverride = determinedSpec.name)
            )
            cards[name] = updatedCard

            // Persist to DB
            updatedCard.cache?.let {
              //Log.debug(TAG, "Persisting updated cache for player ${it.playerName} with new spec ${it.lastKnownSpec}")
              RFDao.playerCacheDao.insert(it)
            }
          }
        }
      }
    }
    updateRaidAttendance()
    inferAndPersistFactionForRaidMembers()
  }

  /*
   * Builds parties of five from the ordered list of raid members and stores them under the raid ID.
   */
  suspend fun updatePlayersForRaidById(raidId: Int, members: List<RaidFramePayload>) {
    mutex.withLock {
      // improvement to the code where if the first raid is empty we clear all raids (because the player left the raid)
      if (raidId == 0 && members.isEmpty()) {
        raids.clear()
        return@withLock
      }
      raids[raidId] = members.chunked(5).take(20)
      // Guard: if main raid has zero real players, also clear co-raid data (ghost raid)
      if (raidId == 0 && members.all { it.playerName.isBlank() }) {
        raids.remove(1)
      }
      members.forEach { member ->
        if (member.playerName.isNotBlank()) {
          recordRaidBuffSnapshot(member)
          createCardIfNoneExists(playerName = member.playerName)
          // update Life Mend stats from buff data — route to the caster, not the target
          member.buffs.forEach { buff ->
            if (buff.buff_id == LIFE_MEND_BUFF_ID && buff.tooltip.healAmount > 0) {
              val caster = lifeMendCasterMap[member.playerName]
              if (caster != null) {
                createCardIfNoneExists(playerName = caster)
                cards[caster]?.let { casterCard ->
                  val lastAmount = casterCard.lifeMendHealAmounts.lastOrNull()
                  if (lastAmount != buff.tooltip.healAmount) {
                    cards[caster] = casterCard.updateLifeMendStats(buff.tooltip.healAmount)
        }
      }
    }

  }
          }
          // Sum the loot buffs a player currently has and track the session peak. An empty
          // (out-of-range / failed) scan is intentionally ignored so we don't overwrite the
          // last known loot buff total with nothing. The total active buff count is tracked
          // separately whenever a fresh scan actually reports one.
          if (member.buffs.isNotEmpty()) {
            cards[member.playerName]?.let { card ->
              cards[member.playerName] = card.updateLootBuffStats(
                member.buffs.map { it.buff_id }.toSet(),
                member.buffCount
              )
            }
          } else if (member.buffCount > 0) {
            cards[member.playerName]?.let { card ->
              cards[member.playerName] = card.copy(sessionCurrentBuffCount = member.buffCount)
            }
          }
        }
      }
    }
  }

  /**
   * Accumulates coherence time from a full roster snapshot (all 100 slots from a FRAMES_UPDATE).
   * Called by CompanionInteractor for each delivered roster frame.
   *
   * The elapsed wall-clock time since the last frame is treated as one time slice (approximate,
   * since IPC timing is not guaranteed). Only slices within a live-update window (1–60s) are
   * counted; larger gaps mean the watchdog/session was paused, so they are ignored. Recording
   * time (the global denominator) only accrues while a real raid (5+ players) is present.
   */
  suspend fun accumulateCoherence(payload: List<RaidFramePayload>) {
    val now = System.currentTimeMillis()
    val prev = lastCoherenceFrameAtMs
    lastCoherenceFrameAtMs = now
    if (prev <= 0L) return
    val delta = now - prev
    if (delta <= 0L || delta > COHERENCE_MAX_FRAME_DELTA_MS) return

    val members = payload.filter { it.playerName.isNotBlank() }
    val raidSize = members.map { it.playerName }.distinct().size
    if (raidSize < COHERENCE_MIN_RAID_SIZE) return

    mutex.withLock {
      coherenceRecordingMs.longValue += delta
      val inRender = members.count { it.distance >= 0 && it.distance <= COHERENCE_RENDER_DISTANCE }
      if (inRender * 2 >= raidSize) {
        coherenceRecorderMs.longValue += delta
      }
      for (member in members) {
        val d = member.distance
        if (d < 0) continue
        val card = cards[member.playerName] ?: continue
        val render = if (d <= COHERENCE_RENDER_DISTANCE) delta else 0L
        val raid = if (d <= COHERENCE_RAID_DISTANCE) delta else 0L
        val clump = if (d <= COHERENCE_CLUMP_DISTANCE) delta else 0L
        if (render > 0L || raid > 0L || clump > 0L) {
          cards[member.playerName] = card.accumulateCoherence(render, raid, clump)
        }
      }
    }
  }

  private fun recordRaidBuffSnapshot(member: RaidFramePayload) {
    val now = System.currentTimeMillis()
    val buffIds = member.buffs.map { it.buff_id }.toSet()
    // Only a non-empty buff read is always a valid confirmation of the player's state.
    if (buffIds.isNotEmpty()) {
      raidBuffHistory[member.playerName] = RaidBuffSnapshot(now, buffIds, member.distance)
    } else if (member.buffScanTimestamp > 0L && member.distance >= 0 && member.distance <= OUT_OF_RANGE_DISTANCE) {
      // An in-range scan that returned no buffs is a real confirmation the player is unbuffed.
      // An empty scan from an out-of-range / unknown-distance / never-zone player is NOT
      // confirmation — the app couldn't see their buffs, so they must remain "not scannable."
      raidBuffHistory[member.playerName] = RaidBuffSnapshot(now, emptySet(), member.distance)
    }
    if (now - lastRaidBuffHistoryCleanupAt >= 60_000L) {
      val cutoff = now - RAID_BUFF_HISTORY_RETENTION_MS
      raidBuffHistory.entries.removeIf { it.value.observedAt < cutoff }
      lastRaidBuffHistoryCleanupAt = now
    }
  }

  fun resolveRaidBuffObservation(member: RaidFramePayload, gracePeriod: RaidBuffGracePeriod): RaidBuffObservation {
    val now = System.currentTimeMillis()
    val currentIds = member.buffs.map { it.buff_id }.toSet()
    val currentTimestamp = member.buffScanTimestamp.takeIf { it > 0L }?.times(1000L)
    // Only trust the live scan when it actually contains buff data.
    // An out-of-range player still produces a fresh buffScanTimestamp but with empty buffs,
    // so we must fall through to history instead of short-circuiting.
    if (currentIds.isNotEmpty() && currentTimestamp != null && now - currentTimestamp <= gracePeriod.millis) {
      return RaidBuffObservation(member, RaidBuffSnapshot(currentTimestamp, currentIds, member.distance), true)
    }
    val snapshot = raidBuffHistory[member.playerName]
      ?.takeIf { now - it.observedAt <= gracePeriod.millis }
    return RaidBuffObservation(member, snapshot, false)
  }

  /*
   * Create card for a player if none exists... Upgrade from NPC to Player occurs inside the PlayerCardExtensions helpers.
   * NOTE: This method must be called within a mutex.withLock block as it is not thread-safe on its own.
   */
  private fun createCardIfNoneExists(cid: String? = null, playerName: String) {
    if (!cards.containsKey(playerName)) {
      var cached = runBlocking {
        RFDao.playerCacheDao.getPlayerCacheFor(playerName)
      }

      val seedEntry = SeedTableInteractor.lookupPlayer(playerName)
      if (seedEntry != null && cached != null) {
        if (SeedTableInteractor.shouldUpdateFromSeedTable(cached.lastSeen, seedEntry)) {
          cached = mergeFromSeedTable(cached, seedEntry)
          Log.info(TAG, "Updated cache for $playerName from seed table (newer data)")
        } else {
          cached = mergeMissingFromSeedTable(cached, seedEntry)
          Log.info(TAG, "Merged missing fields for $playerName from seed table")
        }
      } else if (seedEntry != null && cached == null) {
        cached = PlayerCacheEntity(
          playerName = playerName,
          lastSeen = seedEntry.lastSeen,
          lastKnownSpec = seedEntry.lastKnownSpec,
          lastKnownFaction = seedEntry.lastKnownFaction,
          lastKnownGearScore = seedEntry.gearScore,
          lastKnownGuild = seedEntry.lastKnownGuild
        )
        Log.info(TAG, "Pre-populated cache for $playerName from seed table")
      }

      val previousSpec = cached?.lastKnownSpec ?: SpecType.UNKNOWN.name
      val card = PlayerCard(
        name = playerName,
        recentCids = cid?.let { listOf(it) } ?: listOf(),
        lastEvent = System.currentTimeMillis(),
        lastKnownFaction = Faction.fromString(
          cached?.lastKnownFaction ?: Faction.UNKNOWN.value
        ).value,
        lastKnownFactionStatus = FactionStatus.fromString(
          cached?.lastKnownFactionStatus ?: Faction.UNKNOWN.value
        ).value,
        lastKnownGuild = cached?.lastKnownGuild ?: "",
        lastKnownGearScore = cached?.lastKnownGearScore ?: 0,
        leaderships = cached?.leaderships ?: 0,
        isLoaded = true,
        isRealPlayer = cached != null,
        cache = cached,
        currentBuild = previousSpec,
        currentRole = SpecType.fromName(previousSpec)?.guessPlayerRole()?.value ?: PlayerRole.BLUE.value
      )
      cards[playerName] = card
      // Snapshot the cache at load time for abort support — captures the DB value before any session increments
      if (CombatLogInteractor.isRecording.value) {
        preSessionCacheSnapshot[playerName] = card.cache
      }
    }
  }

  /**
   * Load a player from the local database if they exist there but aren't yet
   * in the in-memory cache. This is used by the battle graph to populate
   * faction / gear-score / spec info for edge targets that haven't produced
   * any tracked event themselves.
   *
   * @return true if a card was loaded from the database, false if the player
   *         was already cached or has no database row.
   */
  suspend fun loadPlayerFromDbIfExists(playerName: String): Boolean {
    if (cards.containsKey(playerName)) return false
    val cached = RFDao.playerCacheDao.getPlayerCacheFor(playerName) ?: return false
    val previousSpec = cached.lastKnownSpec.ifBlank { SpecType.UNKNOWN.name }
    val card = PlayerCard(
      name = playerName,
      recentCids = listOf(),
      lastEvent = System.currentTimeMillis(),
      lastKnownFaction = Faction.fromString(cached.lastKnownFaction).value,
      lastKnownFactionStatus = FactionStatus.fromString(cached.lastKnownFactionStatus).value,
      lastKnownGuild = cached.lastKnownGuild,
      lastKnownGearScore = cached.lastKnownGearScore,
      leaderships = cached.leaderships,
      isLoaded = true,
      isRealPlayer = true,
      cache = cached,
      currentBuild = previousSpec,
      currentRole = SpecType.fromName(previousSpec)?.guessPlayerRole()?.value ?: PlayerRole.BLUE.value
    )
    cards[playerName] = card
    return true
  }

  private fun mergeFromSeedTable(cache: PlayerCacheEntity, seed: SeedTableEntry): PlayerCacheEntity {
    return cache.copy(
      lastSeen = seed.lastSeen,
      lastKnownSpec = seed.lastKnownSpec,
      lastKnownFaction = seed.lastKnownFaction,
      lastKnownGearScore = seed.gearScore,
      lastKnownGuild = seed.lastKnownGuild
    )
  }

  private fun mergeMissingFromSeedTable(cache: PlayerCacheEntity, seed: SeedTableEntry): PlayerCacheEntity {
    return cache.copy(
      lastKnownSpec = if (cache.lastKnownSpec.isBlank()) seed.lastKnownSpec else cache.lastKnownSpec,
      lastKnownFaction = if (Faction.fromString(cache.lastKnownFaction) == Faction.UNKNOWN) seed.lastKnownFaction else cache.lastKnownFaction,
      lastKnownGearScore = if (cache.lastKnownGearScore == 0) seed.gearScore else cache.lastKnownGearScore,
      lastKnownGuild = if (cache.lastKnownGuild.isBlank()) seed.lastKnownGuild else cache.lastKnownGuild
    )
  }

  /*
   * Using this to standardize pet ID keys everywhere. The owner is concatenated with the pet name because pet names can
   * be the same across different owners. This isn't a perfect solution unless the game api were to expose the NPC ID of the source
   * in addition to the target across combat events. That's ok though friends we can filter by spells to count important things like breaths
   * without needing to know. ~
   */
  fun buildPetNameKey(owner: String, petName: String, petType: String): String {
    // build 4 character hex has of pet type to append to the key to avoid collisions (some pets have really long names hence the hash)
    val petTypeHash = petType.hashCode().and(0xFFFF).toString(16).padStart(4, '0')
    return "pet_id_${owner.lowercase().replace(" ", "_")}_${petName.lowercase().replace(" ", "_")}_$petTypeHash"
  }

  /*
   * The same thing except for pets and callable from outside the interactor because pets are a special case.
   */
  fun createOrUpdatePetCard(cid: String? = null, petName: String, owner: String, petType: String) {
    val key = "$owner:$petName"
    // Registration must be visible before the next combat event is drained. The old
    // fire-and-forget update raced the accumulator and lost short pet attacks.
    runBlocking {
      mutex.withLock {
        if (!petCards.containsKey(key)) {
          petCards[key] = PetCard(
            // id is owner + pet name to ensure uniqueness
            petId = buildPetNameKey(owner, petName, petType),
            name = petName,
            owner = owner,
            recentCids = cid?.let { listOf(it) } ?: listOf(),
            lastEvent = System.currentTimeMillis(),
            petTypes = setOf(petType)
          )
        } else {
          petCards[key]?.let { card ->
            petCards[key] = card.copy(
              recentCids = cid?.let { (listOf(it) + card.recentCids).distinct().take(50) } ?: card.recentCids,
              lastEvent = System.currentTimeMillis(),
              petTypes = card.petTypes + petType
            )
            //debug print all cids for pet id
            Log.debug(TAG, "Pet ${card.petId} (${card.name}) CIDs: ${petCards[key]?.recentCids}")
          }
        }
      }
    }
    PetAccumulatorInteractor.onPetRegistered()
  }

  fun createPetCardFromWhitelistedSpell(owner: String, petName: String, petType: String = "Unknown Pet") {
    if (owner.isBlank() || petName.isBlank()) return
    createOrUpdatePetCard(petName = petName, owner = owner, petType = petType)
  }

  fun startNewSession(sessionType: String, allowPvE: Boolean) {
    // Capture snapshot synchronously so the archive step sees the in-memory session totals
    // that exist *before* we clear `cards` below. Reading `RFConfig.state.value` is a direct
    // StateFlow read (no suspend), and `cards.values.toList()` is a snapshot copy.
    val previousConfig = RFConfig.state.value
    val previousSessionStart = previousConfig.lastSessionStart
    val snapshot = if (previousSessionStart > 0L) cards.values.toList() else emptyList()

    scope.launch {
      // Archive the previous session's per-player totals first so they're persisted before
      // the new session's totals overwrite the in-memory state. Skipped on first-ever launch
      // (lastSessionStart == 0) and when there's nothing in the snapshot to archive.
      if (snapshot.isNotEmpty()) {
        archiveSessionSnapshot(
          snapshot = snapshot,
          sessionStart = previousSessionStart,
          sessionType = previousConfig.lastSessionType,
          sessionTitle = previousConfig.lastSessionTitle
        )
      }

      // Snapshot all existing card caches before clearing, so abort can revert lifetime totals
      preSessionCacheSnapshot = cards.values
        .filter { it.cache != null }
        .associate { it.name to it.cache!! }
        .toMutableMap()

      RFConfig.update {
        it.copy(
          allowPVEDamage = allowPvE,
          lastSessionStart = System.currentTimeMillis(),
          lastSessionType = sessionType,
          previousSessionStart = 0L
        )
      }
      // go big or go home: better solution that resetting, just clear on start instead
      mutex.withLock {
        cards.clear()
        petCards.clear()
        raids.clear()
        lifeMendCasterMap.clear()
        recentLeechCasts.clear()
        lastCoherenceFrameAtMs = 0L
        coherenceRecordingMs.longValue = 0L
        coherenceRecorderMs.longValue = 0L
      }
      GraphDataInteractor.clearForSession()
      logSessionMemoryCensus("new session")
      CombatLogInteractor.startRecording()
      Log.info(TAG, "Started new recording session: type=$sessionType, allowPvE=$allowPvE")
    }
  }

  fun stopSession() {
    // Snapshot the in-memory session totals so they survive a hard close of the app later.
    val currentConfig = RFConfig.state.value
    val previousSessionStart = currentConfig.lastSessionStart
    val snapshot = if (previousSessionStart > 0L) cards.values.toList() else emptyList()

    CombatLogInteractor.stopRecording()

    if (snapshot.isNotEmpty()) {
      archiveJob = archiveSessionSnapshot(
        snapshot = snapshot,
        sessionStart = previousSessionStart,
        sessionType = currentConfig.lastSessionType.ifBlank { "manual_stop" },
        sessionTitle = currentConfig.lastSessionTitle
      )
    }
    // Reset start marker so a subsequent startNewSession knows there's nothing in memory to archive.
    val currentSessionStart = RFConfig.state.value.lastSessionStart
    RFConfig.update { it.copy(lastSessionStart = 0L, previousSessionStart = currentSessionStart) }
    Log.info(TAG, "Recording session stopped")
  }

  /**
   * Aborts the current recording session without archiving any data.
   * Restores pre-session lifetime totals from the cache snapshot and reverts the database
   * for any card whose cache may have been persisted mid-session (auto-upgrade, spec determination, etc).
   * Cards created during the session (not in the snapshot) are simply discarded.
   */
  fun abortSession() {
    val currentConfig = RFConfig.state.value
    if (currentConfig.lastSessionStart <= 0L) return

    CombatLogInteractor.stopRecording(abort = true)

    scope.launch {
      mutex.withLock {
        cards.forEach { (name, card) ->
          preSessionCacheSnapshot[name]?.let { preSessionCache ->
            cards[name] = card.copy(cache = preSessionCache)
            RFDao.playerCacheDao.insert(preSessionCache)
          }
        }
        cards.clear()
        petCards.clear()
        raids.clear()
        lastCoherenceFrameAtMs = 0L
        coherenceRecordingMs.longValue = 0L
        coherenceRecorderMs.longValue = 0L
      }
      GraphDataInteractor.clearForSession()
      logSessionMemoryCensus("aborted session")
      preSessionCacheSnapshot = mutableMapOf()
    }

    RFConfig.update {
      it.copy(
        lastSessionStart = 0L,
        lastSessionTitle = "",
        lastSessionType = "",
        lastSessionDurationMs = 0L,
        previousSessionStart = 0L
      )
    }
    Log.info(TAG, "Recording session aborted — no data archived, lifetime totals reverted")
  }

  suspend fun awaitArchive() {
    archiveJob?.join()
  }

  private fun logSessionMemoryCensus(reason: String) {
    val snapshot = cards.values.toList()
    val recentEvents = snapshot.sumOf {
      it.recentCastSuccessfulCastEvents.size + it.recentCastEvents.size +
        it.recentDamageEvents.size + it.recentHealEvents.size +
        it.recentBuffGainedEvents.size + it.recentBuffEndedEvents.size +
        it.recentDebuffGainedEvents.size + it.recentDebuffEndedEvents.size +
        it.recentBuffAppliedEvents.size + it.recentDebuffAppliedEvents.size
    }
    val adjacencyEntries = snapshot.sumOf {
      it.sessionDamageToPlayer.size + it.sessionDamageFromPlayer.size +
        it.sessionHealToPlayer.size + it.sessionHealFromPlayer.size +
        it.sessionCCToPlayer.size + it.sessionCCFromPlayer.size +
        it.sessionDamageToPlayerBySpell.values.sumOf { map -> map.size } +
        it.sessionHealToPlayerBySpell.values.sumOf { map -> map.size } +
        it.sessionCCToPlayerBySpell.values.sumOf { map -> map.size } +
        it.sessionKillsToPlayer.size + it.sessionKillsToPlayerBySpell.values.sumOf { map -> map.size }
    }
    Log.info(TAG, "Session reset completed ($reason): cards=${snapshot.size} pets=${petCards.size} recentEvents=$recentEvents adjacencyEntries=$adjacencyEntries graphPlayers=${GraphDataInteractor.getPlayerNames().size}")
  }

  /**
   * Writes one [PlayerSessionTotalsEntity] per player in [snapshot], capturing every
   * `session*` field of the in-memory [PlayerCard]. No-op when [sessionStart] is unset
   * or when the wall clock has moved backwards. Composite primary key on the entity makes
   * the insert idempotent (REPLACE on conflict).
   */
  private fun archiveSessionSnapshot(
    snapshot: List<PlayerCard>,
    sessionStart: Long,
    sessionType: String,
    sessionTitle: String
  ): Job? {
    if (sessionStart <= 0L) return null
    val sessionEnd = System.currentTimeMillis()
    if (sessionEnd <= sessionStart) return null

    return scope.launch {
      var written = 0
      snapshot.forEach { card ->
        // Only archive real players. NPC cards (e.g. raid mobs, world bosses) get
        // auto-upgraded to real players in `interact()` and would normally have a
        // session*, but skipping them here keeps the historical table scoped to the
        // entities the user actually cares about and cuts the row count meaningfully.
        if (!card.isRealPlayer) return@forEach
        if (!hasAnySessionActivity(card)) return@forEach
        val entity = PlayerSessionTotalsEntity(
          playerName = card.name,
          sessionStart = sessionStart,
          sessionEnd = sessionEnd,
          sessionType = sessionType,
          sessionTitle = sessionTitle,
          totalDamage = card.sessionDamageTotal,
          totalHealing = card.sessionHealTotal,
          totalCC = card.sessionCCTotal,
          totalBuffs = card.sessionBuffTotal,
          totalDebuffs = card.sessionDebuffTotal,
          totalCharms = card.sessionCharmTotal,
          totalSongs = card.sessionSongsTotal,
          totalDistresses = card.sessionDistressTotal,
          totalSilences = card.sessionSilenceTotal,
          totalGliderUses = card.sessionGliderTotal,
          totalItemSkills = card.sessionItemSkillTotal,
          totalPotions = card.sessionPotionTotal,
          totalKills = card.sessionKillTotal,
          totalKillsKB = card.sessionKillTotalKB,
          totalDeaths = card.sessionDeathTotal,
          totalDamageTaken = card.sessionDamageTakenTotal,
          totalHealsReceived = card.sessionHealsReceivedTotal,
          totalOdeHeals = card.sessionOdeHealsTotal,
          totalTigerStrikes = card.sessionTigerStrikeTotal,
          totalFreezes = card.sessionFreezeTotal,
          totalTrips = card.sessionTripsTotal,
          totalBubbles = card.sessionBubblesTotal,
          totalBracings = card.sessionBracingsTotal,
          totalShieldStrip = card.sessionShieldStripTotal,
          totalWeaponDisables = card.sessionWeaponDisablesTotal,
          totalPotionDisables = card.sessionPotionDisablesTotal,
          totalBdGlider = card.sessionBdGliderTotal,
          totalCrystalWings = card.sessionCrystalWingsTotal,
          totalGliderDisables = card.sessionGliderDisablesTotal,
           totalProvoked = card.sessionProvokedTotal
           ,totalDefiance = card.sessionDefianceTotal
           ,totalGardenDefiance = card.sessionGardenDefianceTotal
           ,totalPurges = card.sessionPurgeTotal
           ,totalSacDances = card.sessionSacDanceTotal
           ,totalDeepTranquility = card.sessionDeepTranquilityTotal
           ,totalDeependDebuff = card.sessionDeependDebuffTotal
           ,totalThrowDagger = card.sessionThrowDaggerTotal
           ,totalStuns = card.sessionStunsTotal
           ,totalStaggers = card.sessionStaggersTotal
           ,totalPetrification = card.sessionPetrificationTotal
           ,totalAbsorbLifeforce = card.sessionAbsorbLifeforceTotal
           ,totalCorrosiveBarrage = card.sessionCorrosiveBarrageTotal
           ,totalBlindedByCrows = card.sessionBlindedByCrowsTotal
           ,totalMistSunder = card.sessionMistSunderTotal
           ,totalRegularSunder = card.sessionRegularSunderTotal
           ,totalImpaleImmunity = card.sessionImpaleImmunityTotal
           ,totalProtectiveWings = card.sessionProtectiveWingsTotal
           ,totalCourageousAction = card.sessionCourageousActionTotal
           ,totalManaBarrier = card.sessionManaBarrierTotal
           ,totalRevive = card.sessionReviveTotal
        )
        RFDao.playerSessionDao.insert(entity)
        written++
      }
      Log.info(TAG, "Archived session totals for $written/${snapshot.size} player(s) (session $sessionStart → $sessionEnd)")
      refreshOwnSessionCount()
    }
  }

  // Skip the row entirely if a player had zero activity in the session — keeps the historical
  // table from filling up with empty rows for every NPC that was briefly upgraded then ignored.
  private fun hasAnySessionActivity(card: PlayerCard): Boolean {
    return card.sessionDamageTotal != 0L ||
        card.sessionHealTotal != 0L ||
        card.sessionCCTotal != 0 ||
        card.sessionBuffTotal != 0 ||
        card.sessionDebuffTotal != 0 ||
        card.sessionCharmTotal != 0 ||
        card.sessionSongsTotal != 0 ||
        card.sessionDistressTotal != 0 ||
        card.sessionSilenceTotal != 0 ||
        card.sessionGliderTotal != 0 ||
        card.sessionItemSkillTotal != 0 ||
        card.sessionPotionTotal != 0 ||
        card.sessionKillTotal != 0 ||
        card.sessionKillTotalKB != 0 ||
        card.sessionDeathTotal != 0 ||
        card.sessionDamageTakenTotal != 0 ||
        card.sessionHealsReceivedTotal != 0 ||
        card.sessionOdeHealsTotal != 0L ||
        card.sessionTigerStrikeTotal != 0 ||
        card.sessionFreezeTotal != 0 ||
        card.sessionTripsTotal != 0 ||
        card.sessionBubblesTotal != 0 ||
        card.sessionBracingsTotal != 0 ||
        card.sessionShieldStripTotal != 0 ||
        card.sessionWeaponDisablesTotal != 0 ||
        card.sessionPotionDisablesTotal != 0 ||
        card.sessionBdGliderTotal != 0 ||
        card.sessionCrystalWingsTotal != 0 ||
        card.sessionGliderDisablesTotal != 0 ||
         card.sessionProvokedTotal != 0
         || card.sessionDefianceTotal != 0
         || card.sessionGardenDefianceTotal != 0
         || card.sessionPurgeTotal != 0
         || card.sessionSacDanceTotal != 0
         || card.sessionDeepTranquilityTotal != 0
         || card.sessionDeependDebuffTotal != 0
         || card.sessionThrowDaggerTotal != 0
         || card.sessionStunsTotal != 0
         || card.sessionStaggersTotal != 0
         || card.sessionPetrificationTotal != 0
         || card.sessionAbsorbLifeforceTotal != 0
         || card.sessionCorrosiveBarrageTotal != 0
         || card.sessionBlindedByCrowsTotal != 0
         || card.sessionMistSunderTotal != 0
         || card.sessionRegularSunderTotal != 0
         || card.sessionImpaleImmunityTotal != 0
         || card.sessionProtectiveWingsTotal != 0
         || card.sessionCourageousActionTotal != 0
         || card.sessionManaBarrierTotal != 0
         || card.sessionReviveTotal != 0
  }

  /**
   * Fetches the [limit] most recently archived sessions for [playerName] (newest first by
   * `sessionEnd`) and returns a single aggregated [PlayerSessionTotalsEntity] whose fields are
   * the sum of all returned sessions. Returns null when no historical sessions exist.
   * Pass `null` for [limit] to aggregate every archived session for the player.
   */
  suspend fun getHistoricalTotalsForPlayer(
    playerName: String,
    limit: Int? = null
  ): PlayerSessionTotalsEntity? {
    val sessions = if (limit != null) {
      RFDao.playerSessionDao.getRecentSessionsForPlayer(playerName, limit)
    } else {
      RFDao.playerSessionDao.getSessionsForPlayer(playerName)
    }
    if (sessions.isEmpty()) return null
    return PlayerSessionTotalsEntity(
      playerName = playerName,
      sessionStart = sessions.minOf { it.sessionStart },
      sessionEnd = sessions.maxOf { it.sessionEnd },
      sessionType = sessions.first().sessionType,
      sessionTitle = "${sessions.size} sessions",
      totalDamage = sessions.sumOf { it.totalDamage },
      totalHealing = sessions.sumOf { it.totalHealing },
      totalCC = sessions.sumOf { it.totalCC },
      totalBuffs = sessions.sumOf { it.totalBuffs },
      totalDebuffs = sessions.sumOf { it.totalDebuffs },
      totalCharms = sessions.sumOf { it.totalCharms },
      totalSongs = sessions.sumOf { it.totalSongs },
      totalDistresses = sessions.sumOf { it.totalDistresses },
      totalSilences = sessions.sumOf { it.totalSilences },
      totalGliderUses = sessions.sumOf { it.totalGliderUses },
      totalItemSkills = sessions.sumOf { it.totalItemSkills },
      totalPotions = sessions.sumOf { it.totalPotions },
      totalKills = sessions.sumOf { it.totalKills },
      totalKillsKB = sessions.sumOf { it.totalKillsKB },
      totalDeaths = sessions.sumOf { it.totalDeaths },
      totalDamageTaken = sessions.sumOf { it.totalDamageTaken },
      totalHealsReceived = sessions.sumOf { it.totalHealsReceived },
      totalOdeHeals = sessions.sumOf { it.totalOdeHeals },
      totalTigerStrikes = sessions.sumOf { it.totalTigerStrikes },
      totalFreezes = sessions.sumOf { it.totalFreezes },
      totalTrips = sessions.sumOf { it.totalTrips },
      totalBubbles = sessions.sumOf { it.totalBubbles },
      totalBracings = sessions.sumOf { it.totalBracings },
      totalShieldStrip = sessions.sumOf { it.totalShieldStrip },
      totalWeaponDisables = sessions.sumOf { it.totalWeaponDisables },
      totalPotionDisables = sessions.sumOf { it.totalPotionDisables },
      totalBdGlider = sessions.sumOf { it.totalBdGlider },
      totalCrystalWings = sessions.sumOf { it.totalCrystalWings },
      totalGliderDisables = sessions.sumOf { it.totalGliderDisables },
       totalProvoked = sessions.sumOf { it.totalProvoked },
       totalDefiance = sessions.sumOf { it.totalDefiance },
       totalGardenDefiance = sessions.sumOf { it.totalGardenDefiance },
       totalPurges = sessions.sumOf { it.totalPurges },
       totalSacDances = sessions.sumOf { it.totalSacDances },
       totalDeepTranquility = sessions.sumOf { it.totalDeepTranquility },
       totalDeependDebuff = sessions.sumOf { it.totalDeependDebuff },
       totalThrowDagger = sessions.sumOf { it.totalThrowDagger },
       totalStuns = sessions.sumOf { it.totalStuns },
       totalStaggers = sessions.sumOf { it.totalStaggers },
       totalPetrification = sessions.sumOf { it.totalPetrification },
       totalAbsorbLifeforce = sessions.sumOf { it.totalAbsorbLifeforce },
       totalCorrosiveBarrage = sessions.sumOf { it.totalCorrosiveBarrage },
       totalBlindedByCrows = sessions.sumOf { it.totalBlindedByCrows },
       totalMistSunder = sessions.sumOf { it.totalMistSunder },
       totalRegularSunder = sessions.sumOf { it.totalRegularSunder },
       totalImpaleImmunity = sessions.sumOf { it.totalImpaleImmunity },
       totalProtectiveWings = sessions.sumOf { it.totalProtectiveWings },
       totalCourageousAction = sessions.sumOf { it.totalCourageousAction },
       totalManaBarrier = sessions.sumOf { it.totalManaBarrier },
       totalRevive = sessions.sumOf { it.totalRevive }
    )
  }

  // filter pve damage by checking if the target is a real player
  fun isRealPlayer(playerName: String): Boolean {
    return cards.values.any { it.isRealPlayer && it.name == playerName }
  }

  fun getCard(name: String): PlayerCard? {
    return cards[name]
  }

  /**
   * Reverse-lookup: given a CID, find the player name whose card contains that CID.
   * CIDs are ephemeral identifiers assigned per login session; this finds the player
   * who most recently used the given CID.
   */
  fun getPlayerNameByCid(cid: String): String? {
    return cards.values.find { cid in it.recentCids }?.name
  }

  // gets a list of player cards matching a filter predicate
  fun getGroupCards(filter: (PlayerCard) -> Boolean): List<PlayerCard> {
    return cards.values.filter(filter)
  }

  /**
   * Infers faction for real players in the same raid who have unknown faction.
   * Assumes all same-raid real players share the same faction, and persists the update to cache.
   */
  private suspend fun inferAndPersistFactionForRaidMembers() {
    mutex.withLock {
      raids.forEach { (raidId, parties) ->
        val raidMemberNames = parties.flatten().map { it.playerName }.toSet()
        val raidCards = cards.values.filter { it.name in raidMemberNames && it.isRealPlayer }

        val knownFactionCard = raidCards.find {
          val f = Faction.fromString(it.lastKnownFaction)
          f != Faction.UNKNOWN
        } ?: return@forEach

        val inferredFaction = Faction.fromString(knownFactionCard.lastKnownFaction)
        val inferredStatus = FactionStatus.fromString(knownFactionCard.lastKnownFactionStatus)

        raidCards.forEach { card ->
          if (Faction.fromString(card.lastKnownFaction) == Faction.UNKNOWN) {
            val updatedCard = card.setFaction(inferredFaction, inferredStatus)
            cards[card.name] = updatedCard
            updatedCard.cache?.let { cacheEntity ->
              RFDao.playerCacheDao.insert(cacheEntity)
            }
            Log.info(TAG, "Inferred faction for ${card.name}: $inferredFaction / $inferredStatus (from raid $raidId)")
          }
        }
      }
    }
  }

  /**
   * Tracks attendance by detecting players who joined or left raids.
   * Players who were in a raid but are no longer present are moved to departures.
   */
  private suspend fun updateRaidAttendance() {
    mutex.withLock {
      raids.forEach { (raidId, parties) ->
        val currentMembers = parties.flatten().map { it.playerName }.toSet()

        // Initialize attendance set if needed
        if (!raidAttendance.containsKey(raidId)) {
          raidAttendance[raidId] = mutableSetOf()
        }
        if (!raidDepartures.containsKey(raidId)) {
          raidDepartures[raidId] = mutableSetOf()
        }

        // Add all current members to attendance (set prevents duplicates)
        raidAttendance[raidId]?.addAll(currentMembers)

        // Find members who left (were in attendance but not currently in raid)
        val leftMembers = raidAttendance[raidId]?.filter { it !in currentMembers } ?: emptyList()
        raidDepartures[raidId]?.addAll(leftMembers)
      }
      // Emit an immutable snapshot so observers can react to changes
      _raidDeparturesFlow.value = raidDepartures.mapValues { it.value.toSet() }
    }
  }

  /**
   * Updates a player's leadership status and persists the change.
   */
  fun updatePlayerLeadershipFor(playerName: String, newLeadership: Int) {
    scope.launch {
      mutex.withLock {
        createCardIfNoneExists(playerName = playerName)
        cards[playerName]?.let { card ->
          val updatedCard = card.updatePlayerLeadership(newLeadership)
          cards[playerName] = updatedCard

          // Persist to DB
          updatedCard.cache?.let {
            RFDao.playerCacheDao.insert(it)
          }
        }
      }
    }
  }

  /**
   * Manually assigns a player to a faction and persists the change.
   */
  fun updatePlayerFactionFor(playerName: String, faction: Faction) {
    scope.launch {
      mutex.withLock {
        createCardIfNoneExists(playerName = playerName)
        cards[playerName]?.let { card ->
          val updatedCard = card.setFaction(faction, FactionStatus.FRIENDLY)
          cards[playerName] = updatedCard

          // Persist to DB
          updatedCard.cache?.let {
            RFDao.playerCacheDao.insert(it)
          }
        }
      }
    }
  }

    /**
   * Helps upgrade an NPC card to a real player card immediately based on metadata from the game proving it's a player.
   * Also persists the updated cache to the database right away. (which we didn't use to do and records got lost eek!)
   **/
  fun stronglyAssertIsPlayer(cid: String?, name: String, classMap: Map<String, Int>) {
    val spec = SpecType.fromTrees(classMap.values.mapNotNull { gameId -> SkillTreeType.fromGameId(gameId) }.toSet())
    Log.debug(TAG, "Strongly asserting $name is a real player with spec $spec and recent cid of $cid.")
    scope.launch {
      mutex.withLock {
        createCardIfNoneExists(cid, name)
        cards[name]?.let { card ->
          val updated = card.copy(
            isRealPlayer = true,
            currentBuild = if (spec != SpecType.UNKNOWN) spec.name else card.currentBuild,
            currentRole = if (spec != SpecType.UNKNOWN) spec.guessPlayerRole().value else card.currentRole,
            recentCids = cid?.let { (card.recentCids + it).distinct().takeLast(50) } ?: card.recentCids,
            cache = card.createCacheObject(specOverride = spec.name)
          )
          cards[name] = updated

          // Persist immediately that this is a player
          updated.cache?.let { cacheEntity ->
            RFDao.playerCacheDao.insert(cacheEntity)
            //Log.debug(TAG, "Persisted player cache on strong-assert for ${cacheEntity.playerName}")
          }
        }
      }
    }

    // Clean up stale Leech cast entries (older than 10 seconds)
    val now = System.currentTimeMillis()
    recentLeechCasts.entries.removeIf { now - it.value > 10_000L }
  }

  ///////////////////////////
  // Regular Event Posting //
  ///////////////////////////

  fun postEvent(event: CombatEvent) {
    if (CombatLogInteractor.isRecording.value) {
      when (event) {
        is DamageEvent -> postDamage(event)
        is HealEvent -> postHeal(event)
        is CastingEvent -> postCasting(event)
        is SuccessfulCastEvent -> postSuccessfulCast(event)
        is BuffGainedEvent -> postBuffGained(event)
        is BuffEndedEvent -> postBuffEnded(event)
        is DebuffGainedEvent -> postDebuffGained(event)
        is DebuffEndedEvent -> postDebuffEnded(event)
        else -> {} // no-op for other event types
      }
    }
    if (shouldRecordEvent(event)) {
      CombatLogInteractor.recordEvent(event)
    }
  }

  fun postEventInternal(event: CombatEvent) {
    if (CombatLogInteractor.isRecording.value) {
      when (event) {
        is DamageEvent -> postDamageInternal(event)
        is HealEvent -> postHeal(event)
        is CastingEvent -> postCasting(event)
        is SuccessfulCastEvent -> postSuccessfulCastInternal(event)
        is BuffGainedEvent -> postBuffGained(event)
        is BuffEndedEvent -> postBuffEnded(event)
        is DebuffGainedEvent -> postDebuffGained(event)
        is DebuffEndedEvent -> postDebuffEnded(event)
        else -> {} // no-op for other event types
      }
    }
    // basically whitelisting event types to put in the final rf report for now
    if (shouldRecordEvent(event)) {
      CombatLogInteractor.recordEvent(event)
    }
  }

  private fun shouldRecordEvent(event: CombatEvent): Boolean {
    if (!CombatLogInteractor.isRecording.value) return false
    val allowPvE = RFConfig.state.value.allowPVEDamage
    return isRealPlayer(event.target) || allowPvE
  }

  private fun postDamage(event: DamageEvent) {
    val cleanSource = event.source.replace("\\s*\\([^)]*\\)$".toRegex(), "").trim()
    val eventSourceIsPet = getPetEntriesByName(cleanSource).isNotEmpty()
    val isWhitelistedSkill = isWhitelistedPetSkill(event.spellId, event.spell)
    // Pet attribution is spell-whitelist driven. A registered pet may emit
    // ordinary game abilities, but those abilities must not enter pet totals.
    if (isWhitelistedSkill) {
      PetAccumulatorInteractor.postEvent(event)
      return
    }
    postDamageInternal(event)
  }

  private fun isWhitelistedPetSkill(spellId: Int, spellName: String): Boolean {
    return petSkillWhitelist.any { skill ->
      !skill.isPetInitiator &&
        (skill.id == spellId || skill.possibleNames.any { it.equals(spellName, ignoreCase = true) })
    }
  }

  /**
   * Matches only rider/initiator spells on the pet whitelist.
   * Used for cast routing where we need rider spells to reach PetAccumulatorInteractor
   * for CastWindow/PendingRiderCast attribution. Non-initiator spells must NOT be
   * routed here or they create duplicate breath entries.
   */
  private fun isPetInitiatorSkill(spellId: Int, spellName: String): Boolean {
    return petSkillWhitelist.any { skill ->
      skill.isPetInitiator && (
        skill.id == spellId || skill.possibleNames.any { it.equals(spellName, ignoreCase = true) }
      )
    }
  }

  private fun postDamageInternal(event: DamageEvent) {
    scope.launch {
      mutex.withLock {
        createCardIfNoneExists(cid = null, playerName = event.source)
        createCardIfNoneExists(cid = event.cid, playerName = event.target)
        cards[event.source]?.let { card ->
          cards[event.source] = card.postDamageEvent(event)
        }
        GraphDataInteractor.postEvent(event)
        cards[event.target]?.let { card ->
          cards[event.target] = card.postDamageTakenEvent(event)
        }
      }
    }
  }

  private fun postHeal(event: HealEvent) {
    scope.launch {
      mutex.withLock {
        createCardIfNoneExists(cid = null, event.source)
        createCardIfNoneExists(cid = event.cid, event.target)
        cards[event.source]?.let { card ->
          cards[event.source] = card.postHealEvent(event)
        }
        GraphDataInteractor.postEvent(event)
        cards[event.target]?.let { card ->
          cards[event.target] = card.postHealsReceivedEvent(event)
        }
      }
    }
  }

  private fun postCasting(event: CastingEvent) {
    if (isWhitelistedPetSkill(event.spellId, event.spell)) {
      // A cast-start event is still subject to the pet spell whitelist. Generic
      // abilities such as Melee Attack must not become pet attribution signals.
      return
    }
    scope.launch {
      mutex.withLock {
        createCardIfNoneExists(cid = event.cid, event.source)
        cards[event.source]?.let { card ->
          cards[event.source] = card.postCastingEvent(event)
        }
        if (event.spellId == LEECH_SPELL_ID) {
          recentLeechCasts[event.source] = event.timestamp
        }
      }
    }
  }

  private fun postSuccessfulCast(event: SuccessfulCastEvent) {
    val cleanSource = event.source.replace("\\s*\\([^)]*\\)$".toRegex(), "").trim()
    val eventSourceIsPet = getPetEntriesByName(cleanSource).isNotEmpty()
    // Only route rider/initiator spells to PetAccumulatorInteractor for CastWindow attribution.
    // Non-initiator spells (like the direct breath damage skills) must go through
    // postSuccessfulCastInternal to avoid creating duplicate breath entries.
    val isWhitelistedSkill = isPetInitiatorSkill(event.spellId, event.spell)
    if (isWhitelistedSkill) {
      PetAccumulatorInteractor.postEvent(event)
      return
    }
    postSuccessfulCastInternal(event)
  }

  private fun postSuccessfulCastInternal(event: SuccessfulCastEvent) {
    scope.launch {
      mutex.withLock {
        createCardIfNoneExists(cid = event.cid, event.source)
        cards[event.source]?.let { card ->
          cards[event.source] = card.postSuccessfulCastEvent(event)
        }
        if (event.spellId == LEECH_SPELL_ID) {
          recentLeechCasts[event.source] = event.timestamp
        }
      }
    }
  }

  private fun postBuffGained(event: BuffGainedEvent) {
    scope.launch {
      mutex.withLock {
        createCardIfNoneExists(cid = event.cid, event.target)
        cards[event.target]?.let { card ->
          cards[event.target] = card.postBuffGainedEvent(event)
        }

        // credit the source because they are the buffer
        event.source?.let { source ->
          createCardIfNoneExists(cid = event.cid, source)
          cards[source]?.let { card ->
            cards[source] = card.postBuffAppliedEvent(
              BuffAppliedEvent(
                cid = event.cid,
                timestamp = event.timestamp,
                source = event.source,
                target = event.target,
                buff = event.buff,
                buffId = event.buffId
              )
            )
            // track Life Mend casts on the healer's card
            if (event.buffId == LIFE_MEND_BUFF_ID) {
              // Filter out Life Mend buffs stolen via Leech: if the source recently cast Leech
              // and the buff appeared on themselves (source == target), it's a steal, not a cast.
              val isLeechSteal = event.source == event.target &&
                  (recentLeechCasts[event.source]?.let { event.timestamp - it <= LEECH_WINDOW_MS } == true)
              if (!isLeechSteal) {
                cards[source] = cards[source]!!.postLifeMendApplied(event.target)
                lifeMendCasterMap[event.target] = source
              } else {
                Log.debug(TAG, "Filtered Leech-stolen Life Mend from $source")
              }
            }
          }
        }
      }
    }
  }

  private fun postBuffEnded(event: BuffEndedEvent) {
    scope.launch {
      mutex.withLock {
        createCardIfNoneExists(cid = event.cid, event.target)
        cards[event.target]?.let { card ->
          cards[event.target] = card.postBuffEndedEvent(event)
        }
        // clean up Life Mend caster mapping when buff expires
        if (event.buffId == LIFE_MEND_BUFF_ID) {
          lifeMendCasterMap.remove(event.target)
        }
      }
    }
  }

  private fun postDebuffGained(event: DebuffGainedEvent) {
    scope.launch {
      mutex.withLock {
        createCardIfNoneExists(cid = event.cid, event.target)
        cards[event.target]?.let { card ->
          cards[event.target] = card.postDebuffGainedEvent(event)
        }

        // give credit to the source
        event.source?.let { source ->
          GraphDataInteractor.postEvent(
            DebuffAppliedEvent(
              cid = event.cid,
              timestamp = event.timestamp,
              source = event.source,
              target = event.target,
              debuff = event.debuff,
              debuffId = event.debuffId
            )
          )
          cards[source]?.let { card ->
            cards[source] = card.postDebuffAppliedEvent(
              DebuffAppliedEvent(
                cid = event.cid,
                timestamp = event.timestamp,
                source = event.source,
                target = event.target,
                debuff = event.debuff,
                debuffId = event.debuffId
              )
            )
          }
        }
      }
    }
  }

  private fun postDebuffEnded(event: DebuffEndedEvent) {
    scope.launch {
      mutex.withLock {
        createCardIfNoneExists(cid = event.cid, event.target)
        cards[event.target]?.let { card ->
          cards[event.target] = card.postDebuffEndedEvent(event)
        }
      }
    }
  }

  ///////////////////////
  // Pet Event Posting //
  ///////////////////////

  /**
   * Called by PetAccumulatorInteractor to apply pet damage to a specific pet card key.
   * `petKey` is the internal key format used by petCards: "$owner:$petName".
   */
  fun postPetDamage(petKey: String, event: DamageEvent) {
    if (!CombatLogInteractor.isRecording.value) return
    // PvP/PvE filtering: in PvP mode, only count damage towards real players
    if (!isRealPlayer(event.target) && !RFConfig.state.value.allowPVEDamage) return
    Log.info(TAG, "Posting pet damage event to petKey=$petKey: $event")
    scope.launch {
      mutex.withLock {
        val existing = petCards[petKey]
        if (existing == null) {
          Log.info(TAG, "Pet card not found for key '$petKey' when posting damage. Available keys: ${petCards.keys.toList()}")
          return@withLock
        }

        // Attribute damage to the most recent breath/rocket cast if applicable
        val isBreathDamage = event.spellId in DRAGON_BREATH_DAMAGE_SPELL_IDS ||
            event.spellId in DRAKE_BREATH_DAMAGE_SPELL_IDS ||
            event.spell.contains("Clinging Flame", ignoreCase = true) ||
            event.spell.contains("폭발하는 씨앗", ignoreCase = true) ||
            event.spell.contains("Раскаленная лава", ignoreCase = true) ||
            event.spell.contains("Thunderbreath", ignoreCase = true) ||
            event.spell.contains("천둥의 숨결", ignoreCase = true)
        val isRocketDamage = event.spellId in GUIDED_MISSILES_DAMAGE_SPELL_IDS ||
            event.spell.contains("Guided Missiles", ignoreCase = true) ||
            event.spell.contains("유도탄", ignoreCase = true) ||
            event.spell.contains("Ковровая бомбардировка", ignoreCase = true)

        var breathCasts = existing.sessionBreathCasts
        var rocketCasts = existing.sessionRocketCasts

        if (isBreathDamage && breathCasts.isNotEmpty()) {
          val lastIdx = breathCasts.indexOfLast { cast ->
            event.timestamp >= cast.timestamp && event.timestamp - cast.timestamp <= 15_000L
          }
          if (lastIdx >= 0) {
            val last = breathCasts[lastIdx]
            val updatedTargetMap = last.damageByTarget.toMutableMap()
            updatedTargetMap[event.target] = (updatedTargetMap[event.target] ?: 0L) + event.damage.toLong()
            breathCasts = breathCasts.toMutableList().apply {
              set(lastIdx, last.copy(
                damage = last.damage + event.damage.toLong(),
                damageByTarget = updatedTargetMap
              ))
            }
          }
        } else if (isRocketDamage && rocketCasts.isNotEmpty()) {
          val lastIdx = rocketCasts.indexOfLast { cast ->
            event.timestamp >= cast.timestamp && event.timestamp - cast.timestamp <= 15_000L
          }
          if (lastIdx >= 0) {
            val last = rocketCasts[lastIdx]
            val updatedTargetMap = last.damageByTarget.toMutableMap()
            updatedTargetMap[event.target] = (updatedTargetMap[event.target] ?: 0L) + event.damage.toLong()
            rocketCasts = rocketCasts.toMutableList().apply {
              set(lastIdx, last.copy(
                damage = last.damage + event.damage.toLong(),
                damageByTarget = updatedTargetMap
              ))
            }
          }
        }

        val updated = existing.copy(
          recentDamageEvents = (existing.recentDamageEvents + event).takeLast(100),
          sessionDamageTotal = existing.sessionDamageTotal + event.damage.toLong(),
          recentCids = event.cid?.let { (existing.recentCids + it).distinct().takeLast(50) } ?: existing.recentCids,
          lastEvent = event.timestamp,
          sessionBreathCasts = breathCasts,
          sessionRocketCasts = rocketCasts
        )
        petCards[petKey] = updated
      }
    }
  }

  /**
   * Called by PetAccumulatorInteractor to record pet successful casts (helpful for correlation).
   */
  fun postPetSuccessfulCast(petKey: String, event: SuccessfulCastEvent) {
    if (!CombatLogInteractor.isRecording.value) return
    Log.info(TAG, "Posting pet successful cast event to petKey=$petKey: $event")
    scope.launch {
      mutex.withLock {
        val existing = petCards[petKey]
        if (existing == null) {
          Log.info(TAG, "Pet card not found for key '$petKey' when posting cast. Available keys: ${petCards.keys.toList()}")
          return@withLock
        }

        val isDuplicateRider = existing.sessionBreathCasts.any {
          it.timestamp == event.timestamp && it.spellName.equals(event.spell, ignoreCase = true)
        } || existing.sessionRocketCasts.any {
          it.timestamp == event.timestamp && it.spellName.equals(event.spell, ignoreCase = true)
        }

        // Track rider spell casts for breath/rocket counters
        val petSkill = petSkillWhitelist.find { skill ->
          skill.isPetInitiator && (
            skill.id == event.spellId ||
              skill.possibleNames.any { it.equals(event.spell, ignoreCase = true) }
          )
        }
        val isDragonBreath = petSkill?.id in DRAGON_BREATH_RIDER_SPELL_IDS
        val isDrakeBreath = petSkill?.id == DRAKE_BREATH_RIDER_SPELL_ID
        val isGuidedMissilesRider = petSkill?.id == GUIDED_MISSILES_RIDER_SPELL_ID

        val castEvent = RiderCastEvent(
          timestamp = event.timestamp,
          damage = 0L,
          spellName = event.spell,
          emoji = if (isDrakeBreath) "\u2744\uFE0F" else "\uD83D\uDD25" // ❄️ for drake, 🔥 for dragon
        )

        val updated = existing.copy(
          recentDebuffAppliedEvents = existing.recentDebuffAppliedEvents,
          recentDamageEvents = existing.recentDamageEvents,
          recentCids = event.cid?.let { (existing.recentCids + it).distinct().takeLast(50) } ?: existing.recentCids,
          lastEvent = event.timestamp,
          sessionBreathCasts = if (!isDuplicateRider && (isDragonBreath == true || isDrakeBreath == true)) existing.sessionBreathCasts + castEvent else existing.sessionBreathCasts,
          sessionRocketCasts = if (!isDuplicateRider && isGuidedMissilesRider) existing.sessionRocketCasts + castEvent else existing.sessionRocketCasts
        )
        petCards[petKey] = updated
      }
    }
  }

  /**
   * Lightweight accessor used by PetAccumulatorInteractor to search petCards by pet name.
   * Returns list of Map.Entry\<String, PetCard\> to preserve the internal key.
   */
  fun getPetEntriesByName(petName: String): List<Map.Entry<String, com.reoky.raidframer.core.model.PetCard>> {
    return if (petName.isBlank()) petCards.entries.toList()
    else petCards.entries.filter { it.value.name.equals(petName, ignoreCase = true) }
  }


  /*
   * Returns a list of real players to be used by analysis interactors. (Like the accumulator)
   * This allows other interactors to search player history without holding the main lock.
   */
  fun getRealPlayersSnapshot(): List<PlayerCard> {
    return cards.values.filter { it.isRealPlayer }
  }

  /*
   * When the user tabs over a target the active target is switched here and throughout the app. This is performed inside the
   * interactor to ensure thread-safety and proper synchronization because we will be updating the corresponding player card with
   * the faction info. Which could change if the player exiles or is in a duel.
   */
  fun switchActiveTarget(target: TargetUpdatedPayload) {
    if (target.name.isBlank()) return // don't switch to non-targets
    AppState.selectTarget(target.name)
    val faction = Faction.fromString(target.faction)
    scope.launch {
      mutex.withLock {
        createCardIfNoneExists(playerName = target.name)
        cards[target.name]?.let { card ->
          val updatedCache = card.cache
            // If a DB cache already exists, copy just the small faction fields (cheap)
            ?.copy(
              lastKnownFaction = if (faction != Faction.UNKNOWN) faction.value else card.lastKnownFaction,
              lastKnownFactionStatus = FactionStatus.fromString(target.factionStatus).value,
              lastKnownGuild = target.guild,
              lastKnownGearScore = target.gearScore
            )
          // Otherwise build a full cache object (fills timestamps and defaults) and apply overrides
            ?: card.createCacheObject().copy(
              lastKnownFaction = if (faction != Faction.UNKNOWN) faction.value else card.lastKnownFaction,
              lastKnownFactionStatus = FactionStatus.fromString(target.factionStatus).value,
              lastKnownGuild = target.guild,
              lastKnownGearScore = target.gearScore
            )

          cards[target.name] = card.copy(
            lastKnownFactionStatus = updatedCache.lastKnownFactionStatus,
            lastKnownFaction = updatedCache.lastKnownFaction,
            lastKnownGuild = updatedCache.lastKnownGuild,
            lastKnownGearScore = updatedCache.lastKnownGearScore,
            cache = updatedCache
          )
        }
      }
    }
    if (target.type == "character") {
      stronglyAssertIsPlayer(cid = null, name = target.name, classMap = target.classMap)
    }
    Log.info(TAG, "Player's tab-target switched to ${target.name} with faction $faction and gear score of ${target.gearScore}.")
  }

  /*
   * Processes a batch of resolved death/kill attributions atomically.
   * Input: List of Triple(VictimName, DeathTimestamp, KillerName?)'
   * This doesn't do the calculation of who killed whom, just applies the results to the cache.
   * The calculation is performed in the DeathAccumulatorInteractor. ^_^
   */
  fun processDeathBatch(batchResults: List<DeathAccumulatorInteractor.DeathAttribution>) {
    if (!CombatLogInteractor.isRecording.value) return
    scope.launch {
      mutex.withLock {
        batchResults.forEach { attribution ->

          // First we update the victim with both kill methods
          createCardIfNoneExists(playerName = attribution.victimName)
          cards[attribution.victimName]?.let { victim ->
            cards[attribution.victimName] = victim.postDeathEvent(
              timestamp = attribution.timestamp,
              killerMostDamage = attribution.killerMostDamage,
              killerKillingBlow = attribution.killerKillingBlow
            )
          }

          // 2. Update Killer (Most Damage method)
          attribution.killerMostDamage?.let { killerName ->
            cards[killerName]?.let { killer ->
              cards[killerName] = killer.postKillEvent(
                timestamp = attribution.timestamp,
                victimName = attribution.victimName,
                preDeathSpells = attribution.killerMostDamageSpells
              )
            }
          }

          // 3. Update Killer (Killing Blow method)
          attribution.killerKillingBlow?.let { killerName ->
            cards[killerName]?.let { killer ->
              cards[killerName] = killer.postKillEventKB(
                timestamp = attribution.timestamp,
                victimName = attribution.victimName
              )
            }
          }
        }
      }
    }
  }

  /**
   * Categorize players by faction instead of by raid membership.
   * Returns Pair(ourFactionPlayers, oppositionPlayers) where opposition aggregates all opposing factions
   */
  private fun aggregateSessionLongByFaction(selector: (PlayerCard) -> Number): Map<Faction, Float> {
    val totals = mutableMapOf(
      Faction.HARANYA to 0L,
      Faction.NUIA to 0L,
      Faction.PIRATE to 0L
    )

    // iterate snapshot of cards to avoid concurrent modification issues
    cards.values
      .filter { it.isRealPlayer }
      .forEach { card ->
        val faction = Faction.fromString(card.lastKnownFaction)
        if (faction == Faction.HARANYA || faction == Faction.NUIA || faction == Faction.PIRATE) {
          totals[faction] = totals.getOrDefault(faction, 0L) + selector(card).toLong()
        }
      }
    return totals.mapValues { it.value.toFloat() }
  }

  data class SpellDamage(val spell: String, val total: Double)
  private fun aggregateDamageBySpellForFaction(cards: List<PlayerCard>, faction: Faction): List<SpellDamage> {
    val totals = mutableMapOf<String, Double>()
    cards
      .filter { it.isRealPlayer && Faction.fromString(it.lastKnownFaction) == faction }
      .forEach { card ->
        card.sessionSpellDamageMap.forEach { (spell, damage) ->
          totals[spell] = totals.getOrDefault(spell, 0.0) + damage.toDouble() // accumulate from cards instead of recent events yayaya
        }
      }

    return totals.entries
      .map { SpellDamage(it.key, it.value) }
      .sortedByDescending { it.total }
      .take(100)
  }

  data class ItemUsage(val itemName: StringResource, val count: Int)
  private fun aggregateItemUsesByFaction(cards: List<PlayerCard>, faction: Faction): List<ItemUsage> {
    val totals = mutableMapOf<StringResource, Int>()
    cards
      .filter { it.isRealPlayer && Faction.fromString(it.lastKnownFaction) == faction }
      .forEach { card ->
        card.recentSkillItemUsages.forEach { triple ->
          val itemRes = triple.second
          totals[itemRes] = totals.getOrDefault(itemRes, 0) + 1
        }
      }

    return totals.entries
      .map { ItemUsage(it.key, it.value) }
      .sortedByDescending { it.count }
      .take(100)
  }

  /**
   * You can, like, feed these Comparators to sortedWith() and it allows you compare against a running sequence by returning
   */
  private val gearComparator = Comparator<PlayerCard> { a, b ->
    b.lastKnownGearScore.compareTo(a.lastKnownGearScore)
  }

  /* UI Subscriptions */
  fun observeCard(name: String): StateFlow<PlayerCard?> {
    return snapshotFlow { cards[name]?.copy() }
    .sample(250L)
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), cards[name])
  }

  var topDamage: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionDamageTotal > 0 }.sortedByDescending { it.sessionDamageTotal }
        .take(100)
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  var topHeals: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionHealTotal > 0 }.sortedByDescending { it.sessionHealTotal }
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  var topCC: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionCCTotal > 0 }.sortedByDescending { it.sessionCCTotal }
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  var topBuffs: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionBuffTotal > 0 }.sortedByDescending { it.sessionBuffTotal }
        .take(100)
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  var topLifeMenders: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { cards ->
      cards.filter { it.isRealPlayer && it.lifeMendTotal > 0 && it.lifeMendHealAmounts.isNotEmpty() }
        .sortedByDescending { it.lifeMendTotal }
        .take(25)
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  var topDebuff: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionDebuffTotal > 0 }.sortedByDescending { it.sessionDebuffTotal }
        .take(100)
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  var topCharms: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionCharmTotal > 0 }.sortedByDescending { it.sessionCharmTotal }
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topSilences: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionSilenceTotal > 0 }.sortedByDescending { it.sessionSilenceTotal }
        .take(100)
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topSongs: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionSongsTotal > 0 }.sortedByDescending { it.sessionSongsTotal }
        .take(100)
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topDistresses: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionDistressTotal > 0 }.sortedByDescending { it.sessionDistressTotal }
        .take(100)
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topTigerStrikes: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionTigerStrikeTotal > 0 }.sortedByDescending { it.sessionTigerStrikeTotal }
        .take(100)
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topFreezes: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionFreezeTotal > 0 }.sortedByDescending { it.sessionFreezeTotal }
        .take(100)
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topTrips: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionTripsTotal > 0 }.sortedByDescending { it.sessionTripsTotal }
        .take(100)
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topBubbles: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionBubblesTotal > 0 }.sortedByDescending { it.sessionBubblesTotal }
        .take(100)
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topBracings: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionBracingsTotal > 0 }.sortedByDescending { it.sessionBracingsTotal }
        .take(100)
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topShieldStrip: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionShieldStripTotal > 0 }.sortedByDescending { it.sessionShieldStripTotal }
        .take(100)
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topWeaponDisables: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionWeaponDisablesTotal > 0 }.sortedByDescending { it.sessionWeaponDisablesTotal }
        .take(100)
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topPotionDisables: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionPotionDisablesTotal > 0 }.sortedByDescending { it.sessionPotionDisablesTotal }
        .take(100)
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topBdGlider: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionBdGliderTotal > 0 }.sortedByDescending { it.sessionBdGliderTotal }
        .take(100)
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topCrystalWings: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionCrystalWingsTotal > 0 }.sortedByDescending { it.sessionCrystalWingsTotal }
        .take(100)
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topGliderDisables: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionGliderDisablesTotal > 0 }.sortedByDescending { it.sessionGliderDisablesTotal }
        .take(100)
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topProvoked: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionProvokedTotal > 0 }.sortedByDescending { it.sessionProvokedTotal }
        .take(100)
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topDefiance: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { it.filter { card -> card.isRealPlayer && card.sessionDefianceTotal > 0 }.sortedByDescending { card -> card.sessionDefianceTotal }.take(100) }
    .distinctUntilChanged().stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topGardenDefiance: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { it.filter { card -> card.isRealPlayer && card.sessionGardenDefianceTotal > 0 }.sortedByDescending { card -> card.sessionGardenDefianceTotal }.take(100) }
    .distinctUntilChanged().stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topPurges: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { it.filter { card -> card.isRealPlayer && card.sessionPurgeTotal > 0 }.sortedByDescending { card -> card.sessionPurgeTotal }.take(100) }
    .distinctUntilChanged().stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topSacDances: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { it.filter { card -> card.isRealPlayer && card.sessionSacDanceTotal > 0 }.sortedByDescending { card -> card.sessionSacDanceTotal }.take(100) }
    .distinctUntilChanged().stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topDeepTranquility: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { it.filter { card -> card.isRealPlayer && card.sessionDeepTranquilityTotal > 0 }.sortedByDescending { card -> card.sessionDeepTranquilityTotal }.take(100) }
    .distinctUntilChanged().stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topDeependDebuff: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { it.filter { card -> card.isRealPlayer && card.sessionDeependDebuffTotal > 0 }.sortedByDescending { card -> card.sessionDeependDebuffTotal }.take(100) }
    .distinctUntilChanged().stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topThrowDagger: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { it.filter { card -> card.isRealPlayer && card.sessionThrowDaggerTotal > 0 }.sortedByDescending { card -> card.sessionThrowDaggerTotal }.take(100) }
    .distinctUntilChanged().stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topStuns: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { it.filter { card -> card.isRealPlayer && card.sessionStunsTotal > 0 }.sortedByDescending { card -> card.sessionStunsTotal }.take(100) }
    .distinctUntilChanged().stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topStaggers: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { it.filter { card -> card.isRealPlayer && card.sessionStaggersTotal > 0 }.sortedByDescending { card -> card.sessionStaggersTotal }.take(100) }
    .distinctUntilChanged().stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topPetrification: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { it.filter { card -> card.isRealPlayer && card.sessionPetrificationTotal > 0 }.sortedByDescending { card -> card.sessionPetrificationTotal }.take(100) }
    .distinctUntilChanged().stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topAbsorbLifeforce: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { it.filter { card -> card.isRealPlayer && card.sessionAbsorbLifeforceTotal > 0 }.sortedByDescending { card -> card.sessionAbsorbLifeforceTotal }.take(100) }
    .distinctUntilChanged().stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topCorrosiveBarrage: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { it.filter { card -> card.isRealPlayer && card.sessionCorrosiveBarrageTotal > 0 }.sortedByDescending { card -> card.sessionCorrosiveBarrageTotal }.take(100) }
    .distinctUntilChanged().stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topBlindedByCrows: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { it.filter { card -> card.isRealPlayer && card.sessionBlindedByCrowsTotal > 0 }.sortedByDescending { card -> card.sessionBlindedByCrowsTotal }.take(100) }
    .distinctUntilChanged().stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topMistSunder: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { it.filter { card -> card.isRealPlayer && card.sessionMistSunderTotal > 0 }.sortedByDescending { card -> card.sessionMistSunderTotal }.take(100) }
    .distinctUntilChanged().stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topRegularSunder: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { it.filter { card -> card.isRealPlayer && card.sessionRegularSunderTotal > 0 }.sortedByDescending { card -> card.sessionRegularSunderTotal }.take(100) }
    .distinctUntilChanged().stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topImpaleImmunity: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { it.filter { card -> card.isRealPlayer && card.sessionImpaleImmunityTotal > 0 }.sortedByDescending { card -> card.sessionImpaleImmunityTotal }.take(100) }
    .distinctUntilChanged().stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topProtectiveWings: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { it.filter { card -> card.isRealPlayer && card.sessionProtectiveWingsTotal > 0 }.sortedByDescending { card -> card.sessionProtectiveWingsTotal }.take(100) }
    .distinctUntilChanged().stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topCourageousAction: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { it.filter { card -> card.isRealPlayer && card.sessionCourageousActionTotal > 0 }.sortedByDescending { card -> card.sessionCourageousActionTotal }.take(100) }
    .distinctUntilChanged().stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topManaBarrier: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { it.filter { card -> card.isRealPlayer && card.sessionManaBarrierTotal > 0 }.sortedByDescending { card -> card.sessionManaBarrierTotal }.take(100) }
    .distinctUntilChanged().stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topRevive: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { it.filter { card -> card.isRealPlayer && card.sessionReviveTotal > 0 }.sortedByDescending { card -> card.sessionReviveTotal }.take(100) }
    .distinctUntilChanged().stateIn(scope, SharingStarted.Eagerly, emptyList())

  // Coherence rankings (session time in range of the recorder, sorted by most time at each threshold)
  val topCoherenceRender: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { it.filter { card -> card.isRealPlayer && card.sessionCoherenceRenderMs > 0L }.sortedByDescending { card -> card.sessionCoherenceRenderMs }.take(100) }
    .distinctUntilChanged().stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topCoherenceRaid: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { it.filter { card -> card.isRealPlayer && card.sessionCoherenceRaidMs > 0L }.sortedByDescending { card -> card.sessionCoherenceRaidMs }.take(100) }
    .distinctUntilChanged().stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topCoherenceClump: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { it.filter { card -> card.isRealPlayer && card.sessionCoherenceClumpMs > 0L }.sortedByDescending { card -> card.sessionCoherenceClumpMs }.take(100) }
    .distinctUntilChanged().stateIn(scope, SharingStarted.Eagerly, emptyList())

  // Loot buff rankings (raid-wide, not faction-based). Best peak = highest summed loot
  // buff %, worst peak = lowest summed % among players who actually loot buffed, and
  // top buff count = most simultaneous buffs (the "too many?" signal).
  val topLootPeak: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionPeakLootBuffAmount > 0 }
        .sortedByDescending { it.sessionPeakLootBuffAmount }
        .take(25)
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  val worstLootPeak: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionPeakLootBuffAmount > 0 }
        .sortedBy { it.sessionPeakLootBuffAmount }
        .take(25)
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topBuffCount: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionCurrentBuffCount > 0 }
        .sortedByDescending { it.sessionCurrentBuffCount }
        .take(25)
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  var topGliderGamers: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionGliderTotal > 0 }.sortedByDescending { it.sessionGliderTotal }
        .take(100)
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  var topPotters: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionPotionTotal > 0 }.sortedByDescending { it.sessionPotionTotal }
        .take(100)
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  var topItemSkillCasters: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionItemSkillTotal > 0 }.sortedByDescending { it.sessionItemSkillTotal }
        .take(100)
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topKills: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionKillTotal > 0 }.sortedByDescending { it.sessionKillTotal }
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topKillsKB: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionKillTotalKB > 0 }
        .sortedByDescending { it.sessionKillTotalKB }
        .take(100)
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topHealsOde: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionOdeHealsTotal > 0 }
        .sortedByDescending { it.sessionOdeHealsTotal }
        .take(100)
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topKillsLifetime: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { cards ->
      cards.filter { it.isRealPlayer && (it.cache?.lifetimeTotalKills ?: 0L) > 0L }
        .sortedByDescending { it.cache?.lifetimeTotalKills ?: 0L }
        .take(100)
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topDeaths: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionDeathTotal > 0 }.sortedByDescending { it.sessionDeathTotal }
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topDamageTaken: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionDamageTakenTotal > 0 }
        .sortedByDescending { it.sessionDamageTakenTotal }
        .take(100)
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topHealsReceived: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionHealsReceivedTotal > 0 }
        .sortedByDescending { it.sessionHealsReceivedTotal }
        .take(100)
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  val nearbyNuianRaidParties: StateFlow<List<PlayerCard>> = snapshotFlow {
    val cardList = cards.values.toList()
    val combinedParties = (raids[0] ?: emptyList()) + (raids[1] ?: emptyList())
    Pair(cardList, combinedParties)
  }.map { (allCards, combinedParties) ->
    val candidates = allCards
      .filter { it.isRealPlayer && it.lastKnownFaction == Faction.NUIA.value }
      .sortedWith(gearComparator)

    RaidOrganizer.organize(candidates, ArrangementMode.CLASSIC_ROLES).take(400)
  }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  val nearbyHaraniRaidParties: StateFlow<List<PlayerCard>> = snapshotFlow {
    val cardList = cards.values.toList()
    val combinedParties = (raids[0] ?: emptyList()) + (raids[1] ?: emptyList())
    Pair(cardList, combinedParties)
  }
    .map { (allCards, combinedParties) ->
      val candidates = allCards
        .filter { it.isRealPlayer && it.lastKnownFaction == Faction.HARANYA.value }
        .sortedWith(gearComparator)

      RaidOrganizer.organize(candidates, ArrangementMode.CLASSIC_ROLES).take(400)
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  val nearbyPirateRaidParties: StateFlow<List<PlayerCard>> = snapshotFlow {
    val cardList = cards.values.toList()
    val combinedParties = (raids[0] ?: emptyList()) + (raids[1] ?: emptyList())
    Pair(cardList, combinedParties)
  }
    .map { (allCards, combinedParties) ->
      val candidates = allCards
        .filter { it.isRealPlayer && it.lastKnownFaction == Faction.PIRATE.value }
        .sortedWith(gearComparator)

      RaidOrganizer.organize(candidates, ArrangementMode.CLASSIC_ROLES).take(400)
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  /////////////////////////
  // Faction Comparisons //
  /////////////////////////

  val topDamageSpellsHaranya: StateFlow<List<SpellDamage>> = cardSnapshots
    .map { cardList -> aggregateDamageBySpellForFaction(cardList, Faction.HARANYA) }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topDamageSpellsNuia: StateFlow<List<SpellDamage>> = cardSnapshots
    .map { cardList -> aggregateDamageBySpellForFaction(cardList, Faction.NUIA) }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topDamageSpellsPirate: StateFlow<List<SpellDamage>> = cardSnapshots
    .map { cardList -> aggregateDamageBySpellForFaction(cardList, Faction.PIRATE) }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topItemUsesHaranya: StateFlow<List<ItemUsage>> = cardSnapshots
    .map { cardList -> aggregateItemUsesByFaction(cardList, Faction.HARANYA) }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topItemUsesNuia: StateFlow<List<ItemUsage>> = cardSnapshots
    .map { cardList -> aggregateItemUsesByFaction(cardList, Faction.NUIA) }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topItemUsesPirate: StateFlow<List<ItemUsage>> = cardSnapshots
    .map { cardList -> aggregateItemUsesByFaction(cardList, Faction.PIRATE) }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  // top kills haranya, nuia, and pirate
  val topKillsHaranya: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionKillTotal > 0 && it.lastKnownFaction == Faction.HARANYA.value }
        .sortedByDescending { it.sessionKillTotal }
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())
  val topKillsNuia: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionKillTotal > 0 && it.lastKnownFaction == Faction.NUIA.value }
        .sortedByDescending { it.sessionKillTotal }
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())
  val topKillsPirate: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionKillTotal > 0 && it.lastKnownFaction == Faction.PIRATE.value }.sortedByDescending { it.sessionKillTotal }
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topOdeHaranya: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionOdeHealsTotal > 0 && it.lastKnownFaction == Faction.HARANYA.value }
        .sortedByDescending { it.sessionOdeHealsTotal }
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())
  val topOdeNuia: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionOdeHealsTotal > 0 && it.lastKnownFaction == Faction.NUIA.value }
        .sortedByDescending { it.sessionOdeHealsTotal }
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())
  val topOdePirate: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionOdeHealsTotal > 0 && it.lastKnownFaction == Faction.PIRATE.value }
        .sortedByDescending { it.sessionOdeHealsTotal }
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  /**
   * Compares average charm totals between raid members and opposition.
   * Returns a map with "Our Raid" and "Opposition" as keys.
   */

  // three-way compare
  val factionCharmComparisonAll: StateFlow<Map<String, Float>> = cardSnapshots
    .map {
      val totals = aggregateSessionLongByFaction({ it.sessionCharmTotal })
      mapOf(
        Faction.HARANYA.value to (totals[Faction.HARANYA] ?: 0f),
        Faction.NUIA.value to (totals[Faction.NUIA] ?: 0f),
        Faction.PIRATE.value to (totals[Faction.PIRATE] ?: 0f)
      )
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyMap())

  val factionSilenceComparisonAll: StateFlow<Map<String, Float>> = cardSnapshots
    .map {
      val totals = aggregateSessionLongByFaction({ it.sessionSilenceTotal })
      mapOf(
        Faction.HARANYA.value to (totals[Faction.HARANYA] ?: 0f),
        Faction.NUIA.value to (totals[Faction.NUIA] ?: 0f),
        Faction.PIRATE.value to (totals[Faction.PIRATE] ?: 0f)
      )
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyMap())

  val factionDistressComparisonAll: StateFlow<Map<String, Float>> = cardSnapshots
    .map {
      val totals = aggregateSessionLongByFaction({ it.sessionDistressTotal })
      mapOf(
        Faction.HARANYA.value to (totals[Faction.HARANYA] ?: 0f),
        Faction.NUIA.value to (totals[Faction.NUIA] ?: 0f),
        Faction.PIRATE.value to (totals[Faction.PIRATE] ?: 0f)
      )
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyMap())

  val factionTigerStrikeComparisonAll: StateFlow<Map<String, Float>> = cardSnapshots
    .map {
      val totals = aggregateSessionLongByFaction({ it.sessionTigerStrikeTotal })
      mapOf(
        Faction.HARANYA.value to (totals[Faction.HARANYA] ?: 0f),
        Faction.NUIA.value to (totals[Faction.NUIA] ?: 0f),
        Faction.PIRATE.value to (totals[Faction.PIRATE] ?: 0f)
      )
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyMap())

  val factionFreezeComparisonAll: StateFlow<Map<String, Float>> = cardSnapshots
    .map {
      val totals = aggregateSessionLongByFaction({ it.sessionFreezeTotal })
      mapOf(
        Faction.HARANYA.value to (totals[Faction.HARANYA] ?: 0f),
        Faction.NUIA.value to (totals[Faction.NUIA] ?: 0f),
        Faction.PIRATE.value to (totals[Faction.PIRATE] ?: 0f)
      )
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyMap())

  val factionTripsComparisonAll: StateFlow<Map<String, Float>> = cardSnapshots
    .map {
      val totals = aggregateSessionLongByFaction({ it.sessionTripsTotal })
      mapOf(
        Faction.HARANYA.value to (totals[Faction.HARANYA] ?: 0f),
        Faction.NUIA.value to (totals[Faction.NUIA] ?: 0f),
        Faction.PIRATE.value to (totals[Faction.PIRATE] ?: 0f)
      )
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyMap())

  val factionBubblesComparisonAll: StateFlow<Map<String, Float>> = cardSnapshots
    .map {
      val totals = aggregateSessionLongByFaction({ it.sessionBubblesTotal })
      mapOf(
        Faction.HARANYA.value to (totals[Faction.HARANYA] ?: 0f),
        Faction.NUIA.value to (totals[Faction.NUIA] ?: 0f),
        Faction.PIRATE.value to (totals[Faction.PIRATE] ?: 0f)
      )
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyMap())

  val factionBracingsComparisonAll: StateFlow<Map<String, Float>> = cardSnapshots
    .map {
      val totals = aggregateSessionLongByFaction({ it.sessionBracingsTotal })
      mapOf(
        Faction.HARANYA.value to (totals[Faction.HARANYA] ?: 0f),
        Faction.NUIA.value to (totals[Faction.NUIA] ?: 0f),
        Faction.PIRATE.value to (totals[Faction.PIRATE] ?: 0f)
      )
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyMap())

  val factionShieldStripComparisonAll: StateFlow<Map<String, Float>> = cardSnapshots
    .map {
      val totals = aggregateSessionLongByFaction({ it.sessionShieldStripTotal })
      mapOf(
        Faction.HARANYA.value to (totals[Faction.HARANYA] ?: 0f),
        Faction.NUIA.value to (totals[Faction.NUIA] ?: 0f),
        Faction.PIRATE.value to (totals[Faction.PIRATE] ?: 0f)
      )
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyMap())

  val factionWeaponDisablesComparisonAll: StateFlow<Map<String, Float>> = cardSnapshots
    .map {
      val totals = aggregateSessionLongByFaction({ it.sessionWeaponDisablesTotal })
      mapOf(
        Faction.HARANYA.value to (totals[Faction.HARANYA] ?: 0f),
        Faction.NUIA.value to (totals[Faction.NUIA] ?: 0f),
        Faction.PIRATE.value to (totals[Faction.PIRATE] ?: 0f)
      )
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyMap())

  val factionPotionDisablesComparisonAll: StateFlow<Map<String, Float>> = cardSnapshots
    .map {
      val totals = aggregateSessionLongByFaction({ it.sessionPotionDisablesTotal })
      mapOf(
        Faction.HARANYA.value to (totals[Faction.HARANYA] ?: 0f),
        Faction.NUIA.value to (totals[Faction.NUIA] ?: 0f),
        Faction.PIRATE.value to (totals[Faction.PIRATE] ?: 0f)
      )
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyMap())

  val factionBdGliderComparisonAll: StateFlow<Map<String, Float>> = cardSnapshots
    .map {
      val totals = aggregateSessionLongByFaction({ it.sessionBdGliderTotal })
      mapOf(
        Faction.HARANYA.value to (totals[Faction.HARANYA] ?: 0f),
        Faction.NUIA.value to (totals[Faction.NUIA] ?: 0f),
        Faction.PIRATE.value to (totals[Faction.PIRATE] ?: 0f)
      )
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyMap())

  val factionCrystalWingsComparisonAll: StateFlow<Map<String, Float>> = cardSnapshots
    .map {
      val totals = aggregateSessionLongByFaction({ it.sessionCrystalWingsTotal })
      mapOf(
        Faction.HARANYA.value to (totals[Faction.HARANYA] ?: 0f),
        Faction.NUIA.value to (totals[Faction.NUIA] ?: 0f),
        Faction.PIRATE.value to (totals[Faction.PIRATE] ?: 0f)
      )
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyMap())

  val factionGliderDisablesComparisonAll: StateFlow<Map<String, Float>> = cardSnapshots
    .map {
      val totals = aggregateSessionLongByFaction({ it.sessionGliderDisablesTotal })
      mapOf(
        Faction.HARANYA.value to (totals[Faction.HARANYA] ?: 0f),
        Faction.NUIA.value to (totals[Faction.NUIA] ?: 0f),
        Faction.PIRATE.value to (totals[Faction.PIRATE] ?: 0f)
      )
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyMap())

  val factionProvokedComparisonAll: StateFlow<Map<String, Float>> = cardSnapshots
    .map {
      val totals = aggregateSessionLongByFaction({ it.sessionProvokedTotal })
      mapOf(
        Faction.HARANYA.value to (totals[Faction.HARANYA] ?: 0f),
        Faction.NUIA.value to (totals[Faction.NUIA] ?: 0f),
        Faction.PIRATE.value to (totals[Faction.PIRATE] ?: 0f)
      )
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyMap())

  // PvP performance score pumps (one per faction)
  val topPerformanceHaranya: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { cardList ->
      cardList
        .filter { it.isRealPlayer && it.lastKnownFaction == Faction.HARANYA.value && it.pvpPerformancePoints() > 0 }
        .sortedByDescending { it.pvpPerformancePoints() }
        .take(100)
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topPerformanceNuia: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { cardList ->
      cardList
        .filter { it.isRealPlayer && it.lastKnownFaction == Faction.NUIA.value && it.pvpPerformancePoints() > 0 }
        .sortedByDescending { it.pvpPerformancePoints() }
        .take(100)
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  val topPerformancePirate: StateFlow<List<PlayerCard>> = cardSnapshots
    .map { cardList ->
      cardList
        .filter { it.isRealPlayer && it.lastKnownFaction == Faction.PIRATE.value && it.pvpPerformancePoints() > 0 }
        .sortedByDescending { it.pvpPerformancePoints() }
        .take(100)
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyList())

  val buildCountsHaranya: StateFlow<Map<String, Int>> = cardSnapshots
    .map {
      it
        .filter { it.isRealPlayer && Faction.fromString(it.lastKnownFaction) == Faction.HARANYA && it.hasPvPParticipation() }
        .groupingBy { it.currentBuild }
        .eachCount()
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyMap())

  val buildCountsNuia: StateFlow<Map<String, Int>> = cardSnapshots
    .map {
      it
        .filter { it.isRealPlayer && Faction.fromString(it.lastKnownFaction) == Faction.NUIA && it.hasPvPParticipation() }
        .groupingBy { it.currentBuild }
        .eachCount()
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyMap())

  val buildCountsPirate: StateFlow<Map<String, Int>> = cardSnapshots
    .map {
      it
        .filter { it.isRealPlayer && Faction.fromString(it.lastKnownFaction) == Faction.PIRATE && it.hasPvPParticipation() }
        .groupingBy { it.currentBuild }
        .eachCount()
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyMap())


  // two-way compare (maybe deprecated later)
  val factionCharmComparison: StateFlow<Map<String, Float>> = cardSnapshots
    .map {
      val totals = aggregateSessionLongByFaction({ it.sessionCharmTotal })
      val playerFaction = Faction.fromString(RFConfig.state.value.playerFaction)
      val our = totals[playerFaction] ?: 0f
      val opposition = totals.filterKeys { it != playerFaction }.values.sum()
      mapOf("Our Faction" to our, "Opposition" to opposition)
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyMap())

  val factionSilenceComparison: StateFlow<Map<String, Float>> = cardSnapshots
    .map {
      val totals = aggregateSessionLongByFaction({ it.sessionSilenceTotal })
      val playerFaction = Faction.fromString(RFConfig.state.value.playerFaction)
      val our = totals[playerFaction] ?: 0f
      val opposition = totals.filterKeys { it != playerFaction }.values.sum()
      mapOf("Our Faction" to our, "Opposition" to opposition)
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, emptyMap())

  val factionDistressComparison: StateFlow<Map<String, Float>> = cardSnapshots
    .map {
      val totals = aggregateSessionLongByFaction({ it.sessionDistressTotal })
      val playerFaction = Faction.fromString(RFConfig.state.value.playerFaction)
      val our = totals[playerFaction] ?: 0f
      val opposition = totals.filterKeys { it != playerFaction }.values.sum()
      mapOf("Our Faction" to our, "Opposition" to opposition)
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.WhileSubscribed(20000), emptyMap())

  // Lifetime session count for the current player (archived sessions + 1 if a session is active)
  private val _ownSessionCount = MutableStateFlow(0)
  val ownSessionCount: StateFlow<Int> = _ownSessionCount.asStateFlow()

  /**
   * Queries the archived session count for the current player and adds 1 if there is
   * an active recording session. Called on startup and after every archive step.
   */
  suspend fun refreshOwnSessionCount() {
    val playerName = RFConfig.state.value.playerName
    if (playerName.isBlank()) {
      _ownSessionCount.value = 0
      return
    }
    _ownSessionCount.value = getSessionCountForPlayer(playerName)
  }

  /**
   * Returns the total number of sessions for [playerName] (archived rows in
   * player_session_totals plus one if there is an active recording session for that player).
   */
  suspend fun getSessionCountForPlayer(playerName: String): Int {
    val archivedCount = RFDao.playerSessionDao.getSessionCountForPlayer(playerName)
    val hasActiveSession = RFConfig.state.value.lastSessionStart > 0L && RFConfig.state.value.playerName == playerName
    return archivedCount + if (hasActiveSession) 1 else 0
  }

  var activePets: StateFlow<List<PetCard>> = snapshotFlow { petCards.values.toList() }
    .sample(250L)
    .map { pets ->
      pets.filter { it.sessionDamageTotal > 0 || it.sessionDebuffTotal > 0 || it.sessionBreathCasts.isNotEmpty() || it.sessionRocketCasts.isNotEmpty() || it.recentDamageEvents.isNotEmpty() }
        .sortedByDescending { it.sessionDamageTotal }
        .take(50)
    }
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  fun getRankingFlow(category: CombatRankingCategory): StateFlow<List<PlayerCard>> {
    return when (category) {
      CombatRankingCategory.CHARMS -> topCharms
      CombatRankingCategory.SILENCES -> topSilences
      CombatRankingCategory.DISTRESSES -> topDistresses
      CombatRankingCategory.DEBUFFS -> topDebuff
      CombatRankingCategory.SONGS -> topSongs
      CombatRankingCategory.BUFFS -> topBuffs
      CombatRankingCategory.POTIONS -> topPotters
      CombatRankingCategory.GLIDERS -> topGliderGamers
      CombatRankingCategory.ITEMS -> topItemSkillCasters
      CombatRankingCategory.TIGER_STRIKES -> topTigerStrikes
      CombatRankingCategory.FREEZES -> topFreezes
      CombatRankingCategory.TRIPS -> topTrips
      CombatRankingCategory.BUBBLES -> topBubbles
      CombatRankingCategory.BRACINGS -> topBracings
      CombatRankingCategory.SHIELD_STRIP -> topShieldStrip
      CombatRankingCategory.WEAPON_DISABLES -> topWeaponDisables
      CombatRankingCategory.POTION_DISABLES -> topPotionDisables
      CombatRankingCategory.BD_GLIDER -> topBdGlider
      CombatRankingCategory.CRYSTAL_WINGS -> topCrystalWings
      CombatRankingCategory.GLIDER_DISABLES -> topGliderDisables
      CombatRankingCategory.PROVOKED -> topProvoked
      CombatRankingCategory.KILLS -> topKills
      CombatRankingCategory.COHERENCE_RENDER -> topCoherenceRender
      CombatRankingCategory.COHERENCE_RAID -> topCoherenceRaid
      CombatRankingCategory.COHERENCE_CLUMP -> topCoherenceClump
    }
  }

  /* Raid Parties UI Subscriptions */
  fun getRaidById(raidId: Int): StateFlow<List<Party>> {
    return snapshotFlow { raids[raidId] ?: listOf() }
      .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())
  }

}
