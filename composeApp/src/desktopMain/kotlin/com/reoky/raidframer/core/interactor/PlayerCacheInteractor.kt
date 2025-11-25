package com.reoky.raidframer.core.interactor

import com.reoky.raidframer.core.model.PlayerCard

/**
 * Keeps a cache of players and NPCs seen in the log. When a player is first detected their player card is loaded
 * from the database (if it exists) or created new (if it doesn't). Cards are held in memory, and continually written
 * back to the database for persistence. This allows the app to remember that a player is real vs an NPC across sessions,
 * instead of having to re-discover players every time the app is launched. (Which reduces accuracy of PvP vs PvE damage stats.)
 */
class PlayerCacheInteractor : Interactor() {

  // Mapping of all the players (and NPCs) sorted in no particular order
  val players: HashMap<PlayerCard, Long> = hashMapOf()
  //val npcs: HashMap<PlayerCard, Long> = hashMapOf()

  // main event loop
  override suspend fun interact() {

  }
}
