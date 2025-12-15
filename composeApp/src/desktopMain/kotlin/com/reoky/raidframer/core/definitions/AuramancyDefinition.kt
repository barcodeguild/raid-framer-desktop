package com.reoky.raidframer.core.definitions

object AuramancyDefinition : SkillTreeDefinition {

  override val tree = SkillTreeType.AURAMANCY
  override val skills = listOf(
    Skill(0, "Thwart", 0.0, 15.0, false, listOf("Thwart")),
    Skill(1, "Comet's Boon", 0.0, 12.0, false, listOf("Comet's Boon")), // no casting
    Skill(2, "Conversion Shield", 1.3, 30.0, false, listOf("Conversion Shield")),
    Skill(3, "Vicious Implosion", 0.0, 15.0, true, listOf("Vicious Implosion")), // no casting
    Skill(4, "Teleportation", 0.0, 35.0, false,listOf("Teleportation")),
    Skill(5, "Courageous Action", 0.0, 30.0, false, listOf("Courageous Action")),
    Skill(6, "Meditate", 0.0, 45.0, false,listOf("Meditate")),
    Skill(7, "Shrug It Off", 0.0, 80.0, false, listOf("Shrug It Off")),
    Skill(8, "Health Lift", 0.0, 90.0, false, listOf("Health Lift")),
    Skill(9, "Banishment", 0.0, 40.0, true, listOf("Banishment")), // no casting, no damage, buff only
    Skill(10, "Protective Wings", 1.3, 30.0, false, listOf("Protective Wings")), // assuming stone version
    Skill(11, "Bracing Blast", 0.0, 45.0, true, listOf("Bracing Blast")) // no casting, no damage, no-buff, immunity only
  )
}
