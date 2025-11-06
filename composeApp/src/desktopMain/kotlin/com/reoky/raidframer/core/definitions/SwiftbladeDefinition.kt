package com.reoky.raidframer.core.definitions

object SwiftbladeDefinition : SkillTreeDefinition {

  override val tree = SkillTreeType.SWIFTBLADE
  override val skills = listOf(
    Skill(0, "Blade Flurry", 0.0, 0.0, listOf("Blade Flurry")), // no casting
    Skill(1, "Crescent Slice", 0.0, 12.6, listOf("Crescent Slice")), // no casting
    Skill(2, "Sinister Strike", 0.0, 16.8, listOf("Sinister Strike")),
    Skill(3, "Blink", 0.0, 16.8, listOf("Blick")),
    Skill(4, "Relentless Assault", 0.0, 8.4, listOf("Relentless Assault")), // no casting
    Skill(5, "Reverberate", 0.0, 42.0, listOf("Reverberate")), // nothing
    Skill(6, "Entangle", 0.0, 16.8, listOf("Entangle")),
    Skill(7, "Dusk Shroud", 0.0, 28.0, listOf("Dusk Shroud")), // nothing
    Skill(8, "Fleeting Footsteps", 0.0, 38.5, listOf("Fleeting Footsteps")),
    Skill(9, "Bladeblast", 0.0, 12.6, listOf("Bladeblast")), // no casting
    Skill(10, "Primal Strike", 0.0, 21.0, listOf("Primal Strike", "Primal Strike: Wave")),
    Skill(11, "Twin Shadow", 0.0, 14.7, listOf("Twin Shadow")) // no casting
  )
}
