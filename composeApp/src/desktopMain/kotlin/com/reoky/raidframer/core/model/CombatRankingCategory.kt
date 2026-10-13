package com.reoky.raidframer.core.model

import androidx.compose.ui.graphics.Color
import com.reoky.raidframer.core.helpers.RFColors
import org.jetbrains.compose.resources.StringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_charms
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_silence
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_distress
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_debuffs
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_songs
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_buffs
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_potions
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_glider
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_items
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_tiger_strikes
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_freezes
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_kills
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_trips
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_bubbles
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_bracings
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_shield_strip
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_weapon_disables
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_potion_disables
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_bd_glider
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_crystal_wings
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_glider_disables
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_provoked
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_coherence_render
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_coherence_raid
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_coherence_clump
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_deaths
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_total_damage_taken
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_total_heals_received
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_ode_heals
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_kills_killing_blow
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_defiance
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_garden_defiance
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_purges
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_sac_dances
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_deep_tranquility
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_deepend_debuff
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_throw_dagger
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_stuns
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_staggers
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_petrification
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_absorb_lifeforce
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_corrosive_barrage
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_blinded_by_crows
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_mist_sunder
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_regular_sunder
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_impales
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_protective_wings
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_courageous_action
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_mana_barrier
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_revive
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_blocks
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_parries
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_evades
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_immunes
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_misses
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_absorbs
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_damage_blocked
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_damage_parried
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_crits
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_max_hit
import raid_framer_desktop.composeapp.generated.resources.player_card_stat_average_hit

