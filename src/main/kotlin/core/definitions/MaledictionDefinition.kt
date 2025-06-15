package core.definitions

object MaledictionDefinition : SkillTreeDefinition {

  override val id = 10
  override val name = "Malediction"
  override val skills = listOf(
    Skill(0, "Mana Bolts", 0.0, 0.0, listOf("Mana Bolts")),
    Skill(1, "Serpent's Glare", 0.4, 60.0, listOf("Crashing Wave")),
    Skill(2, "Serpent's Bite", 0.0, 21.0, listOf("Serpent Bite")),
    Skill(3, "Malicious Binding", 0.0, 21.0, listOf("")),
    Skill(4, "Fury", 0.0, 30.0, listOf("")),
    Skill(5, "Soulbound Edge", 0.0, 15.0, listOf("Soulbound Edge")),
    Skill(6, "Ghastly Pack", 0.0, 18.0, listOf("Ghastly Pack")),
    Skill(7, "Grasping Void", 0.9, 60.0, listOf("Grasping Void")),
    Skill(8, "Void Surge", 0.0, 21.0, listOf("Void Surge")),
    Skill(9, "Ring Throw", 0.0, 28.0, listOf("Ring Throw")),
    Skill(10, "Shadow Cloak", 0.0, 70.0, listOf("")),
    Skill(11, "Bladefall", 0.0, 23.0, listOf("Bladefall")),
  )

}