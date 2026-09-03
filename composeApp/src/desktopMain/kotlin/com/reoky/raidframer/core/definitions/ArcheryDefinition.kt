package com.reoky.raidframer.core.definitions

object ArcheryDefinition : SkillTreeDefinition {
  override val gameId = 6
  override val tree = SkillTreeType.ARCHERY
  override val skills = listOf(
    Skill(0, "Endless Arrows", 0.0, 0.0, listOf("Endless Arrows"), possibleCastIDs = listOf()), // no casting
    Skill(1, "Charged Bolt", 0.0, 12.0, listOf("Charged Bolt"), possibleCastIDs = listOf()),
    Skill(2, "Steady Shooting", 0.0, 30.0, listOf("Steady Shooting"), possibleCastIDs = listOf(10694)),
    Skill(3, "Fending Arrow", 0.0, 21.0, listOf("Fending Arrow"), possibleCastIDs = listOf()), // no casting
    Skill(4, "Blazing Arrow", 0.0, 9.0, listOf("Blazing Arrow"), possibleCastIDs = listOf()), // no casting
    Skill(5, "Snare", 0.0, 24.0, listOf("Snare"), possibleCastIDs = listOf(12133, 40580)),
    Skill(6, "Deadeye", 0.0, 80.0, listOf("Deadeye"), possibleCastIDs = listOf(15073)),
    Skill(7, "Concussive Arrow", 3.4, 30.0, listOf("Concussive Arrow"), possibleCastIDs = listOf()), // no casting
    Skill(8, "Hunter's Guile", 0.0, 60.0, listOf("Hunter's Guile"), possibleCastIDs = listOf(10708)),
    Skill(9, "Double Recurve", 0.0, 90.0, listOf("Double Recurve"), possibleCastIDs = listOf(11368, 42851, 42849)),
    Skill(10, "Missile Rain", 0.0, 0.0, listOf("Missile Rain"), possibleCastIDs = listOf(45941)),
    Skill(11, "Snipe", 4.3, 27.0, listOf("Snipe"), possibleCastIDs = listOf(17394, 24020))
  )
}
