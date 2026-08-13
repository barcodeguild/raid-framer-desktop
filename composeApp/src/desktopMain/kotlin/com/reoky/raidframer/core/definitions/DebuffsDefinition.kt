package com.reoky.raidframer.core.definitions

/*
 * Contains a list of debuffs that are considered to be important for tracking. This is separate because debuffs have many skills
 * that can apply them, and we want to keep the list manageable instead of searching for any skills that apply a given debuff.
 */

data class Debuff(
  val ids: List<Int> = emptyList(),
  val name: String,
  val consideredCC: Boolean
)

abstract class DebuffsDefinitions {
  abstract val debuffs: List<Debuff>
}

fun findDebuffByName(query: String): Debuff? {
  val q = query.trim()
  return DebuffsDefinition().debuffs.find { debuff ->
    debuff.name.equals(q, ignoreCase = true)
  }
}

fun findDebuffById(id: Int): Debuff? {
  return DebuffsDefinition().debuffs.find { debuff ->
    id in debuff.ids
  }
}

val charmedDebuffIds = listOf(771, 13916, 15995, 21432, 21434, 21162)
val silencedDebuffIds = listOf(245, 257, 266, 1098 , 1177, 2115, 2116, 2743, 3868, 3928, 4039, 5525, 6147, 6366, 6893, 6981, 7040, 7400, 14730, 15721, 15937, 16100, 16989, 21161, 21987, 22013, 22239, 22520, 22538, 23358, 23469, 23523, 23524, 23815, 24168, 25234, 25718, 26965, 27145, 27345, 27681, 28595, 28646, 28676, 28682, 28683, 29667, 29668, 29926, 29987, 30935, 31862)
val distressedDebuffIds = listOf(828, 6896, 14284, 15175, 24925)
val tigerStrikeDebuffIds = listOf(22253)
val freezeDebuffIds = listOf(93, 94, 21990, 2279, 9000173, 9000156)
val gliderUsageDebuffIds = listOf(4622, 20121, 8000279)
val trippedDebuffIds = listOf(27631, 27632, 138, 141, 17423, 21, 505, 5410, 7)
val bubbleTrapDebuffIds = listOf(21401, 2286)
val bracingBlastImmunityBuffIds = listOf(7105)
val shieldStripDebuffIds = listOf(23654, 23066, 31837)
val weaponDisablesDebuffIds = listOf(23107)
val potionDisablesDebuffIds = listOf(24543)
val bdGliderDebuffIds = listOf(6670)
val crystalWingsDebuffIds = listOf(8000605)
val gliderDisablesDebuffIds = listOf(24544, 22602)
val provokedDebuffIds = listOf(502, 24060)
val defianceBuffIds = listOf(22968, 30018)
val gardenDefianceCastId = 44385
// Purge cast ID: 10712. Purge is counted from the gained buff ID below.
val purgeBuffId = 23347
val sacrificeBuffIds = listOf(30098, 30137, 30141, 30142)
val deepTranquilityBuffId = 29951
val deedendDebuffIds = listOf(29954)
val manaBarrierBuffIds = listOf(16870, 16871, 16872)
val throwDaggerDebuffIds = listOf(6829)
val absorbLifeforceDebuffIds = listOf(23360)
val blindedByCrowsDebuffIds = listOf(4807, 20934)
val mistSunderDebuffIds = listOf(24562)
val corrosiveBarrageDebuffIds = listOf(26722, 29645)
val regularSunderBuffIds = listOf(2596)
val impaleImmunityBuffIds = listOf(5007, 5669, 5929, 6415, 22550)
val protectiveWingsBuffIds = listOf(21630, 258)
val courageousActionBuffIds = listOf(499)
val reviveSpellIds = listOf(10546, 15066, 25518, 36745, 36748, 8002318)
val stunDebuffIds = listOf(243, 416, 501, 1786, 2510, 2846, 3127, 3601, 4825, 4827, 6873, 6892, 18470, 20815, 20825, 20936, 22519, 22532, 23958, 23961, 24196, 24411, 26964, 27707, 29273, 8000340, 8000344, 21361, 27279, 27575, 31372)
val staggerDebuffIds = listOf(26097, 1449, 22900, 26351, 22375, 29903)
val petrificationDebuffIds = listOf(24391, 23198, 27124, 23061)

