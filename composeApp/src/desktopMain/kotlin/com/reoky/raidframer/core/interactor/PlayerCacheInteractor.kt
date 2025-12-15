package com.reoky.raidframer.core.interactor

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshotFlow
import com.reoky.raidframer.core.database.PlayerCacheEntity
import com.reoky.raidframer.core.database.RFDao
import com.reoky.raidframer.core.definitions.SkillTreeType
import com.reoky.raidframer.core.definitions.SpecType
import com.reoky.raidframer.core.definitions.buildSkillTreeLastUsedMap
import com.reoky.raidframer.core.definitions.findSkillTreeForSpell
import com.reoky.raidframer.core.model.BuffEndedEvent
import com.reoky.raidframer.core.model.BuffGainedEvent
import com.reoky.raidframer.core.model.CastingEvent
import com.reoky.raidframer.core.model.CombatEvent
import com.reoky.raidframer.core.model.DamageEvent
import com.reoky.raidframer.core.model.DebuffEndedEvent
import com.reoky.raidframer.core.model.DebuffGainedEvent
import com.reoky.raidframer.core.model.HealEvent
import com.reoky.raidframer.core.model.PlayerCard
import com.reoky.raidframer.core.model.SuccessfulCastEvent
import com.reoky.raidframer.core.model.postBuffEndedEvent
import com.reoky.raidframer.core.model.postBuffGainedEvent
import com.reoky.raidframer.core.model.postCastingEvent
import com.reoky.raidframer.core.model.postDamageEvent
import com.reoky.raidframer.core.model.postDebuffEndedEvent
import com.reoky.raidframer.core.model.postDebuffGainedEvent
import com.reoky.raidframer.core.model.postHealEvent
import com.reoky.raidframer.core.model.postSuccessfulCastEvent
import com.reoky.raidframer.core.model.shouldUpgradeToPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking

/**
 * Keeps a cache of players and NPCs seen in the log. When a player is first detected their player card is loaded
 * from the database (if it exists) or created new (if it doesn't). Cards are held in memory, and continually written
 * back to the database for persistence. This allows the app to remember that a player is real vs an NPC across sessions,
 * instead of having to re-discover players every time the app is launched. (Which reduces accuracy of PvP vs PvE damage stats.)
 */
object PlayerCacheInteractor : Interactor() {

  val TAG = "PlayerCacheInteractor"

  // Mapping of all the players (and NPCs) sorted in no particular order
  private val _cards = mutableStateMapOf<String, PlayerCard>()
  private val _scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

  // main event loop
  override suspend fun interact() {
    val savedCount = RFDao.playerCacheDao.getPlayerCount()
    val cachedCount = _cards.values.count()
    Log.info(TAG, "Persisted $savedCount players. ($cachedCount total entities (mounts,players,pets,mobs,etc) cached in memory)")

    _cards.forEach { (name, card) ->

      // logic to determine if player should be upgraded from NPC to real player
      if (!card.isRealPlayer && card.shouldUpgradeToPlayer()) {
        //Log.info(TAG, "Upgrading ${card.name} from NPC to Real Player based on activity.")
        val upgradedCard = card.copy(
          isRealPlayer = true,
          cache = PlayerCacheEntity(
            playerName = card.name,
            lastSeen = card.lastEvent,
            lastKnownSpec = card.currentBuild
          )
        )
        _cards[name] = upgradedCard
      }

      if (!card.isRealPlayer) return@forEach // only real players have specs

      // identify SpecType based on recent casts
      val skillTreeLastUsed: MutableMap<SkillTreeType, Long> = buildSkillTreeLastUsedMap() // timestamp 0 for all trees
      card.recentCastEvents.take(128).forEach { castEvent ->
        findSkillTreeForSpell(castEvent.spell)?.let { tree ->
          skillTreeLastUsed[tree] = castEvent.timestamp
        }
      }

      // determine three most recently used skill trees
      val threeMostRecentTrees = skillTreeLastUsed.entries
        .asSequence()
        .filter { it.value > 0 }
        .sortedByDescending { it.value } // most recent first
        .take(3) // take top three
        .map { it.key }
        .toSet()

      if (threeMostRecentTrees.count() < 3) {
        return@forEach // not enough data yet, gib unknown
      }

      var determinedSpec = SpecType.fromTrees(threeMostRecentTrees)

      // now update all the cards with the determined spec (some are gonna be unknown! but that's ok friends!)
      val updatedCard = card.copy(
        currentBuild = determinedSpec.name,
        cache = PlayerCacheEntity(
          playerName = card.name,
          lastSeen = card.lastEvent,
          lastKnownSpec = determinedSpec.name
        )
      )
      _cards[name] = updatedCard
      if (determinedSpec != SpecType.UNKNOWN) {
        //Log.info(TAG, "Determined ${card.name} is playing as ${determinedSpec.name}.")
      }

      // store all cached cards back to the database
      _cards[name]?.cache?.let {
        RFDao.playerCacheDao.insert(it)
      }
    }
  }

