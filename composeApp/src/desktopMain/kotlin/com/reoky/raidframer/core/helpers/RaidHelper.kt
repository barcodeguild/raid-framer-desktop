package com.reoky.raidframer.core.helpers

import com.reoky.raidframer.core.definitions.MetaSpecsRepo
import com.reoky.raidframer.core.definitions.SpecType
import com.reoky.raidframer.core.model.PlayerRole

/*
 * Guess the player's role based on their recent actions / spec / past roles. Really whatever we can glean.
 * This is going to be kind of rough and ready at first.
 */
fun SpecType.guessPlayerRole(): PlayerRole {
  val meta = MetaSpecsRepo.current
  if (this in meta.dancer) return PlayerRole.PURPLE
  if (this in meta.cc) return PlayerRole.GREEN // always green for cc specs / tanks
  if (this in meta.melee) return PlayerRole.GREEN // melee dps are green if their gear is above 17k
  if (this in meta.healer) return PlayerRole.PINK
  if (this in meta.mage) return PlayerRole.RED
  if (this in meta.ranged) return PlayerRole.BLUE
  return PlayerRole.BLUE
}
