package com.reoky.raidframer.core.definitions

object ArcheryDefinition : SkillTreeDefinition {
  override val gameId = 6
  override val tree = SkillTreeType.ARCHERY
  override val skills = listOf(
    Skill(0, "Endless Arrows", 0.0, 0.0, false, listOf("Endless Arrows")), // no casting
    Skill(1, "Charged Bolt", 0.0, 12.0, false, listOf("Charged Bolt")),
    Skill(2, "Steady Shooting", 0.0, 30.0, false, listOf("Steady Shooting")),
    Skill(3, "Fending Arrow", 0.0, 21.0, false, listOf("Fending Arrow")), // no casting
    Skill(4, "Blazing Arrow", 0.0, 9.0, false, listOf("Blazing Arrow")), // no casting
    Skill(5, "Snare", 0.0, 24.0, true,listOf("Snare")),
    Skill(6, "Deadeye", 0.0, 80.0, false, listOf("Deadeye")),
    Skill(7, "Concussive Arrow", 3.4, 30.0, false, listOf("Concussive Arrow")), // no casting
    Skill(8, "Hunter's Guile", 0.0, 60.0, false, listOf("Hunter's Guile")),
    Skill(9, "Double Recurve", 0.0, 90.0, false, listOf("Double Recurve")),
    Skill(10, "Missile Rain", 0.0, 0.0, false,listOf("Missile Rain")), // no castings
    Skill(11, "Snipe", 4.3, 27.0, false,listOf("Snipe")) // no casting
  )
}