  /*
   * Create card for a player if none exists... Upgrade from NPC to Player occurs inside the PlayerCardExtensions helpers.
   */
  fun createCardIfNoneExists(playerName: String) {
    if (!_cards.containsKey(playerName)) {
      val cached = runBlocking {
        RFDao.playerCacheDao.getPlayerCacheFor(playerName) // only called if not in memory already
      }
      val card = PlayerCard(
        name = playerName,
        lastEvent = System.currentTimeMillis(),
        isRealPlayer = cached != null,
        cache = cached,
        currentBuild = cached?.lastKnownSpec ?: SpecType.UNKNOWN.name
      )
      _cards[playerName] = card
    }
  }

  /*
   * Reset all session totals and recent events for all players.
   */
  fun resetAllSessions() {
    _cards.forEach { (name, card) ->
      val resetCard = card.copy(
        recentCastSuccessfulCastEvent = listOf(),
        recentCastEvents = listOf(),
        recentDamageEvents = listOf(),
        recentHealEvents = listOf(),
        recentBuffGainedEvents = listOf(),
        recentBuffEndedEvent = listOf(),
        recentDebuffGainedEvent = listOf(),
        recentDebuffEndedEvent = listOf(),
        sessionDamageTotal = 0L,
        sessionHealTotal = 0L,
        sessionCCTotal = 0,
        sessionDebuffTotal = 0
      )
      _cards[name] = resetCard
    }
    _cards.clear()
  }

  // filter pve damage by checking if the target is a real player
  fun isRealPlayer(playerName: String): Boolean {
    return _cards.values.any { it.isRealPlayer && it.name == playerName }
  }

  /* Card Management */

  fun addOrUpdateCard(card: PlayerCard) {
    _cards[card.name] = card
  }

  fun getCard(name: String): PlayerCard? {
    return _cards[name]
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
    createCardIfNoneExists(event.caster)
    _cards[event.caster]?.let { card ->
      _cards[event.caster] = card.postDamageEvent(event)
    }
  }

  private fun postHeal(event: HealEvent) {
    createCardIfNoneExists(event.caster)
    _cards[event.caster]?.let { card ->
      _cards[event.caster] = card.postHealEvent(event)
    }
  }

  private fun postCasting(event: CastingEvent) {
    createCardIfNoneExists(event.caster)
    _cards[event.caster]?.let { card ->
      _cards[event.caster] = card.postCastingEvent(event)
    }
  }

  private fun postSuccessfulCast(event: SuccessfulCastEvent) {
    createCardIfNoneExists(event.caster)
    _cards[event.caster]?.let { card ->
      _cards[event.caster] = card.postSuccessfulCastEvent(event)
    }
  }

  private fun postBuffGained(event: BuffGainedEvent) {
    createCardIfNoneExists(event.target)
    _cards[event.target]?.let { card ->
      _cards[event.target] = card.postBuffGainedEvent(event)
    }
  }

  private fun postBuffEnded(event: BuffEndedEvent) {
    createCardIfNoneExists(event.target)
    _cards[event.target]?.let { card ->
      _cards[event.target] = card.postBuffEndedEvent(event)
    }
  }

  private fun postDebuffGained(event: DebuffGainedEvent) {
    createCardIfNoneExists(event.target)
    _cards[event.target]?.let { card ->
      _cards[event.target] = card.postDebuffGainedEvent(event)
    }
  }

  private fun postDebuffEnded(event: DebuffEndedEvent) {
    createCardIfNoneExists(event.target)
    _cards[event.target]?.let { card ->
      _cards[event.target] = card.postDebuffEndedEvent(event)
    }
  }

  /* UI Subscriptions */
  var topDamage: StateFlow<List<PlayerCard>> = snapshotFlow { _cards.values.toList() }
    .map { cards -> cards.filter { it.isRealPlayer }.sortedByDescending { it.sessionDamageTotal }.take(100) }
    .stateIn(_scope, SharingStarted.WhileSubscribed(5000), emptyList())

  var topHeals: StateFlow<List<PlayerCard>> = snapshotFlow { _cards.values.toList() }
    .map { cards -> cards.filter { it.isRealPlayer }.sortedByDescending { it.sessionHealTotal }.take(100) }
    .stateIn(_scope, SharingStarted.WhileSubscribed(5000), emptyList())

  var topCC: StateFlow<List<PlayerCard>> = snapshotFlow { _cards.values.toList() }
    .map { cards -> cards.filter { it.isRealPlayer }.sortedByDescending { it.sessionCCTotal }.take(100) }
    .stateIn(_scope, SharingStarted.WhileSubscribed(5000), emptyList())

}
