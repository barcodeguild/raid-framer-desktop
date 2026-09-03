package com.reoky.raidframer.core.definitions

object SwiftbladeDefinition : SkillTreeDefinition {
  override val gameId = 12
  override val tree = SkillTreeType.SWIFTBLADE
  override val skills = listOf(
    Skill(0, "Blade Flurry", 0.0, 0.0, listOf("Blade Flurry"), possibleCastIDs = listOf()), // no casting
    Skill(1, "Crescent Slice", 0.0, 12.6, listOf("Crescent Slice"), possibleCastIDs = listOf()), // no casting
    Skill(2, "Sinister Strike", 0.0, 16.8, listOf("Sinister Strike"), possibleCastIDs = listOf(40339, 43213)),
    Skill(3, "Blink", 0.0, 16.8, listOf("Blick"), possibleCastIDs = listOf(41487, 45779)),
    Skill(4, "Relentless Assault", 0.0, 8.4, listOf("Relentless Assault"), possibleCastIDs = listOf()), // no casting
    Skill(5, "Reverberate", 0.0, 42.0, listOf("Reverberate"), possibleCastIDs = listOf()),
    Skill(6, "Entangle", 0.0, 16.8, listOf("Entangle"), possibleCastIDs = listOf(40342)),
    Skill(7, "Dusk Shroud", 0.0, 28.0, listOf("Dusk Shroud"), possibleCastIDs = listOf(41997)),
    Skill(8, "Fleeting Footsteps", 0.0, 38.5, listOf("Fleeting Footsteps"), possibleCastIDs = listOf(40341, 41764)),
    Skill(9, "Bladeblast", 0.0, 12.6, listOf("Bladeblast"), possibleCastIDs = listOf()), // no casting
    Skill(10, "Primal Strike", 0.0, 21.0, listOf("Primal Strike", "Primal Strike: Wave"), possibleCastIDs = listOf(40340, 41762, 49446, 49447, 51241)),
    Skill(11, "Twin Shadow", 0.0, 14.7, listOf("Twin Shadow"), possibleCastIDs = listOf()) // no casting
  )
}
