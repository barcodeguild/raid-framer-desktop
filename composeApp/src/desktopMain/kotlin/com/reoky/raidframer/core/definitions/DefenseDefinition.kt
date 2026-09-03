package com.reoky.raidframer.core.definitions

object DefenseDefinition : SkillTreeDefinition {
  override val gameId = 3
  override val tree = SkillTreeType.DEFENSE
  override val skills = listOf(
    Skill(0, "Shield Slam", 0.0, 2.0, listOf("Shield Slam"), possibleCastIDs = listOf(23701, 45462)),
    Skill(1, "Toughen", 0.0, 60.0, listOf("Toughen"), possibleCastIDs = listOf(11365, 23165)),
    Skill(2, "Bull Rush", 0.0, 30.0, listOf("Bull Rush"), possibleCastIDs = listOf(10501, 23702)),
    Skill(3, "Boastful Roar", 0.0, 24.0, listOf("Boastful Roar"), possibleCastIDs = listOf(12048, 23703, 46007, 46009)),
    Skill(4, "Lasso", 0.2, 30.0, listOf("Lasso"), possibleCastIDs = listOf(15027, 20276, 35555, 35682, 42817, 43801, 9001752)),
    Skill(5, "Redoubt", 0.0, 30.0, listOf("Redoubt"), possibleCastIDs = listOf(10375, 23700, 36458, 36459, 46075)),
    Skill(6, "Mocking Howl", 0.0, 24.0, listOf("Mocking Howl"), possibleCastIDs = listOf(10436, 40578)),
    Skill(7, "Refreshment", 0.0, 120.0, listOf("Refreshment"), possibleCastIDs = listOf(10645)),
    Skill(8, "Retribution", 0.0, 30.0, listOf("Retribution"), possibleCastIDs = listOf(10655, 19555, 19971, 38634, 40575, 40779, 40780, 40808, 40809, 40930, 45840)),
    Skill(9, "Revitalizing Cheer", 0.0, 30.0, listOf("Revitalizing Cheer"), possibleCastIDs = listOf(12046, 42857, 42858, 46136)),
    Skill(10, "Imprison", 0.0, 60.0, listOf("Imprison"), possibleCastIDs = listOf(14529, 36460)),
    Skill(11, "Invincibility", 0.0, 60.0, listOf("Invincibility"), possibleCastIDs = listOf(10372, 29329, 9002020))
  )
}
