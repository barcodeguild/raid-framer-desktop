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
val trippedDebuffIds = listOf(27631)
val bubbleTrapDebuffIds = listOf(21401)
val bracingBlastImmunityBuffIds = listOf(7105)
val shieldStripDebuffIds = listOf(23654, 23066, 31837)
val weaponDisablesDebuffIds = listOf(23107)
val potionDisablesDebuffIds = listOf(24543)
val bdGliderDebuffIds = listOf(6670)
val crystalWingsDebuffIds = listOf(8000605)
val gliderDisablesDebuffIds = listOf(24544, 22602)
val provokedDebuffIds = listOf(502, 24060)

data class DebuffsDefinition(
  override val debuffs: List<Debuff> = listOf(
    Debuff(ids = listOf(771, 21432), name = "Charmed", consideredCC = false),
    Debuff(ids = listOf(23358), name = "Silence", consideredCC = true), // can move but can't cast
    Debuff(ids = listOf(828), name = "Distressed", consideredCC = true), // reduced movement speed, can't heal
    Debuff(ids = listOf(5784, 21461), name = "Slow", consideredCC = true), // reduced movement speed
    Debuff(ids = listOf(27631), name = "Tripped", consideredCC = true), // can't move
    Debuff(ids = listOf(7649), name = "Weak (Rank 4)", consideredCC = true), // increases confinement duration
    Debuff(ids = listOf(), name = "Stunned", consideredCC = true), // can't move can't cast
    Debuff(ids = listOf(), name = "Impaled", consideredCC = true), // can't move
    Debuff(ids = listOf(), name = "Flame Hell Spear", consideredCC = true), // same as impaled
    Debuff(ids = listOf(), name = "Frozen", consideredCC = true), // can't move can't cast
    Debuff(ids = listOf(93, 21990, 2279, 9000173, 9000156), name = "Freeze", consideredCC = true), // can't move can't cast (tracks)
    Debuff(ids = listOf(15216, 9000169), name = "Deep Freeze", consideredCC = true), // can't move can't cast (tracks)
    Debuff(ids = listOf(24586), name = "Frostbite", consideredCC = true), // can't move can't cast (flame barrier)
    Debuff(ids = listOf(94), name = "Ice Shard", consideredCC = true), // can't move can't cast (tracks)
    Debuff(ids = listOf(21557), name = "Greater Shock", consideredCC = true), // can't move can't cast (tracks)
    Debuff(ids = listOf(24391, 23198, 27124), name = "Petrification", consideredCC = true), // can't move or cast
    Debuff(ids = listOf(9000769), name = "Knockdown", consideredCC = true), // can't move or cast
    Debuff(ids = listOf(523), name = "Sleep", consideredCC = true), // can't move or cast
    Debuff(ids = listOf(156), name = "Fear", consideredCC = true), // can't cast, move uncontrollably
    Debuff(ids = listOf(502, 24060), name = "Provoked", consideredCC = true), // can't target others
    Debuff(ids = listOf(21401), name = "Bubble Trap", consideredCC = true), // can't move or cast
    // Debuff(ids = listOf(114), name = "Lassitude", consideredCC = true), // inflicts sleep (523) so no need for duplicate
    Debuff(ids = listOf(4843), name = "Snare", consideredCC = true), // target can't move
    Debuff(ids = listOf(21402), name = "Banshee Wail", consideredCC = true), // feared (the spell also applies slow)
    // Debuff(ids = listOf(), name = "Phantasm's Wail", consideredCC = true), // feared (aoe spell also applies slow) source is the npc banshee and not the banshee's owner
    Debuff(ids = listOf(), name = "Concussion", consideredCC = true), // prevents target from standing
    Debuff(ids = listOf(206), name = "Shackle", consideredCC = true), // Melee and range skills are restrained for 2 sec.
    Debuff(ids = listOf(22253), name = "Overpowered", consideredCC = true), // Prevents movement and periodically deals Melee Damage.
    Debuff(ids = listOf(477, 18388, 23362, 23427, 23842), name = "Wraith's Curse", consideredCC = true), // Similar to slow but also reduces attack speed and cast time.
    Debuff(ids = listOf(16498, 23076, 26512, 26743), name = "Crippling Mire", consideredCC = true), // Reduces movement speed significantly.
    Debuff(ids = listOf(21397, 82, 26455, 21398), name = "Earthen Grip", consideredCC = true), // prevents movement and turning
    Debuff(ids = listOf(6670), name = "Dragonfire", consideredCC = true), // BD glider trips people
    Debuff(ids = listOf(8000605), name = "Freezing", consideredCC = true), // crystal wings freeze slows movement
    Debuff(ids = listOf(18950), name = "Sonic Wave", consideredCC = true), // prevents targeting
    Debuff(ids = listOf(2871, 2872, 2873, 2874, 2875), name = "Focal Concussion", consideredCC = true), // it's a sleep, but the buff isn't the sleep debuff
    Debuff(ids = listOf(), name = "Quake Dahuta's Breath", consideredCC = true), // pushes
    Debuff(ids = listOf(1366, 6957, 24051, 24052, 24179, 24180, 24181), name = "Dahuta's Breath", consideredCC = true), // pushes / pulls enough to pull someone off the castle walls, so that's pvp I guess
    Debuff(ids = listOf(26097), name = "Staggered", consideredCC = true), // player can't move or cast for a short time
    Debuff(ids = listOf(23107), name = "Disables Right-Hand weapon", consideredCC = true), // Ring toss
    Debuff(ids = listOf(23654, 23066, 31837), name = "Disable Left-Hand weapon", consideredCC = true), // shield strip
    Debuff(ids = listOf(7105), name = "Bracing Blast Immunity", consideredCC = true), // result of bracing blast that pushes
    Debuff(ids = listOf(22602), name = "Dragon Roar", consideredCC = true), // result of a dragoon roar that knocks down gliders
    Debuff(ids = listOf(4844), name = "Dominator's Curse", consideredCC = true), // Kraken Scepter
    Debuff(ids = listOf(24543), name = "Unable to use Potions", consideredCC = true), // Banishment Combo
    Debuff(ids = listOf(24544), name = "Glider Disabled", consideredCC = true), // Banishment Combo
    Debuff(ids = listOf(25230), name = "Decreases Move Speed", consideredCC = true), // Decreases Move Speed (id:25230) from Absorb Lifeforce: Wave
  )
) : DebuffsDefinitions()