data class DebuffsDefinition(
  override val debuffs: List<Debuff> = listOf(
    Debuff(ids = listOf(771, 21432, 21434), name = "Charmed", consideredCC = false),
    Debuff(ids = listOf(23358, 245, 266, 21987, 22013, 22520, 23523, 23524, 28595, 28676, 29667, 29668, 31862), name = "Silence", consideredCC = true), // can move but can't cast
    Debuff(ids = listOf(828), name = "Distressed", consideredCC = true), // reduced movement speed, can't heal
    Debuff(ids = listOf(5784, 21461, 21462, 20997, 2309, 23149, 23307, 23962, 26932, 2723, 27706, 2778, 28677, 28686, 28701, 29303, 4693), name = "Slow", consideredCC = true), // reduced movement speed
    Debuff(ids = listOf(27631, 27632, 138, 141, 17423, 21, 505, 5410, 7), name = "Tripped", consideredCC = true), // can't move
    Debuff(ids = listOf(7649), name = "Weak (Rank 4)", consideredCC = true), // increases confinement duration
    // stun ids before validation 243, 416, 443, 501, 509, 913, 925, 991, 1208, 1525, 1561, 1596, 1711, 1786, 15220, 15228, 7191, 7335, 6359, 6360, 6873, 6892, 15731, 15766, 15767, 16257, 16502, 16503, 5135, 5297, 5337, 5352, 5371, 5376, 5477, 5499, 5639, 5947, 2107, 2108, 2109, 2110, 2219, 2228, 2244, 2306, 2510, 2541, 2736, 2819, 2820, 2821, 2822, 2843, 2844, 2845, 2846, 3127, 3601, 3934, 3894, 3960, 3986, 4064, 4186, 4202, 4299, 4429, 4503, 4825, 4827, 4945, 4963, 26173, 26801, 26936, 26964, 20681, 20778, 20780, 20815, 20825, 20936, 21361, 23958, 23961, 24196, 24606, 24617, 24912, 25692, 25712, 23359, 23710, 23818, 27279, 27333, 27362, 27575, 27707, 27850, 17903, 18470, 21982, 21983, 21991, 22519, 22532, 16873, 16953, 16959, 16960, 16999, 17421, 20418, 29273, 29896, 29925, 30726, 30910, 30915, 31372, 31733, 32112, 8000340, 8000344, 9000770, 9002141, 3443, 4091, 26167, 23986, 24411
    // after stun validation:
    Debuff(ids = listOf(243, 416, 501, 1786, 2510, 2846, 3127, 3601, 4825, 4827, 6873, 6892, 18470, 20815, 20825, 20936, 22519, 22532, 23958, 23961, 24196, 24411, 26964, 27707, 29273, 8000340, 8000344, 21361, 27279, 27575, 31372), name = "Stun", consideredCC = true), // can't move can't cast (alternate stun variants)
    Debuff(ids = listOf(18401, 22685, 23642, 23956), name = "Impaled", consideredCC = true), // can't move
    Debuff(ids = listOf(23361), name = "Flame Hell Spear", consideredCC = true), // same as impaled
    Debuff(ids = listOf(93, 21990, 2279, 9000173, 9000156), name = "Freeze", consideredCC = true), // can't move can't cast (tracks)
    Debuff(ids = listOf(15216, 9000169), name = "Deep Freeze", consideredCC = true), // can't move can't cast (tracks)
    Debuff(ids = listOf(24586), name = "Frostbite", consideredCC = true), // can't move can't cast (flame barrier)
    Debuff(ids = listOf(94), name = "Ice Shard", consideredCC = true), // can't move can't cast (tracks)
    Debuff(ids = listOf(21557), name = "Greater Shock", consideredCC = true), // can't move can't cast (tracks)
    Debuff(ids = listOf(24391, 23198, 27124, 23061), name = "Petrification", consideredCC = true), // can't move or cast
    Debuff(ids = listOf(9000769), name = "Knockdown", consideredCC = true), // can't move or cast
    Debuff(ids = listOf(523, 23959, 28251), name = "Sleep", consideredCC = true), // can't move or cast
    Debuff(ids = listOf(156, 2277, 24375, 29304), name = "Fear", consideredCC = true), // can't cast, move uncontrollably
    Debuff(ids = listOf(502, 24060), name = "Provoked", consideredCC = true), // can't target others
    Debuff(ids = listOf(21401, 2286), name = "Bubble Trap", consideredCC = true), // can't move or cast
    // Debuff(ids = listOf(114, 2275), name = "Lassitude", consideredCC = true), // inflicts sleep (523) so no need for duplicate
    Debuff(ids = listOf(4843, 22627, 24494, 2458, 3719, 883), name = "Snare", consideredCC = true), // target can't move
    Debuff(ids = listOf(21402, 21403, 2278, 975), name = "Banshee Wail", consideredCC = true), // feared (the spell also applies slow)
    Debuff(ids = listOf(21404, 21405), name = "Phantasm's Wail", consideredCC = true), // feared (aoe spell also applies slow)
    Debuff(ids = listOf(206, 29302), name = "Shackle", consideredCC = true), // Melee and range skills are restrained for 2 sec.
    Debuff(ids = listOf(22253), name = "Overpowered", consideredCC = true), // Prevents movement and periodically deals Melee Damage.
    Debuff(ids = listOf(477, 18388, 23362, 23427, 23842), name = "Wraith's Curse", consideredCC = true), // Similar to slow but also reduces attack speed and cast time.
    Debuff(ids = listOf(16498, 26512, 26473, 26743), name = "Crippling Mire", consideredCC = true), // Reduces movement speed significantly.
    Debuff(ids = listOf(21397, 82, 26455, 21398), name = "Earthen Grip", consideredCC = true), // prevents movement and turning
    Debuff(ids = listOf(6670), name = "Dragonfire", consideredCC = true), // BD glider trips people
    Debuff(ids = listOf(8000605), name = "Freezing", consideredCC = true), // crystal wings freeze slows movement
    Debuff(ids = listOf(18950), name = "Sonic Wave", consideredCC = true), // prevents targeting
    Debuff(ids = listOf(2871, 2872, 2873, 2874, 2875), name = "Focal Concussion", consideredCC = true), // it's a sleep, but the buff isn't the sleep debuff
    Debuff(ids = listOf(1366, 24051, 31626), name = "Dahuta's Breath", consideredCC = true), // pushes / pulls enough to pull someone off the castle walls, so that's pvp I guess
    Debuff(ids = listOf(26097, 1449, 22900, 26351, 22375, 29903), name = "Staggered", consideredCC = true), // player can't move or cast for a short time
    Debuff(ids = listOf(23107), name = "Disables Right-Hand weapon", consideredCC = true), // Ring toss
    Debuff(ids = listOf(23654, 23066, 31837), name = "Disable Left-Hand weapon", consideredCC = true), // shield strip
    Debuff(ids = listOf(7105), name = "Bracing Blast Immunity", consideredCC = true), // result of bracing blast that pushes
    Debuff(ids = listOf(22602), name = "Dragon Roar", consideredCC = true), // result of a dragoon roar that knocks down gliders
    Debuff(ids = listOf(4844), name = "Dominator's Curse", consideredCC = true), // Kraken Scepter
    Debuff(ids = listOf(24543), name = "Unable to use Potions", consideredCC = true), // Banishment Combo
    Debuff(ids = listOf(24544), name = "Glider Disabled", consideredCC = true), // Banishment Combo
    Debuff(ids = listOf(25230, 22509), name = "Decreases Move Speed", consideredCC = true), // Decreases Move Speed (id:25230) from Absorb Lifeforce: Wave
    Debuff(ids = listOf(23305), name = "Trip", consideredCC = true), // trip variant
  )
) : DebuffsDefinitions()
