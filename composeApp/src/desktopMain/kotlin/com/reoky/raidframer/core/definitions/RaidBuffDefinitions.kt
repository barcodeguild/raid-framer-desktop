package com.reoky.raidframer.core.definitions

import com.reoky.raidframer.core.serialization.RaidFramePayload
import com.reoky.raidframer.core.model.RaidBuffGracePeriod
import com.reoky.raidframer.core.interactor.PlayerCacheInteractor
import com.reoky.raidframer.core.definitions.lootBuffAmountForIds

enum class RaidBuffKey {
  GOBLET,
  FEAST_RIBS,
  LONGING,
  WHISPER,
  BLESSED_ELIXIR,
  ANCIENTS_POTION,
  JINHUI_WISH,
  SECRET_GIFT,
  FAIRY_PROTECTION,
  COOKFIRE,
  WAR_DRUM,
  DAHUTAS_BUBBLE,
  MONSTER_HUNTERS_DREAM,
  RED_FLOWER_FRUIT,
  BLUE_FLOWER_FRUIT,
  STATUE_BUFF,
  FACTION_WAR_TIME,
  MOONLIGHT_JUICE,
  HUNTING_ELIXIR,
  CHOCOLATE,
  LOOT_CAKE,
  SHORTBREAD_COOKIE,
  GOLDEN_TAFFY,
  EGG_OF_FORTUNE
}

enum class RaidBuffSection { MAIN, LOOT }

data class RaidBuffDefinition(
  val key: RaidBuffKey,
  val ids: Set<Int>,
  val labelKey: String,
  val section: RaidBuffSection = RaidBuffSection.MAIN,
  val enhancedIds: Set<Int> = emptySet(),
  val orangeIds: Set<Int> = emptySet(),
  val meatballIds: Set<Int> = emptySet()
)

val RAID_BUFF_DEFINITIONS = listOf(
  // Goblet: Orange (24469-24474) gives HP regen and is class-agnostic; other colors are class-specific.
  // Orange: 24469 24470 24471 24472 24473 24474
  // Blue: 21796 21801 21806 21811 21819 21846
  // Yellow: 21797 21802 21807 21812 21820
  // Purple: 21798 21803 21808 21813 21821
  // Pink: 21799 21804 21809 21814 21822
  // Gray: 21800 21805 21810 21815 21823
  RaidBuffDefinition(RaidBuffKey.GOBLET, setOf(24469, 24470, 24471, 24472, 24473, 24474, 21796, 21801, 21806, 21811, 21819, 21846, 21797, 21802, 21807, 21812, 21820, 21798, 21803, 21808, 21813, 21821, 21799, 21804, 21809, 21814, 21822, 21800, 21805, 21810, 21815, 21823), "raid_buff_goblet", orangeIds = setOf(24469, 24470, 24471, 24472, 24473, 24474)),
  // Feast / Ribs: Feast table (21791-21794) is best; Ribs (685 689 693 697); Meatballs (680 686 690 694) are lower-tier.
  RaidBuffDefinition(RaidBuffKey.FEAST_RIBS, setOf(21791, 21792, 21793, 21794, 2305, 685, 689, 693, 697, 680, 686, 690, 694), "raid_buff_feast_ribs", meatballIds = setOf(680, 686, 690, 694)),
  // Longing (Book): Regular (20552 32381 32382 21795); Enhanced (26581 26582) - has "Require Enhanced Version" toggle.
  RaidBuffDefinition(RaidBuffKey.LONGING, setOf(20552, 32381, 32382, 21795, 26581, 26582), "raid_buff_longing", enhancedIds = setOf(26581, 26582)),
  RaidBuffDefinition(RaidBuffKey.WHISPER, setOf(9001811), "raid_buff_whisper"),
  RaidBuffDefinition(RaidBuffKey.BLESSED_ELIXIR, setOf(31306), "raid_buff_blessed_elixir"),
  RaidBuffDefinition(RaidBuffKey.ANCIENTS_POTION, setOf(9000906, 9001797), "raid_buff_ancients_potion"),
  RaidBuffDefinition(RaidBuffKey.JINHUI_WISH, setOf(8318), "raid_buff_jinhui_wish"),
  RaidBuffDefinition(RaidBuffKey.SECRET_GIFT, setOf(8209), "raid_buff_secret_gift"),
  RaidBuffDefinition(RaidBuffKey.FAIRY_PROTECTION, setOf(26764), "raid_buff_fairy_protection"),
  RaidBuffDefinition(RaidBuffKey.COOKFIRE, setOf(5861, 5862, 5863, 5864, 5865), "raid_buff_cookfire"),
  RaidBuffDefinition(RaidBuffKey.WAR_DRUM, setOf(5700, 32233, 32234, 32235, 32236, 32237, 32238, 32239), "raid_buff_war_drum"),
  RaidBuffDefinition(RaidBuffKey.DAHUTAS_BUBBLE, setOf(6660), "raid_buff_dahutas_bubble"),
  RaidBuffDefinition(RaidBuffKey.MONSTER_HUNTERS_DREAM, setOf(9002009), "raid_buff_monster_hunters_dream"),
  RaidBuffDefinition(RaidBuffKey.RED_FLOWER_FRUIT, setOf(3076), "raid_buff_red_flower_fruit"),
  RaidBuffDefinition(RaidBuffKey.BLUE_FLOWER_FRUIT, setOf(3075), "raid_buff_blue_flower_fruit"),
  RaidBuffDefinition(RaidBuffKey.STATUE_BUFF, setOf(30767, 30764, 9002338, 9002337, 30773, 30772, 30766, 9002340, 30760, 9002339, 30770, 30771, 30768, 30765, 9002342, 9002341), "raid_buff_statue"),
  RaidBuffDefinition(RaidBuffKey.FACTION_WAR_TIME, setOf(23717, 32025), "raid_buff_faction_war_time"),
  RaidBuffDefinition(RaidBuffKey.MOONLIGHT_JUICE, setOf(23215, 9002077), "raid_buff_moonlight_juice", RaidBuffSection.LOOT),
  RaidBuffDefinition(RaidBuffKey.HUNTING_ELIXIR, setOf(22516, 22941, 22929, 31422, 8000681, 8000803, 9001658), "raid_buff_hunting_elixir", RaidBuffSection.LOOT),
  RaidBuffDefinition(RaidBuffKey.CHOCOLATE, setOf(8000726, 9001956, 8000779, 8000794, 8000795, 8000796), "raid_buff_chocolate", RaidBuffSection.LOOT),
  RaidBuffDefinition(RaidBuffKey.LOOT_CAKE, setOf(23492, 23491), "raid_buff_loot_cake", RaidBuffSection.LOOT),
  RaidBuffDefinition(RaidBuffKey.SHORTBREAD_COOKIE, setOf(22292), "raid_buff_shortbread_cookie", RaidBuffSection.LOOT),
  RaidBuffDefinition(RaidBuffKey.GOLDEN_TAFFY, setOf(23094, 23093, 31322), "raid_buff_golden_taffy", RaidBuffSection.LOOT),
  RaidBuffDefinition(RaidBuffKey.EGG_OF_FORTUNE, setOf(8002787), "raid_buff_egg_of_fortune", RaidBuffSection.LOOT)
)

