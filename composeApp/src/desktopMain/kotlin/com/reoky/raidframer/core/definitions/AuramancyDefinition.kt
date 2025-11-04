package com.reoky.raidframer.core.definitions

object AuramancyDefinition : SkillTreeDefinition {

  override val tree = SkillTreeType.AURAMANCY
  override val skills = listOf(
    Skill(0, "Thwart", 0.0, 15.0, listOf("Thwart")),
    Skill(1, "Comet's Boon", 0.0, 12.0, listOf("Comet's Boon")), // no casting
    Skill(2, "Conversion Shield", 1.3, 30.0, listOf("Conversion Shield")),
    Skill(3, "Vicious Implosion", 0.0, 15.0, listOf("Vicious Implosion")), // no casting
    Skill(4, "Teleportation", 0.0, 35.0, listOf("Teleportation")),
    Skill(5, "Courageous Action", 0.0, 30.0, listOf("Courageous Action")),
    Skill(6, "Meditate", 0.0, 45.0, listOf("Meditate")),
    Skill(7, "Shrug It Off", 0.9, 80.0, listOf("Shrug It Off")),
    Skill(8, "Health Lift", 0.0, 90.0, listOf("Health Lift")),
    Skill(9, "Banishment", 0.0, 40.0, listOf("Banishment")), // no casting, no damage, buff only
    Skill(10, "Protective Wings", 1.3, 30.0, listOf("Protective Wings")), // assuming stone version
    Skill(11, "Bracing Blast", 0.0, 45.0, listOf("Bracing Blast")) // no casting, no damage, no-buff, immunity only
  )
}
