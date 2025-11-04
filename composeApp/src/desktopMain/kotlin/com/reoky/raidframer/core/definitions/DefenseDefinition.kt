package com.reoky.raidframer.core.definitions

object DefenseDefinition : SkillTreeDefinition {

  override val tree = SkillTreeType.DEFENSE
  override val skills = listOf(
    Skill(0, "Shield Slam", 0.0, 2.0, listOf("Shield Slam")),
    Skill(1, "Toughen", 0.0, 60.0, listOf("Toughen")),
    Skill(2, "Bull Rush", 0.0, 30.0, listOf("Bull Rush")),
    Skill(3, "Boastful Roar", 0.0, 24.0, listOf("Boastful Roar")),
    Skill(4, "Lasso", 0.2, 30.0, listOf("Lasso")), // no casting, no dmg, buffs are staggered and taunted
    Skill(5, "Redoubt", 0.0, 30.0, listOf("Redoubt")),
    Skill(6, "Mocking Howl", 0.0, 24.0, listOf("Mocking Howl")),
    Skill(7, "Refreshment", 0.9, 120.0, listOf("Refreshment")),
    Skill(8, "Retribution", 0.0, 30.0, listOf("Retribution")),
    Skill(9, "Revitalizing Cheer", 0.0, 30.0, listOf("Revitalizing Cheer")),
    Skill(10, "Imprison", 0.0, 60.0, listOf("Imprison")),
    Skill(11, "Invincibility", 0.0, 60.0, listOf("Invincibility"))
  )
}
