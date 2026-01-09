package com.reoky.raidframer.core.interactor

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshotFlow
import com.reoky.raidframer.core.calc.MetricRawSample
import com.reoky.raidframer.core.calc.RealtimeComputer
import com.reoky.raidframer.core.database.PlayerCacheEntity
import com.reoky.raidframer.core.database.RFDao
import com.reoky.raidframer.core.definitions.SkillTreeType
import com.reoky.raidframer.core.definitions.SpecType
import com.reoky.raidframer.core.definitions.findSkillTreeForSpell
import com.reoky.raidframer.core.model.*
import com.reoky.raidframer.core.serialization.Party
import com.reoky.raidframer.core.serialization.RaidFramePayload
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Keeps a cache of players and NPCs seen in the log. When a player is first detected their player card is loaded
 * from the database (if it exists) or created new (if it doesn't). Cards are held in memory, and continually written
 * back to the database for persistence. This allows the app to remember that a player is real vs an NPC across sessions,
 * instead of having to re-discover players every time the app is launched. (Which reduces accuracy of PvP vs PvE damage stats.)
 */
object PlayerCacheInteractor : Interactor() {

  const val TAG = "PlayerCacheInteractor"

  // Mapping of all the players (and NPCs) sorted in no particular order
  val realtimeComputer = RealtimeComputer(windowBuckets = 60, bucketMillis = 10_000L)
  private val raids = mutableStateMapOf<Int, List<Party>>()
  private val cards = mutableStateMapOf<String, PlayerCard>()
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
                  cache = PlayerCacheEntity(
                    playerName = current.name,
                    lastSeen = current.lastEvent,
                    lastKnownSpec = current.currentBuild
                  )
                )
                cards[name] = upgradedCard
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
              cache = PlayerCacheEntity(
                playerName = currentCard.name,
                lastSeen = currentCard.lastEvent,
                lastKnownSpec = determinedSpec.name
              )
            )
            cards[name] = updatedCard

            // Persist to DB
            updatedCard.cache?.let {
              RFDao.playerCacheDao.insert(it)
            }
          }
        }
      }
    }
  }

  /*
   * Builds parties of five from the ordered list of raid members and stores them under the raid ID.
   */
  fun updatePlayersForRaidById(raidId: Int, members: List<RaidFramePayload>) {
    scope.launch {
      mutex.withLock {
        raids[raidId] = members.chunked(5).take(20)
        members.forEach { member ->
          createCardIfNoneExists(member.playerName)
        }
      }
    }
  }

  /*
   * Create card for a player if none exists... Upgrade from NPC to Player occurs inside the PlayerCardExtensions helpers.
   * NOTE: This method must be called within a mutex.withLock block as it is not thread-safe on its own.
   */
  private fun createCardIfNoneExists(playerName: String) {
    if (!cards.containsKey(playerName)) {
      val cached = runBlocking {
        RFDao.playerCacheDao.getPlayerCacheFor(playerName)
      }
      val card = PlayerCard(
        name = playerName,
        lastEvent = System.currentTimeMillis(),
        isRealPlayer = cached != null,
        cache = cached,
        currentBuild = cached?.lastKnownSpec ?: SpecType.UNKNOWN.name
      )
      cards[playerName] = card
    }
  }

  /*
   * Reset all session totals and recent events for all players.
   */
  fun resetAllSessions() {
    scope.launch {
      mutex.withLock {
        // Iterate on keys to modify values
        cards.keys.toList().forEach { name ->
          cards[name]?.let { card ->
            cards[name] = card.copy(
              recentCastSuccessfulCastEvents = listOf(),
              recentCastEvents = listOf(),
              recentDamageEvents = listOf(),
              recentHealEvents = listOf(),
              recentBuffGainedEvents = listOf(),
              recentBuffEndedEvents = listOf(),
              recentDebuffGainedEvents = listOf(),
              recentDebuffEndedEvents = listOf(),
              sessionDamageTotal = 0L,
              sessionHealTotal = 0L,
              sessionCCTotal = 0,
              sessionDebuffTotal = 0
            )
          }
        }
        cards.clear()
      }
    }
  }

  // filter pve damage by checking if the target is a real player
  fun isRealPlayer(playerName: String): Boolean {
    return cards.values.any { it.isRealPlayer && it.name == playerName }
  }

  /* Card Management */
  fun addOrUpdateCard(card: PlayerCard) {
    scope.launch {
      mutex.withLock {
        cards[card.name] = card
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

  /*
   * Helps upgrade an NPC card to a real player card immediately based on metadata from the game proving it's a player.
   */
  fun stronglyAssertIsPlayer(name: String, classMap: Map<String, Int>) {
    val spec = SpecType.fromTrees(classMap.values.mapNotNull { gameId -> SkillTreeType.fromGameId(gameId) }.toSet())
    Log.debug(TAG, "Strongly asserting $name is a real player with spec $spec based on raid metadata.")
    scope.launch {
      mutex.withLock {
        createCardIfNoneExists(name)
        cards[name]?.let { card ->
          cards[name] = card.copy(
            isRealPlayer = true,
            currentBuild = if (spec != SpecType.UNKNOWN) spec.name else card.currentBuild,
            cache = PlayerCacheEntity(
              playerName = card.name,
              lastSeen = card.lastEvent,
              lastKnownSpec = card.currentBuild
            )
          )
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

  private fun postDamage(event: DamageEvent) {
    scope.launch {
      mutex.withLock {
        createCardIfNoneExists(event.caster)
        //realtimeComputer.push(MetricRawSample(event.timestamp, event.damage.toDouble()))
        cards[event.caster]?.let { card ->
          cards[event.caster] = card.postDamageEvent(event)
        }
      }
    }
  }

  private fun postHeal(event: HealEvent) {
    scope.launch {
      mutex.withLock {
        createCardIfNoneExists(event.caster)
        cards[event.caster]?.let { card ->
          cards[event.caster] = card.postHealEvent(event)
        }
      }
    }
  }

  private fun postCasting(event: CastingEvent) {
    scope.launch {
      mutex.withLock {
        createCardIfNoneExists(event.caster)
        cards[event.caster]?.let { card ->
          cards[event.caster] = card.postCastingEvent(event)
        }
      }
    }
  }

  private fun postSuccessfulCast(event: SuccessfulCastEvent) {
    scope.launch {
      mutex.withLock {
        createCardIfNoneExists(event.caster)
        cards[event.caster]?.let { card ->
          cards[event.caster] = card.postSuccessfulCastEvent(event)
        }
      }
    }
  }

  private fun postBuffGained(event: BuffGainedEvent) {
    scope.launch {
      mutex.withLock {
        createCardIfNoneExists(event.target)
        cards[event.target]?.let { card ->
          cards[event.target] = card.postBuffGainedEvent(event)
        }
      }
    }
  }

  private fun postBuffEnded(event: BuffEndedEvent) {
    scope.launch {
      mutex.withLock {
        createCardIfNoneExists(event.target)
        cards[event.target]?.let { card ->
          cards[event.target] = card.postBuffEndedEvent(event)
        }
      }
    }
  }

  private fun postDebuffGained(event: DebuffGainedEvent) {
    scope.launch {
      mutex.withLock {
        createCardIfNoneExists(event.target)
        cards[event.target]?.let { card ->
          cards[event.target] = card.postDebuffGainedEvent(event)
        }

        // give credit to the source
        event.source?.let { source ->
          createCardIfNoneExists(source)
          cards[source]?.let { card ->
            cards[source] = card.postDebuffAppliedEvent(DebuffAppliedEvent(
              timestamp = event.timestamp,
              source = event.source,
              target = event.target,
              debuff = event.debuff
            ))
          }
        }
      }
    }
  }

  private fun postDebuffEnded(event: DebuffEndedEvent) {
    scope.launch {
      mutex.withLock {
        createCardIfNoneExists(event.target)
        cards[event.target]?.let { card ->
          cards[event.target] = card.postDebuffEndedEvent(event)
        }
      }
    }
  }

  fun postPlayerDeath(playerName: String, timestamp: Long) {
    scope.launch {
      mutex.withLock {
        createCardIfNoneExists(playerName)
        cards[playerName]?.let { card ->
          cards[playerName] = card.postDeathEvent(timestamp)
        }
      }
    }
  }

  /* UI Subscriptions */
  var topDamage: StateFlow<List<PlayerCard>> = snapshotFlow { cards.values.toList() }
    .map { cards -> cards.filter { it.isRealPlayer && it.sessionDamageTotal > 0 }.sortedByDescending { it.sessionDamageTotal }.take(100) }
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  var topHeals: StateFlow<List<PlayerCard>> = snapshotFlow { cards.values.toList() }
    .map { cards -> cards.filter { it.isRealPlayer && it.sessionHealTotal > 0 }.sortedByDescending { it.sessionHealTotal }.take(100) }
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  var topCC: StateFlow<List<PlayerCard>> = snapshotFlow { cards.values.toList() }
    .map { cards -> cards.filter { it.isRealPlayer && it.sessionCCTotal > 0 }.sortedByDescending { it.sessionCCTotal }.take(100) }
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  var topDebuff: StateFlow<List<PlayerCard>> = snapshotFlow { cards.values.toList() }
    .map { cards -> cards.filter { it.isRealPlayer && it.sessionDebuffTotal > 0 }.sortedByDescending { it.sessionDebuffTotal }.take(100) }
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  var topCharmers: StateFlow<List<PlayerCard>> = snapshotFlow { cards.values.toList()  }
    .map { cards -> cards.filter { it.isRealPlayer && it.sessionCharmTotal > 0 }.sortedByDescending { it.sessionCharmTotal }.take(100) }
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  var topGliderGamers: StateFlow<List<PlayerCard>> = snapshotFlow { cards.values.toList()  }
    .map { cards -> cards.filter { it.isRealPlayer && it.sessionGliderTotal > 0 }.sortedByDescending { it.sessionGliderTotal }.take(100) }
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  var topPotters: StateFlow<List<PlayerCard>> = snapshotFlow { cards.values.toList()  }
    .map { cards -> cards.filter { it.isRealPlayer && it.sessionPotionTotal > 0 }.sortedByDescending { it.sessionPotionTotal }.take(100) }
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  var topItemSkillCasters : StateFlow<List<PlayerCard>> = snapshotFlow { cards.values.toList()  }
    .map { cards -> cards.filter { it.isRealPlayer && it.sessionItemSkillTotal > 0 }.sortedByDescending { it.sessionItemSkillTotal }.take(100) }
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  val topDeaths: StateFlow<List<PlayerCard>> = snapshotFlow { cards.values.toList()  }
    .map { cards -> cards.filter { it.isRealPlayer && it.sessionDeathTotal > 0 }.sortedByDescending { it.sessionDeathTotal }.take(100) }
    .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  /* Raid Parties UI Subscriptions */
  fun getRaidById(raidId: Int): StateFlow<List<Party>> {
    return snapshotFlow { raids[raidId] ?: listOf() }
      .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())
  }

}