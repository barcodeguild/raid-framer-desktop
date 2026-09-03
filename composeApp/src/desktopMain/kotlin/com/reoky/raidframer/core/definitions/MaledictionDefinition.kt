package com.reoky.raidframer.core.definitions

object MaledictionDefinition : SkillTreeDefinition {
  override val gameId = 11
  override val tree = SkillTreeType.MALEDICTION
  override val skills = listOf(
    Skill(0, "Mana Bolts", 0.0, 0.0, listOf("Mana Bolts"), possibleCastIDs = listOf(15420)),
    Skill(1, "Serpent's Glare", 0.4, 60.0, listOf("Crashing Wave"), possibleCastIDs = listOf()),
    Skill(2, "Serpent's Bite", 0.0, 21.0, listOf("Serpent Bite"), possibleCastIDs = listOf()),
    Skill(3, "Malicious Binding", 0.2, 21.0, listOf(""), possibleCastIDs = listOf()),
    Skill(4, "Fury", 0.0, 30.0, listOf(""), possibleCastIDs = listOf(16011, 39018)),
    Skill(5, "Soulbound Edge", 0.0, 15.0, listOf("Soulbound Edge"), possibleCastIDs = listOf(39015)),
    Skill(6, "Ghastly Pack", 0.0, 18.0, listOf("Ghastly Pack"), possibleCastIDs = listOf()),
    Skill(7, "Grasping Void", 1.0, 60.0, listOf("Grasping Void"), possibleCastIDs = listOf()),
    Skill(8, "Void Surge", 0.0, 21.0, listOf("Void Surge"), possibleCastIDs = listOf()),
    Skill(9, "Ring Throw", 0.0, 28.0, listOf("Ring Throw"), possibleCastIDs = listOf()),
    Skill(10, "Shadow Cloak", 0.0, 70.0, listOf(""), possibleCastIDs = listOf(39238)),
    Skill(11, "Bladefall", 0.0, 23.0, listOf("Bladefall"), possibleCastIDs = listOf()),
  )
}