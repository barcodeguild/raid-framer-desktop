package com.reoky.raidframer.core.definitions

object ShadowplayDefinition : SkillTreeDefinition {

  override val tree = SkillTreeType.SHADOWPLAY
  override val skills = listOf(
    Skill(0, "Rapid Strike", 0.0, 0.0, listOf("Rapid Strike")),
    Skill(1, "Poisoned Weapons", 0.0, 7.2, listOf("Poisoned Weapons")),
    Skill(2, "Pin Down", 0.0, 16.8, listOf("Pin Down")),
    Skill(3, "Drop Back", 0.0, 9.6, listOf("Drop Back")),
    Skill(4, "Overwhelm", 0.0, 14.4, listOf("Overwhelm")), // no casting
    Skill(5, "Stalker's Mark", 0.0, 21.6, listOf("Stalker's Mark")),
    Skill(6, "Wallop", 0.0, 9.6, listOf("Wallop")), // no casting
    Skill(7, "Stealth", 0.0, 25.0, listOf("Stealth")),
    Skill(8, "Freerunner", 0.0, 71.0, listOf("Freerunner")),
    Skill(9, "Shadowsmite", 0.0, 19.2, listOf("Shadowsmite")),
    Skill(10, "Leech", 0.0, 36.0, listOf("Leech")),
    Skill(11, "Throw Dagger", 0.0, 28.0, listOf("Throw Dagger")) // no casting
  )
}
