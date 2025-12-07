package com.reoky.raidframer.core.interactor

import com.reoky.raidframer.core.database.RFDao
import com.reoky.raidframer.core.model.NPCCard
import com.reoky.raidframer.core.model.PlayerCard

/**
 * Keeps a cache of players and NPCs seen in the log. When a player is first detected their player card is loaded
 * from the database (if it exists) or created new (if it doesn't). Cards are held in memory, and continually written
 * back to the database for persistence. This allows the app to remember that a player is real vs an NPC across sessions,
 * instead of having to re-discover players every time the app is launched. (Which reduces accuracy of PvP vs PvE damage stats.)
 */
object PlayerCacheInteractor : Interactor() {

  // Mapping of all the players (and NPCs) sorted in no particular order
  val players: HashMap<PlayerCard, Long> = hashMapOf()
  val npcs: HashMap<NPCCard, Long> = hashMapOf()

  val TAG = "PlayerCacheInteractor"

  // main event loop
  override suspend fun interact() {

    // Persist all cached players to the database
    //    for ((player, _) in players) {
    //      playerCacheDao.insertOrUpdatePlayerCard(player)
    //    }
    //
    //    // Persist all cached NPCs to the database
    //    for ((npc, _) in npcs) {
    //      playerCacheDao.insertOrUpdateNPCCard(npc)
    //    }
    val playerCount = RFDao.playerCacheDao.getPlayerCount()
    Log.info(TAG, "Currently cached $playerCount player cards. Learning battlefield dynamics.")
  }
}
