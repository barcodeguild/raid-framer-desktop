package com.reoky.raidframer.core.definitions

object UtilityDefinition : SkillItemDefinition {

  override val skills: List<Skill> = listOf(
    Skill(0, "Kraken Scepter", castTime = 0.0, cooldown = 45.0, true, listOf("Desolate Sea Sovereign")), // casting
    Skill(1, "Lib Shield", castTime = 4.0, cooldown = 45.0, true, listOf("Immortal Warden's Shield"))
  )
}