private val definitionsByKey = RAID_BUFF_DEFINITIONS.associateBy { it.key }

fun RaidBuffDefinition.matches(buffIds: Set<Int>, requireOrange: Boolean = false, allowMeatballs: Boolean = false, requireEnhanced: Boolean = false): Boolean {
  val acceptedIds = when {
    key == RaidBuffKey.GOBLET && requireOrange -> orangeIds
    key == RaidBuffKey.LONGING && requireEnhanced -> enhancedIds
    key == RaidBuffKey.FEAST_RIBS && !allowMeatballs -> ids - meatballIds
    else -> ids
  }
  return buffIds.any { it in acceptedIds }
}

data class RaidBuffRequirements(
  val selected: Set<RaidBuffKey> = emptySet(),
  val requireOrangeGoblet: Boolean = false,
  val allowMeatballs: Boolean = false,
  val requireEnhancedLonging: Boolean = false,
  // Combined loot-buff percentage threshold. 0 = "Check for loot buffs?" is disabled.
  // When > 0, a player is only considered buffed if the sum of their loot-buff amounts
  // is greater than or equal to this threshold (range 0-500).
  val lootThreshold: Int = 0
)

fun RaidBuffRequirements.matches(member: RaidFramePayload): Boolean {
  val ids = member.buffs.map { it.buff_id }.toSet()
  val mainMatches = selected.filter { definitionsByKey[it]?.section == RaidBuffSection.MAIN }.all { key ->
    definitionsByKey.getValue(key).matches(ids, requireOrangeGoblet, allowMeatballs, requireEnhancedLonging)
  }
  val lootAmount = lootBuffAmountForIds(ids)
  val lootOk = lootThreshold <= 0 || lootAmount >= lootThreshold
  return mainMatches && lootOk
}

fun RaidBuffRequirements.matchesResolved(member: RaidFramePayload, gracePeriod: RaidBuffGracePeriod): Boolean {
  val observation = PlayerCacheInteractor.resolveRaidBuffObservation(member, gracePeriod)
  val snapshot = observation.snapshot ?: return false
  return matches(member.copy(buffs = snapshot.buffIds.map { id -> com.reoky.raidframer.core.serialization.BuffPayload(buff_id = id) }))
}

fun RaidBuffRequirements.matchedDefinitions(member: RaidFramePayload): List<RaidBuffDefinition> {
  val ids = member.buffs.map { it.buff_id }.toSet()
  return RAID_BUFF_DEFINITIONS.filter { definition ->
    val acceptedIds = when {
      definition.key == RaidBuffKey.FEAST_RIBS && !allowMeatballs -> definition.ids - definition.meatballIds
      definition.key == RaidBuffKey.GOBLET && requireOrangeGoblet -> definition.orangeIds
      definition.key == RaidBuffKey.LONGING && requireEnhancedLonging -> definition.enhancedIds
      else -> definition.ids
    }
    acceptedIds.any(ids::contains)
  }
}

fun RaidBuffRequirements.missingKeys(member: RaidFramePayload): List<RaidBuffKey> {
  val ids = member.buffs.map { it.buff_id }.toSet()
  val missing = selected.mapNotNull { key ->
    val definition = definitionsByKey[key] ?: return@mapNotNull null
    if (definition.matches(ids, requireOrangeGoblet, allowMeatballs, requireEnhancedLonging)) null else key
  }.toMutableList()
  val lootAmount = lootBuffAmountForIds(ids)
  if (lootThreshold > 0 && lootAmount < lootThreshold) missing += RaidBuffKey.MOONLIGHT_JUICE
  return missing
}

fun RaidBuffDefinition.labelKey(): String = labelKey

fun raidBuffDefinition(key: RaidBuffKey): RaidBuffDefinition = definitionsByKey.getValue(key)
