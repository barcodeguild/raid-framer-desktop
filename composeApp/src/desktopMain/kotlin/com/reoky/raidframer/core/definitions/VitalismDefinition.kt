package com.reoky.raidframer.core.definitions

object VitalismDefinition : SkillTreeDefinition {
  override val gameId = 10
  override val tree = SkillTreeType.VITALISM
  override val skills = listOf(
    Skill(0, "Holy Bolt", 1.7, 0.0, listOf("Holy Bolt"), possibleCastIDs = listOf()), // no casting
    Skill(1, "Mirror Light", 0.0, 21.0, listOf("Mirror Light"), possibleCastIDs = listOf(11379, 23716)),
    Skill(2, "Antithesis", 1.7, 3.0, listOf("Antithesis"), possibleCastIDs = listOf(23714, 23791)),
    Skill(3, "Resurgence", 0.0, 0.0, listOf("Resurgence"), possibleCastIDs = listOf(10547, 23715, 23790)),
    Skill(4, "Skewer", 2.6, 18.0, listOf("Skewer"), possibleCastIDs = listOf(23719)),
    Skill(5, "Mend", 3.9, 9.0, listOf("Mend"), possibleCastIDs = listOf()), // no casting
    Skill(6, "Revive", 9.0, 0.0, listOf("Revive"), possibleCastIDs = listOf(10546, 15066, 36745)),
    Skill(7, "Fervent Healing", 0.0, 0.0, listOf("Fervent Healing"), possibleCastIDs = listOf(23788, 23793)),
    Skill(8, "Renewal", 1.3, 0.0, listOf("Renewal"), possibleCastIDs = listOf(17412)),
    Skill(9, "Aranzeb's Boon", 2.6, 8.0, listOf("Aranzeb's Boon"), possibleCastIDs = listOf(16004)),
    Skill(10, "Mana Barrier", 0.0, 35.0, listOf("Mana Barrier"), possibleCastIDs = listOf()), // buff only
    Skill(11, "Healing Circle", 0.0, 50.0, listOf("Healing Circle"), possibleCastIDs = listOf(11948))
  )
}
