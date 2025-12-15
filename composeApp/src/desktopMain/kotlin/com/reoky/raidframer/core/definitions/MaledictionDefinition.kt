package com.reoky.raidframer.core.definitions

object MaledictionDefinition : SkillTreeDefinition {

  override val tree = SkillTreeType.MALEDICTION
  override val skills = listOf(
    Skill(0, "Mana Bolts", 0.0, 0.0, false, listOf("Mana Bolts")),
    Skill(1, "Serpent's Glare", 0.4, 60.0, false, listOf("Crashing Wave")),
    Skill(2, "Serpent's Bite", 0.0, 21.0, true,listOf("Serpent Bite")),
    Skill(3, "Malicious Binding", 0.2, 21.0, true, listOf("")),
    Skill(4, "Fury", 0.0, 30.0, false,listOf("")),
    Skill(5, "Soulbound Edge", 0.0, 15.0, false, listOf("Soulbound Edge")),
    Skill(6, "Ghastly Pack", 0.0, 18.0, true, listOf("Ghastly Pack")),
    Skill(7, "Grasping Void", 1.0, 60.0, true,listOf("Grasping Void")),
    Skill(8, "Void Surge", 0.0, 21.0, false,listOf("Void Surge")),
    Skill(9, "Ring Throw", 0.0, 28.0, true, listOf("Ring Throw")),
    Skill(10, "Shadow Cloak", 0.0, 70.0, false,listOf("")),
    Skill(11, "Bladefall", 0.0, 23.0, false,listOf("Bladefall")),
  )
}