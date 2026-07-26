package com.reoky.raidframer.core.model

import androidx.compose.ui.graphics.Color
import com.reoky.raidframer.core.helpers.RFColors
import org.jetbrains.compose.resources.StringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.category_charms
import raid_framer_desktop.composeapp.generated.resources.category_silences
import raid_framer_desktop.composeapp.generated.resources.category_distresses
import raid_framer_desktop.composeapp.generated.resources.category_debuffs
import raid_framer_desktop.composeapp.generated.resources.category_songs
import raid_framer_desktop.composeapp.generated.resources.category_buffs
import raid_framer_desktop.composeapp.generated.resources.category_potions
import raid_framer_desktop.composeapp.generated.resources.category_gliders
import raid_framer_desktop.composeapp.generated.resources.category_items
import raid_framer_desktop.composeapp.generated.resources.category_tiger_strikes
import raid_framer_desktop.composeapp.generated.resources.category_freezes
import raid_framer_desktop.composeapp.generated.resources.category_kills
import raid_framer_desktop.composeapp.generated.resources.category_tripped
import raid_framer_desktop.composeapp.generated.resources.category_bubble_trap
import raid_framer_desktop.composeapp.generated.resources.category_bracing_blast
import raid_framer_desktop.composeapp.generated.resources.category_shield_strip
import raid_framer_desktop.composeapp.generated.resources.category_weapon_disables
import raid_framer_desktop.composeapp.generated.resources.category_potion_disables
import raid_framer_desktop.composeapp.generated.resources.category_bd_glider
import raid_framer_desktop.composeapp.generated.resources.category_crystal_wings
import raid_framer_desktop.composeapp.generated.resources.category_glider_disables
import raid_framer_desktop.composeapp.generated.resources.category_provoked

enum class CombatRankingCategory(
  val displayNameRes: StringResource,
  val valueColor: Color,
  val icon: String
) {
  CHARMS(
    displayNameRes = Res.string.category_charms,
    valueColor = RFColors.charmPink,
    icon = "\uf004"
  ),
  SILENCES(
    displayNameRes = Res.string.category_silences,
    valueColor = RFColors.silencePurple,
    icon = "\uf714"
  ),
  DISTRESSES(
    displayNameRes = Res.string.category_distresses,
    valueColor = RFColors.distressPurple,
    icon = "\uf0c1"
  ),
  DEBUFFS(
    displayNameRes = Res.string.category_debuffs,
    valueColor = RFColors.silencePurple,
    icon = "\uf714"
  ),
  SONGS(
    displayNameRes = Res.string.category_songs,
    valueColor = RFColors.charmPink,
    icon = "\uf004"
  ),
  BUFFS(
    displayNameRes = Res.string.category_buffs,
    valueColor = RFColors.distressPurple,
    icon = "\uf0c1"
  ),
  POTIONS(
    displayNameRes = Res.string.category_potions,
    valueColor = RFColors.potionTeal,
    icon = "\uf0c3"
  ),
  GLIDERS(
    displayNameRes = Res.string.category_gliders,
    valueColor = RFColors.gliderBlue,
    icon = "\uf5b0"
  ),
  ITEMS(
    displayNameRes = Res.string.category_items,
    valueColor = RFColors.itemSkillYellow,
    icon = "\uf6d5"
  ),
  TIGER_STRIKES(
    displayNameRes = Res.string.category_tiger_strikes,
    valueColor = RFColors.techNoTigerStrikes,
    icon = "\uf21b"
  ),
  FREEZES(
    displayNameRes = Res.string.category_freezes,
    valueColor = RFColors.freezeIceBlue,
    icon = "\uf2dc"
  ),
  KILLS(
    displayNameRes = Res.string.category_kills,
    valueColor = RFColors.killsRed,
    icon = "\uf547"
  ),
  TRIPS(
    displayNameRes = Res.string.category_tripped,
    valueColor = RFColors.tripsAmber,
    icon = "\uf071"
  ),
  BUBBLES(
    displayNameRes = Res.string.category_bubble_trap,
    valueColor = RFColors.bubblesCyan,
    icon = "\uf0eb"
  ),
  BRACINGS(
    displayNameRes = Res.string.category_bracing_blast,
    valueColor = RFColors.bracingsGreen,
    icon = "\uf132"
  ),
  SHIELD_STRIP(
    displayNameRes = Res.string.category_shield_strip,
    valueColor = RFColors.shieldStripOrange,
    icon = "\uf3ed"
  ),
  WEAPON_DISABLES(
    displayNameRes = Res.string.category_weapon_disables,
    valueColor = RFColors.weaponDisablesRed,
    icon = "\uf6e2"
  ),
  POTION_DISABLES(
    displayNameRes = Res.string.category_potion_disables,
    valueColor = RFColors.potionDisablesPurple,
    icon = "\uf484"
  ),
  BD_GLIDER(
    displayNameRes = Res.string.category_bd_glider,
    valueColor = RFColors.bdGliderTeal,
    icon = "\uf072"
  ),
  CRYSTAL_WINGS(
    displayNameRes = Res.string.category_crystal_wings,
    valueColor = RFColors.crystalWingsBlue,
    icon = "\uf06e"
  ),
  GLIDER_DISABLES(
    displayNameRes = Res.string.category_glider_disables,
    valueColor = RFColors.gliderDisablesPink,
    icon = "\uf147"
  ),
  PROVOKED(
    displayNameRes = Res.string.category_provoked,
    valueColor = RFColors.provokesDeepPurple,
    icon = "\uf559"
  );

  companion object {
    val ALL_CATEGORIES = entries.toList()

    fun fromString(value: String): CombatRankingCategory? {
      return entries.find { it.name == value }
    }
  }
}
