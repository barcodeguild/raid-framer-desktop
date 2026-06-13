package com.reoky.raidframer.core.interactor

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshotFlow
import com.reoky.raidframer.AppState
import com.reoky.raidframer.core.calc.ArrangementMode
import com.reoky.raidframer.core.calc.MetricRawSample
import com.reoky.raidframer.core.calc.RaidOrganizer
import com.reoky.raidframer.core.calc.RealtimeComputer
import com.reoky.raidframer.core.config.RFConfig
import com.reoky.raidframer.core.database.RFDao
import com.reoky.raidframer.core.definitions.SkillTreeType
import com.reoky.raidframer.core.definitions.SpecType
import com.reoky.raidframer.core.definitions.findSkillTreeForSpell
import com.reoky.raidframer.core.definitions.petSkillWhitelist
import com.reoky.raidframer.core.helpers.createCacheObject
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.stateIn
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

  // Mapping of all the players (and NPCs) sorted in no particular order
  val realtimeComputer = RealtimeComputer(windowBuckets = 60, bucketMillis = 10_000L)
  private val raids = mutableStateMapOf<Int, List<Party>>()
  private val raidAttendance = mutableStateMapOf<Int, MutableSet<String>>()
  private val raidDepartures = mutableStateMapOf<Int, MutableSet<String>>()
  private val _raidDeparturesFlow = MutableStateFlow<Map<Int, Set<String>>>(emptyMap())
  val raidDeparturesFlow: StateFlow<Map<Int, Set<String>>> = _raidDeparturesFlow.asStateFlow()
  private val cards = mutableStateMapOf<String, PlayerCard>()
  private val petCards = mutableStateMapOf<String, PetCard>()
  private val mutex = Mutex() // to protect critical sections during player card updates from other threads

  init {
    scope.launch {
      while (true) {
        realtimeComputer.push(MetricRawSample(System.currentTimeMillis(), 5000.0))
        delay(1000)
      }
    }
  }

  // main event loop
  override suspend fun interact() {
    // Take a snapshot of values to iterate. Iterating the map directly while updates happen
    // (even with ConcurrentHashMap/MutableStateMap) can be risky with heavy logic,
    // and we want to perform calculations without holding a lock. Ok friends!?
    val snapshot = cards.values.toList()

    snapshot.forEach { card ->
      val name = card.name

      // 1. Logic to determine if player should be upgraded from NPC to real player
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
          createCardIfNoneExists(playerName = member.playerName)
        }
      }
    }
  }

  /*
   * Create card for a player if none exists... Upgrade from NPC to Player occurs inside the PlayerCardExtensions helpers.
   * NOTE: This method must be called within a mutex.withLock block as it is not thread-safe on its own.
   */
  private fun createCardIfNoneExists(cid: String? = null, playerName: String) {
    if (!cards.containsKey(playerName)) {
      val cached = runBlocking {
        RFDao.playerCacheDao.getPlayerCacheFor(playerName)
      }
      val previousSpec = cached?.lastKnownSpec ?: SpecType.UNKNOWN.name
      // pre-populate card fields from cache. Very important to get this right so we don't overwrite data and for isRealPlayer flag
      val card = PlayerCard(
        name = playerName,
        recentCids = cid?.let { listOf(it) } ?: listOf(),
        lastEvent = System.currentTimeMillis(), // because an event triggered this load from db
        lastKnownFaction = Faction.fromString(
          cached?.lastKnownFaction ?: Faction.UNKNOWN.value
        ).value, // fixes bad data over time by fitting into the enum
        lastKnownFactionStatus = FactionStatus.fromString(
          cached?.lastKnownFactionStatus ?: Faction.UNKNOWN.value
        ).value, // always code defensive
        lastKnownGuild = cached?.lastKnownGuild ?: "",
        lastKnownGearScore = cached?.lastKnownGearScore ?: 0,
        leaderships = cached?.leaderships ?: 0,
        isLoaded = true,
        isRealPlayer = cached != null, // cache is for players not NPCs, Mounts, Pets, Vehicles, etc
        cache = cached, // everything
        currentBuild = previousSpec,
        currentRole = SpecType.fromName(previousSpec)?.guessPlayerRole()?.value ?: PlayerRole.BLUE.value
      )
      cards[playerName] = card
    }
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
    scope.launch {
      mutex.withLock {
        if (!petCards.containsKey(key)) {
          petCards[key] = PetCard(
            // id is owner + pet name to ensure uniqueness
            petId = buildPetNameKey(owner, petName, petType),
            name = petName,
            owner = owner,
            recentCids = cid?.let { listOf(it) } ?: listOf(),
            lastEvent = System.currentTimeMillis(),
            petType = petType
          )
        } else {
          petCards[key]?.let { card ->
            petCards[key] = card.copy(
              recentCids = cid?.let { (card.recentCids + it).distinct().takeLast(50) } ?: card.recentCids,
              lastEvent = System.currentTimeMillis()
            )
            //debug print all cids for pet id
            Log.debug(TAG, "Pet ${card.petId} (${card.name}) CIDs: ${petCards[key]?.recentCids}")
          }
        }
      }
    }
  }

  /*
   * Reset all session totals and recent events for all players.
   */
  fun resetAllSessions() {
    scope.launch {
      mutex.withLock {
        cards.keys.toList().forEach { name ->
          cards[name] = cards[name]?.resetSession() ?: return@forEach
        }
        petCards.keys.toList().forEach { petId ->
          petCards[petId] = petCards[petId]?.resetSession() ?: return@forEach
        }
      }
    }
  }

  // filter pve damage by checking if the target is a real player
  fun isRealPlayer(playerName: String): Boolean {
    return cards.values.any { it.isRealPlayer && it.name == playerName }
  }

  /*
   * Clears all raids and their parties from memory when the user leaves or gets kicked_by_self from the game client.
   */
  fun clearAllRaids() {
    scope.launch {
      mutex.withLock {
        raids.clear()
      }
    }
  }

  fun getCard(name: String): PlayerCard? {
    return cards[name]
  }

  // gets a list of player cards matching a filter predicate
  fun getGroupCards(filter: (PlayerCard) -> Boolean): List<PlayerCard> {
    return cards.values.filter(filter)
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
   * Clears raid attendance records.
   * @param raidId The raid to clear, or null to clear all raids
   * @param playerName Specific player to remove, or null to clear all players for the raid
   */
  fun clearRaidAttendance(raidId: Int? = null, playerName: String? = null) {
    scope.launch {
      mutex.withLock {
        when {
          // Clear specific player from specific raid
          raidId != null && playerName != null -> {
            raidAttendance[raidId]?.remove(playerName)
            raidDepartures[raidId]?.remove(playerName)
          }
          // Clear all players from specific raid
          raidId != null -> {
            raidAttendance.remove(raidId)
            raidDepartures.remove(raidId)
          }
          // Clear specific player from all raids
          playerName != null -> {
            raidAttendance.values.forEach { it.remove(playerName) }
            raidDepartures.values.forEach { it.remove(playerName) }
          }
          // Clear everything
          else -> {
            raidAttendance.clear()
            raidDepartures.clear()
          }
        }
        _raidDeparturesFlow.value = raidDepartures.mapValues { it.value.toSet() }
      }
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
  }

  /* Event Posting */

  // catch all
  fun postEvent(event: CombatEvent) {
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

  fun postEventInternal(event: CombatEvent) {
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

  private fun postDamage(event: DamageEvent) {
    val cleanSource = event.source.replace("\\s*\\([^)]*\\)$".toRegex(), "").trim()
    val eventSourceIsPet = getPetEntriesByName(cleanSource).isNotEmpty()
    if (eventSourceIsPet || petSkillWhitelist.any { it.id == event.spellId }) {
      PetAccumulatorInteractor.postEvent(event)
      return
    }
    postDamageInternal(event)
  }

  private fun postDamageInternal(event: DamageEvent) {
    scope.launch {
      mutex.withLock {
        createCardIfNoneExists(cid = event.cid, playerName = event.source)
        cards[event.source]?.let { card ->
          cards[event.source] = card.postDamageEvent(event)
        }
        // credit damage taken to target
        createCardIfNoneExists(cid = event.cid, playerName = event.target)
        cards[event.target]?.let { card ->
          cards[event.target] = card.postDamageTakenEvent(event)
        }
      }
    }
  }

  private fun postHeal(event: HealEvent) {
    scope.launch {
      mutex.withLock {
        createCardIfNoneExists(cid = event.cid, event.source)
        cards[event.source]?.let { card ->
          cards[event.source] = card.postHealEvent(event)
        }
        // credit the target with received heals
        createCardIfNoneExists(cid = event.cid, event.target)
        cards[event.target]?.let { card ->
          cards[event.target] = card.postHealsReceivedEvent(event)
        }
      }
    }
  }

  private fun postCasting(event: CastingEvent) {
    scope.launch {
      mutex.withLock {
        createCardIfNoneExists(cid = event.cid, event.source)
        cards[event.source]?.let { card ->
          cards[event.source] = card.postCastingEvent(event)
        }
      }
    }
  }

  private fun postSuccessfulCast(event: SuccessfulCastEvent) {
    val cleanSource = event.source.replace("\\s*\\([^)]*\\)$".toRegex(), "").trim()
    val eventSourceIsPet = getPetEntriesByName(cleanSource).isNotEmpty()
    if (eventSourceIsPet || petSkillWhitelist.any { it.id == event.spellId }) {
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


  // Posting Pet Events
  /**
   * Called by PetAccumulatorInteractor to apply pet damage to a specific pet card key.
   * `petKey` is the internal key format used by petCards: "$owner:$petName".
   */
  fun postPetDamage(petKey: String, event: DamageEvent) {
    Log.info(TAG, "Posting pet damage event to petKey=$petKey: $event")
    scope.launch {
      mutex.withLock {
        // ensure card exists
        val existing = petCards[petKey]
        if (existing == null) {
          // nothing to do if we don't have metadata for this pet yet
          return@withLock
        }
        val updated = existing.copy(
          recentDamageEvents = (existing.recentDamageEvents + event).takeLast(100),
          sessionDamageTotal = existing.sessionDamageTotal + event.damage.toLong(),
          recentCids = event.cid?.let { (existing.recentCids + it).distinct().takeLast(50) } ?: existing.recentCids,
          lastEvent = event.timestamp
        )
        petCards[petKey] = updated
      }
    }
  }

  /**
   * Called by PetAccumulatorInteractor to record pet successful casts (helpful for correlation).
   */
  fun postPetSuccessfulCast(petKey: String, event: SuccessfulCastEvent) {
    Log.info(TAG, "Posting pet successful cast event to petKey=$petKey: $event")
    scope.launch {
      mutex.withLock {
        val existing = petCards[petKey]
        if (existing == null) {
          return@withLock
        }
        val updated = existing.copy(
          recentDebuffAppliedEvents = existing.recentDebuffAppliedEvents, // keep if needed elsewhere
          recentDamageEvents = existing.recentDamageEvents, // no change here
          recentCids = event.cid?.let { (existing.recentCids + it).distinct().takeLast(50) } ?: existing.recentCids,
          lastEvent = event.timestamp
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
    return petCards.entries.filter { it.value.name.equals(petName, ignoreCase = true) }
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
                victimName = attribution.victimName
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
    return snapshotFlow {
      cards[name]?.copy() // force a new instance on each emission
    }
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), cards[name])
  }

  var topDamage: StateFlow<List<PlayerCard>> = snapshotFlow { cards.values.toList() }
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionDamageTotal > 0 }.sortedByDescending { it.sessionDamageTotal }
        .take(100)
    }
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  var topHeals: StateFlow<List<PlayerCard>> = snapshotFlow { cards.values.toList() }
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionHealTotal > 0 }.sortedByDescending { it.sessionHealTotal }
    }
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  var topCC: StateFlow<List<PlayerCard>> = snapshotFlow { cards.values.toList() }
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionCCTotal > 0 }.sortedByDescending { it.sessionCCTotal }
    }
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  var topBuffs: StateFlow<List<PlayerCard>> = snapshotFlow { cards.values.toList() }
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionBuffTotal > 0 }.sortedByDescending { it.sessionBuffTotal }
        .take(100)
    }
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  var topDebuff: StateFlow<List<PlayerCard>> = snapshotFlow { cards.values.toList() }
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionDebuffTotal > 0 }.sortedByDescending { it.sessionDebuffTotal }
        .take(100)
    }
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  var topCharms: StateFlow<List<PlayerCard>> = snapshotFlow { cards.values.toList() }
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionCharmTotal > 0 }.sortedByDescending { it.sessionCharmTotal }
    }
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  val topSilences: StateFlow<List<PlayerCard>> = snapshotFlow { cards.values.toList() }
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionSilenceTotal > 0 }.sortedByDescending { it.sessionSilenceTotal }
        .take(100)
    }
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  val topSongs: StateFlow<List<PlayerCard>> = snapshotFlow { cards.values.toList() }
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionSongsTotal > 0 }.sortedByDescending { it.sessionSongsTotal }
        .take(100)
    }
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  val topDistresses: StateFlow<List<PlayerCard>> = snapshotFlow { cards.values.toList() }
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionDistressTotal > 0 }.sortedByDescending { it.sessionDistressTotal }
        .take(100)
    }
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  var topGliderGamers: StateFlow<List<PlayerCard>> = snapshotFlow { cards.values.toList() }
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionGliderTotal > 0 }.sortedByDescending { it.sessionGliderTotal }
        .take(100)
    }
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  var topPotters: StateFlow<List<PlayerCard>> = snapshotFlow { cards.values.toList() }
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionPotionTotal > 0 }.sortedByDescending { it.sessionPotionTotal }
        .take(100)
    }
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  var topItemSkillCasters: StateFlow<List<PlayerCard>> = snapshotFlow { cards.values.toList() }
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionItemSkillTotal > 0 }.sortedByDescending { it.sessionItemSkillTotal }
        .take(100)
    }
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  val topKills: StateFlow<List<PlayerCard>> = snapshotFlow { cards.values.toList() }
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionKillTotal > 0 }.sortedByDescending { it.sessionKillTotal }
    }
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  val topKillsKB: StateFlow<List<PlayerCard>> = snapshotFlow { cards.values.toList() }
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionKillTotalKB > 0 }
        .sortedByDescending { it.sessionKillTotalKB }
        .take(100)
    }
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  val topHealsOde: StateFlow<List<PlayerCard>> = snapshotFlow { cards.values.toList() }
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionOdeHealsTotal > 0 }
        .sortedByDescending { it.sessionOdeHealsTotal }
        .take(100)
    }
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  val topKillsLifetime: StateFlow<List<PlayerCard>> = snapshotFlow { cards.values.toList() }
    .map { cards ->
      cards.filter { it.isRealPlayer && (it.cache?.lifetimeTotalKills ?: 0L) > 0L }
        .sortedByDescending { it.cache?.lifetimeTotalKills ?: 0L }
        .take(100)
    }
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  val topDeaths: StateFlow<List<PlayerCard>> = snapshotFlow { cards.values.toList() }
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionDeathTotal > 0 }.sortedByDescending { it.sessionDeathTotal }
    }
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  val topDamageTaken: StateFlow<List<PlayerCard>> = snapshotFlow { cards.values.toList() }
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionDamageTakenTotal > 0 }
        .sortedByDescending { it.sessionDamageTakenTotal }
        .take(100)
    }
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  val topHealsReceived: StateFlow<List<PlayerCard>> = snapshotFlow { cards.values.toList() }
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionHealsReceivedTotal > 0 }
        .sortedByDescending { it.sessionHealsReceivedTotal }
        .take(100)
    }
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  val nearbyNuianRaidParties: StateFlow<List<PlayerCard>> = snapshotFlow {
    val cardList = cards.values.toList()
    val combinedParties = (raids[0] ?: emptyList()) + (raids[1] ?: emptyList())
    Pair(cardList, combinedParties)
  }.map { (allCards, combinedParties) ->
    val raidNames = combinedParties.flatten().map { it.playerName }.toSet()
    val candidates = allCards
      .filter { it.isRealPlayer && it.lastKnownFaction == Faction.NUIA.value && it.name !in raidNames }
      .sortedWith(gearComparator)

    RaidOrganizer.organize(candidates, ArrangementMode.CLASSIC_ROLES).take(400)
  }
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  val nearbyHaraniRaidParties: StateFlow<List<PlayerCard>> = snapshotFlow {
    val cardList = cards.values.toList()
    val combinedParties = (raids[0] ?: emptyList()) + (raids[1] ?: emptyList())
    Pair(cardList, combinedParties)
  }
    .map { (allCards, combinedParties) ->
      val raidNames = combinedParties.flatten().map { it.playerName }.toSet()
      val candidates = allCards
        .filter { it.isRealPlayer && it.lastKnownFaction == Faction.HARANYA.value && it.name !in raidNames }
        .sortedWith(gearComparator)

      RaidOrganizer.organize(candidates, ArrangementMode.CLASSIC_ROLES).take(400)
    }
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  val nearbyPirateRaidParties: StateFlow<List<PlayerCard>> = snapshotFlow {
    val cardList = cards.values.toList()
    val combinedParties = (raids[0] ?: emptyList()) + (raids[1] ?: emptyList())
    Pair(cardList, combinedParties)
  }
    .map { (allCards, combinedParties) ->
      val raidNames = combinedParties.flatten().map { it.playerName }.toSet()
      val candidates = allCards
        .filter { it.isRealPlayer && it.lastKnownFaction == Faction.PIRATE.value && it.name !in raidNames }
        .sortedWith(gearComparator)

      RaidOrganizer.organize(candidates, ArrangementMode.CLASSIC_ROLES).take(400)
    }
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  /////////////////////////
  // Faction Comparisons //
  /////////////////////////

  val topDamageSpellsHaranya: StateFlow<List<SpellDamage>> = snapshotFlow { cards.values.toList() }
    .map { cardList -> aggregateDamageBySpellForFaction(cardList, Faction.HARANYA) }
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  val topDamageSpellsNuia: StateFlow<List<SpellDamage>> = snapshotFlow { cards.values.toList() }
    .map { cardList -> aggregateDamageBySpellForFaction(cardList, Faction.NUIA) }
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  val topDamageSpellsPirate: StateFlow<List<SpellDamage>> = snapshotFlow { cards.values.toList() }
    .map { cardList -> aggregateDamageBySpellForFaction(cardList, Faction.PIRATE) }
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  val topItemUsesHaranya: StateFlow<List<ItemUsage>> = snapshotFlow { cards.values.toList() }
    .map { cardList -> aggregateItemUsesByFaction(cardList, Faction.HARANYA) }
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  val topItemUsesNuia: StateFlow<List<ItemUsage>> = snapshotFlow { cards.values.toList() }
    .map { cardList -> aggregateItemUsesByFaction(cardList, Faction.NUIA) }
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  val topItemUsesPirate: StateFlow<List<ItemUsage>> = snapshotFlow { cards.values.toList() }
    .map { cardList -> aggregateItemUsesByFaction(cardList, Faction.PIRATE) }
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  // top kills haranya, nuia, and pirate
  val topKillsHaranya: StateFlow<List<PlayerCard>> = snapshotFlow { cards.values.toList() }
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionKillTotal > 0 && it.lastKnownFaction == Faction.HARANYA.value }
        .sortedByDescending { it.sessionKillTotal }
    }
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())
  val topKillsNuia: StateFlow<List<PlayerCard>> = snapshotFlow { cards.values.toList() }
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionKillTotal > 0 && it.lastKnownFaction == Faction.NUIA.value }
        .sortedByDescending { it.sessionKillTotal }
    }
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())
  val topKillsPirate: StateFlow<List<PlayerCard>> = snapshotFlow { cards.values.toList() }
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionKillTotal > 0 && it.lastKnownFaction == Faction.PIRATE.value }.sortedByDescending { it.sessionKillTotal }
    }
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  val topOdeHaranya: StateFlow<List<PlayerCard>> = snapshotFlow { cards.values.toList() }
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionOdeHealsTotal > 0 && it.lastKnownFaction == Faction.HARANYA.value }
        .sortedByDescending { it.sessionOdeHealsTotal }
    }
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())
  val topOdeNuia: StateFlow<List<PlayerCard>> = snapshotFlow { cards.values.toList() }
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionOdeHealsTotal > 0 && it.lastKnownFaction == Faction.NUIA.value }
        .sortedByDescending { it.sessionOdeHealsTotal }
    }
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())
  val topOdePirate: StateFlow<List<PlayerCard>> = snapshotFlow { cards.values.toList() }
    .map { cards ->
      cards.filter { it.isRealPlayer && it.sessionOdeHealsTotal > 0 && it.lastKnownFaction == Faction.PIRATE.value }
        .sortedByDescending { it.sessionOdeHealsTotal }
    }
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  /**
   * Compares average charm totals between raid members and opposition.
   * Returns a map with "Our Raid" and "Opposition" as keys.
   */

  // three-way compare
  val factionCharmComparisonAll: StateFlow<Map<String, Float>> = snapshotFlow { cards.values.toList() }
    .map {
      val totals = aggregateSessionLongByFaction({ it.sessionCharmTotal })
      mapOf(
        Faction.HARANYA.value to (totals[Faction.HARANYA] ?: 0f),
        Faction.NUIA.value to (totals[Faction.NUIA] ?: 0f),
        Faction.PIRATE.value to (totals[Faction.PIRATE] ?: 0f)
      )
    }
    .stateIn(scope, SharingStarted.WhileSubscribed(20000), emptyMap())

  val factionSilenceComparisonAll: StateFlow<Map<String, Float>> = snapshotFlow { cards.values.toList() }
    .map {
      val totals = aggregateSessionLongByFaction({ it.sessionSilenceTotal })
      mapOf(
        Faction.HARANYA.value to (totals[Faction.HARANYA] ?: 0f),
        Faction.NUIA.value to (totals[Faction.NUIA] ?: 0f),
        Faction.PIRATE.value to (totals[Faction.PIRATE] ?: 0f)
      )
    }
    .stateIn(scope, SharingStarted.WhileSubscribed(20000), emptyMap())

  val factionDistressComparisonAll: StateFlow<Map<String, Float>> = snapshotFlow { cards.values.toList() }
    .map {
      val totals = aggregateSessionLongByFaction({ it.sessionDistressTotal })
      mapOf(
        Faction.HARANYA.value to (totals[Faction.HARANYA] ?: 0f),
        Faction.NUIA.value to (totals[Faction.NUIA] ?: 0f),
        Faction.PIRATE.value to (totals[Faction.PIRATE] ?: 0f)
      )
    }
    .stateIn(scope, SharingStarted.WhileSubscribed(20000), emptyMap())

  val buildCountsHaranya: StateFlow<Map<String, Int>> = snapshotFlow { cards.values.toList() }
    .map {
      cards.values
        .filter { it.isRealPlayer && Faction.fromString(it.lastKnownFaction) == Faction.HARANYA && it.hasPvPParticipation() }
        .groupingBy { it.currentBuild }
        .eachCount()
    }
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyMap())

  val buildCountsNuia: StateFlow<Map<String, Int>> = snapshotFlow { cards.values.toList() }
    .map {
      cards.values
        .filter { it.isRealPlayer && Faction.fromString(it.lastKnownFaction) == Faction.NUIA && it.hasPvPParticipation() }
        .groupingBy { it.currentBuild }
        .eachCount()
    }
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyMap())

  val buildCountsPirate: StateFlow<Map<String, Int>> = snapshotFlow { cards.values.toList() }
    .map {
      cards.values
        .filter { it.isRealPlayer && Faction.fromString(it.lastKnownFaction) == Faction.PIRATE && it.hasPvPParticipation() }
        .groupingBy { it.currentBuild }
        .eachCount()
    }
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyMap())


  // two-way compare (maybe deprecated later)
  val factionCharmComparison: StateFlow<Map<String, Float>> = snapshotFlow { cards.values.toList() }
    .map {
      val totals = aggregateSessionLongByFaction({ it.sessionCharmTotal })
      val playerFaction = Faction.fromString(RFConfig.state.value.playerFaction)
      val our = totals[playerFaction] ?: 0f
      val opposition = totals.filterKeys { it != playerFaction }.values.sum()
      mapOf("Our Faction" to our, "Opposition" to opposition)
    }
    .stateIn(scope, SharingStarted.WhileSubscribed(20000), emptyMap())

  val factionSilenceComparison: StateFlow<Map<String, Float>> = snapshotFlow { cards.values.toList() }
    .map {
      val totals = aggregateSessionLongByFaction({ it.sessionSilenceTotal })
      val playerFaction = Faction.fromString(RFConfig.state.value.playerFaction)
      val our = totals[playerFaction] ?: 0f
      val opposition = totals.filterKeys { it != playerFaction }.values.sum()
      mapOf("Our Faction" to our, "Opposition" to opposition)
    }
    .stateIn(scope, SharingStarted.WhileSubscribed(20000), emptyMap())

  val factionDistressComparison: StateFlow<Map<String, Float>> = snapshotFlow { cards.values.toList() }
    .map {
      val totals = aggregateSessionLongByFaction({ it.sessionDistressTotal })
      val playerFaction = Faction.fromString(RFConfig.state.value.playerFaction)
      val our = totals[playerFaction] ?: 0f
      val opposition = totals.filterKeys { it != playerFaction }.values.sum()
      mapOf("Our Faction" to our, "Opposition" to opposition)
    }
    .stateIn(scope, SharingStarted.WhileSubscribed(20000), emptyMap())

  var activePets: StateFlow<List<PetCard>> = snapshotFlow { petCards.values.toList() }
    .sample(250L) // Only take the latest emitted value every 250ms to limit UI recomposition
    .map { pets ->
      pets.filter { it.sessionDamageTotal > 0 || it.sessionDebuffTotal > 0 }
        .sortedByDescending { it.sessionDamageTotal }
        .take(50)
    }
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  /* Raid Parties UI Subscriptions */
  fun getRaidById(raidId: Int): StateFlow<List<Party>> {
    return snapshotFlow { raids[raidId] ?: listOf() }
      .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())
  }

}
