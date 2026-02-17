package com.reoky.raidframer.core.definitions

import com.reoky.raidframer.core.model.SuccessfulCastEvent
import org.jetbrains.compose.resources.StringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.potion_name_healing_rank_one_through_seven
import raid_framer_desktop.composeapp.generated.resources.potion_name_minor_healing_rank_eight
import raid_framer_desktop.composeapp.generated.resources.potion_name_minor_healing_rank_five
import raid_framer_desktop.composeapp.generated.resources.potion_name_minor_healing_rank_four
import raid_framer_desktop.composeapp.generated.resources.potion_name_minor_healing_rank_one
import raid_framer_desktop.composeapp.generated.resources.potion_name_minor_healing_rank_seven
import raid_framer_desktop.composeapp.generated.resources.potion_name_minor_healing_rank_six
import raid_framer_desktop.composeapp.generated.resources.potion_name_minor_healing_rank_three
import raid_framer_desktop.composeapp.generated.resources.potion_name_minor_healing_rank_two
import raid_framer_desktop.composeapp.generated.resources.potion_name_minor_healing_titan

// minor health 38429

// just basic tracking of potion casts instead of going overboard
interface PotionItem {
  val skillId: Int
  val cooldown: Double
  val friendlyNameRes: StringResource // name of potion casted
  val possibleSpellNames: List<String> // spell names that might appear in logs
}

enum class PotionDefinition(
  override val skillId: Int,
  override val cooldown: Double,
  override val friendlyNameRes: StringResource,
  override val possibleSpellNames: List<String>
) : PotionItem {
  MinorHealthRankOne(35234, 21.0, Res.string.potion_name_minor_healing_rank_one, listOf("Minor Healing Potion")),
  MinorHealthRankTwo(46252, 21.0, Res.string.potion_name_minor_healing_rank_two, listOf("Minor Healing Potion")),
  MinorHealthRankThree(46253, 21.0, Res.string.potion_name_minor_healing_rank_three, listOf("Minor Healing Potion")),
  MinorHealthRankFour(46254, 21.0, Res.string.potion_name_minor_healing_rank_four, listOf("Minor Healing Potion")),
  MinorHealthRankFive(46255, 21.0, Res.string.potion_name_minor_healing_rank_five, listOf("Minor Healing Potion")),
  MinorHealthRankSix(46256, 21.0, Res.string.potion_name_minor_healing_rank_six, listOf("Minor Healing Potion")),
  MinorHealthRankSeven(38429, 21.0, Res.string.potion_name_minor_healing_rank_seven, listOf("Minor Healing Potion")),
  MinorHealthRankEight(44476, 21.0, Res.string.potion_name_minor_healing_rank_eight, listOf("Minor Healing Potion")),

  HealthPotionRankRankOneThroughSeven(35236, 21.0, Res.string.potion_name_healing_rank_one_through_seven, listOf("Healing Potion")),

  MinorHealthTitan(35662, 12.0, Res.string.potion_name_minor_healing_titan, listOf("Found Wild Ginseng!")),
  ;
}

/**
 * Check if a successful cast event pertains to any potion.
 */
fun SuccessfulCastEvent.pertainsToPotion(potion: PotionItem): Boolean {
  return (this.spellId == potion.skillId) ||
      potion.possibleSpellNames.any { it.equals(this.spell, ignoreCase = true) }
}

/**
 * Find a potion definition by skill ID or spell name.
 */
fun findPotionByEvent(event: SuccessfulCastEvent): PotionDefinition? {
  return PotionDefinition.entries.firstOrNull { event.pertainsToPotion(it) }
}

