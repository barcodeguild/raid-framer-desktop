package com.reoky.raidframer.core.definitions

object AuramancyDefinition : SkillTreeDefinition {
  override val gameId = 4
  override val tree = SkillTreeType.AURAMANCY
  override val skills = listOf(
    Skill(0, "Thwart", 0.0, 15.0, listOf("Thwart"), possibleCastIDs = listOf(16486, 36462, 36463)),
    Skill(1, "Comet's Boon", 0.0, 12.0, listOf("Comet's Boon"), possibleCastIDs = listOf(23712)),
    Skill(2, "Conversion Shield", 1.3, 30.0, listOf("Conversion Shield"), possibleCastIDs = listOf(11869, 23710, 36464, 36465, 46137)),
    Skill(3, "Vicious Implosion", 0.0, 15.0, listOf("Vicious Implosion"), possibleCastIDs = listOf(23709)),
    Skill(4, "Teleportation", 0.0, 35.0, listOf("Teleportation"), possibleCastIDs = listOf(10152, 21094, 23708, 32618, 37870, 39293, 39294, 8001296, 9003113)),
    Skill(5, "Courageous Action", 0.0, 30.0, listOf("Courageous Action"), possibleCastIDs = listOf(11424)),
    Skill(6, "Meditate", 0.0, 45.0, listOf("Meditate"), possibleCastIDs = listOf(11989)),
    Skill(7, "Shrug It Off", 0.0, 80.0, listOf("Shrug It Off"), possibleCastIDs = listOf(11429)),
    Skill(8, "Health Lift", 0.0, 90.0, listOf("Health Lift"), possibleCastIDs = listOf(47342)),
    Skill(9, "Banishment", 0.0, 40.0, listOf("Banishment"), possibleCastIDs = listOf()), // buff only
    Skill(10, "Protective Wings", 1.3, 30.0, listOf("Protective Wings"), possibleCastIDs = listOf(10714, 36466, 36467, 46138)),
    Skill(11, "Bracing Blast", 0.0, 45.0, listOf("Bracing Blast"), possibleCastIDs = listOf(9002796))
  )
}
