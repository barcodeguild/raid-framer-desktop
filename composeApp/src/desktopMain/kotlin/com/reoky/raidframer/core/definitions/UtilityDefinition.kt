package com.reoky.raidframer.core.definitions

import com.reoky.raidframer.core.database.incrementPackedItemUsage
import com.reoky.raidframer.core.model.BuffAppliedEvent
import com.reoky.raidframer.core.model.BuffGainedEvent
import com.reoky.raidframer.core.model.CombatEvent
import com.reoky.raidframer.core.model.DamageEvent
import com.reoky.raidframer.core.model.DebuffAppliedEvent
import com.reoky.raidframer.core.model.PlayerCard
import com.reoky.raidframer.core.model.SuccessfulCastEvent
import org.jetbrains.compose.resources.StringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.item_name_halcy_neck
import raid_framer_desktop.composeapp.generated.resources.item_name_honor_nodachi
import raid_framer_desktop.composeapp.generated.resources.item_name_jola_shield
import raid_framer_desktop.composeapp.generated.resources.item_name_kraken_scepter
import raid_framer_desktop.composeapp.generated.resources.item_name_lib_shield
import raid_framer_desktop.composeapp.generated.resources.item_name_library_greatclub
import raid_framer_desktop.composeapp.generated.resources.item_name_soul_neck
import kotlin.compareTo
import kotlin.plus
import kotlin.text.toLong
import kotlin.times


/**
 * Common contract for any utility item category (items, gliders, potions, etc). This is the magic right here friends. ~
 * All the utility item enums implement this interface to provide a common way to identify and handle their usage. The
 * detection "middleware" is executed as events are processed to see if any utility items were used and the updateCard
 * transformation is applied to the relevant player's PlayerCard. (good design reoky mhmm!)
 */
interface UtilityItem {
  val itemSpecificSkillIds: List<Int>
  val itemSpecificBuffIds: List<Int>
  val castTime: Double
  val cooldown: Double
  val friendlyNameRes: StringResource
  val possibleSpellNames: List<String>
  val updateCard: (PlayerCard) -> PlayerCard
}

/**
 * Checks if the successful cast event pertains to the given item spell.
 */
fun CombatEvent.pertainsToUtilityItem(item: UtilityItem): Boolean = when (this) {
  is SuccessfulCastEvent -> (this.spellId in item.itemSpecificSkillIds) ||
      item.possibleSpellNames.any { it.equals(this.spell, ignoreCase = true) }
  is BuffGainedEvent -> (this.buffId in item.itemSpecificBuffIds) ||
      item.possibleSpellNames.any { it.equals(this.buff, ignoreCase = true) }
  is DamageEvent -> item.possibleSpellNames.any { it.equals(this.spell, ignoreCase = true) }
  is DebuffAppliedEvent -> (this.debuffId in item.itemSpecificBuffIds) ||
      item.possibleSpellNames.any { it.equals(this.debuff, ignoreCase = true) }
  is BuffAppliedEvent -> (this.buffId in item.itemSpecificBuffIds) ||
      item.possibleSpellNames.any { it.equals(this.buff, ignoreCase = true) }
  // Not sure if we need to handle other event types here because items would match buff gained / ended events and create duplicates
  else -> false
}

/**
 * Middleware function to create a copy of the player card with updated item spell usage info
 * based on the successful cast event.
 */
fun PlayerCard.copiedWithUtilityItemDetectionMiddleWare(event: CombatEvent): PlayerCard {
  val now = System.currentTimeMillis()

  val matching = ItemSpell.entries.filter { event.pertainsToUtilityItem(it) }
  if (matching.isEmpty()) return this

  // Build set of item names that are still on cooldown (per-item cooldown)
  val recentItemKeys = recentSkillItemUsages
    .mapNotNull { usage ->
      val found = ItemSpell.entries.find { it.friendlyNameRes == usage.second }
      if (found != null) {
        val cooldownMillis = (found.cooldown * 1000).toLong()
        if (now - usage.first < cooldownMillis) found.name else null
      } else null
    }
    .toSet()

  // Filter out items that are still on cooldown
  val newMatches = matching.filterNot { it.name in recentItemKeys }
  if (newMatches.isEmpty()) return this

  // Record usages using wall-clock time so debounce/cooldown checks are consistent
  val updatedRecentSkillItemUsages = newMatches.fold(recentSkillItemUsages) { acc, item ->
    acc + Triple(now, item.friendlyNameRes, event.target)
  }

  val updatedCard = newMatches.fold(this) { acc, item -> item.updateCard(acc) }

  return updatedCard.copy(
    lastEvent = now,
    lastItemUse = now,
    cache = cache?.copy(
      lastSeen = now,
      lifetimeTotalItemSkillsUsed = cache.lifetimeTotalItemSkillsUsed + newMatches.size,
    ),
    sessionItemSkillTotal = sessionItemSkillTotal + newMatches.size,
    recentSkillItemUsages = updatedRecentSkillItemUsages
  )
}
