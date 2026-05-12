package com.reoky.raidframer.core.definitions

import com.reoky.raidframer.core.model.CombatEvent
import com.reoky.raidframer.core.model.PlayerCard
import com.reoky.raidframer.core.model.SuccessfulCastEvent
import org.jetbrains.compose.resources.StringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.potion_name_healing_guild_courage
import raid_framer_desktop.composeapp.generated.resources.potion_name_healing_rank_eight
import raid_framer_desktop.composeapp.generated.resources.potion_name_healing_rank_one_through_six
import raid_framer_desktop.composeapp.generated.resources.potion_name_minor_healing_rank_eight
import raid_framer_desktop.composeapp.generated.resources.potion_name_minor_healing_rank_five
import raid_framer_desktop.composeapp.generated.resources.potion_name_minor_healing_rank_four
import raid_framer_desktop.composeapp.generated.resources.potion_name_minor_healing_rank_one
import raid_framer_desktop.composeapp.generated.resources.potion_name_minor_healing_rank_seven
import raid_framer_desktop.composeapp.generated.resources.potion_name_minor_healing_rank_six
import raid_framer_desktop.composeapp.generated.resources.potion_name_minor_healing_rank_three
import raid_framer_desktop.composeapp.generated.resources.potion_name_minor_healing_rank_two
import raid_framer_desktop.composeapp.generated.resources.potion_name_minor_healing_titan
import raid_framer_desktop.composeapp.generated.resources.potion_name_minor_mana_rank_one
import raid_framer_desktop.composeapp.generated.resources.potion_name_minor_mana_titan
import raid_framer_desktop.composeapp.generated.resources.potion_name_phoenix_tears_tincture
import raid_framer_desktop.composeapp.generated.resources.potion_name_wild_ginseng
import kotlin.collections.plus
import kotlin.compareTo
import kotlin.plus
import kotlin.text.toLong
import kotlin.times

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

  // minor health
  MinorHealthRankOne(35234, 21.0, Res.string.potion_name_minor_healing_rank_one, listOf("Minor Healing Potion")),
  MinorHealthRankTwo(46252, 21.0, Res.string.potion_name_minor_healing_rank_two, listOf("Minor Healing Potion")),
  MinorHealthRankThree(46253, 21.0, Res.string.potion_name_minor_healing_rank_three, listOf("Minor Healing Potion")),
  MinorHealthRankFour(46254, 21.0, Res.string.potion_name_minor_healing_rank_four, listOf("Minor Healing Potion")),
  MinorHealthRankFive(46255, 21.0, Res.string.potion_name_minor_healing_rank_five, listOf("Minor Healing Potion")),
  MinorHealthRankSix(46256, 21.0, Res.string.potion_name_minor_healing_rank_six, listOf("Minor Healing Potion")),
  MinorHealthRankSeven(38429, 21.0, Res.string.potion_name_minor_healing_rank_seven, listOf("Minor Healing Potion")),
  MinorHealthRankEight(44476, 21.0, Res.string.potion_name_minor_healing_rank_eight, listOf("Minor Healing Potion")),

  // major health
  HealthPotionRankRankOneThroughSix(35236, 90.0, Res.string.potion_name_healing_rank_one_through_six, listOf("Healing Potion")),
  HealthPotionRankSeven(44477, 90.0, Res.string.potion_name_healing_rank_eight, listOf("Healing Potion")),
  GuildCourageHealthPotion(50855, 30.0, Res.string.potion_name_healing_guild_courage, listOf("Healing Potion")),

  // minor mana
  MinorManaRankSeven(38430, 21.0, Res.string.potion_name_minor_mana_rank_one, listOf("Minor Mana Potion")),

  // major mana

  // titan potions
  MinorHealthTitan(35662, 12.0, Res.string.potion_name_minor_healing_titan, listOf("Use Titan's Health Potion")),
  MinorManaTitan(38663, 12.0, Res.string.potion_name_minor_mana_titan, listOf("Use Titan's Mana Potion")),

  // misc
  PhoenixTearTincture(9002233, 180.0, Res.string.potion_name_phoenix_tears_tincture, listOf("Phoenix Tears Tincture")),
  WildGinseng(12333, 600.0, Res.string.potion_name_wild_ginseng, listOf("Found Wild Ginseng!")),


  ;
}

/**
 * Middleware to detect potion usages from a CombatEvent and update the PlayerCard.
 * - No-op for non-SuccessfulCastEvent
 * - Debounces per-potion by cooldown using recentSkillItemUsages
 * - Records usages and increments sessionPotionTotal
 */
fun PlayerCard.copiedWithPotionDetectionMiddleWare(event: CombatEvent): PlayerCard {
  if (event !is SuccessfulCastEvent) return this
  val eventTs = event.timestamp
  val now = System.currentTimeMillis()

  val matched = PotionDefinition.entries.firstOrNull { it.skillId == event.spellId }
//    ?: PotionDefinition.entries.firstOrNull { potion ->
//      potion.possibleSpellNames.any { it.equals(event.spell, ignoreCase = true) }
//    } no longer doing spell names, only spell ids, because of the potential for false positives and the fact that we can reliably track potion casts by spell id alone
  if (matched == null) return this

  // Use the first possibleSpellName as a canonical group key (fallback to enum name)
  val matchedGroup = matched.possibleSpellNames.firstOrNull()?.lowercase()?.trim() ?: matched.name.lowercase()
  val cooldownMillis = (matched.cooldown * 1000).toLong()

  // Consider a recent usage "the same physical potion" when its group key matches.
  val onCooldown = recentSkillItemUsages.any { usage ->
    // Find the potion enum that corresponds to the stored friendlyNameRes (if any)
    val usedPotion = PotionDefinition.entries.find { it.friendlyNameRes == usage.second }
    val usedGroup = usedPotion?.possibleSpellNames?.firstOrNull()?.lowercase()?.trim() ?: usedPotion?.name?.lowercase()
    if (usedGroup == null) return@any false
    if (usedGroup != matchedGroup) return@any false

    // usage.first may be an event timestamp or wall-clock; check both relative to eventTs and now
    val sinceEvent = eventTs - usage.first
    val sinceNow = now - usage.first
    (sinceEvent < cooldownMillis) || (sinceNow < cooldownMillis)
  }
  if (onCooldown) return this

  // Record using the event timestamp so persisted/reloaded history stays consistent
  val updatedRecent = recentSkillItemUsages + Triple(eventTs, matched.friendlyNameRes, event.target)

  return this.copy(
    lastEvent = eventTs,
    cache = this.cache?.copy(
      lastSeen = eventTs,
      lifetimeTotalPotionUsages = this.cache.lifetimeTotalPotionUsages + 1
    ),
    sessionPotionTotal = this.sessionPotionTotal + 1,
    recentSkillItemUsages = updatedRecent
  )
}


