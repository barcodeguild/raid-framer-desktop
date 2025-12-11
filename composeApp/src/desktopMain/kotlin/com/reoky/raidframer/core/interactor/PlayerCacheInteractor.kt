package com.reoky.raidframer.core.interactor

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshotFlow
import com.reoky.raidframer.core.database.RFDao
import com.reoky.raidframer.core.definitions.SpecType
import com.reoky.raidframer.core.model.DamageEvent
import com.reoky.raidframer.core.model.HealEvent
import com.reoky.raidframer.core.model.PlayerCard
import com.reoky.raidframer.core.model.postDamageEvent
import com.reoky.raidframer.core.model.postHealEvent
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
    Log.info(TAG, "Currently saved $savedCount player cards. Cached count in memory is: $cachedCount")

    // Post an event to the card
    val damageEvent = DamageEvent(
      timestamp = System.currentTimeMillis(),
      caster = "Reoky",
      target = "Fren",
      damage = 16445,
      spell = "Flamebolt",
      critical = false
    )
    postDamage(damageEvent)
    _cards.forEach { string, card ->
      Log.info(TAG, "PlayerCard for $string: (total dmg: ${card.sessionDamageTotal}, total heal: ${card.sessionHealTotal}, isRealPlayer: ${card.isRealPlayer}), recentDamageEvents: ${card.recentDamageEvents.size}, recentHealEvents: ${card.recentHealEvents.size}")
    }

    // logic to determine if player should be upgraded from NPC to real player
    _cards.forEach { name, card ->
      if (!card.isRealPlayer && card.shouldUpgradeToPlayer()) {
        Log.info(TAG, "Upgrading ${card.name} from NPC to Real Player based on activity.")
        val upgradedCard = card.copy(isRealPlayer = true)
        _cards[name] = upgradedCard
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
        currentBuild = SpecType.UNKNOWN.name
      )
      _cards[playerName] = card
    }
  }

  /* Card Management */

  fun addOrUpdateCard(card: PlayerCard) {
    _cards[card.name] = card
  }

  fun getCard(name: String): PlayerCard? {
    return _cards[name]
  }

  private fun shouldUpgradeToPlayer(card: PlayerCard): Boolean {
    // Simple heuristic: if the player has done any healing or crowd control, consider them a real player
    val hasHealed = card.sessionHealTotal > 0
    val hasCC = card.sessionCCTotal > 0
    return hasHealed || hasCC
  }

  /* Event Posting */

  fun postDamage(event: DamageEvent) {
    createCardIfNoneExists(event.caster)
    _cards[event.caster]?.let { card ->
      val updatedCard = card.postDamageEvent(event)
      // Re-assign the updated card back into the map
      _cards.remove(event.caster)
      _cards[event.caster] = updatedCard
    }
  }

  fun postHeal(event: HealEvent) {
    createCardIfNoneExists(event.caster)
    _cards[event.caster]?.let { card ->
      val updatedCard = card.postHealEvent(event)
      // Re-assign the updated card back into the map
      _cards.remove(event.caster)
      _cards[event.caster] = updatedCard
    }
  }

  /* UI Subscriptions */
  val topDamage: StateFlow<List<PlayerCard>> = snapshotFlow { _cards.values.toList() }
    .map { cards -> cards.sortedByDescending { it.sessionDamageTotal }.take(100) }
    .stateIn(_scope, SharingStarted.WhileSubscribed(5000), emptyList())

  val topHeals: StateFlow<List<PlayerCard>> = snapshotFlow { _cards.values.toList() }
    .map { cards -> cards.sortedByDescending { it.sessionHealTotal }.take(100) }
    .stateIn(_scope, SharingStarted.WhileSubscribed(5000), emptyList())

  val topCC: StateFlow<List<PlayerCard>> = snapshotFlow { _cards.values.toList() }
    .map { cards -> cards.sortedByDescending { it.sessionCCTotal }.take(100) }
    .stateIn(_scope, SharingStarted.WhileSubscribed(5000), emptyList())

}
