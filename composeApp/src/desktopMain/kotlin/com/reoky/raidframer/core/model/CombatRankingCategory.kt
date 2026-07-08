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
  );

  companion object {
    val ALL_CATEGORIES = entries.toList()

    fun fromString(value: String): CombatRankingCategory? {
      return entries.find { it.name == value }
    }
  }
}