enum class CombatRankingCategory(
  val displayNameRes: StringResource,
  val valueColor: Color,
  val icon: String
) {
  CHARMS(
    displayNameRes = Res.string.player_card_stat_charms,
    valueColor = RFColors.charmPink,
    icon = "\uf004"
  ),
  SILENCES(
    displayNameRes = Res.string.player_card_stat_silence,
    valueColor = RFColors.silencePurple,
    icon = "\uf714"
  ),
  DISTRESSES(
    displayNameRes = Res.string.player_card_stat_distress,
    valueColor = RFColors.distressPurple,
    icon = "\uf0c1"
  ),
  DEBUFFS(
    displayNameRes = Res.string.player_card_stat_debuffs,
    valueColor = RFColors.silencePurple,
    icon = "\uf714"
  ),
  SONGS(
    displayNameRes = Res.string.player_card_stat_songs,
    valueColor = RFColors.charmPink,
    icon = "\uf004"
  ),
  BUFFS(
    displayNameRes = Res.string.player_card_stat_buffs,
    valueColor = RFColors.distressPurple,
    icon = "\uf0c1"
  ),
  POTIONS(
    displayNameRes = Res.string.player_card_stat_potions,
    valueColor = RFColors.potionTeal,
    icon = "\uf0c3"
  ),
  GLIDERS(
    displayNameRes = Res.string.player_card_stat_glider,
    valueColor = RFColors.gliderBlue,
    icon = "\uf5b0"
  ),
  ITEMS(
    displayNameRes = Res.string.player_card_stat_items,
    valueColor = RFColors.itemSkillYellow,
    icon = "\uf6d5"
  ),
  TIGER_STRIKES(
    displayNameRes = Res.string.player_card_stat_tiger_strikes,
    valueColor = RFColors.techNoTigerStrikes,
    icon = "\uf21b"
  ),
  FREEZES(
    displayNameRes = Res.string.player_card_stat_freezes,
    valueColor = RFColors.freezeIceBlue,
    icon = "\uf2dc"
  ),
  KILLS(
    displayNameRes = Res.string.player_card_stat_kills,
    valueColor = RFColors.killsRed,
    icon = "\uf547"
  ),
  TRIPS(
    displayNameRes = Res.string.player_card_stat_trips,
    valueColor = RFColors.tripsAmber,
    icon = "\uf071"
  ),
  BUBBLES(
    displayNameRes = Res.string.player_card_stat_bubbles,
    valueColor = RFColors.bubblesCyan,
    icon = "\uf0eb"
  ),
  BRACINGS(
    displayNameRes = Res.string.player_card_stat_bracings,
    valueColor = RFColors.bracingsGreen,
    icon = "\uf132"
  ),
  SHIELD_STRIP(
    displayNameRes = Res.string.player_card_stat_shield_strip,
    valueColor = RFColors.shieldStripOrange,
    icon = "\uf3ed"
  ),
  WEAPON_DISABLES(
    displayNameRes = Res.string.player_card_stat_weapon_disables,
    valueColor = RFColors.weaponDisablesRed,
    icon = "\uf6e2"
  ),
  POTION_DISABLES(
    displayNameRes = Res.string.player_card_stat_potion_disables,
    valueColor = RFColors.potionDisablesPurple,
    icon = "\uf484"
  ),
  BD_GLIDER(
    displayNameRes = Res.string.player_card_stat_bd_glider,
    valueColor = RFColors.bdGliderTeal,
    icon = "\uf072"
  ),
  CRYSTAL_WINGS(
    displayNameRes = Res.string.player_card_stat_crystal_wings,
    valueColor = RFColors.crystalWingsBlue,
    icon = "\uf06e"
  ),
  GLIDER_DISABLES(
    displayNameRes = Res.string.player_card_stat_glider_disables,
    valueColor = RFColors.gliderDisablesPink,
    icon = "\uf147"
  ),
  PROVOKED(
    displayNameRes = Res.string.player_card_stat_provoked,
    valueColor = RFColors.provokesDeepPurple,
    icon = "\uf559"
  ),
  COHERENCE_RENDER(
    displayNameRes = Res.string.player_card_stat_coherence_render,
    valueColor = RFColors.gliderBlue,
    icon = "\uf06e"
  ),
  COHERENCE_RAID(
    displayNameRes = Res.string.player_card_stat_coherence_raid,
    valueColor = RFColors.purgeGreen,
    icon = "\uf547"
  ),
  COHERENCE_CLUMP(
    displayNameRes = Res.string.player_card_stat_coherence_clump,
    valueColor = RFColors.bubblesCyan,
    icon = "\uf0eb"
  ),

  // Older gaps added 10/05/26 - ranking flows and browser filters already existed for these
  DEATHS(
    displayNameRes = Res.string.player_card_stat_deaths,
    valueColor = RFColors.killsRed,
    icon = "\uf54c"
  ),
  DAMAGE_TAKEN(
    displayNameRes = Res.string.player_card_stat_total_damage_taken,
    valueColor = RFColors.killsRed,
    icon = "\uf071"
  ),
  HEALS_RECEIVED(
    displayNameRes = Res.string.player_card_stat_total_heals_received,
    valueColor = RFColors.healsGreen,
    icon = "\uf004"
  ),
  ODE_HEALS(
    displayNameRes = Res.string.player_card_stat_ode_heals,
    valueColor = RFColors.healsGreen,
    icon = "\uf0eb"
  ),
  KILLS_KB(
    displayNameRes = Res.string.player_card_stat_kills_killing_blow,
    valueColor = RFColors.killsHaranyaGreen,
    icon = "\uf547"
  ),

  // 08/04 + deep tranquility batch
  DEFIANCE(
    displayNameRes = Res.string.player_card_stat_defiance,
    valueColor = RFColors.defianceGold,
    icon = "\uf559"
  ),
  GARDEN_DEFIANCE(
    displayNameRes = Res.string.player_card_stat_garden_defiance,
    valueColor = RFColors.gardenDefianceBlue,
    icon = "\uf067"
  ),
  PURGES(
    displayNameRes = Res.string.player_card_stat_purges,
    valueColor = RFColors.purgeGreen,
    icon = "\uf0e2"
  ),
  SAC_DANCES(
    displayNameRes = Res.string.player_card_stat_sac_dances,
    valueColor = RFColors.sacDancePurple,
    icon = "\uf0c0"
  ),
  DEEP_TRANQUILITY(
    displayNameRes = Res.string.player_card_stat_deep_tranquility,
    valueColor = RFColors.deepTranquilityTeal,
    icon = "\uf1da"
  ),

  // 08/13 batch
  DEEPEND_DEBUFF(
    displayNameRes = Res.string.player_card_stat_deepend_debuff,
    valueColor = RFColors.deedendDebuffRed,
    icon = "\uf147"
  ),
  THROW_DAGGER(
    displayNameRes = Res.string.player_card_stat_throw_dagger,
    valueColor = RFColors.throwDaggerAmber,
    icon = "\uf337"
  ),
  STUNS(
    displayNameRes = Res.string.player_card_stat_stuns,
    valueColor = RFColors.stunDeepRed,
    icon = "\uf011"
  ),
  STAGGERS(
    displayNameRes = Res.string.player_card_stat_staggers,
    valueColor = RFColors.staggerBrown,
    icon = "\uf019"
  ),
  PETRIFICATION(
    displayNameRes = Res.string.player_card_stat_petrification,
    valueColor = RFColors.petrificationGray,
    icon = "\uf2ed"
  ),
  ABSORB_LIFEFORCE(
    displayNameRes = Res.string.player_card_stat_absorb_lifeforce,
    valueColor = RFColors.absorbLifeforceMagenta,
    icon = "\uf6d1"
  ),
  CORROSIVE_BARRAGE(
    displayNameRes = Res.string.player_card_stat_corrosive_barrage,
    valueColor = RFColors.corrosiveBarrageLime,
    icon = "\uf3a5"
  ),
  BLINDED_BY_CROWS(
    displayNameRes = Res.string.player_card_stat_blinded_by_crows,
    valueColor = RFColors.blindedByCrowsDark,
    icon = "\uf06e"
  ),
  MIST_SUNDER(
    displayNameRes = Res.string.player_card_stat_mist_sunder,
    valueColor = RFColors.mistSunderCyan,
    icon = "\uf1e0"
  ),
  REGULAR_SUNDER(
    displayNameRes = Res.string.player_card_stat_regular_sunder,
    valueColor = RFColors.regularSunderOrange,
    icon = "\uf200"
  ),
  IMPALE_IMMUNITY(
    displayNameRes = Res.string.player_card_stat_impales,
    valueColor = RFColors.impaleImmunitySteel,
    icon = "\uf0c5"
  ),
  PROTECTIVE_WINGS(
    displayNameRes = Res.string.player_card_stat_protective_wings,
    valueColor = RFColors.protectiveWingsGold,
    icon = "\uf072"
  ),
  COURAGEOUS_ACTION(
    displayNameRes = Res.string.player_card_stat_courageous_action,
    valueColor = RFColors.courageousActionBright,
    icon = "\uf0c7"
  ),
  MANA_BARRIER(
    displayNameRes = Res.string.player_card_stat_mana_barrier,
    valueColor = RFColors.manaBarrierBlue,
    icon = "\uf12c"
  ),
  REVIVE(
    displayNameRes = Res.string.player_card_stat_revive,
    valueColor = RFColors.reviveGhostWhite,
    icon = "\uf013"
  ),

  // Avoidance batch (10/05/26)
  BLOCKS(
    displayNameRes = Res.string.player_card_stat_blocks,
    valueColor = RFColors.avoidanceBlockBlue,
    icon = "\uf3ed"
  ),
  PARRIES(
    displayNameRes = Res.string.player_card_stat_parries,
    valueColor = RFColors.avoidanceParryAmber,
    icon = "\uf0c8"
  ),
  EVADES(
    displayNameRes = Res.string.player_card_stat_evades,
    valueColor = RFColors.avoidanceEvadeGreen,
    icon = "\uf03e"
  ),
  IMMUNES(
    displayNameRes = Res.string.player_card_stat_immunes,
    valueColor = RFColors.avoidanceImmunePurple,
    icon = "\uf00d"
  ),
  MISSES(
    displayNameRes = Res.string.player_card_stat_misses,
    valueColor = RFColors.avoidanceMissGray,
    icon = "\uf128"
  ),
  ABSORBS(
    displayNameRes = Res.string.player_card_stat_absorbs,
    valueColor = RFColors.avoidanceAbsorbCyan,
    icon = "\uf030"
  ),
  DAMAGE_BLOCKED(
    displayNameRes = Res.string.player_card_stat_damage_blocked,
    valueColor = RFColors.avoidanceBlockBlue,
    icon = "\uf02d"
  ),
  DAMAGE_PARRIED(
    displayNameRes = Res.string.player_card_stat_damage_parried,
    valueColor = RFColors.avoidanceParryAmber,
    icon = "\uf04d"
  ),

  // Hit quality batch (10/11/26)
  CRITS(
    displayNameRes = Res.string.player_card_stat_crits,
    valueColor = RFColors.critGold,
    icon = "\uD83C\uDF1F"
  ),
  MAX_HIT(
    displayNameRes = Res.string.player_card_stat_max_hit,
    valueColor = RFColors.maxHitRed,
    icon = "\uD83D\uDCA5"
  ),
  AVERAGE_HIT(
    displayNameRes = Res.string.player_card_stat_average_hit,
    valueColor = RFColors.averageHitTeal,
    icon = "\uD83D\uDCC8"
  );

  companion object {
    val ALL_CATEGORIES = entries.toList()

    fun fromString(value: String): CombatRankingCategory? {
      return entries.find { it.name == value }
    }
  }
}
