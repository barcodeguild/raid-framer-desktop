package com.reoky.raidframer.core.helpers

import com.reoky.raidframer.core.definitions.META_CC_SPECS
import com.reoky.raidframer.core.definitions.META_DANCER_SPECS
import com.reoky.raidframer.core.definitions.META_HEALER_SPECS
import com.reoky.raidframer.core.definitions.META_MAGE_SPECS
import com.reoky.raidframer.core.definitions.META_MELEE_SPECS
import com.reoky.raidframer.core.definitions.META_RANGED_SPEC
import com.reoky.raidframer.core.definitions.SpecType
import com.reoky.raidframer.core.model.PlayerRole

/*
 * Guess the player's role based on their recent actions / spec / past roles. Really whatever we can glean.
 * This is going to be kind of rough and ready at first.
 */
fun SpecType.guessPlayerRole(): PlayerRole {
  if (this in META_DANCER_SPECS) return PlayerRole.PURPLE
  if (this in META_CC_SPECS) return PlayerRole.GREEN // always green for cc specs / tanks
  if (this in META_MELEE_SPECS) return PlayerRole.GREEN // melee dps are green if their gear is above 17k
  if (this in META_HEALER_SPECS) return PlayerRole.PINK
  if (this in META_MAGE_SPECS) return PlayerRole.RED
  if (this in META_RANGED_SPEC) return PlayerRole.BLUE
  return PlayerRole.BLUE
}
