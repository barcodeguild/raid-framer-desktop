package com.reoky.raidframer.core.definitions

object VitalismDefinition : SkillTreeDefinition {
  override val gameId = 10
  override val tree = SkillTreeType.VITALISM
  override val skills = listOf(
    Skill(0, "Holy Bolt", 1.7, 0.0, false, listOf("Holy Bolt")), // no casting
    Skill(1, "Mirror Light", 0.0, 21.0, false, listOf("Mirror Light")),
    Skill(2, "Antithesis", 1.7, 3.0, false, listOf("Antithesis")), // no casting
    Skill(3, "Resurgence", 0.0, 0.0, false, listOf("Resurgence")),
    Skill(4, "Skewer", 2.6, 18.0, true, listOf("Skewer")),
    Skill(5, "Mend", 3.9, 9.0, false,listOf("Mend")),
    Skill(6, "Revive", 9.0, 0.0, false, listOf("Revive")), // ambiguous with scroll version
    Skill(7, "Fervent Healing", 0.0, 0.0, false, listOf("Fervent Healing")), // no casting
    Skill(8, "Renewal", 1.3, 0.0, false, listOf("Renewal")),
    Skill(9, "Aranzeb's Boon", 2.6, 8.0, false, listOf("Aranzeb's Boon")),
    Skill(10, "Mana Barrier", 0.0, 35.0, false,listOf("Mana Barrier")), // buff only, no casting, no dmg
    Skill(11, "Healing Circle", 0.0, 50.0, false, listOf("Healing Circle"))
  )
}
