package com.reoky.raidframer.core.definitions

object SwiftbladeDefinition : SkillTreeDefinition {
  override val gameId = 12
  override val tree = SkillTreeType.SWIFTBLADE
  override val skills = listOf(
    Skill(0, "Blade Flurry", 0.0, 0.0, false, listOf("Blade Flurry")), // no casting
    Skill(1, "Crescent Slice", 0.0, 12.6, true, listOf("Crescent Slice")), // no casting
    Skill(2, "Sinister Strike", 0.0, 16.8, false, listOf("Sinister Strike")),
    Skill(3, "Blink", 0.0, 16.8, false, listOf("Blick")),
    Skill(4, "Relentless Assault", 0.0, 8.4, false,listOf("Relentless Assault")), // no casting
    Skill(5, "Reverberate", 0.0, 42.0, false, listOf("Reverberate")), // nothing
    Skill(6, "Entangle", 0.0, 16.8, false, listOf("Entangle")),
    Skill(7, "Dusk Shroud", 0.0, 28.0, false, listOf("Dusk Shroud")), // nothing
    Skill(8, "Fleeting Footsteps", 0.0, 38.5, false,  listOf("Fleeting Footsteps")),
    Skill(9, "Bladeblast", 0.0, 12.6, false, listOf("Bladeblast")), // no casting
    Skill(10, "Primal Strike", 0.0, 21.0, false, listOf("Primal Strike", "Primal Strike: Wave")),
    Skill(11, "Twin Shadow", 0.0, 14.7, false,listOf("Twin Shadow")) // no casting
  )
}
