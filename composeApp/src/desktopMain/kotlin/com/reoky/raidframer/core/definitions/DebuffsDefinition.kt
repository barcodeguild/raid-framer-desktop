package com.reoky.raidframer.core.definitions

/*
 * Contains a list of debuffs that are considered to be important for tracking. This is separate because debuffs have many skills
 * that can apply them, and we want to keep the list manageable instead of searching for any skills that apply a given debuff.
 */

data class Debuff(
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

data class DebuffsDefinition(
  override val debuffs: List<Debuff> = listOf(
    Debuff(name = "Charmed", consideredCC = false),
    Debuff(name = "Silence", consideredCC = true), // can move but can't cast
    Debuff(name = "Distressed", consideredCC = true), // reduced movement speed, can't heal
    Debuff(name = "Slow", consideredCC = true), // reduced movement speed
    Debuff(name = "Tripped", consideredCC = true), // can't move
    Debuff(name = "Stunned", consideredCC = true), // can't move can't cast
    Debuff(name = "Impaled", consideredCC = true), // can't move
    Debuff(name = "Flame Hell Spear", consideredCC = true), // same as impaled
    Debuff(name = "Frozen", consideredCC = true), // can't move can't cast
    Debuff(name = "Freeze", consideredCC = true), // can't move can't cast (tracks)
    Debuff(name = "Petrification", consideredCC = true), // can't move or cast
    Debuff(name = "Knockdown", consideredCC = true), // can't move or cast
    Debuff(name = "Sleep", consideredCC = true), // can't move or cast
    Debuff(name = "Fear", consideredCC = true), // can't cast, move uncontrollably
    Debuff(name = "Provoked", consideredCC = true), // can't target others
    Debuff(name = "Bubble Trap", consideredCC = true), // can't move or cast
    Debuff(name = "Snare", consideredCC = true), // target can't move
    Debuff(name = "Banshee Wail", consideredCC = true), // feared (the spell also applies slow)
    Debuff(name = "Phantasm's Wail", consideredCC = true), // feared (aoe spell also applies slow)
    Debuff(name = "Concussion", consideredCC = true), // prevents target from standing
    Debuff(name = "Shackle", consideredCC = true), // Melee and range skills are restrained for 2 sec.
    Debuff(name = "Overpowered", consideredCC = true), // Prevents movement and periodically deals Melee Damage.
    Debuff(name = "Wraith's Curse", consideredCC = true), // Similar to slow but also reduces attack speed and cast time.
    Debuff(name = "Crippling Mire", consideredCC = true), // Reduces movement speed significantly.
    Debuff(name = "Earthen Grip", consideredCC = true), // prevents movement and turning
    Debuff(name = "Dragonfire", consideredCC = true), // BD glider trips people
    Debuff(name = "Freezing", consideredCC = true), // crystal wings freeze slows movement
    Debuff(name = "Sonic Wave", consideredCC = true), // prevents targeting
    Debuff(name = "Focal Concussion", consideredCC = true), // it's a sleep, but the buff isn't the sleep debuff
    Debuff(name = "Quake Dahuta's Breath", consideredCC = true), // pushes
    Debuff(name = "Dahuta's Breath", consideredCC = true), // pushes / pulls enough to pull someone off the castle walls, so that's pvp I guess
    Debuff(name = "Staggered", consideredCC = true), // player can't move or cast for a short time
    Debuff(name = "Disables Right-Hand weapon", consideredCC = true), // Ring toss
    Debuff(name = "Disable Left-Hand weapon", consideredCC = true), // shield strip
    Debuff(name = "Bracing Blast Immunity", consideredCC = true), // result of bracing blast that pushes
    Debuff(name = "Dragon Roar", consideredCC = true) // result of a dragoon roar that knocks down gliders
  )
) : DebuffsDefinitions()