package com.reoky.raidframer.core.definitions

object BattlerageDefinition : SkillTreeDefinition {

  override val tree = SkillTreeType.BATTLERAGE
  override val skills = listOf(
    Skill(0, "Triple Slash", 0.0, 0.0, false, listOf("Triple Slash")), // no casting quake version
    Skill(1, "Charge", 0.0, 12.0, true, listOf("Charge")), // no casting
    Skill(2, "Battle Focus", 0.0, 90.0, false, listOf("Battle Focus")),
    Skill(3, "Whirlwind Slash", 0.0, 12.0, true, listOf("Whirlwind Slash")), // no casting
    Skill(4, "Sunder Earth", 0.0, 16.0, false, listOf("Sunder Earth")),
    Skill(5, "Frenzy", 0.0, 90.0, false, listOf("Frenzy")),
    Skill(6, "Precision Strike", 0.0, 21.0, false, listOf("Precision Strike")), // no casting ancestral versions
    Skill(7, "Tiger Strike", 0.0, 18.0, true, listOf("Tiger Strike")), // no casting
    Skill(8, "Bondbreaker", 0.0, 18.0, false, listOf("Bondbreaker")),
    Skill(9, "Terrifying Roar", 0.0, 18.0, true, listOf("Terrifying Roar")),
    Skill(10, "Hammer Toss", 0.0, 29.0, true, listOf("Hammer Toss")), // no casting (gives puncture)
    Skill(11, "Behind Enemy Lines", 0.0, 21.0, true, listOf("Behind Enemy Lines"))
  )
}
